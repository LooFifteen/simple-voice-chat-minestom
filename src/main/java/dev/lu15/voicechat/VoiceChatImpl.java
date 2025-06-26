package dev.lu15.voicechat;

import dev.lu15.voicechat.event.*;
import dev.lu15.voicechat.group.Group;
import dev.lu15.voicechat.network.minecraft.*;
import dev.lu15.voicechat.network.minecraft.packets.clientbound.*;
import dev.lu15.voicechat.network.minecraft.packets.serverbound.*;
import dev.lu15.voicechat.group.GroupManager;
import dev.lu15.voicechat.network.voice.VoicePacket;
import dev.lu15.voicechat.network.voice.VoiceServer;
import dev.lu15.voicechat.network.voice.encryption.SecretUtilities;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;


import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.utils.PacketSendingUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class VoiceChatImpl implements VoiceChat {

    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(VoiceChatImpl.class);

    private final @NotNull MinecraftPacketHandler packetHandler = new MinecraftPacketHandler();
    private final @NotNull DynamicRegistry<Category> categories = DynamicRegistry.create(Key.key("voicechat:categories"));

    private final @NotNull VoiceServer server;
    private final int port;
    private final @NotNull String publicAddress;
    private final int mtu;
    private final @NotNull Codec codec;
    private final boolean groups;
    private final boolean recording;
    private final int distance;
    private final int keepalive;
    private final @NotNull GroupManager groupManager;


    @SuppressWarnings("PatternValidation")
    private VoiceChatImpl(@NotNull InetAddress address,
                          int port,
                          @NotNull String publicAddress,
                          @NotNull EventNode<Event> eventNode,
                          int mtu,
                          @NotNull Codec codec,
                          boolean groups,
                          int distance,
                          int keepalive,
                          boolean recording) {
        this.port = port;
        this.publicAddress = publicAddress;
        this.mtu = mtu;
        this.codec = codec;
        this.groups = groups;
        this.distance = distance;
        this.keepalive = keepalive;
        this.recording = recording;
        this.groupManager = new GroupManager(this.packetHandler);

        // minestom doesn't allow removal of items from registries by default, so
        // we have to enable this feature to allow for the removal of categories
        System.setProperty("minestom.registry.unsafe-ops", "true");

        EventNode<Event> voiceServerEventNode = EventNode.all("voice-server");
        eventNode.addChild(voiceServerEventNode);
        this.server = new VoiceServer(this, address, voiceServerEventNode, groupManager, port, distance);

        this.server.start();
        LOGGER.info("voice server started on {}:{}", address, port);

        eventNode.addListener(PlayerPluginMessageEvent.class, event -> {
            String channel = event.getIdentifier();
            if (!Key.parseable(channel)) return;
            Key identifier = Key.key(channel);

            if (!identifier.namespace().equals("voicechat")) return;

            try {
                Packet<?> packet = this.packetHandler.read(channel, event.getMessage());
                final Player player = event.getPlayer();
                switch (packet) {
                    case HandshakePacket p -> this.handle(player, p);
                    case UpdateStatePacket p -> this.handle(player, p);
                    case CreateGroupPacket p -> {
                        if (groups) this.handle(player, p);
                    }
                    case LeaveGroupPacket p -> {
                        if (groups) this.handle(player, p);
                    }
                    case JoinGroupPacket p -> {
                        if (groups) this.handle(player, p);
                    }
                    case null -> LOGGER.warn("received unknown packet from {}: {}", player.getUsername(), channel);
                    default -> throw new UnsupportedOperationException("unimplemented packet: " + packet);
                }
            } catch (Exception e) {
                // we ignore this exception because it's most
                // likely to be caused by the client sending
                // an invalid packet.
                LOGGER.debug("failed to read plugin message", e);
            }
        });

        // send existing categories to newly joining players
        eventNode.addListener(PlayerJoinVoiceChatEvent.class, event -> {
            for (Category category : this.categories.values()) {
                RegistryKey<Category> key = this.categories.getKey(category);
                if (key == null) throw new IllegalStateException("category not found in registry");
                this.sendPacket(event.getPlayer(), new CategoryAddedPacket(key.key().namespace(), category));
            }
        });
    }

    private void handle(@NotNull Player player, @NotNull HandshakePacket packet) {
        if (packet.version() != 18) {
            LOGGER.warn("player {} using wrong version: {}", player.getUsername(), packet.version());
            return;
        }

        if (SecretUtilities.hasSecret(player)) {
            LOGGER.warn("player {} already has a secret", player.getUsername());
            return;
        }

        PlayerHandshakeVoiceChatEvent event = new PlayerHandshakeVoiceChatEvent(player, SecretUtilities.generateSecret());

        EventDispatcher.callCancellable(event, () -> {
            SecretUtilities.setSecret(player, event.getSecret());

            player.sendPacket(this.packetHandler.write(new HandshakeAcknowledgePacket(
                    event.getSecret(),
                    this.port,
                    player.getUuid(),
                    codec,
                    mtu,
                    distance,
                    keepalive,
                    groups,
                    this.publicAddress,
                    recording // todo: configurable (recording)
            )));

            // send all non-hidden groups to the player
            if (!this.groups) return;
            this.groupManager.getGroups().values().stream()
                    .filter(group -> !group.isHidden())
                    .map(group -> this.packetHandler.write(new GroupCreatedPacket(group)))
                    .forEach(player::sendPacket);
        });
    }

    private void handle(@NotNull Player player, @NotNull UpdateStatePacket packet) {
        // todo: set state when players disconnect from voice chat server - NOT when they disconnect from the minecraft server
        VoiceState state = player.getTag(Tags.PLAYER_STATE);
        if (state == null) state = new VoiceState(
            packet.disabled(),
            false,
            player.getUuid(),
            player.getUsername(),
            null
        ); else state = state.withDisabled(packet.disabled());

        player.setTag(Tags.PLAYER_STATE, state);
        PacketSendingUtils.broadcastPlayPacket(this.packetHandler.write(new VoiceStatePacket(state)));
        EventDispatcher.call(new PlayerUpdateVoiceStateEvent(player, state));
    }

    private void handle(@NotNull Player player, @NotNull CreateGroupPacket packet) {
        VoiceState playerVoiceState = player.getTag(Tags.PLAYER_STATE);
        if (playerVoiceState == null || playerVoiceState.disabled()) return;

        String name = packet.name();
        String password = packet.password();

        if (name.length() > 24) return;
        if (name.isBlank()) return;
        if (password != null && password.isBlank()) return;

        // Player-created groups are not persistent and not hidden by default
        Group prospectiveGroup = Group.builder()
                .name(name)
                .type(packet.type())
                .password(password)
                .build();

        PlayerCreateGroupEvent event = new PlayerCreateGroupEvent(player, prospectiveGroup);
        EventDispatcher.callCancellable(event, () -> {
            this.groupManager.register(prospectiveGroup);
            // Now set the player's group using the managed method
            // This will handle moving them from any old group and all necessary packet sending
            this.setGroup(player, prospectiveGroup);
        });
    }

    private void handle(@NotNull Player player, @NotNull LeaveGroupPacket packet) {
        VoiceState playerVoiceState = player.getTag(Tags.PLAYER_STATE);
        if (playerVoiceState == null || playerVoiceState.disabled()) return;

        UUID currentGroupId = playerVoiceState.group();
        if (currentGroupId == null) return;

        Group currentGroupObject = groupManager.getGroup(currentGroupId).orElse(null);
        if (currentGroupObject == null) {
            // Should not happen if state is consistent, but handle defensively
            this.groupManager.setGroup(player, null); // Force remove from (ghost) group
            return;
        }

        PlayerLeaveGroupEvent event = new PlayerLeaveGroupEvent(player, currentGroupObject);
        EventDispatcher.callCancellable(event, () -> {
            // Setting group to null will trigger leaving logic, including auto-removal of old group if needed
            this.setGroup(player, null);
        });
    }

    private void handle(@NotNull Player player, @NotNull JoinGroupPacket packet) {
        VoiceState playerVoiceState = player.getTag(Tags.PLAYER_STATE);
        if (playerVoiceState == null || playerVoiceState.disabled()) return;

        if (!groupManager.hasGroup(packet.group())) return;

        Group targetGroup = groupManager.getGroup(packet.group()).orElse(null);
        if (targetGroup == null) return;

        // todo: validate password before event is called
        PlayerJoinGroupEvent event = new PlayerJoinGroupEvent(player, targetGroup);
        EventDispatcher.callCancellable(event, () -> this.groupManager.joinGroup(player, targetGroup, packet.password()));
    }

    @Override
    public <T extends Packet<T>> void sendPacket(@NotNull Player player, @NotNull T packet) {
        player.sendPacket(this.packetHandler.write(packet));
    }

    @Override
    public <T extends VoicePacket<T>> void sendPacket(@NotNull Player player, @NotNull T packet) {
        this.server.write(player, packet);
    }

    @Override
    public @NotNull @Unmodifiable Collection<Category> getCategories() {
        return Collections.unmodifiableCollection(this.categories.values());
    }

    @Override
    public @NotNull RegistryKey<Category> addCategory(@NotNull Key id, @NotNull Category category) {
        Category existing = this.categories.get(id);
        RegistryKey<Category> key = this.categories.register(id, category);

        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (!player.hasTag(Tags.VOICE_CLIENT)) continue;

            if (existing != null) this.sendPacket(player, new CategoryRemovedPacket(id));
            this.sendPacket(player, new CategoryAddedPacket(id, category));
        }

        return key;
    }

    @Override
    public boolean removeCategory(@NotNull RegistryKey<Category> category) {
        boolean removed = this.categories.remove(category.key());
        if (!removed) return false;

        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (!player.hasTag(Tags.VOICE_CLIENT)) continue;
            this.sendPacket(player, new CategoryRemovedPacket(category.key()));
        }

        return true;
    }

    @Override
    public boolean groupsEnabled() {
        return this.groups;
    }

    @Override
    public @NotNull @Unmodifiable Collection<Group> getGroups() {
        if (!this.groups) throw new IllegalStateException("groups are not enabled on this server");
        return this.groupManager.getGroups().values();
    }

    @Override
    public @NotNull Optional<Group> getGroup(@NotNull UUID groupId) {
        return this.groupManager.getGroup(groupId);
    }

    @Override
    public void registerGroup(@NotNull Group group) {
        if (!this.groups) throw new IllegalStateException("groups are not enabled on this server");
        this.groupManager.register(group);
    }

    @Override
    public void unregisterGroup(@NotNull Group group) {
        if (!this.groups) throw new IllegalStateException("groups are not enabled on this server");
        this.groupManager.unregister(group);
    }

    @Override
    public void setGroup(@NotNull Player player, @Nullable Group group) {
        if (!this.groups) throw new IllegalStateException("groups are not enabled on this server");
        this.groupManager.setGroup(player, group);
    }

    final static class BuilderImpl implements Builder {

        private final @NotNull InetAddress address;
        private final int port;
        private int mtu = 1024;
        private @NotNull Codec codec = Codec.VOIP;
        private int distance = 48;
        private boolean groups = false;
        private int keepAlive = 1000;
        private boolean recording = false;

        private @NotNull String publicAddress = ""; // this causes the client to attempt to connect to the same ip as the minecraft server

        private @Nullable EventNode<Event> eventNode;

        BuilderImpl(@NotNull String address, int port) {
            try {
                this.address = InetAddress.getByName(address);
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException("invalid address", e);
            }
            this.port = port;
        }

        @Override
        public @NotNull Builder eventNode(@NotNull EventNode<Event> eventNode) {
            this.eventNode = eventNode;
            return this;
        }

        @Override
        public @NotNull Builder publicAddress(@NotNull String publicAddress) {
            this.publicAddress = publicAddress;
            return this;
        }

        @Override
        public @NotNull VoiceChat enable() {
            // if the user did not provide an event node, create and register one
            if (this.eventNode == null) {
                this.eventNode = EventNode.all("voice-chat");
                MinecraftServer.getGlobalEventHandler().addChild(this.eventNode);
            }

            return new VoiceChatImpl(this.address, this.port, this.publicAddress, this.eventNode, this.mtu, this.codec, this.groups, this.distance, this.keepAlive, this.recording);
        }

        @Override
        public @NotNull Builder mtu(int mtu) {
            this.mtu = mtu;
            return this;
        }

        @Override
        public @NotNull Builder codec(Codec codec) {
            this.codec = codec;
            return this;
        }

        @Override
        public @NotNull Builder distance(int distance) {
            this.distance = distance;
            return this;
        }

        @Override
        public @NotNull Builder groups() {
            this.groups = true;
            return this;
        }

        @Override
        public @NotNull Builder keepAlive(int keepAlive) {
            this.keepAlive = keepAlive;
            return this;
        }

        @Override
        public @NotNull Builder recording() {
            this.recording = true;
            return this;
        }
    }
}