package dev.lu15.voicechat.network.minecraft.packets.clientbound;

import static dev.lu15.voicechat.TestUtilities.generateString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.minestom.server.network.NetworkBuffer;
import org.junit.jupiter.api.Test;

public final class CategoryRemovedPacketTests {

    @Test
    public void valid() {
        NetworkBuffer buffer = NetworkBuffer.resizableBuffer();

        String category = generateString(16);
        CategoryRemovedPacket packet = new CategoryRemovedPacket(category);

        packet.serializer().write(buffer, packet);

        assertEquals(category, buffer.read(NetworkBuffer.STRING));
    }

    @Test
    public void invalid() {
        String category = generateString(17);
        assertThrows(IllegalArgumentException.class, () -> new CategoryRemovedPacket(category));
    }

}
