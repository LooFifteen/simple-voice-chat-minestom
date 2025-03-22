package dev.lu15.voicechat;

import dev.lu15.voicechat.network.minecraft.Category;
import dev.lu15.voicechat.network.minecraft.Packet;
import dev.lu15.voicechat.network.voice.VoicePacket;
import java.util.Collection;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.utils.NamespaceID;
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

    @NotNull DynamicRegistry.Key<Category> addCategory(@NotNull NamespaceID id, @NotNull Category category);

    boolean removeCategory(@NotNull DynamicRegistry.Key<Category> category);

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
         * Set the mtu of the voice server. This is used to determine the largest size of a packet.
         * By default, this is set to 1024.
         * @param mtu the public address of the voice server
         * @return this builder
         */
        @NotNull Builder setMTU(Integer mtu);

        /**
         * Set the codec of the voice server. This is used by clients to determine which audio codec to use.
         * By default, this is set to VOIP.
         * @param codec the voice codec to be used by clients
         * @return this builder
         */
        @NotNull Builder setCodec(Codec codec);

        /**
         * Set the voice distance of the server. This is used to determine how far users can hear each other from..
         * By default, this is set to 48.
         * @param distance the distance players can hear each other from.
         * @return this builder
         */
        @NotNull Builder setDistance(Integer distance);

        /**
         * Enables/Disables the use of groups on the server.
         * By default, this is set to false.
         * @param enabled used to determine if groups should be enabled.
         * @return this builder
         */
        @NotNull Builder enableGroups(Boolean enabled);

        /**
         * Set the keepalive delay for the server.
         * By default, this is set to 1000.
         * @param keepalive used to determine what the keepalive delay should be set to.
         * @return this builder
         */
        @NotNull Builder setKeepalive(Integer keepalive);

        /**
         * Enables/Disables the use of recording on the server.
         * By default, this is set to false.
         * @param enabled used to determine if recording should be enabled.
         * @return this builder
         */
        @NotNull Builder enableRecording(Boolean enabled);

        /**
         * Enable the voice chat server.
         * @return the voice chat server
         */
        @NotNull VoiceChat enable();
    }

}
