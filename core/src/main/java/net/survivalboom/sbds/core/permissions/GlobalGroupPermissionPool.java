package net.survivalboom.sbds.core.permissions;

import net.survivalboom.sbds.api.permissions.IGlobalGroupPermissionsPool;
import net.survivalboom.sbds.api.permissions.IGlobalPermissionGroup;
import net.survivalboom.sbds.api.permissions.Permission;
import net.survivalboom.sbds.api.registrations.Registration;
import net.survivalboom.sbds.api.utils.valid.Valid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GlobalGroupPermissionPool extends Valid implements IGlobalGroupPermissionsPool {

    private final GlobalPermissionGroup group;

    protected Registration<IGlobalGroupPermissionsPool> registration;

    private final Map<String, Permission> permissionMap = new HashMap<>();


    public GlobalGroupPermissionPool(@NotNull GlobalPermissionGroup group) {
        this.group = group;
    }


    @Override
    public @NotNull Registration<IGlobalGroupPermissionsPool> getRegistration() {
        return registration;
    }

    @Override
    public @NotNull IGlobalPermissionGroup getGroup() {
        return group;
    }

    //
    // PERMISSIONS
    //

    // ADD //

    @Override
    public void addPermission(@NotNull Permission permission) {

        Objects.requireNonNull(permission, "permission == null");
        checkValid();

        String key = permission.permission();

        permissionMap.put(key, permission);
        group.permissionMap.put(key, permission);

    }

    @Override
    public void addPermissions(@NotNull Collection<Permission> permissions) {

        Objects.requireNonNull(permissions, "permissions == null");
        checkValid();

        permissions.forEach(permission -> {
            permissionMap.put(permission.permission(), permission);
            group.permissionMap.put(permission.permission(), permission);
        });

    }

    @Override
    public void setPermissions(@Nullable Collection<Permission> permissions) {

        checkValid();

        this.permissionMap.forEach((key, value) -> group.permissionMap.remove(key));
        this.permissionMap.clear();

        if (permissions != null) {
            permissions.forEach(permission -> {
                this.permissionMap.put(permission.permission(), permission);
                this.group.permissionMap.put(permission.permission(), permission);
            });
        }

    }

    // REMOVE //

    @Override
    public void removePermission(@NotNull String permission) {

        Objects.requireNonNull(permission, "permission == null");
        checkValid();

        Permission perm = permissionMap.remove(permission);
        if (perm != null) {
            group.permissionMap.remove(permission);
        }

    }

    // GET //

    @Override
    public @Nullable Permission getPermission(@NotNull String permission) {
        checkValid();
        return permissionMap.get(permission);
    }

    @Override
    public @NotNull Map<String, Permission> getPermissions() {
        return new HashMap<>(permissionMap);
    }

}
