package dev.lu15.voicechat;

import dev.lu15.voicechat.event.PlayerJoinVoiceChatEvent;
import dev.lu15.voicechat.network.minecraft.Category;
import dev.lu15.voicechat.network.minecraft.VoiceState;
import dev.lu15.voicechat.event.PlayerHandshakeVoiceChatEvent;
import dev.lu15.voicechat.event.PlayerUpdateVoiceStateEvent;
import dev.lu15.voicechat.network.minecraft.MinecraftPacketHandler;
import dev.lu15.voicechat.network.minecraft.Packet;
import dev.lu15.voicechat.network.minecraft.packets.clientbound.CategoryAddedPacket;
import dev.lu15.voicechat.network.minecraft.packets.clientbound.CategoryRemovedPacket;
import dev.lu15.voicechat.network.minecraft.packets.clientbound.VoiceStatePacket;
import dev.lu15.voicechat.network.minecraft.packets.clientbound.HandshakeAcknowledgePacket;
import dev.lu15.voicechat.network.minecraft.packets.serverbound.HandshakePacket;
import dev.lu15.voicechat.network.minecraft.packets.serverbound.UpdateStatePacket;
import dev.lu15.voicechat.network.voice.VoicePacket;
import dev.lu15.voicechat.network.voice.VoiceServer;
import dev.lu15.voicechat.network.voice.encryption.SecretUtilities;
import java.net.InetAddress;
import java.net.UnknownHostException;
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

    @SuppressWarnings("PatternValidation")
    private VoiceChatImpl(@NotNull InetAddress address, int port, @NotNull EventNode<Event> eventNode, @NotNull String publicAddress) {
        this.port = port;
        this.publicAddress = publicAddress;

        // minestom doesn't allow removal of items from registries by default, so
        // we have to enable this feature to allow for the removal of categories
        System.setProperty("minestom.registry.unsafe-ops", "true");

        EventNode<Event> voiceServerEventNode = EventNode.all("voice-server");
        eventNode.addChild(voiceServerEventNode);
        this.server = new VoiceServer(this, address, port, voiceServerEventNode);

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
                    player.getUuid(), // why is this sent? the client already knows the player's uuid
                    Codec.VOIP, // todo: configurable
                    1024, // todo: configurable
                    48, // todo: configurable
                    1000, // todo: configurable
                    false, // todo: configurable
                    this.publicAddress,
                    false // todo: configurable
            )));
        });
    }

    private void handle(@NotNull Player player, @NotNull UpdateStatePacket packet) {
        // todo: set state when players disconnect from voice chat server - NOT when they disconnect from the minecraft server
        VoiceState state = new VoiceState(
                packet.disabled(),
                false,
                player.getUuid(),
                player.getUsername(),
                null
        );
        player.setTag(Tags.PLAYER_STATE, state);
        PacketSendingUtils.broadcastPlayPacket(this.packetHandler.write(new VoiceStatePacket(state)));

        EventDispatcher.call(new PlayerUpdateVoiceStateEvent(player, state));
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

            return new VoiceChatImpl(this.address, this.port, this.eventNode, this.publicAddress);
        }

    }

}
