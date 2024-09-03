package dev.lu15.voicechat.network.voice;

import dev.lu15.voicechat.network.voice.encryption.AES;
import dev.lu15.voicechat.network.voice.packets.AuthenticatePacket;
import dev.lu15.voicechat.network.voice.packets.AuthenticationAcknowledgedPacket;
import dev.lu15.voicechat.network.voice.packets.GroupSoundPacket;
import dev.lu15.voicechat.network.voice.packets.KeepAlivePacket;
import dev.lu15.voicechat.network.voice.packets.MicrophonePacket;
import dev.lu15.voicechat.network.voice.packets.PingPacket;
import dev.lu15.voicechat.network.voice.packets.PlayerSoundPacket;
import dev.lu15.voicechat.network.voice.packets.PositionedSoundPacket;
import dev.lu15.voicechat.network.voice.packets.YeaImHerePacket;
import dev.lu15.voicechat.network.voice.packets.YouHereBroPacket;
import java.nio.ByteBuffer;
import java.util.UUID;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.collection.ObjectArray;
import org.jetbrains.annotations.NotNull;

public final class VoicePacketHandler {

    private static final byte MAGIC_BYTE = (byte) 0b11111111;

    private final @NotNull ObjectArray<NetworkBuffer.Reader<VoicePacket>> suppliers = ObjectArray.singleThread(0xA);

    public VoicePacketHandler() {
        this.register(0x1, MicrophonePacket::new);
        this.register(0x2, PlayerSoundPacket::new);
        this.register(0x3, GroupSoundPacket::new);
        this.register(0x4, PositionedSoundPacket::new);
        this.register(0x5, AuthenticatePacket::new);
        this.register(0x6, AuthenticationAcknowledgedPacket::new);
        this.register(0x7, PingPacket::new);
        this.register(0x8, KeepAlivePacket::new);
        this.register(0x9, YouHereBroPacket::new);
        this.register(0xA, YeaImHerePacket::new);
    }

    public void register(int id, @NotNull NetworkBuffer.Reader<VoicePacket> supplier) {
        this.suppliers.set(id, supplier);
    }

    public @NotNull VoicePacket read(@NotNull RawPacket packet, @NotNull SecretHolder secretHolder) throws Exception {
        byte[] data = packet.data();
        NetworkBuffer outer = new NetworkBuffer(ByteBuffer.wrap(data));

        if (outer.read(NetworkBuffer.BYTE) != MAGIC_BYTE) throw new IllegalStateException("invalid magic byte");

        UUID player = outer.read(NetworkBuffer.UUID);
        UUID secret = secretHolder.getSecret(player);
        if (secret == null) throw new IllegalStateException("no secret for player");

        byte[] decrypted = AES.decrypt(secret, outer.read(NetworkBuffer.BYTE_ARRAY));
        NetworkBuffer buffer = new NetworkBuffer(ByteBuffer.wrap(decrypted));

        int id = buffer.read(NetworkBuffer.BYTE);
        NetworkBuffer.Reader<VoicePacket> supplier = this.suppliers.get(id);
        if (supplier == null) throw new IllegalStateException("invalid packet id");

        return supplier.read(buffer);
    }

    public byte @NotNull[] write(@NotNull UUID player, @NotNull VoicePacket packet, @NotNull SecretHolder secretHolder) {
        try {
            return this.write0(player, packet, secretHolder);
        } catch (Exception e) {
            // the code on the server should be trusted, so this runtime exception is fine to throw
            throw new RuntimeException("failed to write packet", e);
        }
    }

    private byte @NotNull[] write0(@NotNull UUID player, @NotNull VoicePacket packet, @NotNull SecretHolder secretHolder) throws Exception {
        NetworkBuffer buffer = new NetworkBuffer();
        buffer.write(NetworkBuffer.BYTE, MAGIC_BYTE);

        UUID secret = secretHolder.getSecret(player);
        if (secret == null) throw new IllegalStateException("no secret for player");

        NetworkBuffer inner = new NetworkBuffer();
        inner.write(NetworkBuffer.BYTE, (byte) packet.id());
        packet.write(inner);

        byte[] data = new byte[inner.readableBytes()];
        inner.copyTo(0, data, 0, data.length);

        byte[] encrypted = AES.encrypt(secret, data);
        buffer.write(NetworkBuffer.BYTE_ARRAY, encrypted);

        byte[] result = new byte[buffer.readableBytes()];
        buffer.copyTo(0, result, 0, result.length);

        return result;
    }

}
