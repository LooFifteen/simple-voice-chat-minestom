package dev.lu15.voicechat.api;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.EntityTracker;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

public interface SoundSelector {

    static @NotNull SoundSelector distance(double distance) {
        return new SoundSelector() {
            @Override
            public @NotNull Set<Player> canHear(@NotNull Player player) {
                Instance instance = player.getInstance();
                if (instance == null) return Set.of();

                Set<Player> players = new HashSet<>();
                instance.getEntityTracker().nearbyEntities(player.getPosition(), distance, EntityTracker.Target.PLAYERS, players::add);
                return players;
            }

            @Override
            public float distance() {
                return (float) distance;
            }
        };
    }

    @NotNull Set<Player> canHear(@NotNull Player player);

    float distance();

}
