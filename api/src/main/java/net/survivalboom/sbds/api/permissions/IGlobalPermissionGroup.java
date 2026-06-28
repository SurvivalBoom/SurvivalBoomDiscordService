package net.survivalboom.sbds.api.permissions;

import net.survivalboom.sbds.api.modules.IModule;
import net.survivalboom.sbds.api.modules.ModuleMain;
import net.survivalboom.sbds.api.registrations.Registration;
import net.survivalboom.sbds.api.utils.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface IGlobalPermissionGroup extends IPermissionsHolder {

    @NotNull IPermissionManager getManager();

    @NotNull Registration<IGlobalPermissionGroup> getRegistration();

    //
    // POOLS
    //

    // CREATE //

    @NotNull IGlobalGroupPermissionsPool createPool(@NotNull IModule module, @NotNull String name);

    default @NotNull IGlobalGroupPermissionsPool createPool(@NotNull ModuleMain module, @NotNull String name) {
        return createPool(module.getModule(), name);
    }

    // REMOVE //

    void removePool(@NotNull IGlobalGroupPermissionsPool pool);

    default @NotNull IGlobalGroupPermissionsPool removePool(@NotNull NamespacedKey key) {

        var pool = getPool(key);
        if (pool == null) {
            throw new IllegalArgumentException("No pool with key `" + key + "` was found");
        }

        removePool(pool);

        return pool;

    }

    default @NotNull IGlobalGroupPermissionsPool removePool(@NotNull String key) {
        return removePool(NamespacedKey.fromString(key));
    }

    // GET //

    @Nullable IGlobalGroupPermissionsPool getPool(@NotNull NamespacedKey key);

    default @Nullable IGlobalGroupPermissionsPool getPool(@NotNull String key) {
        return getPool(NamespacedKey.fromString(key));
    }

    @NotNull List<IGlobalGroupPermissionsPool> getPools();

    // OBTAIN //

    default @NotNull IGlobalGroupPermissionsPool obtainPool(@NotNull IModule module, @NotNull String name) {

        var pool = getPool(NamespacedKey.fromModule(module, name));
        if (pool != null) {
            return pool;
        }

        return createPool(module, name);

    }

    default @NotNull IGlobalGroupPermissionsPool obtainPool(@NotNull ModuleMain module, @NotNull String name) {
        return obtainPool(module.getModule(), name);
    }

}
