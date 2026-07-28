package dev.lu15.voicechat.network.voice.encryption;

import dev.lu15.voicechat.Tags;
import java.util.UUID;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SecretUtilities {

    private SecretUtilities() {}

    public static @Nullable Secret getSecret(@NotNull UUID player) {
        // todo: this method is O(n), is it worth storing a map of UUIDs to players ourselves?
        Player p = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(player);
        return p != null ? getSecret(p) : null;
    }

    public static @Nullable Secret getSecret(@NotNull Player player) {
        return player.getTag(Tags.SECRET);
    }

    public static boolean hasSecret(@NotNull Player player) {
        return player.hasTag(Tags.SECRET);
    }

    public static void setSecret(@NotNull Player player, @Nullable Secret secret) {
        player.setTag(Tags.SECRET, secret);
    }

    public static @NotNull Secret generateSecret() {
        return Secret.generate();
    }

}
