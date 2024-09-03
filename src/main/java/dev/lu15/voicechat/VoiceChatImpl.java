package dev.lu15.voicechat;

import dev.lu15.voicechat.event.PlayerUpdateVoiceStateEvent;
import dev.lu15.voicechat.network.minecraft.MinecraftPacketHandler;
import dev.lu15.voicechat.network.minecraft.Packet;
import dev.lu15.voicechat.network.minecraft.packets.PlayerStatePacket;
import dev.lu15.voicechat.network.minecraft.packets.SecretPacket;
import dev.lu15.voicechat.network.minecraft.packets.SecretRequestPacket;
import dev.lu15.voicechat.network.minecraft.packets.UpdateStatePacket;
import dev.lu15.voicechat.network.voice.SecretHolder;
import dev.lu15.voicechat.network.voice.VoiceServer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.utils.PacketUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class VoiceChatImpl implements VoiceChat, SecretHolder {

    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(VoiceChatImpl.class);

    private final @NotNull MinecraftPacketHandler packetHandler = new MinecraftPacketHandler();
    private final @NotNull Map<UUID, UUID> secrets = new HashMap<>();

    private final @NotNull VoiceServer server;
    private final int port;
    private final @NotNull EventNode<Event> eventNode;
    private final @NotNull String publicAddress;

    private VoiceChatImpl(@NotNull InetAddress address, int port, @NotNull EventNode<Event> eventNode, @NotNull String publicAddress) {
        this.port = port;
        this.eventNode = eventNode;
        this.publicAddress = publicAddress;
        this.server = new VoiceServer(this, address, port);

        this.server.start();
        LOGGER.info("voice server started on {}:{}", address, port);

        this.eventNode.addListener(PlayerPluginMessageEvent.class, event -> {
            String channel = event.getIdentifier();
            if (!Key.parseable(channel)) return;

            //noinspection PatternValidation
            Key identifier = Key.key(channel);

            if (!identifier.namespace().equals("voicechat")) return;

            try {
                Packet packet = this.packetHandler.read(channel, event.getMessage());
                switch (packet) {
                    case SecretRequestPacket p -> this.handle(event.getPlayer(), p);
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

    private void handle(@NotNull Player player, @NotNull SecretRequestPacket packet) {
        LOGGER.debug("received secret request packet from {}", player.getUsername());

        if (packet.version() != 18) {
            LOGGER.warn("player {} using outdated version: {}", player.getUsername(), packet.version());
            return;
        }

        UUID secret = this.generateSecret(player.getUuid());
        if (secret == null) return;

        player.sendPacket(this.packetHandler.write(new SecretPacket(
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
        PacketUtils.broadcastPlayPacket(this.packetHandler.write(new PlayerStatePacket(state)));

        EventDispatcher.call(new PlayerUpdateVoiceStateEvent(player, state));
    }

    private @Nullable UUID generateSecret(@NotNull UUID player) {
        if (this.secrets.containsKey(player)) return null;

        Random random = new SecureRandom(); // todo: wtf?
        UUID secret = new UUID(random.nextLong(), random.nextLong());
        this.secrets.put(player, secret);
        return secret;
    }

    @Override
    public @Nullable UUID getSecret(@NotNull UUID player) {
        return this.secrets.get(player);
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
