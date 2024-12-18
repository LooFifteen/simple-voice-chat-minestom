package dev.lu15.voicechat.network.minecraft.packets.clientbound;

import dev.lu15.voicechat.network.minecraft.VoiceState;
import dev.lu15.voicechat.network.minecraft.Packet;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

public record VoiceStatePacket(@NotNull VoiceState state) implements Packet<VoiceStatePacket> {

    public static final @NotNull NetworkBuffer.Type<VoiceStatePacket> SERIALIZER = NetworkBufferTemplate.template(
            VoiceState.NETWORK_TYPE, VoiceStatePacket::state,
            VoiceStatePacket::new
    );

    @Override
    public @NotNull String id() {
        return "voicechat:player_state";
    }

    @Override
    public NetworkBuffer.@NotNull Type<VoiceStatePacket> serializer() {
        return SERIALIZER;
    }

}
