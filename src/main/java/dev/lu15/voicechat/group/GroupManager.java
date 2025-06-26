package dev.lu15.voicechat.group;

import dev.lu15.voicechat.Tags;
import dev.lu15.voicechat.network.minecraft.MinecraftPacketHandler;
import dev.lu15.voicechat.network.minecraft.VoiceState;
import dev.lu15.voicechat.network.minecraft.packets.clientbound.GroupChangedPacket;
import dev.lu15.voicechat.network.minecraft.packets.clientbound.GroupCreatedPacket;
import dev.lu15.voicechat.network.minecraft.packets.clientbound.GroupRemovedPacket;
import dev.lu15.voicechat.network.minecraft.packets.clientbound.VoiceStatePacket;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.common.PluginMessagePacket;
import net.minestom.server.utils.PacketSendingUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

public final class GroupManager {

    private final @NotNull Map<UUID, Group> groups = new HashMap<>();

    private final @NotNull MinecraftPacketHandler handler;

    public GroupManager(@NotNull MinecraftPacketHandler handler) {
        this.handler = handler;
    }

    public void register(@NotNull Group group) {
        if (groups.containsKey(group.getId())) throw new IllegalArgumentException("group with id " + group.getId() + " already exists.");
        this.groups.put(group.getId(), group);

        // send creation packet to all players
        if (group.isHidden()) return;
        PluginMessagePacket packet = this.handler.write(new GroupCreatedPacket(group));
        PacketSendingUtils.sendGroupedPacket(
                MinecraftServer.getConnectionManager().getOnlinePlayers(),
                packet,
                player -> player.hasTag(Tags.VOICE_CLIENT)
        );
    }

    public void unregister(@NotNull Group group) {
        if (!groups.containsKey(group.getId())) throw new IllegalArgumentException("group with id " + group.getId() + " does not exist.");
        this.groups.remove(group.getId());

        // remove all players from the group
        for (Player player : group.getPlayers()) this.setGroup(player, null);

        // send removal packet to all players
        Collection<Player> players;
        if (group.isHidden()) players = group.getPlayers();
        else players = MinecraftServer.getConnectionManager().getOnlinePlayers();

        PluginMessagePacket packet = this.handler.write(new GroupChangedPacket(group.getId(), false));
        PacketSendingUtils.sendGroupedPacket(
                players,
                packet,
                player -> player.hasTag(Tags.VOICE_CLIENT)
        );
    }

    public @NotNull @Unmodifiable Map<UUID, Group> getGroups() {
        return Collections.unmodifiableMap(groups);
    }

    public @NotNull Optional<Group> getGroup(@NotNull UUID groupId) {
        return Optional.ofNullable(this.groups.get(groupId));
    }

    public @NotNull Optional<Group> getGroup(@NotNull Player player) {
        VoiceState state = player.getTag(Tags.PLAYER_STATE);
        if (state == null) return Optional.empty();
        UUID group = state.group();
        if (group == null) return Optional.empty();
        return this.getGroup(group);
    }

    public boolean hasGroup(@NotNull UUID groupId) {
        return this.groups.containsKey(groupId);
    }

    // todo: players cannot leave groups properly at the moment, need to look into this
    public void setGroup(@NotNull Player player, @Nullable Group group) {
        VoiceState state = player.getTag(Tags.PLAYER_STATE);
        if (state == null) throw new IllegalStateException("player " + player.getUsername() + " does not have a voice state.");

        Group previousGroup = Optional.ofNullable(state.group())
                .flatMap(this::getGroup)
                .orElse(null);
        if (previousGroup != null) previousGroup.removePlayer(player);

        if (group == null) {
            player.setTag(Tags.PLAYER_STATE, state.withGroup(null));

            // send packet to client
            PluginMessagePacket packet = this.handler.write(new GroupChangedPacket(null, false));
            player.sendPacket(packet);
        } else {
            UUID id = group.getId();
            if (!this.groups.containsKey(id)) throw new IllegalArgumentException("group with id " + id + " does not exist.");
            group.addPlayer(player);
            player.setTag(Tags.PLAYER_STATE, state.withGroup(group.getId()));

            // if the group is hidden, we must create it on the client
            if (group.isHidden()) {
                PluginMessagePacket packet = this.handler.write(new GroupCreatedPacket(group));
                player.sendPacket(packet);
            }

            // send packet to client
            PluginMessagePacket packet = this.handler.write(new GroupChangedPacket(id, false));
            player.sendPacket(packet);
        }

        // send state update to all players
        PluginMessagePacket statePacket = this.handler.write(new VoiceStatePacket(player.getTag(Tags.PLAYER_STATE)));
        PacketSendingUtils.sendGroupedPacket(
                MinecraftServer.getConnectionManager().getOnlinePlayers(),
                statePacket,
                p -> p.hasTag(Tags.PLAYER_STATE)
        );

        if (previousGroup == null) return;

        // if the previous group was hidden, we should remove it on their client
        if (previousGroup.isHidden()) {
            PluginMessagePacket packet = this.handler.write(new GroupRemovedPacket(previousGroup.getId()));
            player.sendPacket(packet);
        }

        // if the previous group is not persistent and has zero players, we can remove it
        if (!previousGroup.isPersistent() && previousGroup.getPlayers().isEmpty()) this.unregister(previousGroup);
    }

    public boolean joinGroup(@NotNull Player player, @NotNull Group group, @Nullable String password) {
        if (!group.validatePassword(password)) {
            // inform client that the password is incorrect
            PluginMessagePacket packet = this.handler.write(new GroupChangedPacket(null, true));
            player.sendPacket(packet);

            return false;
        }

        // otherwise, set the group
        this.setGroup(player, group);

        return true;
    }

}