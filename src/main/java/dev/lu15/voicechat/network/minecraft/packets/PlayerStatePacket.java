package dev.lu15.voicechat.network.minecraft.packets;

import dev.lu15.voicechat.PlayerState;
import dev.lu15.voicechat.network.minecraft.Packet;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

public record PlayerStatePacket(@NotNull PlayerState state) implements Packet {

    public PlayerStatePacket(@NotNull NetworkBuffer buffer) {
        this(buffer.read(PlayerState.NETWORK_TYPE));
    }

    @Override
    public void write(@NotNull NetworkBuffer writer) {
        writer.write(PlayerState.NETWORK_TYPE, this.state);
    }

    @Override
    public @NotNull String id() {
        return "voicechat:player_state";
    }

}
