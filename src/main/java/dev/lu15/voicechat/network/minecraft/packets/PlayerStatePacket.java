package dev.lu15.voicechat.network.minecraft.packets;

import dev.lu15.voicechat.VoiceState;
import dev.lu15.voicechat.network.minecraft.Packet;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

public record PlayerStatePacket(@NotNull VoiceState state) implements Packet {

    public PlayerStatePacket(@NotNull NetworkBuffer buffer) {
        this(buffer.read(VoiceState.NETWORK_TYPE));
    }

    @Override
    public void write(@NotNull NetworkBuffer writer) {
        writer.write(VoiceState.NETWORK_TYPE, this.state);
    }

    @Override
    public @NotNull String id() {
        return "voicechat:player_state";
    }

}
