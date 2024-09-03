package dev.lu15.voicechat.network.minecraft.packets;

import dev.lu15.voicechat.VoiceState;
import dev.lu15.voicechat.network.minecraft.Packet;
import java.util.Collection;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

public record VoiceStatesPacket(@NotNull Collection<VoiceState> states) implements Packet {

    private static final @NotNull NetworkBuffer.Type<Collection<VoiceState>> NETWORK_TYPE = Packet.IntIndexedCollection(VoiceState.NETWORK_TYPE);

    public VoiceStatesPacket(@NotNull NetworkBuffer buffer) {
        this(buffer.read(NETWORK_TYPE));
    }

    @Override
    public void write(@NotNull NetworkBuffer writer) {
        writer.write(NETWORK_TYPE, this.states);
    }

    @Override
    public @NotNull String id() {
        return "voicechat:player_states";
    }

}
