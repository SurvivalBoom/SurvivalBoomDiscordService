package net.survivalboom.sbds.api.permissions;

import net.survivalboom.sbds.api.utils.valid.IValid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IPermissionsHolder extends IValid {

    @NotNull IPermissionManager getManager();

    @NotNull String getName();

    int getWeight();

    void setWeight(int weight);

    //
    // PERMISSIONS
    //

    // HAS PERMISSION //

    boolean hasPermission(@NotNull String permission, boolean defaultAllow);

    boolean hasPermission(@NotNull Permission permission);

    // SET PERMISSIONS //

    void setPermission(@NotNull Permission permission);

    @NotNull Permission setPermission(@NotNull String permission, boolean value);

    void setPermissions(@Nullable Map<String, @Nullable Permission> permissions, boolean override);

    void setPermissions(@Nullable Collection<Permission> permissions, boolean override);

    // REMOVE PERMISSIONS //

    void removePermission(@NotNull String permission);

    void removePermission(@NotNull Permission permission);

    // GET PERMISSIONS //

    @Nullable Permission getPermission(@NotNull String permission);

    @NotNull Map<String, Permission> getPermissions();

    @NotNull List<Permission> getPermissionList();

    int getPermissionsCount();

}
