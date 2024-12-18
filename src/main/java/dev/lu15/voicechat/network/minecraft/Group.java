package dev.lu15.voicechat.network.minecraft;

import dev.lu15.voicechat.network.NetworkTypes;
import java.util.UUID;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

public record Group(
        @NotNull UUID id,
        @NotNull String name,
        boolean passwordProtected,
        boolean persistent,
        boolean hidden,
        @NotNull Type type
) {

    public static final @NotNull NetworkBuffer.Type<Group> NETWORK_TYPE = NetworkBufferTemplate.template(
            NetworkBuffer.UUID, Group::id,
            NetworkBuffer.STRING, Group::name,
            NetworkBuffer.BOOLEAN, Group::passwordProtected,
            NetworkBuffer.BOOLEAN, Group::persistent,
            NetworkBuffer.BOOLEAN, Group::hidden,
            NetworkTypes.ShortEnum(Type.class), Group::type,
            Group::new
    );

    public enum Type {
        NORMAL,
        OPEN,
        ISOLATED
    }

}
