package net.survivalboom.sbds.core.database.users;

import net.survivalboom.sbds.api.database.IRepository;
import net.survivalboom.sbds.api.database.users.IUserData;
import net.survivalboom.sbds.api.database.users.IUserDataManager;
import net.survivalboom.sbds.api.utils.map.SoftRefHashMap;
import net.survivalboom.sbds.api.utils.valid.Manager;
import net.survivalboom.sbds.core.SBDS;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;

public class UserDataManager extends Manager implements IUserDataManager {

    private final SBDS sbds;

    private IRepository<UserDataRecord> repository;

    private final Map<Long, IUserData> cache = new SoftRefHashMap<>();


    public UserDataManager(@NotNull SBDS sbds) {
        this.sbds = sbds;
    }


    @Override
    public @NotNull SBDS getSbds() {
        return sbds;
    }

    //
    // MANAGER
    //

    @Override
    protected void init0() {
        this.repository = sbds.getDatabase().createRepository0(null, UserDataRecord.class);
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
    public @NotNull CompletableFuture<@NotNull IUserData> create(long id) {
        return get(id).thenCompose(user -> {

            if (user != null) {
                throw new IllegalStateException("User data with id `" + id + "` already exists");
            }

            return repository.queueSessionReturnRequest(session -> {

                UserDataRecord record = new UserDataRecord(id);
                session.persist(record);

                UserData userData = new UserData(record, this);
                cache.put(id, userData);

                return userData;

            });

        });
    }

    // DELETE //

    @Override
    public @NotNull CompletableFuture<Void> delete(long id) {
        return get(id).thenCompose(user -> {

            if (user == null) {
                throw new IllegalArgumentException("User data with id `" + id + "` does not exist");
            }

            return repository.queueSessionRequest(session -> {

                UserData userData = (UserData) user;
                UserDataRecord record = userData.getRecord();

                userData.setValid(false);

                session.remove(record);

                cache.remove(id);

            });

        });
    }

    // GET //

    @Override
    public @NotNull CompletableFuture<@Nullable IUserData> get(long id) {

        checkValid();

        if (cache.containsKey(id)) {
            return CompletableFuture.completedFuture(cache.get(id));
        }

        return repository.queueSessionReturnRequest(session -> {

            UserDataRecord record = session.get(UserDataRecord.class, id);
            if (record == null) {
                cache.put(id, null);
                return null;
            }

            UserData userData = new UserData(record, this);
            cache.put(id, userData);

            return userData;

        });

    }

    // OBTAIN //

    @Override
    public @NotNull CompletableFuture<@NotNull IUserData> obtain(long id) {
        return get(id).thenCompose(user -> {

            if (user != null) {
                return CompletableFuture.completedFuture(user);
            }

            return create(id);

        });
    }

    // SAVE //

    @Override
    public void save(@NotNull IUserData user) {

        checkValid();

        UserData userData = (UserData) user;
        if (!cache.containsKey(userData.getUser().getIdLong()) || !userData.isValid()) {
            throw new IllegalArgumentException("UserData object `" + userData + "` is no longer valid");
        }

        repository.saveRecord(userData.getRecord());

    }

}
