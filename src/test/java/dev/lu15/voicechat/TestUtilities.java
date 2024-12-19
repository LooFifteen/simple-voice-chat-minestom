package dev.lu15.voicechat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

public final class TestUtilities {

    private static final @NotNull Random RANDOM = new Random();

    private TestUtilities() {}

    public static @NotNull String generateString(int length) {
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'

        return RANDOM.ints(leftLimit, rightLimit + 1)
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    @Test
    public void testGenerateString() {
        for (int i = 0; i < 100; i++) {
            String generated = generateString(i);
            assertEquals(i, generated.length());
        }
    }

}
