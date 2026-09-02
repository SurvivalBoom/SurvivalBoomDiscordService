package net.survivalboom.sbds.core.database;

import net.survivalboom.sbds.api.database.DataRecord;
import net.survivalboom.sbds.api.database.IDatabase;
import net.survivalboom.sbds.api.database.IRepository;
import net.survivalboom.sbds.api.registrations.Registration;
import net.survivalboom.sbds.api.utils.valid.Valid;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public class Repository<T extends DataRecord> extends Valid implements IRepository<T> {

    private final Class<T> recordClass;

    private final Database database;

    protected Registration<IRepository<T>> registration;


    public Repository(
            @NotNull Class<T> clazz,
            @NotNull Database database
    ) {
        this.recordClass = clazz;
        this.database = database;
    }

    //
    // REPOSITORY
    //

    @Override
    public @NotNull Registration<IRepository<T>> getRegistration() {
        return registration;
    }

    @Override
    public @NotNull Class<T> getRecordClass() {
        return recordClass;
    }

    @Override
    public @NotNull IDatabase getDatabase() {
        return database;
    }

    //
    // DATABASE QUERIES
    //

    // SESSIONS //

    @Override
    public @NotNull Session requestSession() {
        checkValid();
        return database.requestSession(this);
    }

    @Override
    public @NotNull <V> CompletableFuture<V> queueSessionReturnRequest(@NotNull Function<Session, V> function) {
        return database.queueSessionRequest(this, function);
    }

    @Override
    public @NotNull CompletableFuture<Void> queueSessionRequest(@NotNull Consumer<Session> consumer) {
        return database.queueSessionRequest(this, consumer);
    }

    // OPERATIONS //

    @Override
    public @NotNull CompletableFuture<T> saveRecord(@NotNull T record) {
        Objects.requireNonNull(record, "record == null");
        return queueSessionReturnRequest(session -> session.merge(record));
    }

    @Override
    public @NotNull CompletableFuture<Void> deleteRecord(@NotNull T record) {
        Objects.requireNonNull(record, "record == null");
        return queueSessionRequest(session -> session.remove(record));
    }

    @Override
    public @NotNull CompletableFuture<Void> deleteRecord(long id) {

        return getRecordById(id).thenCompose(record -> {

            if (record == null) {
                return CompletableFuture.completedFuture(null);
            }

            return queueSessionRequest(session -> session.remove(record));

        });

    }

    @Override
    public @NotNull CompletableFuture<@Nullable T> getRecordById(long id) {
        return queueSessionReturnRequest(session -> session.get(recordClass, id));
    }

}
