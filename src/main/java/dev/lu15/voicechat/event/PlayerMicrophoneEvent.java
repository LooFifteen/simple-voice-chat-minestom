package dev.lu15.voicechat.event;

import dev.lu15.voicechat.api.SoundSelector;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Called whenever microphone data is sent by a player. This event is called a lot,
 * so it is recommended to keep listeners as lightweight as possible.
 */
public final class PlayerMicrophoneEvent implements PlayerInstanceEvent, CancellableEvent {

    private final @NotNull Player player;

    private @NotNull SoundSelector soundSelector = SoundSelector.distance(48);

    private byte @NotNull[] audio;
    private boolean cancelled;

    public PlayerMicrophoneEvent(@NotNull Player player, byte @NotNull[] audio) {
        this.player = player;
        this.audio = audio;
    }

    @Override
    public @NotNull Player getPlayer() {
        return this.player;
    }

    public byte @NotNull[] getAudio() {
        return audio;
    }

    public void setAudio(byte @NotNull[] audio) {
        this.audio = audio;
    }

    public @NotNull SoundSelector getSoundSelector() {
        return soundSelector;
    }

    public void setSoundSelector(@NotNull SoundSelector soundSelector) {
        this.soundSelector = soundSelector;
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
