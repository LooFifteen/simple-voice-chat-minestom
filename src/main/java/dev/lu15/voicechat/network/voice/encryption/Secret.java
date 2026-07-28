package dev.lu15.voicechat.network.voice.encryption;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

public final class Secret {

    public static final int LENGTH = 16;

    public static final @NotNull NetworkBuffer.Type<Secret> SERIALIZER =
            NetworkBuffer.FixedRawBytes(LENGTH).transform(Secret::new, Secret::bytes);

    private static final @NotNull Random RANDOM = new SecureRandom();
    private static final @NotNull String CIPHER = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final byte @NotNull[] bytes;
    private final @NotNull SecretKeySpec key;

    public Secret(byte @NotNull[] bytes) {
        if (bytes.length != LENGTH) throw new IllegalArgumentException("secret must be " + LENGTH + " bytes");
        this.bytes = bytes.clone();
        this.key = new SecretKeySpec(this.bytes, "AES");
    }

    public static @NotNull Secret generate() {
        byte[] bytes = new byte[LENGTH];
        RANDOM.nextBytes(bytes);
        return new Secret(bytes);
    }

    public byte @NotNull[] bytes() {
        return this.bytes.clone();
    }

    public byte @NotNull[] encrypt(byte @NotNull[] data) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        byte[] iv = new byte[IV_LENGTH_BYTES];
        RANDOM.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(CIPHER);
        cipher.init(Cipher.ENCRYPT_MODE, this.key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));

        byte[] encrypted = cipher.doFinal(data);
        byte[] result = new byte[iv.length + encrypted.length];

        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
        return result;
    }

    public byte @NotNull[] decrypt(byte @NotNull[] payload) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        if (payload.length < IV_LENGTH_BYTES) throw new IllegalArgumentException("payload too short");

        byte[] iv = Arrays.copyOfRange(payload, 0, IV_LENGTH_BYTES);
        byte[] data = Arrays.copyOfRange(payload, IV_LENGTH_BYTES, payload.length);

        Cipher cipher = Cipher.getInstance(CIPHER);
        cipher.init(Cipher.DECRYPT_MODE, this.key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));

        return cipher.doFinal(data);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Secret secret)) return false;
        return Arrays.equals(this.bytes, secret.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.bytes);
    }

}
