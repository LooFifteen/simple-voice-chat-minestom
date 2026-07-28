package dev.lu15.voicechat.event;

import dev.lu15.voicechat.network.voice.encryption.Secret;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public final class PlayerHandshakeVoiceChatEvent implements PlayerEvent, CancellableEvent {

    private final @NotNull Player player;

    private boolean cancelled;
    private @NotNull Secret secret;

    public PlayerHandshakeVoiceChatEvent(@NotNull Player player, @NotNull Secret secret) {
        this.player = player;
        this.secret = secret;
    }

    @Override
    public @NotNull Player getPlayer() {
        return this.player;
    }

    public @NotNull Secret getSecret() {
        return this.secret;
    }

    public void setSecret(@NotNull Secret secret) {
        this.secret = secret;
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
