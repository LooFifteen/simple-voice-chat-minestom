package dev.lu15.voicechat.network.voice;

import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record Flags(
        boolean whispering,
        @Nullable String category
) {

    public static final byte WHISPERING = 0b1;
    public static final byte CATEGORY = 0b10;

    public static final @NotNull NetworkBuffer.Type<Flags> SERIALIZER = new NetworkBuffer.Type<>() {
        @Override
        public void write(@NotNull NetworkBuffer buffer, Flags value) {
            byte flags = 0;
            if (value.whispering) flags |= WHISPERING;
            if (value.category != null) flags |= CATEGORY;

            buffer.write(NetworkBuffer.BYTE, flags);
            if (value.category != null) buffer.write(NetworkBuffer.STRING, value.category);
        }

        @Override
        public Flags read(@NotNull NetworkBuffer buffer) {
            byte flags = buffer.read(NetworkBuffer.BYTE);
            boolean whispering = (flags & WHISPERING) != 0;
            String category = (flags & CATEGORY) != 0 ? buffer.read(NetworkBuffer.STRING) : null;
            return new Flags(whispering, category);
        }
    };

    public static @NotNull Flags category(@Nullable String category) {
        return new Flags(false, category);
    }

    public static @NotNull Flags flags(boolean whispering, @Nullable String category) {
        return new Flags(whispering, category);
    }

}
