package dev.lu15.voicechat.network.minecraft;

import java.util.UUID;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record VoiceState(
        boolean disabled,
        boolean disconnected,
        @NotNull UUID uuid,
        @NotNull String name,
        @Nullable UUID group
) {

    // todo!
    // the group field is sent to all clients, even if the group is hidden.
    // this will leak the existence of the group to clients that are not in it.
    // we should probably separate the voice state into a state kept on the server
    // and a serialized state sent to clients, which will allow us to hide the group field
    // from clients that are not in the group.

    public static final @NotNull NetworkBuffer.Type<VoiceState> NETWORK_TYPE = NetworkBufferTemplate.template(
            NetworkBuffer.BOOLEAN, VoiceState::disabled,
            NetworkBuffer.BOOLEAN, VoiceState::disconnected,
            NetworkBuffer.UUID, VoiceState::uuid,
            NetworkBuffer.STRING, VoiceState::name,
            NetworkBuffer.OPT_UUID, VoiceState::group,
            VoiceState::new
    );

    public @NotNull VoiceState withGroup(@Nullable UUID group) {
        return new VoiceState(disabled, disconnected, uuid, name, group);
    }

    public @NotNull VoiceState withDisabled(boolean disabled) {
        return new VoiceState(disabled, disconnected, uuid, name, group);
    }

}
