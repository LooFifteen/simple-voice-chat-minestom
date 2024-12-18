package dev.lu15.voicechat.network.voice.encryption;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;
import java.util.UUID;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

public final class AES {

    private AES() {}

    private static final @NotNull Random RANDOM = new SecureRandom();
    private static final @NotNull String CIPHER = "AES/CBC/PKCS5Padding";
    private static final int UUID_LENGTH = 16;

    public static byte @NotNull[] getBytesFromUuid(@NotNull UUID uuid) {
        NetworkBuffer buffer = NetworkBuffer.staticBuffer(UUID_LENGTH);
        buffer.write(NetworkBuffer.UUID, uuid);
        byte[] bytes = new byte[UUID_LENGTH];
        buffer.copyTo(0, bytes, 0, UUID_LENGTH);
        return bytes;
    }

    private static byte @NotNull[] generateIv() {
        byte[] iv = new byte[UUID_LENGTH];
        RANDOM.nextBytes(iv);
        return iv;
    }

    private static @NotNull SecretKeySpec createKey(@NotNull UUID secret) {
        return new SecretKeySpec(getBytesFromUuid(secret), "AES");
    }

    public static byte @NotNull[] encrypt(@NotNull UUID secret, byte @NotNull[] data) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        byte[] iv = generateIv();
        IvParameterSpec spec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance(CIPHER);
        cipher.init(Cipher.ENCRYPT_MODE, createKey(secret), spec);

        byte[] encrypted = cipher.doFinal(data);
        byte[] result = new byte[iv.length + encrypted.length];

        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
        return result;
    }

    public static byte @NotNull[] decrypt(@NotNull UUID secret, byte @NotNull[] result) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        byte[] iv = new byte[UUID_LENGTH];
        System.arraycopy(result, 0, iv, 0, iv.length);

        byte[] data = new byte[result.length - iv.length];
        System.arraycopy(result, iv.length, data, 0, data.length);

        IvParameterSpec spec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance(CIPHER);
        cipher.init(Cipher.DECRYPT_MODE, createKey(secret), spec);

        return cipher.doFinal(data);
    }

}
