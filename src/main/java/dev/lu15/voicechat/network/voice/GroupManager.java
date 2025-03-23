package dev.lu15.voicechat.network.voice;

import dev.lu15.voicechat.network.minecraft.Group;
import net.minestom.server.entity.Player;

import java.util.Collection;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GroupManager {

    private final Map<UUID, Group> groups = new HashMap<>();
    private final Map<Player, UUID> playerGroups = new HashMap<>();
    private final Map<UUID, ArrayList<Player>> groupPlayers = new HashMap<>();
    private final Map<UUID, String> groupPassword = new HashMap<>();

    public Group getGroup(Player player) {
        return groups.get(playerGroups.get(player));
    }

    public Group getGroup(UUID group) {
        return groups.get(group);
    }

    public Collection<Group> getGroups() {
        return groups.values();
    }

    public ArrayList<Player> getPlayers(Group group) {
        return groupPlayers.get(group.id());
    }

    public ArrayList<Player> getPlayers(UUID group) {
        return groupPlayers.get(group);
    }

    public boolean hasGroup(UUID group) {
        return groups.containsKey(group);
    }

    public boolean hasGroup(Group group) {
        return groups.containsValue(group);
    }

    public String getPassword(Group group) {
        return groupPassword.get(group.id());
    }

    public String getPassword(UUID group) {
        return groupPassword.get(group);
    }

    public void createGroup(Group group, String password) {
        groupPassword.put(group.id(), password);
        groups.put(group.id(), group);
        groupPlayers.put(group.id(), new ArrayList<>());
    }

    public void setGroup(Player player, Group group) {
        ArrayList<Player> players = groupPlayers.get(group.id());
        players.add(player);
        groupPlayers.put(group.id(), players);
        playerGroups.put(player, group.id());
    }

    public void setGroup(Player player, UUID group) {
        ArrayList<Player> players = groupPlayers.get(group);
        players.add(player);
        groupPlayers.put(group, players);
        playerGroups.put(player, group);
    }

    public void leaveGroup(Player player) {
        groupPlayers.get(playerGroups.get(player)).remove(player);
        playerGroups.remove(player);
    }

    public void removeGroup(Group group) {
        groups.remove(group.id());
        groupPassword.remove(group.id());
    }

    public void removeGroup(UUID group) {
        groups.remove(group);
        groupPassword.remove(group);
    }

}
