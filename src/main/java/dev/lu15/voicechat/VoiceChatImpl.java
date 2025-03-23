package dev.lu15.voicechat;

import dev.lu15.voicechat.event.*;
import dev.lu15.voicechat.network.minecraft.*;
import dev.lu15.voicechat.network.minecraft.packets.clientbound.*;
import dev.lu15.voicechat.network.minecraft.packets.serverbound.*;
import dev.lu15.voicechat.network.voice.GroupManager;
import dev.lu15.voicechat.network.voice.VoicePacket;
import dev.lu15.voicechat.network.voice.VoiceServer;
import dev.lu15.voicechat.network.voice.encryption.SecretUtilities;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.UUID;
import java.util.Collection;
import java.util.Collections;


import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.utils.PacketSendingUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class VoiceChatImpl implements VoiceChat {

    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(VoiceChatImpl.class);

    private final @NotNull MinecraftPacketHandler packetHandler = new MinecraftPacketHandler();
    private final @NotNull DynamicRegistry<Category> categories = DynamicRegistry.create("voicechat:categories");

    private final @NotNull VoiceServer server;
    private final int port;
    private final @NotNull String publicAddress;
    private final int mtu;
    private final Codec codec;
    private final boolean groups;
    private final boolean recording;
    private final int distance;
    private final int keepalive;

    private final GroupManager groupManager = new GroupManager();


    @SuppressWarnings("PatternValidation")
    private VoiceChatImpl(@NotNull InetAddress address,
                          int port,
                          @NotNull String publicAddress,
                          @NotNull EventNode<Event> eventNode,
                          int mtu,
                          Codec codec,
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
                    case CreateGroupPacket p -> this.handle(player, p);
                    case LeaveGroupPacket p -> this.handle(player, p);
                    case JoinGroupPacket p -> this.handle(player, p);
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
                DynamicRegistry.Key<Category> key = this.categories.getKey(category);
                if (key == null) throw new IllegalStateException("category not found in registry");
                this.sendPacket(event.getPlayer(), new CategoryAddedPacket(key.namespace(), category));
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
            groupManager.getGroups().forEach((group) -> player.sendPacket(this.packetHandler.write(new GroupCreatedPacket(group))));
        });
    }

    private void handle(@NotNull Player player, @NotNull UpdateStatePacket packet) {
        // todo: set state when players disconnect from voice chat server - NOT when they disconnect from the minecraft server
        VoiceState oldState = player.getTag(Tags.PLAYER_STATE);
        UUID group = null;
        if(oldState != null && oldState.group() != null) {
            group = oldState.group();
        }
        VoiceState state = new VoiceState(
                packet.disabled(),
                false,
                player.getUuid(),
                player.getUsername(),
                group
        );
        player.setTag(Tags.PLAYER_STATE, state);
        PacketSendingUtils.broadcastPlayPacket(this.packetHandler.write(new VoiceStatePacket(state)));
        EventDispatcher.call(new PlayerUpdateVoiceStateEvent(player, state));
    }

    private void handle(@NotNull Player player, @NotNull CreateGroupPacket packet) {
        if(player.getTag(Tags.PLAYER_STATE).disabled()) return;
        boolean password = false;
        if(packet.name().length()>24) return;
        if(packet.password() != null) {
            if(packet.password().length() > 24) return;
            password = true;
        }
        Group group = new Group(UUID.randomUUID(), packet.name(), password, false, false, packet.type());
        groupManager.createGroup(group, packet.password());
        groupManager.setGroup(player, group);
        VoiceState state = new VoiceState(
                false,
                false,
                player.getUuid(),
                player.getUsername(),
                group.id()
        );
        player.setTag(Tags.PLAYER_STATE, state);
        PacketSendingUtils.broadcastPlayPacket(this.packetHandler.write(new VoiceStatePacket(state)));
        PacketSendingUtils.broadcastPlayPacket(this.packetHandler.write(new GroupCreatedPacket(group)));
        player.sendPacket(this.packetHandler.write(new GroupChangedPacket(group.id(), false)));
        EventDispatcher.call(new PlayerCreateGroupEvent(player, group));
    }

    private void handle(@NotNull Player player, @NotNull LeaveGroupPacket packet) {
        if(player.getTag(Tags.PLAYER_STATE).disabled()) return;
        VoiceState oldState = player.getTag(Tags.PLAYER_STATE);
        UUID group = null;
        if(oldState != null && oldState.group() != null) {
            group = oldState.group();
        }
        if(group == null) return;
        VoiceState state = new VoiceState(
                false,
                false,
                player.getUuid(),
                player.getUsername(),
                null
        );
        player.setTag(Tags.PLAYER_STATE, state);
        PacketSendingUtils.broadcastPlayPacket(this.packetHandler.write(new VoiceStatePacket(state)));
        if(groupManager.getPlayers(group).size() > 1) {
            // more players
            groupManager.leaveGroup(player);
            player.sendPacket(this.packetHandler.write(new GroupChangedPacket(null, false)));
        } else {
            // only player
            groupManager.leaveGroup(player);
            groupManager.removeGroup(group);
            player.sendPacket(this.packetHandler.write(new GroupChangedPacket(null, false)));
            PacketSendingUtils.broadcastPlayPacket(this.packetHandler.write(new GroupRemovedPacket(group)));
        }
        EventDispatcher.call(new PlayerLeaveGroupEvent(player, groupManager.getGroup(group)));
    }

    private void handle(@NotNull Player player, @NotNull JoinGroupPacket packet) {
        if(player.getTag(Tags.PLAYER_STATE).disabled()) return;
        if(!groupManager.hasGroup(packet.group())) return;
        String password = groupManager.getPassword(packet.group());
        if(password != null && !Objects.equals(password, packet.password())) {
            player.sendPacket(this.packetHandler.write(new GroupChangedPacket(packet.group(), true)));
            return;
        }
        VoiceState state = new VoiceState(
                false,
                false,
                player.getUuid(),
                player.getUsername(),
                packet.group()
        );
        groupManager.setGroup(player, packet.group());
        player.setTag(Tags.PLAYER_STATE, state);
        PacketSendingUtils.broadcastPlayPacket(this.packetHandler.write(new VoiceStatePacket(state)));
        player.sendPacket(this.packetHandler.write(new GroupChangedPacket(packet.group(), false)));
        EventDispatcher.call(new PlayerJoinGroupEvent(player, groupManager.getGroup(packet.group())));
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
    public DynamicRegistry.@NotNull Key<Category> addCategory(@NotNull NamespaceID id, @NotNull Category category) {
        Category existing = this.categories.get(id);
        DynamicRegistry.Key<Category> key = this.categories.register(id, category);

        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (!player.hasTag(Tags.VOICE_CLIENT)) continue; // only send to voice chat clients

            // remove the existing category if it exists, then add the new one
            if (existing != null) this.sendPacket(player, new CategoryRemovedPacket(id));
            this.sendPacket(player, new CategoryAddedPacket(id, category));
        }

        return key;
    }

    @Override
    public boolean removeCategory(@NotNull DynamicRegistry.Key<Category> category) {
        boolean removed = this.categories.remove(category.namespace());
        if (!removed) return false;

        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (!player.hasTag(Tags.VOICE_CLIENT)) continue; // only send to voice chat clients
            this.sendPacket(player, new CategoryRemovedPacket(category.namespace()));
        }

        return true;
    }

    final static class BuilderImpl implements Builder {

        private final @NotNull InetAddress address;
        private final int port;
        private int mtu = 1024;
        private Codec codec = Codec.VOIP;
        private int distance = 48;
        private boolean groups = false;
        private int keepalive = 1000;
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

            return new VoiceChatImpl(this.address, this.port, this.publicAddress, this.eventNode, this.mtu, this.codec, this.groups, this.distance, this.keepalive, this.recording);
        }

        @Override
        public @NotNull Builder mtu(Integer mtu) {
            this.mtu = mtu;
            return this;
        }

        @Override
        public @NotNull Builder codec(Codec codec) {
            this.codec = codec;
            return this;
        }

        @Override
        public @NotNull Builder distance(Integer distance) {
            this.distance = distance;
            return this;
        }

        @Override
        public @NotNull Builder groups(Boolean enabled) {
            this.groups = enabled;
            return this;
        }

        @Override
        public @NotNull Builder keepalive(Integer keepalive) {
            this.keepalive = keepalive;
            return this;
        }

        @Override
        public @NotNull Builder recording(Boolean enabled) {
            this.recording = enabled;
            return this;
        }

    }

}
