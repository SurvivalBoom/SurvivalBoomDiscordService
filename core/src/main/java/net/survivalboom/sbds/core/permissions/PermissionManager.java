package net.survivalboom.sbds.core.permissions;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.survivalboom.sbds.api.ISBDS;
import net.survivalboom.sbds.api.database.guilds.IGuildDataManager;
import net.survivalboom.sbds.api.database.members.IMemberDataManager;
import net.survivalboom.sbds.api.modules.IModule;
import net.survivalboom.sbds.api.permissions.*;
import net.survivalboom.sbds.api.utils.CommonUtils;
import net.survivalboom.sbds.api.utils.valid.Manager;
import net.survivalboom.sbds.core.SBDS;
import net.survivalboom.sbds.core.registration.InternalRegistrationManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class PermissionManager extends Manager implements IPermissionManager {

    private static final Logger log = LoggerFactory.getLogger(PermissionManager.class.getSimpleName());

    private final SBDS sbds;


    private final IGuildDataManager guildDataManager;

    private final IMemberDataManager memberDataManager;


    private final InternalRegistrationManager<IGlobalPermissionGroup> globalGroupRegistry;

    private final Map<Long, Map<String, IGuildPermissionsGroup>> permissionsGroupMap = new WeakHashMap<>();

    private final Map<Long, IMemberPermissions> memberPermissionsMap = new WeakHashMap<>();



    public PermissionManager(@NotNull SBDS sbds) {

        this.sbds = sbds;
        this.globalGroupRegistry = new InternalRegistrationManager<>(this, null, sbds.getRegistrationRegistry());

        this.guildDataManager = sbds.getGuildDataManager();
        this.memberDataManager = sbds.getMemberDataManager();

    }

    //
    // MANAGER
    //

    @Override
    protected void init0() {

        globalGroupRegistry.init();

        sbds.getDatabase().registerSerializer0(null, Permission.class, new PermissionConverter());

        loadGlobalPermissions();

    }

    @Override
    protected void shutdown0() {

        memberPermissionsMap.clear();
        permissionsGroupMap.clear();

        globalGroupRegistry.shutdown();

    }

    private void loadGlobalPermissions() {

        ConfigurationNode section = sbds.getConfiguration().node("global-permissions");

        for (var entry : section.childrenMap().entrySet()) {

            String name = (String) entry.getKey();
            ConfigurationNode node = entry.getValue();

            try {

                int weight = node.node("weight").getInt();
                List<String> permissionsRaw = node.node("permissions").getList(String.class);

                List<Permission> permissions = permissionsRaw.stream()
                        .map(Permission::fromString)
                        .toList();

                GlobalPermissionGroup group = (GlobalPermissionGroup) createGlobalGroup0(null, name);
                group.createPool0(null, name).setPermissions(permissions);
                group.setWeight(weight);

            }

            catch (Throwable t) {
                log.error("Failed to load global permission `{}`.", name, t);
            }

        }

    }


    //
    // PERMISSION CHECKING
    //

    @Override
    public boolean hasPermission(long guildId, long userId, @NotNull String permission, boolean allowDefault) {

        Objects.requireNonNull(permission, "permission == null");
        checkValid();

        Guild guild = sbds.getBot().getGuildById(guildId);
        Objects.requireNonNull(guild, "guild == null; invalid guild id?");

        Member member = guild.retrieveMemberById(userId).complete();
        Objects.requireNonNull(member, "member == null; invalid user id? user is not a member of a guild?");

        // Якщо користувач адміністратор на сервері - він має усі права. А інакше буде soft lock.
        if (member.hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR)) {
            return true;
        }

        IMemberPermissions memberPermissions = getMemberPermissions(guildId, userId).join();
        Map<String, Permission> permissionMap = memberPermissions.getPermissionMap();

        Permission perm = permissionMap.get(permission);
        if (perm == null) {
            return allowDefault;
        }

        return perm.value();

    }

    //
    // GUILD PERMISSION GROUPS
    //

    // CREATE //

    @Override
    public @NotNull CompletableFuture<IGuildPermissionsGroup> createGuildGroup(long guildId, @NotNull String name) {

        Objects.requireNonNull(name, "name == null");
        checkValid();

        return getGuildGroup(guildId, name).thenCompose(abobus -> {

            if (abobus != null) {
                throw new IllegalStateException("Guild group with name `" + name + "` already exists");
            }

            return guildDataManager.obtain(guildId)
                .thenApply(guildData -> {

                    GuildPermissionsGroup group = new GuildPermissionsGroup(name, null, 1, guildData, this);
                    group.save();

                    permissionsGroupMap.computeIfAbsent(guildId, k -> new WeakHashMap<>()).put(name, group);

                    return group;

                });

        });

    }

    // DELETE //

    @Override
    public void deleteGuildGroup(@NotNull IGuildPermissionsGroup igroup) {

        Objects.requireNonNull(igroup, "group == null");
        checkValid();

        GuildPermissionsGroup group = (GuildPermissionsGroup) igroup;

        if (!group.isValid()) {
            throw new IllegalStateException("object is no longer valid");
        }

        var map = permissionsGroupMap.get(group.getGuild().getGuild().getIdLong());
        if (map == null) {
            throw new RuntimeException("Something went wrong! This object does not exist in permissionGroupMap");
        }

        group.setValid(false);
        map.remove(group.getName());

        group.getGuild().container().getNode(PERMISSION_CONTAINER_KEY).ifPresent(node -> {
            try {
                node.node(group.getName()).set(null);
            } catch (SerializationException e) {
                throw new RuntimeException(e);
            }
        });

        group.getGuild().save();

    }

    @Override
    public @NotNull CompletableFuture<@Nullable IGuildPermissionsGroup> deleteGuildGroup(long guildId, @NotNull String name) {
        return getGuildGroup(guildId, name);
    }

    @Override
    public @NotNull CompletableFuture<@Nullable IGuildPermissionsGroup> getGuildGroup(long guildId, @NotNull String name) {

        Objects.requireNonNull(name, "name == null");
        checkValid();

        var map = permissionsGroupMap.computeIfAbsent(guildId, k -> new WeakHashMap<>());

        IGuildPermissionsGroup group = map.get(name);
        if (group != null) {
            return CompletableFuture.completedFuture(group);
        }

        return guildDataManager.get(guildId).thenApply(guildData -> {

            if (guildData == null) {
                return null;
            }

            ConfigurationNode node = guildData.container().getNode(PERMISSION_CONTAINER_KEY).orElse(null);
            if (node == null) {
                return null;
            }

            ConfigurationNode section = node.node(name);
            if (section.virtual()) {
                return null;
            }

            int weight = section.node("wight").getInt(1);

            List<Permission> permissions;
            try {
                permissions = section.node("permissions").getList(Permission.class);
            } catch (SerializationException e) {
                throw new RuntimeException(e);
            }

            return new GuildPermissionsGroup(name, permissions, weight, guildData, this);

        }).thenApply(group0 -> map.put(name, group0));

    }

    @Override
    public @NotNull CompletableFuture<List<IGuildPermissionsGroup>> getGuildGroups(long guildId) {

        checkValid();

        return guildDataManager.get(guildId).thenApply(guildData -> {

            if (guildData == null) {
                return null;
            }

            ConfigurationNode node = guildData.container().getNode(PERMISSION_CONTAINER_KEY).orElse(null);
            if (node == null) {
                return null;
            }

            List<IGuildPermissionsGroup> groups = new ArrayList<>();
            for (var entry : node.childrenMap().entrySet()) {

                String name = (String) entry.getKey();
                ConfigurationNode section = entry.getValue();

                int weight = section.getInt(1);

                List<Permission> permissions;
                try {
                    permissions = section.node("permissions").getList(Permission.class);
                } catch (SerializationException e) {
                    throw new RuntimeException(e);
                }

                GuildPermissionsGroup group = new GuildPermissionsGroup(name, permissions, weight, guildData, this);
                groups.add(group);

            }

            return groups;

        }).thenApply(groups -> {

            if (groups == null) {
                permissionsGroupMap.put(guildId, null);
            }

            else {
                permissionsGroupMap.computeIfAbsent(guildId, k -> new WeakHashMap<>());
            }

            return groups;

        });

    }

    //
    // GUILD MEMBER PERMISSIONS
    //

    @Override
    public @NotNull CompletableFuture<IMemberPermissions> getMemberPermissions(long guildId, long userId) {

        checkValid();

        long hash = CommonUtils.longHash(guildId, userId);

        IMemberPermissions memberPermissions = memberPermissionsMap.get(hash);
        if (memberPermissions != null) {
            return CompletableFuture.completedFuture(memberPermissions);
        }

        return memberDataManager.obtain(guildId, userId).thenApply(memberData -> {

            ConfigurationNode node = memberData.container().getNode(PERMISSION_CONTAINER_KEY).orElse(null);
            if (node == null) {
                return new MemberPermissions(null, memberData, this);
            }

            List<Permission> permissions;
            try {
                permissions = node.getList(Permission.class);
            } catch (SerializationException e) {
                throw new RuntimeException(e);
            }

            return new MemberPermissions(permissions, memberData, this);

        }).thenApply(permissions -> {
            memberPermissionsMap.put(hash, permissions);
            return permissions;
        });

    }


    //
    // GLOBAL GROUPS
    //

    // REG //

    @Override
    public @NotNull IGlobalPermissionGroup createGlobalGroup(@NotNull IModule module, @NotNull String name) {
        Objects.requireNonNull(module, "module == null");
        return createGlobalGroup0(module, name);
    }

    private @NotNull IGlobalPermissionGroup createGlobalGroup0(@Nullable IModule module, @NotNull String name) {

        Objects.requireNonNull(name, "name == null");
        checkValid();

        GlobalPermissionGroup group = new GlobalPermissionGroup(name, this);
        group.registration = globalGroupRegistry.register0(module, name, group);

        return group;

    }

    // UNREG //

    @Override
    public boolean removeGlobalGroup(@NotNull IGlobalPermissionGroup group) {
        checkValid();
        return globalGroupRegistry.unregister(group) != null;
    }

    @Override
    public @Nullable IGlobalPermissionGroup removeGlobalGroup(@NotNull String name) {

        IGlobalPermissionGroup group = getGlobalGroup(name);
        if (group == null) {
            return null;
        }

        removeGlobalGroup(group);

        return group;

    }

    // GET //

    @Override
    public @Nullable IGlobalPermissionGroup getGlobalGroup(@NotNull String group) {
        checkValid();
        return globalGroupRegistry.getRegisteredObjects().stream()
                .filter(g -> g.getName().equals(group))
                .findAny()
                .orElse(null);
    }

    @Override
    public @NotNull List<IGlobalPermissionGroup> getGlobalGroups() {
        checkValid();
        return globalGroupRegistry.getRegisteredObjects();
    }

    // OBTAIN //

    @Override
    public @NotNull IGlobalPermissionGroup obtainGlobalGroup(@NotNull IModule module, @NotNull String group) {

        checkValid();

        IGlobalPermissionGroup gg = getGlobalGroup(group);
        if (gg != null) {
            return gg;
        }

        return createGlobalGroup(module, group);

    }

    //
    // MISC
    //

    @Override
    public @NotNull ISBDS getSbds() {
        return sbds;
    }

}
