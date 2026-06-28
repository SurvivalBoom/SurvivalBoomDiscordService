package net.survivalboom.sbds.api.permissions;

import net.survivalboom.sbds.api.registrations.Registration;
import net.survivalboom.sbds.api.utils.valid.IValid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IGlobalGroupPermissionsPool extends IValid {

    @NotNull Registration<IGlobalGroupPermissionsPool> getRegistration();

    @NotNull IGlobalPermissionGroup getGroup();

    //
    // PERMISSIONS
    //

    // ADD //

    void addPermission(@NotNull Permission permission);

    default void addPermission(@NotNull String permission, boolean value) {
        addPermission(new Permission(permission, value));
    }

    void addPermissions(@NotNull Collection<Permission> permissions);

    void setPermissions(@Nullable Collection<Permission> permissions);

    // REMOVE //

    void removePermission(@NotNull String permission);

    default void removePermission(@NotNull Permission permission) {
        removePermission(permission.permission());
    }

    // GETTERS //

    @Nullable Permission getPermission(@NotNull String permission);

    @NotNull Map<String, Permission> getPermissions();

}
