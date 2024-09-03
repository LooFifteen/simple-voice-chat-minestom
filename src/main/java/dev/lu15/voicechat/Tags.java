package dev.lu15.voicechat;

import java.net.SocketAddress;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

public final class Tags {

    public static final @NotNull Tag<SocketAddress> VOICE_CLIENT = Tag.Transient("voice-client");
    public static final @NotNull Tag<PlayerState> PLAYER_STATE = Tag.Transient("player-state");

    private Tags() {}

}
