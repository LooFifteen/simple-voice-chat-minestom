package dev.lu15.voicechat.network.voice;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface SecretHolder {

    @Nullable UUID getSecret(@NotNull UUID player);

}
