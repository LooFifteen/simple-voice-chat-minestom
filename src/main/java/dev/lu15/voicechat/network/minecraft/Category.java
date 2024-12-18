package dev.lu15.voicechat.network.minecraft;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record Category(
        @NotNull String id,
        @NotNull String name,
        @Nullable String description,
        int @Nullable [] @NotNull [] icon
) {

    private static final int ICON_SIZE = 16;
    private static final @NotNull NetworkBuffer.Type<int[][]> ICON_SERIALIZER = new NetworkBuffer.Type<>() {
        @Override
        public void write(@NotNull NetworkBuffer buffer, int[][] value) {
            for (int i = 0; i < ICON_SIZE; i++) {
                for (int j = 0; j < ICON_SIZE; j++) {
                    buffer.write(NetworkBuffer.INT, value[i][j]);
                }
            }
        }

        @Override
        public int[][] read(@NotNull NetworkBuffer buffer) {
            int[][] icon = new int[ICON_SIZE][ICON_SIZE];
            for (int i = 0; i < ICON_SIZE; i++) {
                for (int j = 0; j < ICON_SIZE; j++) {
                    icon[i][j] = buffer.read(NetworkBuffer.INT);
                }
            }
            return icon;
        }
    };
    public static final @NotNull NetworkBuffer.Type<Category> NETWORK_TYPE = NetworkBufferTemplate.template(
            NetworkBuffer.STRING, Category::id,
            NetworkBuffer.STRING, Category::name,
            NetworkBuffer.STRING.optional(), Category::description,
            ICON_SERIALIZER.optional(), Category::icon,
            Category::new
    );

}
