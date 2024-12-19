package dev.lu15.voicechat.network.voice;

import dev.lu15.voicechat.network.voice.encryption.AES;
import dev.lu15.voicechat.network.voice.encryption.SecretUtilities;
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
import java.util.UUID;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.collection.ObjectArray;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage") // thanks, minestom
public final class VoicePacketHandler {

    private static final byte MAGIC_BYTE = (byte) 0b11111111;

    private final @NotNull ObjectArray<NetworkBuffer.Type<VoicePacket<?>>> suppliers = ObjectArray.singleThread(0xA);

    public VoicePacketHandler() {
        this.register(0x1, MicrophonePacket.SERIALIZER);
        this.register(0x2, PlayerSoundPacket.SERIALIZER);
        this.register(0x3, GroupSoundPacket.SERIALIZER);
        this.register(0x4, PositionedSoundPacket.SERIALIZER);
        this.register(0x5, AuthenticatePacket.SERIALIZER);
        this.register(0x6, AuthenticationAcknowledgedPacket.SERIALIZER);
        this.register(0x7, PingPacket.SERIALIZER);
        this.register(0x8, KeepAlivePacket.SERIALIZER);
        this.register(0x9, YouHereBroPacket.SERIALIZER);
        this.register(0xA, YeaImHerePacket.SERIALIZER);
    }

    @SuppressWarnings("unchecked")
    public <T extends VoicePacket<T>> void register(int id, @NotNull NetworkBuffer.Type<T> supplier) {
        this.suppliers.set(id, (NetworkBuffer.Type<VoicePacket<?>>) supplier);
    }

    public @Nullable VoicePacket<?> read(@NotNull RawPacket packet) throws Exception {
        byte[] data = packet.data();
        NetworkBuffer outer = NetworkBuffer.wrap(data, 0, data.length);

        if (outer.read(NetworkBuffer.BYTE) != MAGIC_BYTE) throw new IllegalStateException("invalid magic byte");

        Player player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(outer.read(NetworkBuffer.UUID));
        if (player == null || !player.isOnline()) return null; // player has disconnected

        UUID secret = SecretUtilities.getSecret(player.getUuid());
        if (secret == null) throw new IllegalStateException("no secret for player");

        byte[] decrypted = AES.decrypt(secret, outer.read(NetworkBuffer.BYTE_ARRAY));
        NetworkBuffer buffer = NetworkBuffer.wrap(decrypted, 0, decrypted.length);

        int id = buffer.read(NetworkBuffer.BYTE);
        NetworkBuffer.Type<VoicePacket<?>> supplier = this.suppliers.get(id);
        if (supplier == null) throw new IllegalStateException("invalid packet id");

        return supplier.read(buffer);
    }

    public <T extends VoicePacket<T>> byte @NotNull[] write(@NotNull Player player, @NotNull T packet) {
        try {
            return this.write0(player, packet);
        } catch (Exception e) {
            // the code running this method should be from simple-voice-chat-minestom itself,
            // so it should be safe to throw a runtime exception here - it's a b_ug in the code
            throw new RuntimeException("failed to write packet", e);
        }
    }

    private <T extends VoicePacket<T>> byte @NotNull[] write0(@NotNull Player player, @NotNull T packet) throws Exception {
        NetworkBuffer buffer = NetworkBuffer.resizableBuffer();
        buffer.write(NetworkBuffer.BYTE, MAGIC_BYTE);

        UUID secret = SecretUtilities.getSecret(player);
        if (secret == null) throw new IllegalStateException("no secret for player");

        NetworkBuffer inner = NetworkBuffer.resizableBuffer();
        inner.write(NetworkBuffer.BYTE, (byte) packet.id());
        packet.serializer().write(inner, packet);

        byte[] data = new byte[(int) inner.writeIndex()];
        inner.copyTo(0, data, 0, data.length);

        byte[] encrypted = AES.encrypt(secret, data);
        buffer.write(NetworkBuffer.BYTE_ARRAY, encrypted);

        byte[] result = new byte[(int) buffer.writeIndex()];
        buffer.copyTo(0, result, 0, result.length);

        return result;
    }

}
