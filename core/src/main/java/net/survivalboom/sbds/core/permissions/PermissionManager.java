package net.survivalboom.sbds.core.permissions;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.survivalboom.sbds.api.ISBDS;
import net.survivalboom.sbds.api.database.IRepository;
import net.survivalboom.sbds.api.modules.IModule;
import net.survivalboom.sbds.api.permissions.*;
import net.survivalboom.sbds.api.utils.CommonUtils;
import net.survivalboom.sbds.api.utils.valid.Manager;
import net.survivalboom.sbds.core.SBDS;
import net.survivalboom.sbds.core.database.Database;
import net.survivalboom.sbds.core.permissions.records.GuildGroupPermissionRecord;
import net.survivalboom.sbds.core.permissions.records.GuildPermissionsGroupRecord;
import net.survivalboom.sbds.core.permissions.records.GuildUserPermissionRecord;
import net.survivalboom.sbds.core.registration.InternalRegistrationManager;
import net.survivalboom.sbds.core.utils.InternalPushQueue;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class PermissionManager extends Manager implements IPermissionManager {

    private static final Logger log = LoggerFactory.getLogger(PermissionManager.class.getSimpleName());

    private final SBDS sbds;


    private IRepository<GuildUserPermissionRecord> userPermissionRepository;

    private IRepository<GuildPermissionsGroupRecord> guildGroupRepository;

    private IRepository<GuildGroupPermissionRecord> groupPermissionRepository;


    private final InternalPushQueue<GuildPermissionsGroup> guildGroupSaveQueue;

    private final InternalPushQueue<MemberPermissions> memberPermSaveQueue;


    private final InternalRegistrationManager<IGlobalPermissionGroup> globalGroupRegistry;

    private final Map<Long, IMemberPermissions> memberPermissionsMap = new WeakHashMap<>();

    private final Map<Long, Map<String, IGuildPermissionsGroup>> permissionsGroupMap = new WeakHashMap<>();

    private final Map<Long, Map<String, Permission>> cachedUsersPermissionMaps = new WeakHashMap<>();



    public PermissionManager(@NotNull SBDS sbds) {

        this.sbds = sbds;
        this.globalGroupRegistry = new InternalRegistrationManager<>(this, null, sbds.getRegistrationRegistry());

        this.guildGroupSaveQueue = new InternalPushQueue<>(this::saveGroup0, "GuildGroup", 500, sbds);
        this.memberPermSaveQueue = new InternalPushQueue<>(this::saveMember0, "MemberPermissions", 500, sbds);

    }

    //
    // MANAGER
    //

    @Override
    protected void init0() {

        globalGroupRegistry.init();

        Database database = sbds.getDatabase();
        userPermissionRepository = database.createRepository0(null, "guild_user_permissions", GuildUserPermissionRecord.class);
        guildGroupRepository = database.createRepository0(null, "guild_permission_groups", GuildPermissionsGroupRecord.class);
        groupPermissionRepository = database.createRepository0(null, "guild_group_permissions", GuildGroupPermissionRecord.class);

        loadGlobalPermissions();

        guildGroupSaveQueue.init();
        memberPermSaveQueue.init();

    }

    @Override
    protected void shutdown0() {

        guildGroupSaveQueue.shutdown();
        memberPermSaveQueue.shutdown();

        memberPermissionsMap.clear();
        permissionsGroupMap.clear();
        cachedUsersPermissionMaps.clear();

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

        Map<String, Permission> permissionMap = getMemberPermissionMap(guildId, userId);

        Permission perm = permissionMap.get(permission);
        if (perm == null) {
            return allowDefault;
        }

        return perm.value();

    }

    @Override
    public @NotNull Map<String, Permission> getMemberPermissionMap(long guildId, long userId) {

        long userPermMapHash = CommonUtils.longHash(guildId, userId);
        if (cachedUsersPermissionMaps.containsKey(userPermMapHash)) {
            return cachedUsersPermissionMaps.get(userPermMapHash);
        }

        // Дістаємо усі дозволи які нам потрібні //

        IMemberPermissions memberPermissions = getMemberPermissions(guildId, userId).join();
        List<IGuildPermissionsGroup> guildGroups = getGuildGroups(guildId).join().stream()
                .filter(memberPermissions::hasGroup)
                .sorted(Comparator.comparing(IGuildPermissionsGroup::getWeight))
                .toList();

        List<IGlobalPermissionGroup> globalGroups = getGlobalGroups().stream()
                .filter(memberPermissions::hasGroup)
                .sorted(Comparator.comparing(IGlobalPermissionGroup::getWeight))
                .toList();

        // Створюємо permission map і впихуємо туди усі дозволи за пріоритетами //

        Map<String, Permission> permissionMap = new HashMap<>();

        globalGroups.forEach(group -> permissionMap.putAll(group.getPermissions()));
        guildGroups.forEach(group -> permissionMap.putAll(group.getPermissions()));
        permissionMap.putAll(memberPermissions.getPermissions());

        cachedUsersPermissionMaps.put(userPermMapHash, permissionMap);

        return permissionMap;

    }

    //
    // GUILD PERMISSION GROUPS
    //

    // CREATE //

    @Override
    public @NotNull CompletableFuture<IGuildPermissionsGroup> createGuildGroup(long guildId, @NotNull String name) {

        Objects.requireNonNull(name, "name == null");
        checkValid();

        return getGuildGroup(guildId, name).thenCompose(sex -> {

            if (sex != null) {
                throw new IllegalStateException("Guild group with name `" + name + "` already exists");
            }

            return guildGroupRepository.queueSessionReturnRequest(session -> {

                GuildPermissionsGroupRecord record = new GuildPermissionsGroupRecord(guildId, name, 0);

                guildGroupRepository.saveRecord(record);

                IGuildPermissionsGroup group = new GuildPermissionsGroup(record, this);

                permissionsGroupMap.computeIfAbsent(guildId, k -> new HashMap<>()).put(name, group);

                return group;

            });

        });

    }

    // DELETE //

    @Override
    public @NotNull CompletableFuture<Void> deleteGuildGroup(@NotNull IGuildPermissionsGroup igroup) {

        Objects.requireNonNull(igroup, "group == null");
        checkValid();

        GuildPermissionsGroup group = (GuildPermissionsGroup) igroup;

        if (!group.isValid()) {
            throw new IllegalStateException("object is no longer valid");
        }

        var map = permissionsGroupMap.get(group.getGuild().getIdLong());
        if (map == null) {
            throw new RuntimeException("Something went wrong! This object does not exist in permissionGroupMap");
        }

        map.remove(group.getName());

        return groupPermissionRepository.deleteRecord(group.getId());

    }

    @Override
    public @NotNull CompletableFuture<@Nullable IGuildPermissionsGroup> getGuildGroup(long guildId, @NotNull String name) {

        Objects.requireNonNull(name, "name == null");
        checkValid();

        var map = permissionsGroupMap.computeIfAbsent(guildId, k -> new HashMap<>());

        IGuildPermissionsGroup group = map.get(name);
        if (group != null) {
            return CompletableFuture.completedFuture(group);
        }

        long hash = CommonUtils.longHash(guildId, name);

        return guildGroupRepository.queueSessionReturnRequest(session -> {

            // Витягаємо з бази даних групу сервера //

            GuildPermissionsGroupRecord record = session.get(GuildPermissionsGroupRecord.class, hash);
            if (record == null) {
                map.put(name, null);
                return null;
            }

            IGuildPermissionsGroup ggroup = new GuildPermissionsGroup(record, this);
            map.put(name, ggroup);

            return ggroup;

        }).thenCompose(ggroup -> {

            if (ggroup == null) {
                return CompletableFuture.completedFuture(null);
            }

            return groupPermissionRepository.queueSessionReturnRequest(session -> {

                // Витягаємо з бази даних дозволи цієї групи //

                var cb = session.getCriteriaBuilder();
                var query = cb.createQuery(GuildGroupPermissionRecord.class);
                var root = query.from(GuildGroupPermissionRecord.class);

                var guildIdPredicate = cb.equal(root.get("guildId"), guildId);
                var groupNamePredicate = cb.equal(root.get("groupName"), name);

                query.select(root).where(cb.and(guildIdPredicate, groupNamePredicate));

                var result = session.createQuery(query).getResultList();

                var perms = result.stream()
                        .map(rec -> new Permission(rec.getPermission(), rec.getValue()))
                        .toList();

                ggroup.setPermissions(perms, true);

                return ggroup;

            });

        });

    }

    @Override
    public @NotNull CompletableFuture<List<IGuildPermissionsGroup>> getGuildGroups(long guildId) {

        checkValid();

        var map = permissionsGroupMap.computeIfAbsent(guildId, k -> new HashMap<>());
        if (!map.isEmpty()) {
            return CompletableFuture.completedFuture(
                    map.values().stream()
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList())
            );
        }

        return groupPermissionRepository.queueSessionReturnRequest(session -> {

            // Витягаємо з бази даних усі групи на вказаному сервері //

            var cb = session.getCriteriaBuilder();

            JpaCriteriaQuery<String> query = cb.createQuery(String.class);
            JpaRoot<GuildPermissionsGroupRecord> root = query.from(GuildPermissionsGroupRecord.class);

            var guildIdPredicate = cb.equal(root.get("guildId"), guildId);

            query.select(root.get("groupName")).where(guildIdPredicate);

            return session.createQuery(query).getResultList();

        }).thenCompose(groupNames -> {

            // Якийсь катастрофічний асинхронний пиздець //
            // Нахуй багатопоточність! Фрізимо нахуй Main Thread! //
            // БУ-ГА-ГА-ГА!!! java.lang.OutOfMemoryError //

            if (groupNames == null || groupNames.isEmpty()) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            }

            List<CompletableFuture<IGuildPermissionsGroup>> futures = groupNames.stream()
                    .map(name -> getGuildGroup(guildId, name))
                    .toList();

            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
            );

            return allFutures.thenApply(v -> futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .toList()
            );

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

        return userPermissionRepository.queueSessionReturnRequest(session -> {

            var cb = session.getCriteriaBuilder();
            var query = cb.createQuery(GuildUserPermissionRecord.class);
            var root = query.from(GuildUserPermissionRecord.class);

            var guildIdPredicate = cb.equal(root.get("guildId"), guildId);
            var userIdPredicate = cb.equal(root.get("userId"), userId);

            query.select(root).where(cb.and(guildIdPredicate, userIdPredicate));

            var result = session.createQuery(query).getResultList();

            var perms = result.stream()
                            .map(record -> new Permission(record.getPermission(), record.getValue()))
                            .toList();

            Guild guild = sbds.getBot().getGuildById(guildId);
            if (guild == null) {
                return null;
            }

            Member member = guild.retrieveMemberById(userId).complete();
            if (member == null) {
                return null;
            }

            var mp = new MemberPermissions(member, this);
            mp.setPermissions(perms, true);

            this.memberPermissionsMap.put(hash, mp);

            return mp;

        }).thenApply(result -> {

            if (result == null) {
                throw new IllegalStateException("member data of `" + guildId + "`->`" + userId + "` not found");
            }

            return result;

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
    // SAVING
    //

    // GROUP //

    private void saveGroup0(@NotNull InternalPushQueue<GuildPermissionsGroup> queue) {

        var list = queue.getQueue();

        for (var group : list) {

            guildGroupRepository.saveRecord(group.getRecord());

            groupPermissionRepository.queueSessionRequest(session -> {

                for (var permission : group.getPermissions().values()) {
                    GuildGroupPermissionRecord record = new GuildGroupPermissionRecord(
                            group.getGuild().getIdLong(),
                            group.getName(),
                            permission.permission(),
                            permission.value()
                    );
                    session.merge(record);
                }

            });

        }

    }

    // MEMBER //

    private void saveMember0(@NotNull InternalPushQueue<MemberPermissions> queue) {

        for (var member : queue.getQueue()) {

            userPermissionRepository.queueSessionRequest(session -> {

                for (var permission : member.getPermissions().values()) {
                    GuildUserPermissionRecord record = new GuildUserPermissionRecord(
                            member.getMember().getGuild().getIdLong(),
                            member.getMember().getIdLong(),
                            permission.permission(),
                            permission.value()
                    );
                    session.merge(record);
                }

            });

        }

    }

    //
    // MISC
    //

    @Override
    public @NotNull ISBDS getSbds() {
        return sbds;
    }

}
