package dev.lu15.voicechat;

import dev.lu15.voicechat.network.minecraft.Category;
import dev.lu15.voicechat.event.PlayerJoinVoiceChatEvent;
import dev.lu15.voicechat.network.minecraft.Group;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.advancements.FrameType;
import net.minestom.server.advancements.Notification;
import net.minestom.server.command.CommandManager;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.extras.lan.OpenToLAN;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.utils.entity.EntityFinder;
import net.minestom.server.world.DimensionType;

import java.util.Collection;
import java.util.UUID;

import static dev.lu15.voicechat.Codec.VOIP;

public final class TestServer {

    private static VoiceChat voicechat; // Made static

    public static void main(String[] args) {
        MinecraftServer server = MinecraftServer.init();

        DimensionType dimensionType = DimensionType.builder().ambientLight(1f).build();
        RegistryKey<DimensionType> dimension = MinecraftServer.getDimensionTypeRegistry().register(Key.key("test:fullbright"), dimensionType);

        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer(dimension);
        instance.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.GRASS_BLOCK));

        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.setSpawningInstance(instance);
            event.getPlayer().setRespawnPoint(new Pos(0, 40, 0));
        });

        voicechat = VoiceChat.builder("0.0.0.0", 21000) // Assign to static field
                .mtu(2048)
                .codec(VOIP)
                .groups(true)
                .distance(64)
                .keepalive(1000)
                .recording(true)
                .enable();
        voicechat.addCategory(Key.key("voicechat", "test"), new Category("Test", "A test category", null)); // Added default range/suffix for Category

        Notification notification = new Notification(Component.text("Connected to voice chat"), FrameType.GOAL, Material.NOTE_BLOCK);
        MinecraftServer.getGlobalEventHandler().addListener(PlayerJoinVoiceChatEvent.class, event -> {
            final Player player = event.getPlayer();
            player.sendNotification(notification);
        });

        registerCommands(); // Register custom commands

        OpenToLAN.open();
        //MojangAuth.init();

        server.start("0.0.0.0", 20000);
    }

    private static void registerCommands() {
        CommandManager commandManager = MinecraftServer.getCommandManager();

        // Command: /vc_creategroup <name> [password] [persistent:true|false] [hidden:true|false] [type:NORMAL|OPEN|ISOLATED]
        var createGroupCommand = new Command("vc_creategroup");
        var nameArg = ArgumentType.String("name");
        var passwordArg = ArgumentType.String("password").setDefaultValue("");
        var persistentArg = ArgumentType.Boolean("persistent").setDefaultValue(false);
        var hiddenArg = ArgumentType.Boolean("hidden").setDefaultValue(false);
        var typeArg = ArgumentType.Enum("type", Group.Type.class).setDefaultValue(Group.Type.NORMAL); // Assuming Group.Type enum

        createGroupCommand.addSyntax((sender, context) -> {
            String name = context.get(nameArg);
            String password = context.get(passwordArg);
            boolean persistent = context.get(persistentArg);
            boolean hidden = context.get(hiddenArg);
            Group.Type type = context.get(typeArg);
            String actualPassword;
            if (password.isEmpty()) {
                actualPassword = null;
            } else {
                actualPassword = password;
            }

            Group createdGroup = voicechat.createManagedGroup(name, type, actualPassword , persistent, hidden);
            if (createdGroup != null) {
                sender.sendMessage(Component.text("Group created: " + createdGroup.name() + " (ID: " + createdGroup.id() + ")", NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("Failed to create group. Check console for details (e.g., groups disabled, invalid params).", NamedTextColor.RED));
            }
        }, nameArg, passwordArg, persistentArg, hiddenArg, typeArg);
        commandManager.register(createGroupCommand);


        // Command: /vc_removegroup <uuid>
        var removeGroupCommand = new Command("vc_removegroup");
        var groupIdArg = ArgumentType.String("group_uuid");

        removeGroupCommand.addSyntax((sender, context) -> {
            String uuidStr = context.get(groupIdArg);
            try {
                UUID groupId = UUID.fromString(uuidStr);
                if (voicechat.removeManagedGroup(groupId)) {
                    sender.sendMessage(Component.text("Group " + groupId + " removed successfully.", NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("Failed to remove group " + groupId + ". It might not exist or groups might be disabled.", NamedTextColor.RED));
                }
            } catch (IllegalArgumentException e) {
                sender.sendMessage(Component.text("Invalid UUID format: " + uuidStr, NamedTextColor.RED));
            }
        }, groupIdArg);
        commandManager.register(removeGroupCommand);


        // Command: /vc_setplayergroup <player_name> [group_uuid] [password_for_new_group]
        var setPlayerGroupCommand = new Command("vc_setplayergroup");
        var playerArg = ArgumentType.Entity("player_name").singleEntity(true).onlyPlayers(true);
        var targetGroupIdArg = ArgumentType.String("group_uuid").setDefaultValue(""); // Optional: if null, removes from group
        var groupPasswordArg = ArgumentType.String("password_for_new_group").setDefaultValue("");


        setPlayerGroupCommand.addSyntax((sender, context) -> {
            EntityFinder playerFinder = context.get(playerArg);
            Player targetPlayer = playerFinder.findFirstPlayer(sender);

            if (targetPlayer == null) {
                sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                return;
            }

            String targetGroupIdStr = context.get(targetGroupIdArg);
            String passwordForNewGroup = context.get(groupPasswordArg);

            String actualGroupIdStr;
            if (targetGroupIdStr.isEmpty()) {
                actualGroupIdStr = null;
            } else {
                actualGroupIdStr = targetGroupIdStr;
            }

            String actualPassword;
            if (passwordForNewGroup.isEmpty()) {
                actualPassword = null;
            } else {
                actualPassword = passwordForNewGroup;
            }


            UUID targetGroupId = null;

            if (actualGroupIdStr != null) {
                try {
                    targetGroupId = UUID.fromString(actualGroupIdStr);
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(Component.text("Invalid group UUID format: " + actualGroupIdStr, NamedTextColor.RED));
                    return;
                }
            }

            if (voicechat.setPlayerManagedGroup(targetPlayer, targetGroupId, actualPassword)) {
                if (targetGroupId != null) {
                    sender.sendMessage(Component.text("Set " + targetPlayer.getUsername() + "'s group to " + targetGroupId + ".", NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text(targetPlayer.getUsername() + " removed from any group.", NamedTextColor.GREEN));
                }
            } else {
                sender.sendMessage(Component.text("Failed to set " + targetPlayer.getUsername() + "'s group. Check console/params.", NamedTextColor.RED));
            }
        }, playerArg, targetGroupIdArg, groupPasswordArg);
        commandManager.register(setPlayerGroupCommand);

        // Command: /vc_listgroups
        var listGroupsCommand = new Command("vc_listgroups");
        listGroupsCommand.setDefaultExecutor((sender, context) -> {
            Collection<Group> groups = voicechat.getManagedGroups();
            if (groups.isEmpty()) {
                sender.sendMessage(Component.text("No voice chat groups found or groups are disabled.", NamedTextColor.YELLOW));
                return;
            }
            sender.sendMessage(Component.text("Current Voice Chat Groups:", NamedTextColor.GOLD));
            for (Group group : groups) {
                sender.sendMessage(Component.text("- Name: " + group.name() +
                                ", ID: " + group.id() +
                                ", Type: " + group.type() +
                                ", Persistent: " + group.persistent() +
                                ", Hidden: " + group.hidden() +
                                ", Password: " + group.passwordProtected(), // Assuming Group is a record
                        NamedTextColor.AQUA));
            }
        });
        commandManager.register(listGroupsCommand);
    }
}