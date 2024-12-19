package dev.lu15.voicechat.network.minecraft.packets.clientbound;

import static dev.lu15.voicechat.TestUtilities.generateString;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.lu15.voicechat.network.minecraft.Category;
import net.minestom.server.network.NetworkBuffer;
import org.junit.jupiter.api.Test;

public final class CategoryAddedPacketTests {

    @Test
    public void valid() {
        NetworkBuffer buffer = NetworkBuffer.resizableBuffer();

        String id = generateString(16);
        String name = generateString(16);
        String description = generateString(64);
        int[][] icon = new int[16][16];
        CategoryAddedPacket packet = new CategoryAddedPacket(id, new Category(name, description, icon));

        packet.serializer().write(buffer, packet);

        assertEquals(id, buffer.read(NetworkBuffer.STRING));
        assertEquals(name, buffer.read(NetworkBuffer.STRING));
        assertEquals(description, buffer.read(NetworkBuffer.STRING.optional()));
        assertArrayEquals(icon, buffer.read(Category.ICON_SERIALIZER.optional()));
    }

    @Test
    public void validNoDescription() {
        NetworkBuffer buffer = NetworkBuffer.resizableBuffer();

        String id = generateString(16);
        String name = generateString(16);
        int[][] icon = new int[16][16];
        CategoryAddedPacket packet = new CategoryAddedPacket(id, new Category(name, null, icon));

        packet.serializer().write(buffer, packet);

        assertEquals(id, buffer.read(NetworkBuffer.STRING));
        assertEquals(name, buffer.read(NetworkBuffer.STRING));
        assertEquals(false, buffer.read(NetworkBuffer.BOOLEAN));
        assertArrayEquals(icon, buffer.read(Category.ICON_SERIALIZER.optional()));
    }

    @Test
    public void validNoIcon() {
        NetworkBuffer buffer = NetworkBuffer.resizableBuffer();

        String id = generateString(16);
        String name = generateString(16);
        String description = generateString(64);
        CategoryAddedPacket packet = new CategoryAddedPacket(id, new Category(name, description, null));

        packet.serializer().write(buffer, packet);

        assertEquals(id, buffer.read(NetworkBuffer.STRING));
        assertEquals(name, buffer.read(NetworkBuffer.STRING));
        assertEquals(description, buffer.read(NetworkBuffer.STRING.optional()));
        assertEquals(false, buffer.read(NetworkBuffer.BOOLEAN));
    }

    @Test
    public void invalidIdLength() {
        String id = generateString(17);
        String name = generateString(16);
        String description = generateString(64);
        int[][] icon = new int[16][16];

        assertThrows(IllegalArgumentException.class, () -> new CategoryAddedPacket(id, new Category(name, description, icon)));
    }

    @Test
    public void invalidIdCharacters() {
        String id = generateString(7) + ":" + generateString(8);
        String name = generateString(16);
        String description = generateString(64);
        int[][] icon = new int[16][16];

        assertThrows(IllegalArgumentException.class, () -> new CategoryAddedPacket(id, new Category(name, description, icon)));
    }

    @Test
    public void invalidName() {
        String id = generateString(16);
        String name = generateString(17);
        String description = generateString(64);
        int[][] icon = new int[16][16];

        assertThrows(IllegalArgumentException.class, () -> new CategoryAddedPacket(id, new Category(name, description, icon)));
    }

    @Test
    public void invalidDescription() {
        String id = generateString(16);
        String name = generateString(16);
        String description = generateString(32768);
        int[][] icon = new int[16][16];

        assertThrows(IllegalArgumentException.class, () -> new CategoryAddedPacket(id, new Category(name, description, icon)));
    }

    @Test
    public void invalidIcon1() {
        String id = generateString(16);
        String name = generateString(16);
        String description = generateString(64);
        int[][] icon = new int[16][17];

        assertThrows(IllegalArgumentException.class, () -> new CategoryAddedPacket(id, new Category(name, description, icon)));
    }

    @Test
    public void invalidIcon2() {
        String id = generateString(16);
        String name = generateString(16);
        String description = generateString(64);
        int[][] icon = new int[17][16];

        assertThrows(IllegalArgumentException.class, () -> new CategoryAddedPacket(id, new Category(name, description, icon)));
    }

}
