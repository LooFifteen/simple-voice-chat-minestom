package dev.lu15.voicechat.network.minecraft;

import dev.lu15.voicechat.network.minecraft.packets.VoiceStatePacket;
import dev.lu15.voicechat.network.minecraft.packets.HandshakeAcknowledgePacket;
import dev.lu15.voicechat.network.minecraft.packets.HandshakePacket;
import dev.lu15.voicechat.network.minecraft.packets.UpdateStatePacket;
import dev.lu15.voicechat.network.minecraft.packets.VoiceStatesPacket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.common.PluginMessagePacket;
import org.jetbrains.annotations.NotNull;

public final class MinecraftPacketHandler {

    private final @NotNull Map<String, NetworkBuffer.Type<Packet<?>>> serializers = new HashMap<>();

    public MinecraftPacketHandler() {
        this.register("voicechat:request_secret", HandshakePacket.SERIALIZER);
        this.register("voicechat:secret", HandshakeAcknowledgePacket.SERIALIZER);
        this.register("voicechat:update_state", UpdateStatePacket.SERIALIZER);
        this.register("voicechat:player_state", VoiceStatePacket.SERIALIZER);
        this.register("voicechat:player_states", VoiceStatesPacket.SERIALIZER);
    }

    @SuppressWarnings("unchecked")
    public <T extends Packet<T>> void register(@NotNull String id, @NotNull NetworkBuffer.Type<T> supplier) {
        this.serializers.put(id, (NetworkBuffer.Type<Packet<?>>) supplier);
    }

    public @NotNull Packet<?> read(@NotNull String identifier, byte[] data) {
        NetworkBuffer.Type<Packet<?>> supplier = this.serializers.get(identifier);
        if (supplier == null) throw new IllegalStateException(String.format("invalid packet id: %s", identifier));

        NetworkBuffer buffer = NetworkBuffer.wrap(data, 0, data.length);
        return supplier.read(buffer);
    }

    public <T extends Packet<T>> @NotNull PluginMessagePacket write(@NotNull T packet) {
        NetworkBuffer.Type<T> serializer = packet.serializer();
        NetworkBuffer buffer = NetworkBuffer.resizableBuffer();
        buffer.write(serializer, packet);

        byte[] data = new byte[(int) buffer.writeIndex()];
        buffer.copyTo(0, data, 0, data.length);

        return new PluginMessagePacket(packet.id(), data);
    }

}
