package net.survivalboom.sbds.api.utils.container;

import net.survivalboom.sbds.api.database.IDatabase;
import net.survivalboom.sbds.api.utils.NamespacedKey;
import net.survivalboom.sbds.api.utils.valid.Valid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.jackson.JacksonConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.*;

public class NamespacedDataContainer extends Valid implements INamespacedDataContainer {

    private final ConfigurationNode map = IDatabase.createConfigurateLoader().build().createNode();

    // CREATE //

    @Override
    public @NotNull ConfigurationNode createNode(@NotNull NamespacedKey key) {

        checkValid();

        if (map.hasChild(key)) {
            throw new IllegalStateException("Data with key `" + key + "` already exists");
        }

        return map.node(key);

    }

    // REMOVE //

    @Override
    public @Nullable ConfigurationNode removeNode(@NotNull NamespacedKey key) {

        checkValid();

        ConfigurationNode node = getNode(key).orElse(null);
        if (node == null) {
            return null;
        }

        try {
            node.set(null);
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }

        return node;

    }

    // GET //

    @Override
    public @NotNull Optional<ConfigurationNode> getNode(@NotNull NamespacedKey key) {

        checkValid();

        ConfigurationNode node = map.node(key);
        if (node.virtual()) {
            return Optional.empty();
        }

        return Optional.of(node);

    }

    // OBTAIN //

    @Override
    public @NotNull ConfigurationNode obtainNode(@NotNull NamespacedKey key) {
        checkValid();
        return Objects.requireNonNullElseGet(getNode(key).orElse(null), () -> createNode(key));
    }

    @Override
    public boolean hasNode(@NotNull NamespacedKey key) {
        checkValid();
        return map.hasChild(key);
    }

    // MISC //

    @Override
    public @NotNull Map<NamespacedKey, ConfigurationNode> getAsMap() {

        Map<NamespacedKey, ConfigurationNode> map = new HashMap<>();
        for (var entry : this.map.childrenMap().entrySet()) {

            NamespacedKey key = (NamespacedKey) entry.getKey();
            ConfigurationNode node = entry.getValue();

            map.put(key, node);

        }

        return map;

    }

}
