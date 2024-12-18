package dev.lu15.voicechat.network.minecraft;

import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

public interface Packet<T extends Packet<T>> {

    @NotNull String id();

    @NotNull NetworkBuffer.Type<T> serializer();

}
