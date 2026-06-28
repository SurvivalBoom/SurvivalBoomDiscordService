package net.survivalboom.sbds.api.permissions;

import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public interface IMemberPermissions extends IPermissionsHolder {

    @NotNull Member getMember();

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

}
