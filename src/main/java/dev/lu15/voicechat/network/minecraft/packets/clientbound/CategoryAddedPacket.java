package dev.lu15.voicechat.network.minecraft.packets.clientbound;

import dev.lu15.voicechat.VoiceChat;
import dev.lu15.voicechat.network.minecraft.Category;
import dev.lu15.voicechat.network.minecraft.Packet;
import java.util.regex.Pattern;
import net.kyori.adventure.key.Key;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record CategoryAddedPacket(@NotNull String identifier, @NotNull Category category) implements Packet<CategoryAddedPacket> {

    public static final @NotNull Key IDENTIFIER = VoiceChat.key("add_category");
    public static final @NotNull NetworkBuffer.Type<CategoryAddedPacket> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.STRING, CategoryAddedPacket::identifier,
            Category.NETWORK_TYPE, CategoryAddedPacket::category,
            CategoryAddedPacket::new
    );
    public static final @NotNull Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-z_]{1,16}$");

    public CategoryAddedPacket(@NotNull Key identifier, @NotNull Category category) {
        this(identifier.toString().replace(':', '_'), category);
    }

    public CategoryAddedPacket {
        if (!IDENTIFIER_PATTERN.matcher(identifier).matches()) throw new IllegalArgumentException("category id does not match pattern " + IDENTIFIER_PATTERN.pattern());
        if (category.name().length() > 16) throw new IllegalArgumentException("category name is too long, found " + category.name().length() + " characters, maximum is 16");
        checkOptionalLength("name translation key", category.nameTranslationKey());
        checkOptionalLength("description", category.description());
        checkOptionalLength("description translation key", category.descriptionTranslationKey());
        if (category.icon() != null) {
            if (category.icon().length != 16) throw new IllegalArgumentException("category icon is not 16x16, found " + category.icon().length + "x" + category.icon().length);
            for (int[] row : category.icon()) {
                if (row.length != 16) throw new IllegalArgumentException("category icon is not 16x16, found " + row.length + "x16");
            }
        }
    }

    private static void checkOptionalLength(@NotNull String field, @Nullable String value) {
        if (value != null && value.length() > 32767) throw new IllegalArgumentException("category " + field + " is too long, found " + value.length() + " characters, maximum is 32767");
    }

    @Override
    public @NotNull Key id() {
        return IDENTIFIER;
    }

    @Override
    public NetworkBuffer.@NotNull Type<CategoryAddedPacket> serializer() {
        return SERIALIZER;
    }

}
