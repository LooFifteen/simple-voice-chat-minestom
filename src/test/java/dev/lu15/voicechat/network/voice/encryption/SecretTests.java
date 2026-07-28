package dev.lu15.voicechat.network.voice.encryption;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.crypto.AEADBadTagException;
import net.minestom.server.network.NetworkBuffer;
import org.junit.jupiter.api.Test;

public final class SecretTests {
    @Test
    public void serializesAsRawBytes() {
        NetworkBuffer buffer = NetworkBuffer.resizableBuffer();

        Secret secret = Secret.generate();
        buffer.write(Secret.SERIALIZER, secret);

        assertEquals(Secret.LENGTH, buffer.writeIndex());
        assertArrayEquals(secret.bytes(), buffer.read(NetworkBuffer.FixedRawBytes(Secret.LENGTH)));
    }

    @Test
    public void deserializes() {
        NetworkBuffer buffer = NetworkBuffer.resizableBuffer();

        Secret secret = Secret.generate();
        buffer.write(Secret.SERIALIZER, secret);

        assertEquals(secret, buffer.read(Secret.SERIALIZER));
    }

    @Test
    public void roundTripsEncryption() throws Exception {
        Secret secret = Secret.generate();
        byte[] data = "hello, voice chat".getBytes(StandardCharsets.UTF_8);

        byte[] encrypted = secret.encrypt(data);
        assertFalse(Arrays.equals(data, encrypted));

        assertArrayEquals(data, secret.decrypt(encrypted));
    }

    @Test
    public void rejectsWrongSecret() throws Exception {
        byte[] encrypted = Secret.generate().encrypt("hello, voice chat".getBytes(StandardCharsets.UTF_8));
        assertThrows(AEADBadTagException.class, () -> Secret.generate().decrypt(encrypted));
    }

    @Test
    public void rejectsWrongLength() {
        assertThrows(IllegalArgumentException.class, () -> new Secret(new byte[Secret.LENGTH - 1]));
    }
}
