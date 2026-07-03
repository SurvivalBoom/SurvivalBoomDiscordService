package net.survivalboom.sbds.core.permissions;

import net.survivalboom.sbds.api.permissions.IPermissionManager;
import net.survivalboom.sbds.api.permissions.IPermissionsHolder;
import net.survivalboom.sbds.api.permissions.Permission;
import net.survivalboom.sbds.api.utils.valid.Valid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractPermissionHolder extends Valid implements IPermissionsHolder {

    protected final PermissionManager manager;

    protected final Map<String, Permission> permissionMap = new HashMap<>();

    protected int weight = 0;


    public AbstractPermissionHolder(@NotNull PermissionManager manager) {
        this.manager = manager;
    }

    @Override
    public @NotNull IPermissionManager getManager() {
        return manager;
    }

    @Override
    public int getWeight() {
        return weight;
    }

    @Override
    public void setWeight(int weight) {
        this.weight = weight;
        save();
    }

    //
    // PERMISSION
    //

    // HAS PERMISSION //

    @Override
    public boolean hasPermission(@NotNull String permission, boolean defaultAllow) {

        Objects.requireNonNull(permission, "permission == null");
        checkValid();

        Permission perm = permissionMap.get(permission);
        if (perm == null) {
            return defaultAllow;
        }

        return perm.value();

    }

    @Override
    public boolean hasPermission(@NotNull Permission permission) {
        return hasPermission(permission.permission(), !permission.value());
    }

    // SET PERMISSION //

    @Override
    public void setPermission(@NotNull Permission permission) {

        Objects.requireNonNull(permission, "permission == null");
        checkValid();

        permissionMap.put(permission.permission(), permission);
        save();

    }

    @Override
    public @NotNull Permission setPermission(@NotNull String permission, boolean value) {
        Permission permission0 = new Permission(permission, value);
        setPermission(permission0);
        return permission0;
    }

    @Override
    public void setPermissions(@Nullable Map<String, @Nullable Permission> permissions, boolean override) {

        checkValid();

        if (permissions == null) {
            permissionMap.clear();
            save();
            return;
        }

        for (var entry : permissions.entrySet()) {

            String key = entry.getKey();
            Permission perm = entry.getValue();

            if (perm != null && override) {
                permissionMap.put(key, perm);
            }

            else if (perm == null && override) {
                permissionMap.remove(key);
            }

        }

        save();

    }

    @Override
    public void setPermissions(@Nullable Collection<Permission> permissions, boolean override) {

        if (permissions == null) {
            setPermissions((Map<String, Permission>) null, override);
            return;
        }

        Map<String, Permission> map = permissions.stream().collect(Collectors.toMap(Permission::permission, p -> p));
        setPermissions(map, override);

    }

    // REMOVE //

    @Override
    public void removePermission(@NotNull Permission permission) {

        Objects.requireNonNull(permission, "permission == null");
        checkValid();

        permissionMap.remove(permission.permission());
        save();

    }

    @Override
    public void removePermission(@NotNull String permission) {
        checkValid();
        permissionMap.remove(permission);
        save();
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

    @Override
    public @NotNull List<Permission> getPermissionList() {
        return new ArrayList<>(permissionMap.values());
    }

    @Override
    public int getPermissionsCount() {
        return permissionMap.size();
    }

    // LIVECYCLE //

    protected abstract void save();

    @Override
    protected void setValid(boolean v) {

        if (v) {
            return;
        }

        permissionMap.clear();

    }

}
