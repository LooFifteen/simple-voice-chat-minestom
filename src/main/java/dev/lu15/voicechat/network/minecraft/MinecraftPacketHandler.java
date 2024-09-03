package dev.lu15.voicechat.network.minecraft;

import dev.lu15.voicechat.network.minecraft.packets.VoiceStatePacket;
import dev.lu15.voicechat.network.minecraft.packets.HandshakeAcknowledgePacket;
import dev.lu15.voicechat.network.minecraft.packets.HandshakePacket;
import dev.lu15.voicechat.network.minecraft.packets.UpdateStatePacket;
import dev.lu15.voicechat.network.minecraft.packets.VoiceStatesPacket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.common.PluginMessagePacket;
import org.jetbrains.annotations.NotNull;

public final class MinecraftPacketHandler {

    private final @NotNull Map<String, NetworkBuffer.Reader<Packet>> suppliers = new HashMap<>();

    public MinecraftPacketHandler() {
        this.register("voicechat:request_secret", HandshakePacket::new);
        this.register("voicechat:secret", HandshakeAcknowledgePacket::new);
        this.register("voicechat:update_state", UpdateStatePacket::new);
        this.register("voicechat:player_state", VoiceStatePacket::new);
        this.register("voicechat:player_states", VoiceStatesPacket::new);
    }

    public void register(@NotNull String id, @NotNull NetworkBuffer.Reader<Packet> supplier) {
        this.suppliers.put(id, supplier);
    }

    public @NotNull Packet read(@NotNull String identifier, byte[] data) throws Exception {
        NetworkBuffer.Reader<Packet> supplier = this.suppliers.get(identifier);
        if (supplier == null) throw new IllegalStateException(String.format("invalid packet id: %s", identifier));

        NetworkBuffer buffer = new NetworkBuffer(ByteBuffer.wrap(data));
        return supplier.read(buffer);
    }

    public @NotNull PluginMessagePacket write(@NotNull Packet packet) {
        NetworkBuffer buffer = new NetworkBuffer();
        packet.write(buffer);

        byte[] data = new byte[buffer.readableBytes()];
        buffer.copyTo(0, data, 0, data.length);

        return new PluginMessagePacket(packet.id(), data);
    }

}
