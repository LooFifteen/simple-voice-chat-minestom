package dev.lu15.voicechat.network.voice.encryption;

import dev.lu15.voicechat.Tags;
import java.security.SecureRandom;
import java.util.Random;
import java.util.UUID;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SecretUtilities {

    private static final @NotNull Random RANDOM = new SecureRandom();

    private SecretUtilities() {}

    public static @Nullable UUID getSecret(@NotNull UUID player) {
        // todo: this method is O(n), is it worth storing a map of UUIDs to players ourselves?
        Player p = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(player);
        return p != null ? getSecret(p) : null;
    }

    public static @Nullable UUID getSecret(@NotNull Player player) {
        return player.getTag(Tags.SECRET);
    }

    public static @Nullable UUID generateSecret(@NotNull Player player) {
        if (getSecret(player) != null) return null;

        UUID secret = new UUID(RANDOM.nextLong(), RANDOM.nextLong());
        player.setTag(Tags.SECRET, secret);
        return secret;
    }

}
