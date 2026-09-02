package net.survivalboom.sbds.core.database.member;

import net.survivalboom.sbds.api.ISBDS;
import net.survivalboom.sbds.api.database.IRepository;
import net.survivalboom.sbds.api.database.members.IMemberData;
import net.survivalboom.sbds.api.database.members.IMemberDataManager;
import net.survivalboom.sbds.api.utils.CommonUtils;
import net.survivalboom.sbds.api.utils.map.SoftRefHashMap;
import net.survivalboom.sbds.api.utils.valid.Manager;
import net.survivalboom.sbds.core.SBDS;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;

public class MemberDataManager extends Manager implements IMemberDataManager {

    private final SBDS sbds;

    private IRepository<MemberDataRecord> repository;

    private final Map<Long, IMemberData> cache = new SoftRefHashMap<>();


    public MemberDataManager(@NotNull SBDS sbds) {
        this.sbds = sbds;
    }

    @Override
    public @NotNull ISBDS getSbds() {
        return sbds;
    }

    //
    // MANAGER
    //

    @Override
    protected void init0() {
        this.repository = sbds.getDatabase().createRepository0(null, MemberDataRecord.class);
    }

    @Override
    protected void shutdown0() {
        this.repository = null;
        cache.clear();
    }

    //
    // DATA
    //

    // CREATE //

    @Override
    public @NotNull CompletableFuture<@NotNull IMemberData> create(long guildId, long userId) {
        long id = CommonUtils.longHash(guildId, userId);
        return get(id).thenCompose(data -> {

            if (data != null) {
                throw new IllegalStateException("Member data with id `" + guildId + ":" + userId + "` already exists");
            }

            return repository.queueSessionReturnRequest(session -> {

                MemberDataRecord record = new MemberDataRecord(guildId, userId);
                session.persist(record);

                MemberData memberData = new MemberData(record, this);
                cache.put(id, memberData);

                return memberData;

            });

        });
    }

    // DELETE //

    @Override
    public @NotNull CompletableFuture<Void> delete(long id) {
        return get(id).thenCompose(member -> {

            if (member == null) {
                throw new IllegalArgumentException("Member data with id `" + id + "` does not exist");
            }

            return repository.queueSessionRequest(session -> {

                MemberData data = (MemberData) member;
                MemberDataRecord record = data.getRecord();

                data.setValid(false);

                session.remove(record);

                cache.remove(id);

            });

        });
    }

    // GET //

    @Override
    public @NotNull CompletableFuture<@Nullable IMemberData> get(long id) {

        checkValid();

        if (cache.containsKey(id)) {
            return CompletableFuture.completedFuture(cache.get(id));
        }

        return repository.queueSessionReturnRequest(session -> {

            MemberDataRecord record = session.get(MemberDataRecord.class, id);
            if (record == null) {
                cache.put(id, null);
                return null;
            }

            MemberData data = new MemberData(record, this);
            cache.put(id, data);

            return data;

        });

    }

    // OBTAIN //

    @Override
    public @NotNull CompletableFuture<@NotNull IMemberData> obtain(long guildId, long userId) {
        return get(guildId, userId).thenCompose(data -> {

            if (data != null) {
                return CompletableFuture.completedFuture(data);
            }

            return create(guildId, userId);

        });
    }

    // SAVE //

    @Override
    public void save(@NotNull IMemberData memberData) {

        checkValid();

        MemberData data = (MemberData) memberData;
        if (!cache.containsKey(data.getId()) || !data.isValid()) {
            throw new IllegalArgumentException("MemberData object `" + data + "` is no longer valid");
        }

        repository.saveRecord(data.getRecord());

    }

}
