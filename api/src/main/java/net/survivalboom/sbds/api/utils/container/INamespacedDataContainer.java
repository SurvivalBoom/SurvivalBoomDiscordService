package net.survivalboom.sbds.api.utils.container;

import net.survivalboom.sbds.api.modules.IModule;
import net.survivalboom.sbds.api.modules.ModuleMain;
import net.survivalboom.sbds.api.utils.NamespacedKey;
import net.survivalboom.sbds.api.utils.valid.IValid;
import org.checkerframework.checker.units.qual.N;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.Map;
import java.util.Optional;

public interface INamespacedDataContainer extends IValid {

    //
    // NODES
    //

    // CREATE //

    @NotNull ConfigurationNode createNode(@NotNull NamespacedKey key);

    default @NotNull ConfigurationNode createNode(@NotNull String key) {
        return createNode(NamespacedKey.fromString(key));
    }

    default @NotNull ConfigurationNode createNode(@NotNull IModule module, @NotNull String key) {
        return createNode(NamespacedKey.fromModule(module, key));
    }

    default @NotNull ConfigurationNode createNode(@NotNull ModuleMain main, @NotNull String key) {
        return createNode(NamespacedKey.fromModule(main, key));
    }

    // REMOVE //

    @Nullable ConfigurationNode removeNode(@NotNull NamespacedKey key);

    default @Nullable ConfigurationNode removeNode(@NotNull String key) {
        return removeNode(NamespacedKey.fromString(key));
    }

    default @Nullable ConfigurationNode removeNode(@NotNull IModule module, @NotNull String key) {
        return removeNode(NamespacedKey.fromModule(module, key));
    }

    default @Nullable ConfigurationNode removeNode(@NotNull ModuleMain main, @NotNull String key) {
        return removeNode(NamespacedKey.fromModule(main, key));
    }

    // get //

    @NotNull Optional<ConfigurationNode> getNode(@NotNull NamespacedKey key);

    default @NotNull Optional<ConfigurationNode> getNode(@NotNull String key) {
        return getNode(NamespacedKey.fromString(key));
    }

    default @NotNull Optional<ConfigurationNode> getNode(@NotNull IModule module, @NotNull String key) {
        return getNode(NamespacedKey.fromModule(module, key));
    }

    default @NotNull Optional<ConfigurationNode> getNode(@NotNull ModuleMain main, @NotNull String key) {
        return getNode(NamespacedKey.fromModule(main, key));
    }

    // obtain //

    @NotNull ConfigurationNode obtainNode(@NotNull NamespacedKey key);

    default @NotNull ConfigurationNode obtainNode(@NotNull String key) {
        return obtainNode(NamespacedKey.fromString(key));
    }

    default @NotNull ConfigurationNode obtainNode(@NotNull IModule module, @NotNull String key) {
        return obtainNode(NamespacedKey.fromModule(module, key));
    }

    default @NotNull ConfigurationNode obtainNode(@NotNull ModuleMain main, @NotNull String key) {
        return obtainNode(NamespacedKey.fromModule(main, key));
    }

    // HAS //

    boolean hasNode(@NotNull NamespacedKey key);

    default boolean hasNode(@NotNull String key) {
        return hasNode(NamespacedKey.fromString(key));
    }

    default boolean hasNode(@NotNull IModule module, @NotNull String key) {
        return hasNode(NamespacedKey.fromModule(module, key));
    }

    default boolean hasNode(@NotNull ModuleMain module, @NotNull String key) {
        return hasNode(NamespacedKey.fromModule(module, key));
    }

    //
    // MISC
    //

    @NotNull Map<NamespacedKey, ConfigurationNode> getAsMap();

}
