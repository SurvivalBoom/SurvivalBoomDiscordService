package net.survivalboom.sbds.modules.logging.module.storage;

import jakarta.persistence.criteria.Predicate;
import net.survivalboom.sbds.api.database.IRepository;
import net.survivalboom.sbds.api.scheduler.ISchedulerTask;
import net.survivalboom.sbds.api.utils.map.SoftRefHashMap;
import net.survivalboom.sbds.api.utils.valid.Manager;
import net.survivalboom.sbds.modules.logging.api.ILoggingModule;
import net.survivalboom.sbds.modules.logging.api.storage.ILogDataManager;
import net.survivalboom.sbds.modules.logging.api.storage.ILogRecordData;
import net.survivalboom.sbds.modules.logging.api.storage.ILoggedMessageData;
import net.survivalboom.sbds.modules.logging.module.LoggingModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class LogDataManager extends Manager implements ILogDataManager {

    private final LoggingModule module;

    private IRepository<LogDataRecord> logRepository;

    private IRepository<LoggedMessageRecord> messageRepository;

    private ISchedulerTask cleanupTask;


    private final Map<Long, LoggedMessageRecord> messageCache = new SoftRefHashMap<>();

    public LogDataManager(@NotNull LoggingModule module) {
        this.module = module;
    }

    @Override
    public @NotNull ILoggingModule getModule() { return module; }

    @Override
    protected void init0() {
        this.logRepository = module.getDatabase().createRepository(module, LogDataRecord.class);
        this.messageRepository = module.getDatabase().createRepository(module, LoggedMessageRecord.class);

        this.cleanupTask = module.getScheduler().schedule(
                module,
                "logging_db_cleanup",
                task -> runDatabaseCleanup(),
                3600_000,
                3600_000
        );
    }

    @Override
    protected void shutdown0() {
        if (cleanupTask != null) cleanupTask.tryCancel();
        messageCache.clear();
        this.logRepository = null;
        this.messageRepository = null;
    }

    private void runDatabaseCleanup() {

        if (logRepository == null || messageRepository == null) return;
        ConfigurationNode config = module.getConfig();

        long logsHours = config.node("database", "audit_ttl_hours").getLong(336L);
        long messagesHours = config.node("database", "messages_ttl_hours").getLong(336L);
        long currentTime = System.currentTimeMillis();

        if (messagesHours > 0) {

            long msgThreshold = currentTime - (messagesHours * 3600 * 1000L);
            messageRepository.queueSessionRequest(session ->
                    session.createMutationQuery("DELETE FROM LoggedMessageRecord WHERE timestamp < :threshold")
                            .setParameter("threshold", msgThreshold)
                            .executeUpdate()
            );

        }

        if (logsHours > 0) {

            Instant logsThreshold = Instant.ofEpochMilli(currentTime - (logsHours * 3600 * 1000L));
            logRepository.queueSessionRequest(session ->
                    session.createMutationQuery("DELETE FROM LogDataRecord WHERE time < :threshold")
                            .setParameter("threshold", logsThreshold)
                            .executeUpdate()
            );

        }

    }

    // Кастілічек чтобі нормальна запускать асинхроніє таскі, патамушта камплітабел ф'ючер какашка
    private <T> CompletableFuture<T> runAsync(String taskName, Callable<T> action) {

        CompletableFuture<T> future = new CompletableFuture<>();
        module.schedule(taskName, () -> {

            try {
                T result = action.call();
                future.complete(result);
            }

            catch (Throwable t) {
                future.completeExceptionally(t);
                throw t;
            }

        }, 0, 0);

        return future;

    }

    @Override
    public @NotNull CompletableFuture<@NotNull ILogRecordData> create(long guildId, long userId, @Nullable Long moderatorId, @NotNull String action, @Nullable String reason, @Nullable String payload) {
        checkValid();
        return runAsync("log_create_" + userId, () -> {
            LogDataRecord record = new LogDataRecord(guildId, userId, moderatorId, action, reason, payload);
            LogDataRecord saved = logRepository.saveRecord(record).join();
            return new LogData(saved, this);
        });
    }

    @Override
    public @NotNull CompletableFuture<@NotNull List<ILogRecordData>> getHistory(long guildId, long userId) {
        checkValid();
        return runAsync("history_fetch_" + userId, () -> {
            return logRepository.queueSessionReturnRequest(session -> {
                var cb = session.getCriteriaBuilder();
                var query = cb.createQuery(LogDataRecord.class);
                var root = query.from(LogDataRecord.class);

                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(root.get("guildId"), guildId));
                predicates.add(cb.equal(root.get("userId"), userId));

                query.select(root).where(cb.and(predicates.toArray(new Predicate[0]))).orderBy(cb.desc(root.get("time")));

                return session.createQuery(query).getResultList().stream()
                        .map(r -> (ILogRecordData) new LogData(r, this)).collect(Collectors.toList());
            }).join();
        });
    }

    @Override
    public @NotNull CompletableFuture<Void> delete(long id) {
        checkValid();
        return runAsync("log_delete_" + id, () -> {
            logRepository.deleteRecord(id).join();
            return null;
        });
    }

    @Override
    public @NotNull CompletableFuture<Void> save(@NotNull ILogRecordData logData) {
        checkValid();
        return runAsync("log_save", () -> {
            if (logData instanceof LogData data && data.isValid()) {
                logRepository.saveRecord(data.getRecord()).join();
                return null;
            }
            throw new IllegalArgumentException("Unsupported record type!");
        });
    }

    public @NotNull CompletableFuture<Void> saveMessage(@NotNull ILoggedMessageData record) {
        checkValid();
        return runAsync("msg_save_" + record.getMessageId(), () -> {
            if (!(record instanceof LoggedMessageRecord messageRecord)) {
                throw new IllegalArgumentException("Record must be an instance of LoggedMessageRecord");
            }
            messageCache.put(messageRecord.getMessageId(), messageRecord);
            module.getDatabase().queueSave(messageRecord);
            return null;
        });
    }

    public @NotNull CompletableFuture<@Nullable ILoggedMessageData> getMessage(long messageId) {
        checkValid();
        return runAsync("msg_get_" + messageId, () -> {
            LoggedMessageRecord cached = messageCache.get(messageId);
            if (cached != null) return cached;

            if (messageRepository == null) return null;

            LoggedMessageRecord record = messageRepository.getRecordById(messageId).join();
            if (record != null) messageCache.put(messageId, record);
            return record;
        });
    }
}