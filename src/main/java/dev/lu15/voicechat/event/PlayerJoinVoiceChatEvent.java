package dev.lu15.voicechat.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public final class PlayerJoinVoiceChatEvent implements PlayerEvent {

    private final @NotNull Player player;

    public PlayerJoinVoiceChatEvent(@NotNull Player player) {
        this.player = player;
    }

    @Override
    public @NotNull Player getPlayer() {
        return this.player;
    }

}
