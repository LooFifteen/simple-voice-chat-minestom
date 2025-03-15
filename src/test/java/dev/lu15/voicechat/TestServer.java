package dev.lu15.voicechat;

import dev.lu15.voicechat.network.minecraft.Category;
import dev.lu15.voicechat.event.PlayerJoinVoiceChatEvent;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.advancements.FrameType;
import net.minestom.server.advancements.Notification;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.extras.lan.OpenToLAN;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.world.DimensionType;

public final class TestServer {

    public static void main(String[] args) {
        MinecraftServer server = MinecraftServer.init();

        DimensionType dimensionType = DimensionType.builder().ambientLight(1f).build();
        DynamicRegistry.Key<DimensionType> dimension = MinecraftServer.getDimensionTypeRegistry().register(NamespaceID.from("test:fullbright"), dimensionType);

        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer(dimension);
        instance.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.GRASS_BLOCK));

        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.setSpawningInstance(instance);
            event.getPlayer().setRespawnPoint(new Pos(0, 40, 0));
        });

        VoiceChat voicechat = VoiceChat.builder("0.0.0.0", 21000)
                .setMTU(2048)
                .enable();
        voicechat.addCategory(NamespaceID.from("voicechat", "test"), new Category("Test", "A test category", null));

        Notification notification = new Notification(Component.text("Connected to voice chat"), FrameType.GOAL, Material.NOTE_BLOCK);
        MinecraftServer.getGlobalEventHandler().addListener(PlayerJoinVoiceChatEvent.class, event -> {
            final Player player = event.getPlayer();
            player.sendNotification(notification);
        });

        OpenToLAN.open();
        //MojangAuth.init();

        server.start("0.0.0.0", 20000);
    }

}
