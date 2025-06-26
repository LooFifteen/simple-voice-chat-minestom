package dev.lu15.voicechat.group;

import dev.lu15.voicechat.network.NetworkTypes;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minestom.server.entity.Player;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

public final class Group {

    public static final @NotNull NetworkBuffer.Type<Group> NETWORK_TYPE = NetworkBufferTemplate.template(
            NetworkBuffer.UUID, Group::getId,
            NetworkBuffer.STRING, Group::getName,
            NetworkBuffer.BOOLEAN, Group::isPasswordProtected,
            NetworkBuffer.BOOLEAN, Group::isPersistent,
            NetworkBuffer.BOOLEAN, Group::isHidden,
            NetworkTypes.ShortEnum(Group.Type.class), Group::getType,
            (a, b, c, d, e, f) -> {
                throw new UnsupportedOperationException("groups cannot be created from a network buffer");
            }
    );

    private final @NotNull UUID id = UUID.randomUUID();
    private final @NotNull Set<Player> players = new HashSet<>();

    private final @NotNull String name;
    private final @NotNull Type type;
    private final @Nullable String password;
    private final boolean persistent;
    private final boolean hidden;

    public static @NotNull Builder.NameProvider<Builder.TypeProvider<Builder>> builder() {
        return name -> {
            if (name.isBlank()) throw new IllegalArgumentException("name cannot be blank");
            if (name.length() > 24) throw new IllegalArgumentException("name cannot be longer than 24 characters");
            return type -> new Builder(name, type);
        };
    }

    private Group(
            @NotNull String name,
            @Nullable String password,
            @NotNull Type type,
            boolean persistent,
            boolean hidden
    ) {
        this.name = name;
        this.password = password;
        this.type = type;
        this.persistent = persistent;
        this.hidden = hidden;
    }

    public @NotNull UUID getId() {
        return id;
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull Type getType() {
        return type;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public boolean isHidden() {
        return hidden;
    }

    public boolean isPasswordProtected() {
        return password != null;
    }

    boolean validatePassword(@Nullable String password) {
        if (this.password == null) return true;
        return this.password.equals(password);
    }

    boolean addPlayer(@NotNull Player player) {
        return this.players.add(player);
    }

    boolean removePlayer(@NotNull Player player) {
        return this.players.remove(player);
    }

    public @NotNull @Unmodifiable Collection<Player> getPlayers() {
        return Collections.unmodifiableCollection(players);
    }

    public enum Type {
        /**
         * Players who are not in your group can't hear you, but you can hear them.
         */
        NORMAL,

        /**
         * Players who are not in your group can hear you, and you can hear them too.
         */
        OPEN,

        /**
         * Players who are not in your group can't hear you, and you can't hear them either.
         */
        ISOLATED
    }

    public static final class Builder {

        private final @NotNull String name;
        private final @NotNull Type type;

        private @Nullable String password;
        private boolean persistent = false;
        private boolean hidden = false;

        public interface NameProvider<T> {
            T name(@NotNull String name);
        }

        public interface TypeProvider<T> {
            T type(@NotNull Type type);
        }

        private Builder(
                @NotNull String name,
                @NotNull Type type
        ) {
            this.name = name;
            this.type = type;
        }

        public Builder password(@Nullable String password) {
            if (password != null && password.isBlank()) throw new IllegalArgumentException("password cannot be blank");
            this.password = password;
            return this;
        }

        public Builder persistent() {
            this.persistent = true;
            return this;
        }

        public Builder hidden() {
            this.hidden = true;
            return this;
        }

        public @NotNull Group build() {
            return new Group(
                    this.name,
                    this.password,
                    this.type,
                    this.persistent,
                    this.hidden
            );
        }

    }

}
