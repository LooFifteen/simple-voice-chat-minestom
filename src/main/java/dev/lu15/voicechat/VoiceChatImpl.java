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

    private final @NotNull GroupManager groupManager = new GroupManager();


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
            groupManager.getGroups().forEach((group) -> player.sendPacket(this.packetHandler.write(new GroupCreatedPacket(group))));
        });
    }

    private void handle(@NotNull Player player, @NotNull UpdateStatePacket packet) {
        // todo: set state when players disconnect from voice chat server - NOT when they disconnect from the minecraft server
        VoiceState oldState = player.getTag(Tags.PLAYER_STATE);
        UUID group = oldState == null ? null : oldState.group();
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
        VoiceState playerVoiceState = player.getTag(Tags.PLAYER_STATE);
        if (playerVoiceState == null || playerVoiceState.disabled()) return;

        if (packet.name().length() > 24) return;
        if (packet.password() != null && packet.password().length() > 24) return;

        // Player-created groups are not persistent and not hidden by default
        Group prospectiveGroup = new Group(UUID.randomUUID(), packet.name(), packet.password() != null, false, false, packet.type());

        PlayerCreateGroupEvent event = new PlayerCreateGroupEvent(player, prospectiveGroup);
        EventDispatcher.callCancellable(event, () -> {
            groupManager.createGroup(prospectiveGroup, packet.password());
            PacketSendingUtils.broadcastPlayPacket(this.packetHandler.write(new GroupCreatedPacket(prospectiveGroup)));
            // Now set the player's group using the managed method
            // This will handle moving them from any old group and all necessary packet sending
            setPlayerManagedGroup(player, prospectiveGroup.id(), packet.password());
        });
    }

    private void handle(@NotNull Player player, @NotNull LeaveGroupPacket packet) {
        VoiceState playerVoiceState = player.getTag(Tags.PLAYER_STATE);
        if (playerVoiceState == null || playerVoiceState.disabled()) return;

        UUID currentGroupId = playerVoiceState.group();
        if (currentGroupId == null) return;

        Group currentGroupObject = groupManager.getGroup(currentGroupId);
        if (currentGroupObject == null) {
            // Should not happen if state is consistent, but handle defensively
            setPlayerManagedGroup(player, null, null); // Force remove from (ghost) group
            return;
        }

        PlayerLeaveGroupEvent event = new PlayerLeaveGroupEvent(player, currentGroupObject);
        EventDispatcher.callCancellable(event, () -> {
            // Setting group to null will trigger leaving logic, including auto-removal of old group if needed
            setPlayerManagedGroup(player, null, null);
        });
    }

    private void handle(@NotNull Player player, @NotNull JoinGroupPacket packet) {
        VoiceState playerVoiceState = player.getTag(Tags.PLAYER_STATE);
        if (playerVoiceState == null || playerVoiceState.disabled()) return;

        if (!groupManager.hasGroup(packet.group())) return;

        Group targetGroup = groupManager.getGroup(packet.group());
        if (targetGroup == null) return; // Should be caught by hasGroup, but defensive

        // Check password before firing event
        if (targetGroup.passwordProtected()) { // Assumes Group record has a boolean 'password' field, accessor is .password()
            String actualPassword = groupManager.getPassword(packet.group());
            if (!Objects.equals(actualPassword, packet.password())) {
                player.sendPacket(this.packetHandler.write(new GroupChangedPacket(packet.group(), true)));
                return;
            }
        }

        PlayerJoinGroupEvent event = new PlayerJoinGroupEvent(player, targetGroup);
        EventDispatcher.callCancellable(event, () -> setPlayerManagedGroup(player, packet.group(), packet.password()));
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

    private void checkAndAutoRemoveGroup(@Nullable UUID groupIdToCheck) {
        if (!this.groups) return;
        if (groupIdToCheck == null) return;

        Group groupObject = groupManager.getGroup(groupIdToCheck);
        if (groupObject != null && !groupObject.persistent()) {
            List<Player> playersInGroup = groupManager.getPlayers(groupIdToCheck);
            if (playersInGroup == null || playersInGroup.isEmpty()) {
                if (groupManager.hasGroup(groupIdToCheck)) {
                    groupManager.removeGroup(groupIdToCheck);
                    PacketSendingUtils.broadcastPlayPacket(this.packetHandler.write(new GroupRemovedPacket(groupIdToCheck)));
                    LOGGER.info("Group '{}' ({}) auto-removed as it became empty and was not persistent.", groupObject.name(), groupIdToCheck);
                }
            }
        }
    }

    @Override
    public @NotNull @Unmodifiable Collection<Group> getManagedGroups() {
        if (!this.groups) { // Respect group feature flag
            return Collections.emptyList();
        }
        return this.groupManager.getGroups(); // groupManager.getGroups() already returns Collection<Group>
    }

    @Override
    public @Nullable Group createManagedGroup(@NotNull String name, @NotNull Group.Type type, @Nullable String password, boolean persistent, boolean hidden) {
        if (!this.groups) {
            LOGGER.warn("Attempted to create a group via API, but groups are disabled.");
            return null;
        }
        if (name.length() > 24) {
            LOGGER.warn("API: Group name '{}' too long (max 24 chars).", name);
            return null;
        }
        if (password != null && password.length() > 24) {
            LOGGER.warn("API: Group password too long (max 24 chars).");
            return null;
        }

        Group group = new Group(UUID.randomUUID(), name, password != null, persistent, hidden, type);
        groupManager.createGroup(group, password);
        PacketSendingUtils.broadcastPlayPacket(this.packetHandler.write(new GroupCreatedPacket(group)));
        LOGGER.info("Group '{}' ({}) created via API.", group.name(), group.id());
        return group;
    }

    @Override
    public boolean removeManagedGroup(@NotNull UUID groupId) {
        if (!this.groups) {
            LOGGER.warn("Attempted to remove a group via API, but groups are disabled.");
            return false;
        }
        Group group = groupManager.getGroup(groupId);
        if (group == null) {
            LOGGER.warn("API: Attempted to remove non-existent group: {}", groupId);
            return false;
        }

        List<Player> playersInGroupNullable = groupManager.getPlayers(groupId);
        List<Player> playersInGroupCopy = (playersInGroupNullable == null) ?
                Collections.emptyList() : new ArrayList<>(playersInGroupNullable);

        for (Player player : playersInGroupCopy) {
            VoiceState oldState = player.getTag(Tags.PLAYER_STATE);
            if (oldState != null && groupId.equals(oldState.group())) {
                VoiceState newState = new VoiceState(
                        oldState.disabled(),
                        false,
                        player.getUuid(),
                        player.getUsername(),
                        null
                );
                player.setTag(Tags.PLAYER_STATE, newState);
                PacketSendingUtils.broadcastPlayPacket(this.packetHandler.write(new VoiceStatePacket(newState)));
                this.sendPacket(player, new GroupChangedPacket(null, false));
            }
        }

        groupManager.removeGroup(groupId);
        PacketSendingUtils.broadcastPlayPacket(this.packetHandler.write(new GroupRemovedPacket(groupId)));
        LOGGER.info("Group '{}' ({}) removed via API.", group.name(), group.id());
        return true;
    }

    @Override
    public boolean setPlayerManagedGroup(@NotNull Player player, @Nullable UUID newGroupId, @Nullable String passwordForNewGroup) {
        if (!this.groups) {
            LOGGER.warn("Player {} group set attempt failed: groups disabled.", player.getUsername());
            return false;
        }

        VoiceState currentVoiceState = player.getTag(Tags.PLAYER_STATE);
        if (currentVoiceState == null) {
            LOGGER.warn("Player {} group set attempt failed: no voice state.", player.getUsername());
            return false;
        }
        if (currentVoiceState.disabled()) {
            LOGGER.warn("Player {} group set attempt failed: voice chat disabled.", player.getUsername());
            return false;
        }

        UUID oldGroupId = currentVoiceState.group();

        if (newGroupId == null) {
            if (oldGroupId == null) return true;

            Group oldGroupObject = groupManager.getGroup(oldGroupId);

            groupManager.leaveGroup(player);
            VoiceState updatedState = new VoiceState(false, false, player.getUuid(), player.getUsername(), null);
            player.setTag(Tags.PLAYER_STATE, updatedState);
            PacketSendingUtils.broadcastPlayPacket(this.packetHandler.write(new VoiceStatePacket(updatedState)));
            this.sendPacket(player, new GroupChangedPacket(null, false));
            checkAndAutoRemoveGroup(oldGroupId);
            if (oldGroupObject != null) {
                LOGGER.info("Player {} left group '{}' ({}) via managed set.", player.getUsername(), oldGroupObject.name(), oldGroupId);
            } else {
                LOGGER.info("Player {} left (ghost) group {} via managed set.", player.getUsername(), oldGroupId);
            }
            return true;
        }

        if (Objects.equals(oldGroupId, newGroupId)) return true;

        Group targetGroup = groupManager.getGroup(newGroupId);
        if (targetGroup == null) {
            LOGGER.warn("Player {} failed to join group {}: group not found.", player.getUsername(), newGroupId);
            return false;
        }

        if (targetGroup.passwordProtected()) {
            String actualPassword = groupManager.getPassword(newGroupId);
            if (!Objects.equals(actualPassword, passwordForNewGroup)) {
                LOGGER.warn("Player {} failed password for group '{}' ({}).", player.getUsername(), targetGroup.name(), newGroupId);
                this.sendPacket(player, new GroupChangedPacket(newGroupId, true));
                return false;
            }
        }

        if (oldGroupId != null) {
            groupManager.leaveGroup(player);
            Group oldGrpObjForLogging = groupManager.getGroup(oldGroupId);
            if (oldGrpObjForLogging != null) {
                LOGGER.info("Player {} left old group '{}' ({}) to join new one.", player.getUsername(), oldGrpObjForLogging.name(), oldGroupId);
            } else {
                LOGGER.info("Player {} left old (ghost) group {} to join new one.", player.getUsername(), oldGroupId);
            }
        }

        groupManager.setGroup(player, newGroupId);
        VoiceState newState = new VoiceState(false, false, player.getUuid(), player.getUsername(), newGroupId);
        player.setTag(Tags.PLAYER_STATE, newState);
        PacketSendingUtils.broadcastPlayPacket(this.packetHandler.write(new VoiceStatePacket(newState)));
        this.sendPacket(player, new GroupChangedPacket(newGroupId, false));
        LOGGER.info("Player {} joined group '{}' ({}) via managed set.", player.getUsername(), targetGroup.name(), newGroupId);

        if (oldGroupId != null) {
            checkAndAutoRemoveGroup(oldGroupId);
        }
        return true;
    }

    final static class BuilderImpl implements Builder {

        private final @NotNull InetAddress address;
        private final int port;
        private int mtu = 1024;
        private @NotNull Codec codec = Codec.VOIP;
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
        public @NotNull Builder groups(boolean enabled) {
            this.groups = enabled;
            return this;
        }

        @Override
        public @NotNull Builder keepalive(int keepalive) {
            this.keepalive = keepalive;
            return this;
        }

        @Override
        public @NotNull Builder recording(boolean enabled) {
            this.recording = enabled;
            return this;
        }
    }
}