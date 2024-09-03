package dev.lu15.voicechat.network.voice;

import java.net.SocketAddress;
import org.jetbrains.annotations.NotNull;

public record RawPacket(
        byte @NotNull[] data,
        @NotNull SocketAddress address,
        long timestamp
) {}
