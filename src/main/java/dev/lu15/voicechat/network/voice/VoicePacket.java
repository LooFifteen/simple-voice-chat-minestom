package dev.lu15.voicechat.network.voice;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

public interface VoicePacket<T extends VoicePacket<T>> {

    @NotNull NetworkBuffer.Type<Point> POSITION = new NetworkBuffer.Type<>() {
        @Override
        public Point read(@NotNull NetworkBuffer buffer) {
            return new Pos(
                    buffer.read(NetworkBuffer.DOUBLE),
                    buffer.read(NetworkBuffer.DOUBLE),
                    buffer.read(NetworkBuffer.DOUBLE)
            );
        }

        @Override
        public void write(@NotNull NetworkBuffer buffer, Point value) {
            buffer.write(NetworkBuffer.DOUBLE, value.x());
            buffer.write(NetworkBuffer.DOUBLE, value.y());
            buffer.write(NetworkBuffer.DOUBLE, value.z());
        }
    };

    int id();

    @NotNull NetworkBuffer.Type<T> serializer();

    default long ttl() {
        return 10_000;
    }

}
