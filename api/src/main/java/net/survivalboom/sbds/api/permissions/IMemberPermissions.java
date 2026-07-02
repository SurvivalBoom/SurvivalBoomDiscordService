package net.survivalboom.sbds.api.permissions;

import net.survivalboom.sbds.api.database.members.IMemberData;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface IMemberPermissions extends IPermissionsHolder {

    @NotNull IMemberData getMember();

    // HAS GROUP //

    default boolean hasGroup(@NotNull String group) {
        return hasPermission("group." + group, false);
    }

    default boolean hasGroup(@NotNull IGuildPermissionsGroup group) {
        return hasGroup(group.getName());
    }

    default boolean hasGroup(@NotNull IGlobalPermissionGroup group) {
        return hasGroup(group.getName());
    }

    // GET GROUPS //

    @NotNull CompletableFuture<List<IPermissionsHolder>> getMemberGroups();

    @NotNull Map<String, Permission> getPermissionMap();

}
