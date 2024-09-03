package dev.lu15.voicechat.network.minecraft;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

    static <T> NetworkBuffer.@NotNull Type<Collection<T>> IntIndexedCollection(@NotNull NetworkBuffer.Type<T> backend) {
        return new NetworkBuffer.Type<>() {
            @Override
            public void write(@NotNull NetworkBuffer buffer, Collection<T> value) {
                buffer.write(NetworkBuffer.INT, value.size());
                for (T element : value) buffer.write(backend, element);
            }

            @Override
            public Collection<T> read(@NotNull NetworkBuffer buffer) {
                int size = buffer.read(NetworkBuffer.INT);
                List<T> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) list.add(buffer.read(backend));
                return list;
            }
        };
    }

    @NotNull String id();

}
