package net.survivalboom.sbds.core.permissions;

import net.dv8tion.jda.api.entities.Member;
import net.survivalboom.sbds.api.permissions.*;
import net.survivalboom.sbds.api.utils.CommonUtils;
import net.survivalboom.sbds.api.utils.valid.Valid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class MemberPermissions extends AbstractPermissionHolder implements IMemberPermissions {

    private final PermissionManager manager;

    private final Member member;


    public MemberPermissions(@NotNull Member member, @NotNull PermissionManager manager) {
        super(manager);
        this.member = member;
        this.manager = manager;
        permissionMap.put("group.default", new Permission("group.default", true)); // Додаємо стандартну групу для усіх.
    }

    @Override
    public @NotNull IPermissionManager getManager() {
        return manager;
    }

    @Override
    public @NotNull Member getMember() {
        return member;
    }

    @Override
    public @NotNull String getName() {
        return member.getEffectiveName();
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull CompletableFuture<List<IPermissionsHolder>> getMemberGroups() {

        checkValid();

        List<CompletableFuture<IPermissionsHolder>> futures = new ArrayList<>();
        for (String perm : getPermissions().keySet()) {

            if (!perm.startsWith("group.")) {
                continue;
            }

            String groupName = perm.substring(6);

            // Виглядає як катастрофічний триндець.
            var future = (CompletableFuture<IPermissionsHolder>) (CompletableFuture<?>) manager.getGuildGroup(member.getGuild(), groupName);
            var future2 = (CompletableFuture<IPermissionsHolder>) (CompletableFuture<?>) CompletableFuture.completedFuture(manager.getGlobalGroup(groupName));

            futures.add(future);
            futures.add(future2);

        }

        return CommonUtils.sequenceAsync(futures);

    }


}
