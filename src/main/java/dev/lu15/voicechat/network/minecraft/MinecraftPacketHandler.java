package dev.lu15.voicechat.network.minecraft;

import dev.lu15.voicechat.network.minecraft.packets.clientbound.CategoryAddedPacket;
import dev.lu15.voicechat.network.minecraft.packets.serverbound.CreateGroupPacket;
import dev.lu15.voicechat.network.minecraft.packets.clientbound.GroupCreatedPacket;
import dev.lu15.voicechat.network.minecraft.packets.clientbound.GroupChangedPacket;
import dev.lu15.voicechat.network.minecraft.packets.clientbound.GroupRemovedPacket;
import dev.lu15.voicechat.network.minecraft.packets.serverbound.JoinGroupPacket;
import dev.lu15.voicechat.network.minecraft.packets.serverbound.LeaveGroupPacket;
import dev.lu15.voicechat.network.minecraft.packets.clientbound.VoiceStatePacket;
import dev.lu15.voicechat.network.minecraft.packets.clientbound.HandshakeAcknowledgePacket;
import dev.lu15.voicechat.network.minecraft.packets.serverbound.HandshakePacket;
import dev.lu15.voicechat.network.minecraft.packets.serverbound.UpdateStatePacket;
import dev.lu15.voicechat.network.minecraft.packets.clientbound.VoiceStatesPacket;
import java.util.HashMap;
import java.util.Map;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.common.PluginMessagePacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MinecraftPacketHandler {

    private final @NotNull Map<String, NetworkBuffer.Type<Packet<?>>> serializers = new HashMap<>();

    public MinecraftPacketHandler() {
        // clientbound
        this.register("voicechat:secret", HandshakeAcknowledgePacket.SERIALIZER);
        this.register("voicechat:player_state", VoiceStatePacket.SERIALIZER);
        this.register("voicechat:player_states", VoiceStatesPacket.SERIALIZER);
        this.register("voicechat:add_group", GroupCreatedPacket.SERIALIZER);
        this.register("voicechat:joined_group", GroupChangedPacket.SERIALIZER);
        this.register("voicechat:remove_group", GroupRemovedPacket.SERIALIZER);
        this.register("voicechat:add_category", CategoryAddedPacket.SERIALIZER);
        this.register("voicechat:remove_category", CategoryAddedPacket.SERIALIZER);

        // serverbound
        this.register("voicechat:request_secret", HandshakePacket.SERIALIZER);
        this.register("voicechat:update_state", UpdateStatePacket.SERIALIZER);
        this.register("voicechat:set_group", JoinGroupPacket.SERIALIZER);
        this.register("voicechat:leave_group", LeaveGroupPacket.SERIALIZER);
        this.register("voicechat:create_group", CreateGroupPacket.SERIALIZER);
    }

    @SuppressWarnings("unchecked")
    public <T extends Packet<T>> void register(@NotNull String id, @NotNull NetworkBuffer.Type<T> serializer) {
        this.serializers.put(id, (NetworkBuffer.Type<Packet<?>>) serializer);
    }

    public @Nullable Packet<?> read(@NotNull String identifier, byte[] data) {
        NetworkBuffer.Type<Packet<?>> serializer = this.serializers.get(identifier);
        if (serializer == null) return null;

        NetworkBuffer buffer = NetworkBuffer.wrap(data, 0, data.length);
        return serializer.read(buffer);
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
