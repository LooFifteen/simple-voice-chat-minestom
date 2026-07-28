package dev.lu15.voicechat;

import dev.lu15.voicechat.network.minecraft.Category;
import dev.lu15.voicechat.event.PlayerJoinVoiceChatEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.advancements.FrameType;
import net.minestom.server.advancements.Notification;
import net.minestom.server.color.Color;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.extras.lan.OpenToLAN;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;
import net.minestom.server.world.attribute.EnvironmentAttribute;

public final class TestServer {

    public static void main(String[] args) {
        MinecraftServer server = MinecraftServer.init(new Auth.Online());

        DimensionType dimensionType = DimensionType.builder()
                .ambientLight(1f)
                .setAttribute(EnvironmentAttribute.AMBIENT_LIGHT_COLOR, Color.WHITE)
                .build();
        RegistryKey<DimensionType> dimension = MinecraftServer.getDimensionTypeRegistry().register(Key.key("test", "fullbright"), dimensionType);

        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer(dimension);
        instance.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.GRASS_BLOCK));

        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.setSpawningInstance(instance);
            event.getPlayer().setRespawnPoint(new Pos(0, 40, 0));
        });

        VoiceChat voicechat = VoiceChat.builder("0.0.0.0", 25565).enable();
        voicechat.addCategory(VoiceChat.key("test"), new Category("Test", null, "A test category", null, null));

        Notification notification = new Notification(Component.text("Connected to voice chat"), FrameType.GOAL, Material.NOTE_BLOCK);
        MinecraftServer.getGlobalEventHandler().addListener(PlayerJoinVoiceChatEvent.class, event -> {
            final Player player = event.getPlayer();
            player.sendNotification(notification);
        });

        OpenToLAN.open();

        server.start("0.0.0.0", 25565);
    }

}
