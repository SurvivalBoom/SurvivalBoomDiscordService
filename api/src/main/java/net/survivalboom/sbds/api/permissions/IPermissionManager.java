package net.survivalboom.sbds.api.permissions;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.survivalboom.sbds.api.ISBDS;
import net.survivalboom.sbds.api.modules.IModule;
import net.survivalboom.sbds.api.modules.ModuleMain;
import net.survivalboom.sbds.api.utils.NamespacedKey;
import net.survivalboom.sbds.api.utils.valid.IManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public interface IPermissionManager extends IManager {

    @NotNull ISBDS getSbds();

    //
    // PERMISSION CHECKING
    //

    // HAS PERMISSION //

    boolean hasPermission(long guildId, long userId, @NotNull String permission, boolean allowDefault);

    default boolean hasPermission(long guildId, long userId, @NotNull Permission permission) {
        return hasPermission(guildId, userId, permission.permission(), permission.value());
    }

    default boolean hasPermission(@NotNull Member member, @NotNull Permission permission) {
        return hasPermission(member.getGuild().getIdLong(), member.getUser().getIdLong(), permission);
    }

    default boolean hasPermission(@NotNull Member member, @NotNull String permission, boolean allowDefault) {
        return hasPermission(member.getGuild().getIdLong(), member.getIdLong(), permission, allowDefault);
    }

    default boolean hasPermission(@NotNull Guild guild, @NotNull User user, @NotNull Permission permission) {
        return hasPermission(guild.getIdLong(), user.getIdLong(), permission);
    }

    // PERMISSION MAP //

    @NotNull Map<String, Permission> getMemberPermissionMap(long guildId, long memberId);

    default @NotNull Map<String, Permission> getMemberPermissionMap(@NotNull Guild guild, @NotNull User user) {
        return getMemberPermissionMap(guild.getIdLong(), user.getIdLong());
    }

    default @NotNull Map<String, Permission> getMemberPermissionMap(@NotNull Member member) {
        return getMemberPermissionMap(member.getGuild(), member.getUser());
    }

    //
    // GUILD PERMISSION GROUPS
    //

    // CREATE //

    @NotNull CompletableFuture<IGuildPermissionsGroup> createGuildGroup(long guildId, @NotNull String name);

    default @NotNull CompletableFuture<IGuildPermissionsGroup> createGuildGroup(@NotNull Guild guild, @NotNull String name) {
        return createGuildGroup(guild.getIdLong(), name);
    }

    // DELETE //

    @NotNull CompletableFuture<Void> deleteGuildGroup(@NotNull IGuildPermissionsGroup group);

    // GET //

    @NotNull CompletableFuture<@Nullable IGuildPermissionsGroup> getGuildGroup(long guildId, @NotNull String name);

    default @NotNull CompletableFuture<@Nullable IGuildPermissionsGroup> getGuildGroup(@NotNull Guild guild, @NotNull String name) {
        return getGuildGroup(guild.getIdLong(), name);
    }


    @NotNull CompletableFuture<List<IGuildPermissionsGroup>> getGuildGroups(long guildId);

    default @NotNull CompletableFuture<List<IGuildPermissionsGroup>> getGuildGroups(@NotNull Guild guild) {
        return getGuildGroups(guild.getIdLong());
    }

    // OBTAIN //

    default @NotNull CompletableFuture<IGuildPermissionsGroup> obtainGuildGroup(long guildId, @NotNull String name) {

        return getGuildGroup(guildId, name).thenCompose(group -> {

            if (group != null) {
                return CompletableFuture.completedFuture(group);
            }

            return createGuildGroup(guildId, name);

        });

    }

    default @NotNull CompletableFuture<IGuildPermissionsGroup> obtainGuildGroup(@NotNull Guild guild, @NotNull String name) {
        return obtainGuildGroup(guild.getIdLong(), name);
    }

    //
    // USER PERMISSIONS
    //

    @NotNull CompletableFuture<IMemberPermissions> getMemberPermissions(long guildId, long userId);

    default @NotNull CompletableFuture<IMemberPermissions> getMemberPermissions(@NotNull Member member) {
        return getMemberPermissions(member.getGuild(), member.getUser());
    }

    default @NotNull CompletableFuture<IMemberPermissions> getMemberPermissions(@NotNull Guild guild, @NotNull User user) {
        return getMemberPermissions(guild.getIdLong(), user.getIdLong());
    }

    //
    // GLOBAL GROUPS
    //

    // REG //

    @NotNull IGlobalPermissionGroup createGlobalGroup(@NotNull IModule module, @NotNull String name);

    default @NotNull IGlobalPermissionGroup createGlobalGroup(@NotNull ModuleMain main, @NotNull String name) {
        return createGlobalGroup(main.getModule(), name);
    }

    // UNREG //

    boolean removeGlobalGroup(@NotNull IGlobalPermissionGroup group);

    @Nullable IGlobalPermissionGroup removeGlobalGroup(@NotNull String name);

    // GET //

    @Nullable IGlobalPermissionGroup getGlobalGroup(@NotNull String name);

    @NotNull List<IGlobalPermissionGroup> getGlobalGroups();

    // OBTAIN //

    @NotNull IGlobalPermissionGroup obtainGlobalGroup(@NotNull IModule module, @NotNull String group);

    default @NotNull IGlobalPermissionGroup obtainGlobalGroup(@NotNull ModuleMain module, @NotNull String group) {
        return obtainGlobalGroup(module.getModule(), group);
    }

}
