package dev.lu15.voicechat.network.voice;

import dev.lu15.voicechat.SoundSources;
import dev.lu15.voicechat.Tags;
import dev.lu15.voicechat.api.SoundSelector;
import dev.lu15.voicechat.event.PlayerJoinVoiceChatEvent;
import dev.lu15.voicechat.event.PlayerMicrophoneEvent;
import dev.lu15.voicechat.network.voice.packets.AuthenticatePacket;
import dev.lu15.voicechat.network.voice.packets.AuthenticationAcknowledgedPacket;
import dev.lu15.voicechat.network.voice.packets.KeepAlivePacket;
import dev.lu15.voicechat.network.voice.packets.MicrophonePacket;
import dev.lu15.voicechat.network.voice.packets.PingPacket;
import dev.lu15.voicechat.network.voice.packets.PlayerSoundPacket;
import dev.lu15.voicechat.network.voice.packets.YeaImHerePacket;
import dev.lu15.voicechat.network.voice.packets.YouHereBroPacket;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.advancements.FrameType;
import net.minestom.server.advancements.notifications.Notification;
import net.minestom.server.advancements.notifications.NotificationCenter;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.instance.EntityTracker;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VoiceServer {

    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(VoiceServer.class);

    private final @NotNull VoiceSocket socket = new VoiceSocket();
    private final @NotNull VoicePacketHandler packetHandler = new VoicePacketHandler();
    private final @NotNull LinkedBlockingQueue<RawPacket> packetQueue = new LinkedBlockingQueue<>();
    private final @NotNull Map<SocketAddress, Player> connections = new HashMap<>();

    private final @NotNull SecretHolder secretHolder;
    private final @NotNull InetAddress address;
    private final int port;

    private boolean running;
    private long lastKeepAlive;

    public VoiceServer(@NotNull SecretHolder secretHolder, @NotNull InetAddress address, int port) {
        this.secretHolder = secretHolder;
        this.address = address;
        this.port = port;
    }

    public void start() {
        this.running = true;
        Thread.ofVirtual().name("voice-server-entrypoint").start(this::entrypoint);
        Thread.ofVirtual().name("voice-processor").start(this::processor);
    }

    public void stop() {
        this.running = false;
    }

    private void entrypoint() {
        try {
            this.socket.open(this.address, this.port);

            while (!this.socket.closed() || !this.running) {
                try {
                    RawPacket packet = this.socket.read();
                    this.packetQueue.put(packet);
                } catch (IOException e) {
                    // we ignore this exception because it's most
                    // likely to be caused by the client sending
                    // an invalid packet.
                    LOGGER.debug("failed to read raw packet", e);
                } catch (InterruptedException e) {
                    // wait interrupted, ignore
                    LOGGER.debug("interrupted while waiting for packet queue", e);
                }
            }

            LOGGER.debug("voice server closed");
        } catch (SocketException e) {
            LOGGER.error("failed to open voice socket", e);
        } finally {
            this.running = false;
        }
    }

    private void processor() {
        while (this.running) {
            try {
                long keepAliveTime = System.currentTimeMillis();
                if (keepAliveTime - this.lastKeepAlive > 1000) {
                    // todo: make this configurable
                    this.checkKeepAlives();
                    this.lastKeepAlive = keepAliveTime;
                }

                RawPacket rawPacket = this.packetQueue.poll(10, TimeUnit.MILLISECONDS);
                if (rawPacket == null) continue;

                VoicePacket packet = this.packetHandler.read(rawPacket, this.secretHolder);

                if (System.currentTimeMillis() - rawPacket.timestamp() > packet.ttl()) {
                    LOGGER.warn("dropping expired voice packet: {}", packet);
                    LOGGER.warn("your server may be overloaded");
                    continue;
                }

                if (packet instanceof AuthenticatePacket auth) {
                    Player player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(auth.player());
                    if (player == null) {
                        LOGGER.warn("received authentication packet from unknown player: {}", auth.player());
                        continue;
                    }
                    this.handle(player, auth, rawPacket.address());
                    continue;
                }

                SocketAddress address = rawPacket.address();
                Player player = this.connections.get(address);
                if (player == null) {
                    LOGGER.warn("received voice packet from unknown address: {}", address);
                    continue;
                }

                switch (packet) {
                    case YouHereBroPacket p -> this.handle(player, p);
                    case MicrophonePacket p -> this.handle(player, p);
                    case KeepAlivePacket p -> this.handle(player, p);
                    case PingPacket p -> this.handle(player, p);
                    default -> throw new IllegalStateException("unexpected packet: " + packet);
                }
            } catch (InterruptedException ignored) {
                // wait interrupted, ignore
            } catch (Exception e) {
                // we ignore this exception because it's most
                // likely to be caused by the client sending
                // an invalid packet.
                LOGGER.debug("failed to read voice packet", e);
            }
        }
    }

    private void write(@NotNull Player player, @NotNull VoicePacket packet) {
        try {
            this.write0(player, packet);
        } catch (IOException e) {
            // we ignore this exception because it's most
            // likely to be caused by the client disconnecting.
            LOGGER.debug("failed to write voice packet", e);
        }
    }

    private void write0(@NotNull Player player, @NotNull VoicePacket packet) throws IOException {
        SocketAddress address = this.retrieveSocketAddress(player);
        if (address == null) return;

        this.socket.write(this.packetHandler.write(player.getUuid(), packet, this.secretHolder), address);
    }

    private @Nullable SocketAddress retrieveSocketAddress(@NotNull Player player) {
        if (!player.hasTag(Tags.VOICE_CLIENT)) return null;
        return player.getTag(Tags.VOICE_CLIENT);
    }

    private void checkKeepAlives() {
        long timestamp = System.currentTimeMillis();

        // todo: disconnect client if no keep alive received for a certain amount of time

        this.connections.forEach((address, player) -> this.write(player, new KeepAlivePacket()));
    }

    private void handle(@NotNull Player player, @NotNull AuthenticatePacket packet, @NotNull SocketAddress address) {
        if (this.connections.containsKey(address)) {
            LOGGER.warn("received duplicate authentication packet from {}", address);
            return;
        }

        if (!packet.secret().equals(this.secretHolder.getSecret(player.getUuid()))) {
            LOGGER.warn("received invalid secret from {}", player.getUsername());
            player.kick(Component.text("Simple Voice Chat | Received incorrect secret, please rejoin."));
            return;
        }

        player.setTag(Tags.VOICE_CLIENT, address);
        this.connections.put(address, player);
        this.write(player, new AuthenticationAcknowledgedPacket());
    }

    private void handle(@NotNull Player player, @NotNull YouHereBroPacket ignored) {
        // todo: send keepalive for clients that take a long time to initially connect

        // this is the packet that is sent when the client is ready to receive voice packets
        // this means they are successfully connected to the voice server
        EventDispatcher.call(new PlayerJoinVoiceChatEvent(player));

        this.write(player, new YeaImHerePacket());
    }

    private void handle(@NotNull Player player, @NotNull MicrophonePacket packet) {
        // todo: implement groups?

        PlayerMicrophoneEvent event = new PlayerMicrophoneEvent(player, packet.data());
        EventDispatcher.callCancellable(event, () -> {
            PlayerSoundPacket soundPacket = new PlayerSoundPacket(
                    player.getUuid(), // the channel is the sender's UUID
                    player.getUuid(),
                    event.getAudio(),
                    packet.sequenceNumber(),
                    event.getSoundSelector().distance(),
                    packet.whispering(),
                    SoundSources.PROXIMITY
            );

            event.getSoundSelector().canHear(player).stream().filter(p -> {
                if (p.equals(player)) return false;
                return !p.hasTag(Tags.PLAYER_STATE) || !p.getTag(Tags.PLAYER_STATE).isDisabled();
            }).forEach(p -> this.write(p, soundPacket));
        });
    }

    private void handle(@NotNull Player player, @NotNull KeepAlivePacket packet) {
        // todo: handle keepalive
    }

    private void handle(@NotNull Player player, @NotNull PingPacket packet) {
        LOGGER.debug("received ping packet: {}", packet);
        // todo: handle pings
    }

}
