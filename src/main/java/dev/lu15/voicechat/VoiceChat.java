package dev.lu15.voicechat;

import dev.lu15.voicechat.network.minecraft.Category;
import dev.lu15.voicechat.network.minecraft.Packet;
import dev.lu15.voicechat.network.voice.VoicePacket;
import java.util.Collection;

import net.kyori.adventure.key.Key;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.registry.RegistryKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

public sealed interface VoiceChat permits VoiceChatImpl {

    /**
     * Construct a new voice chat server. The server will start after building.
     * @param address the address to bind to
     * @param port the port to bind to, this can be the same as the Minecraft server port
     * @return a new voice chat server builder
     */
    static @NotNull Builder builder(@NotNull String address, int port) {
        return new VoiceChatImpl.BuilderImpl(address, port);
    }

    <T extends Packet<T>> void sendPacket(@NotNull Player player, @NotNull T packet);

    <T extends VoicePacket<T>> void sendPacket(@NotNull Player player, @NotNull T packet);

    @NotNull @Unmodifiable Collection<Category> getCategories();

    @NotNull RegistryKey<Category> addCategory(@NotNull Key id, @NotNull Category category);

    boolean removeCategory(@NotNull RegistryKey<Category> category);

    sealed interface Builder permits VoiceChatImpl.BuilderImpl {

        /**
         * Set the event node to use for voice chat events. This must be registered by yourself.
         * @param eventNode the event node
         * @return this builder
         */
        @NotNull Builder eventNode(@NotNull EventNode<Event> eventNode);

        /**
         * Set the public address of the voice server. This is used to tell clients where to connect to.
         * By default, this is blank and clients will use the address they connected to the Minecraft server with.
         * @param publicAddress the public address of the voice server
         * @return this builder
         */
        @NotNull Builder publicAddress(@NotNull String publicAddress);

        /**
         * Enable the voice chat server.
         * @return the voice chat server
         */
        @NotNull VoiceChat enable();

    }

}
