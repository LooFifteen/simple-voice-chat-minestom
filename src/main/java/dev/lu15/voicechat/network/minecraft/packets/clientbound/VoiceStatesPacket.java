package dev.lu15.voicechat.network.minecraft.packets.clientbound;

import dev.lu15.voicechat.network.minecraft.VoiceState;
import dev.lu15.voicechat.network.NetworkTypes;
import dev.lu15.voicechat.network.minecraft.Packet;
import java.util.Collection;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

public record VoiceStatesPacket(@NotNull Collection<VoiceState> states) implements Packet<VoiceStatesPacket> {

    public static final @NotNull NetworkBuffer.Type<VoiceStatesPacket> SERIALIZER = NetworkBufferTemplate.template(
            NetworkTypes.IntIndexedCollection(VoiceState.NETWORK_TYPE), VoiceStatesPacket::states,
            VoiceStatesPacket::new
    );

    @Override
    public @NotNull String id() {
        return "voicechat:player_states";
    }

    @Override
    public NetworkBuffer.@NotNull Type<VoiceStatesPacket> serializer() {
        return SERIALIZER;
    }

}
