package dev.lu15.voicechat;

import dev.lu15.voicechat.event.PlayerUpdateVoiceStateEvent;
import dev.lu15.voicechat.network.minecraft.MinecraftPacketHandler;
import dev.lu15.voicechat.network.minecraft.Packet;
import dev.lu15.voicechat.network.minecraft.packets.VoiceStatePacket;
import dev.lu15.voicechat.network.minecraft.packets.HandshakeAcknowledgePacket;
import dev.lu15.voicechat.network.minecraft.packets.HandshakePacket;
import dev.lu15.voicechat.network.minecraft.packets.UpdateStatePacket;
import dev.lu15.voicechat.network.voice.VoicePacket;
import dev.lu15.voicechat.network.voice.VoiceServer;
import dev.lu15.voicechat.network.voice.encryption.SecretUtilities;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class VoiceChatImpl implements VoiceChat {

    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(VoiceChatImpl.class);

    private final @NotNull MinecraftPacketHandler packetHandler = new MinecraftPacketHandler();

    private final @NotNull VoiceServer server;
    private final int port;
    private final @NotNull String publicAddress;

    @SuppressWarnings("PatternValidation")
    private VoiceChatImpl(@NotNull InetAddress address, int port, @NotNull EventNode<Event> eventNode, @NotNull String publicAddress) {
        this.port = port;
        this.publicAddress = publicAddress;

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
                switch (packet) {
                    case HandshakePacket p -> this.handle(event.getPlayer(), p);
                    case UpdateStatePacket p -> this.handle(event.getPlayer(), p);
                    default -> throw new IllegalStateException("unexpected packet: " + packet);
                }
            } catch (Exception e) {
                // we ignore this exception because it's most
                // likely to be caused by the client sending
                // an invalid packet.
                LOGGER.debug("failed to read plugin message", e);
            }
        });
    }

    private void handle(@NotNull Player player, @NotNull HandshakePacket packet) {
        if (packet.version() != 18) {
            LOGGER.warn("player {} using wrong version: {}", player.getUsername(), packet.version());
            return;
        }

        UUID secret = SecretUtilities.generateSecret(player);
        if (secret == null) {
            LOGGER.warn("player {} already has a secret", player.getUsername());
            return;
        }

        player.sendPacket(this.packetHandler.write(new HandshakeAcknowledgePacket(
                secret,
                this.port,
                player.getUuid(),
                Codec.VOIP, // todo: configurable
                1024, // todo: configurable
                48, // todo: configurable
                1000, // todo: configurable
                false, // todo: configurable
                this.publicAddress,
                false // todo: configurable
        )));
    }

    private void handle(@NotNull Player player, @NotNull UpdateStatePacket packet) {
        // todo: set state when players disconnect from voice chat server - NOT when they disconnect from the minecraft server
        VoiceState state = new VoiceState(
                player.getUuid(),
                player.getUsername(),
                packet.disabled(),
                false,
                null
        );
        player.setTag(Tags.PLAYER_STATE, state);
        this.packetHandler.write(new VoiceStatePacket(state));

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
