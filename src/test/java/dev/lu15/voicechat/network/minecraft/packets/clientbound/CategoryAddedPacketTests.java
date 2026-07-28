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
        String nameTranslationKey = generateString(32);
        String description = generateString(64);
        String descriptionTranslationKey = generateString(32);
        int[][] icon = new int[16][16];
        CategoryAddedPacket packet = new CategoryAddedPacket(id, new Category(name, nameTranslationKey, description, descriptionTranslationKey, icon));

        packet.serializer().write(buffer, packet);

        assertEquals(id, buffer.read(NetworkBuffer.STRING));
        assertEquals(name, buffer.read(NetworkBuffer.STRING));
        assertEquals(nameTranslationKey, buffer.read(NetworkBuffer.STRING.optional()));
        assertEquals(description, buffer.read(NetworkBuffer.STRING.optional()));
        assertEquals(descriptionTranslationKey, buffer.read(NetworkBuffer.STRING.optional()));
        assertArrayEquals(icon, buffer.read(Category.ICON_SERIALIZER.optional()));
    }

    @Test
    public void validNoOptionalStrings() {
        NetworkBuffer buffer = NetworkBuffer.resizableBuffer();

        String id = generateString(16);
        String name = generateString(16);
        int[][] icon = new int[16][16];
        CategoryAddedPacket packet = new CategoryAddedPacket(id, new Category(name, null, null, null, icon));

        packet.serializer().write(buffer, packet);

        assertEquals(id, buffer.read(NetworkBuffer.STRING));
        assertEquals(name, buffer.read(NetworkBuffer.STRING));
        assertEquals(false, buffer.read(NetworkBuffer.BOOLEAN));
        assertEquals(false, buffer.read(NetworkBuffer.BOOLEAN));
        assertEquals(false, buffer.read(NetworkBuffer.BOOLEAN));
        assertArrayEquals(icon, buffer.read(Category.ICON_SERIALIZER.optional()));
    }

    @Test
    public void validNoIcon() {
        NetworkBuffer buffer = NetworkBuffer.resizableBuffer();

        String id = generateString(16);
        String name = generateString(16);
        String description = generateString(64);
        CategoryAddedPacket packet = new CategoryAddedPacket(id, new Category(name, null, description, null, null));

        packet.serializer().write(buffer, packet);

        assertEquals(id, buffer.read(NetworkBuffer.STRING));
        assertEquals(name, buffer.read(NetworkBuffer.STRING));
        assertEquals(false, buffer.read(NetworkBuffer.BOOLEAN));
        assertEquals(description, buffer.read(NetworkBuffer.STRING.optional()));
        assertEquals(false, buffer.read(NetworkBuffer.BOOLEAN));
        assertEquals(false, buffer.read(NetworkBuffer.BOOLEAN));
    }

    @Test
    public void consumesEntireBuffer() {
        NetworkBuffer buffer = NetworkBuffer.resizableBuffer();

        CategoryAddedPacket packet = new CategoryAddedPacket(
                generateString(16),
                new Category(generateString(16), null, generateString(64), null, null)
        );

        packet.serializer().write(buffer, packet);

        buffer.read(NetworkBuffer.STRING);
        buffer.read(Category.NETWORK_TYPE);
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void invalidIdLength() {
        String id = generateString(17);
        Category category = new Category(generateString(16), null, generateString(64), null, new int[16][16]);

        assertThrows(IllegalArgumentException.class, () -> new CategoryAddedPacket(id, category));
    }

    @Test
    public void invalidIdCharacters() {
        String id = generateString(7) + ":" + generateString(8);
        Category category = new Category(generateString(16), null, generateString(64), null, new int[16][16]);

        assertThrows(IllegalArgumentException.class, () -> new CategoryAddedPacket(id, category));
    }

    @Test
    public void invalidName() {
        String id = generateString(16);
        Category category = new Category(generateString(17), null, generateString(64), null, new int[16][16]);

        assertThrows(IllegalArgumentException.class, () -> new CategoryAddedPacket(id, category));
    }

    @Test
    public void invalidDescription() {
        String id = generateString(16);
        Category category = new Category(generateString(16), null, generateString(32768), null, new int[16][16]);

        assertThrows(IllegalArgumentException.class, () -> new CategoryAddedPacket(id, category));
    }

    @Test
    public void invalidNameTranslationKey() {
        String id = generateString(16);
        Category category = new Category(generateString(16), generateString(32768), null, null, new int[16][16]);

        assertThrows(IllegalArgumentException.class, () -> new CategoryAddedPacket(id, category));
    }

    @Test
    public void invalidDescriptionTranslationKey() {
        String id = generateString(16);
        Category category = new Category(generateString(16), null, null, generateString(32768), new int[16][16]);

        assertThrows(IllegalArgumentException.class, () -> new CategoryAddedPacket(id, category));
    }

    @Test
    public void invalidIcon1() {
        String id = generateString(16);
        Category category = new Category(generateString(16), null, generateString(64), null, new int[16][17]);

        assertThrows(IllegalArgumentException.class, () -> new CategoryAddedPacket(id, category));
    }

    @Test
    public void invalidIcon2() {
        String id = generateString(16);
        Category category = new Category(generateString(16), null, generateString(64), null, new int[17][16]);

        assertThrows(IllegalArgumentException.class, () -> new CategoryAddedPacket(id, category));
    }

}
