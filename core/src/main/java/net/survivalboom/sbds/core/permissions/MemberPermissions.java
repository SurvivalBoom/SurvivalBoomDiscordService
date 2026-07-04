package net.survivalboom.sbds.core.permissions;

import net.survivalboom.sbds.api.database.members.IMemberData;
import net.survivalboom.sbds.api.permissions.*;
import net.survivalboom.sbds.api.utils.CommonUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class MemberPermissions extends AbstractPermissionHolder implements IMemberPermissions {

    private final PermissionManager manager;

    private final IMemberData memberData;

    private final Map<String, Permission> memberPermissionsMapCache = new HashMap<>();


    public MemberPermissions(
            @Nullable Collection<Permission> permissions,
            @NotNull IMemberData memberData,
            @NotNull PermissionManager manager
    ) {
        super(manager);

        this.memberData = memberData;
        this.manager = manager;

        permissionMap.put("group.default", new Permission("group.default", true)); // Додаємо стандартну групу для усіх.

        if (permissions != null) {
            permissions.forEach(p -> permissionMap.put(p.permission(), p));
        }

    }

    @Override
    public @NotNull IPermissionManager getManager() {
        return manager;
    }

    @Override
    public @NotNull IMemberData getMember() {
        return memberData;
    }

    @Override
    public @NotNull String getName() {
        return memberData.getMember().getEffectiveName();
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
            var future = (CompletableFuture<IPermissionsHolder>) (CompletableFuture<?>) manager.getGuildGroup(memberData.getGuild(), groupName);
            var future2 = (CompletableFuture<IPermissionsHolder>) (CompletableFuture<?>) CompletableFuture.completedFuture(manager.getGlobalGroup(groupName));

            futures.add(future);
            futures.add(future2);

        }

        return CommonUtils.sequenceAsync(futures).thenApply(list -> {
            list.sort(Comparator.comparing(IPermissionsHolder::getWeight));
            return list;
        });

    }

    @Override
    public @NotNull Map<String, Permission> getPermissionMap() {

        checkValid();

        if (!memberPermissionsMapCache.isEmpty()) {
            return new HashMap<>(memberPermissionsMapCache);
        }

        // Створюємо permission map і впихуємо туди усі дозволи за пріоритетами //

        Map<String, Permission> permissionMap = new HashMap<>();

        List<IPermissionsHolder> groups = getMemberGroups().join();
        groups.forEach(group -> permissionMap.putAll(group.getPermissions()));

        permissionMap.putAll(this.permissionMap);

        this.memberPermissionsMapCache.clear();
        this.memberPermissionsMapCache.putAll(permissionMap);

        return permissionMap;

    }

    @Override
    protected void save() {

        memberPermissionsMapCache.clear();

        List<Permission> permissions = getPermissionList();

        try {
            ConfigurationNode node = memberData.container().obtainNode(IPermissionManager.PERMISSION_CONTAINER_KEY);
            node.setList(Permission.class, permissions);
        }

        catch (SerializationException e) {
            throw new RuntimeException(e);
        }

        memberData.save();

    }

}
