package dev.lu15.voicechat.event;

import dev.lu15.voicechat.group.Group;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerCreateGroupEvent implements PlayerEvent, CancellableEvent {

    private final @NotNull Player player;
    private final @NotNull Group group;
    private boolean cancelled;

    public PlayerCreateGroupEvent(@NotNull Player player, @NotNull Group group) {
        this.player = player;
        this.group = group;
    }

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }

    public @NotNull Group getGroup() {
        return group;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

}