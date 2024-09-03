package dev.lu15.voicechat.network.minecraft.packets;

import dev.lu15.voicechat.network.minecraft.Packet;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

public record UpdateStatePacket(boolean disabled) implements Packet {

    public UpdateStatePacket(@NotNull NetworkBuffer buffer) {
        this(buffer.read(NetworkBuffer.BOOLEAN));
    }

    @Override
    public void write(@NotNull NetworkBuffer writer) {
        writer.write(NetworkBuffer.BOOLEAN, this.disabled);
    }

    @Override
    public @NotNull String id() {
        return "voicechat:update_state";
    }

}
