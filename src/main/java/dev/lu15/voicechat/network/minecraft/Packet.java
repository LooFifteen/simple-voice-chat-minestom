package dev.lu15.voicechat.network.minecraft;

import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

public interface Packet extends NetworkBuffer.Writer {

    static <E extends Enum<E>> NetworkBuffer. @NotNull Type<E> ByteEnum(@NotNull Class<E> enumClass) {
        return new NetworkBuffer.Type<>() {
            @Override
            public void write(@NotNull NetworkBuffer buffer, E value) {
                buffer.write(NetworkBuffer.BYTE, (byte) value.ordinal());
            }

            @Override
            public E read(@NotNull NetworkBuffer buffer) {
                return enumClass.getEnumConstants()[buffer.read(NetworkBuffer.BYTE)];
            }
        };
    }

    @NotNull String id();

}
