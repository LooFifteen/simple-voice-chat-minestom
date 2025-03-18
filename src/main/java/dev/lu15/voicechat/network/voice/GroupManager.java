package dev.lu15.voicechat.network.voice;

import dev.lu15.voicechat.network.minecraft.Group;
import net.minestom.server.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class GroupManager {

    public HashMap<UUID, Group> groups = new HashMap<>();
    public HashMap<Player, UUID> playerGroups = new HashMap<>();
    public HashMap<UUID, ArrayList<Player>> groupPlayers = new HashMap<>();
    public HashMap<UUID, String> groupPassword = new HashMap<>();

    public Group getGroup(Player player) {
        return groups.get(playerGroups.get(player));
    }

    public Group getGroup(UUID group) {
        return groups.get(group);
    }

    public ArrayList<Player> getPlayers(Group group) {
        return groupPlayers.get(group.id());
    }

    public ArrayList<Player> getPlayers(UUID group) {
        return groupPlayers.get(group);
    }

}
