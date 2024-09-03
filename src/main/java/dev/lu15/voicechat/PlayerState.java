package dev.lu15.voicechat;

import java.util.UUID;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PlayerState {

    public static final @NotNull NetworkBuffer.Type<PlayerState> NETWORK_TYPE = new NetworkBuffer.Type<>() {
        @Override
        public void write(@NotNull NetworkBuffer buffer, PlayerState value) {
            buffer.write(NetworkBuffer.BOOLEAN, value.disabled);
            buffer.write(NetworkBuffer.BOOLEAN, value.disconnected);
            buffer.write(NetworkBuffer.UUID, value.uuid);
            buffer.write(NetworkBuffer.STRING, value.name);
            buffer.write(NetworkBuffer.OPT_UUID, value.group);
        }

        @Override
        public PlayerState read(@NotNull NetworkBuffer buffer) {
            boolean disabled = buffer.read(NetworkBuffer.BOOLEAN);
            boolean disconnected = buffer.read(NetworkBuffer.BOOLEAN);
            UUID uuid = buffer.read(NetworkBuffer.UUID);
            String name = buffer.read(NetworkBuffer.STRING);
            UUID group = buffer.read(NetworkBuffer.OPT_UUID);

            return new PlayerState(
                    uuid,
                    name,
                    disabled,
                    disconnected,
                    group
            );
        }
    };

    private final @NotNull UUID uuid;
    private final @NotNull String name;
    private boolean disabled;
    private boolean disconnected;
    private @Nullable UUID group;

    public PlayerState(@NotNull UUID uuid, @NotNull String name, boolean disabled, boolean disconnected, @Nullable UUID group) {
        this.uuid = uuid;
        this.name = name;
        this.disabled = disabled;
        this.disconnected = disconnected;
        this.group = group;
    }

    public @NotNull UUID getUuid() {
        return this.uuid;
    }

    public @NotNull String getName() {
        return this.name;
    }

    public boolean isDisabled() {
        return this.disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public boolean isDisconnected() {
        return this.disconnected;
    }

    public void setDisconnected(boolean disconnected) {
        this.disconnected = disconnected;
    }

    public @Nullable UUID getGroup() {
        return this.group;
    }

    public void setGroup(@Nullable UUID group) {
        this.group = group;
    }

}
