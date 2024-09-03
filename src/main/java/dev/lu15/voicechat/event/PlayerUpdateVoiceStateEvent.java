package dev.lu15.voicechat.event;

import dev.lu15.voicechat.VoiceState;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public final class PlayerUpdateVoiceStateEvent implements PlayerEvent {

    private final @NotNull Player player;
    private final @NotNull VoiceState state;

    public PlayerUpdateVoiceStateEvent(@NotNull Player player, @NotNull VoiceState state) {
        this.player = player;
        this.state = state;
    }

    @Override
    public @NotNull Player getPlayer() {
        return this.player;
    }

    public @NotNull VoiceState getState() {
        return state;
    }

}
