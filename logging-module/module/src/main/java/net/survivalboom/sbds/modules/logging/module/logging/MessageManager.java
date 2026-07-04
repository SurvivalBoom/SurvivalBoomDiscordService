package net.survivalboom.sbds.modules.logging.module.logging;

import net.survivalboom.sbds.api.database.IRepository;
import net.survivalboom.sbds.api.scheduler.ISchedulerTask;
import net.survivalboom.sbds.api.utils.valid.Manager;
import net.survivalboom.sbds.modules.logging.module.LoggingModule;
import net.survivalboom.sbds.modules.logging.api.ILoggedMessage;
import net.survivalboom.sbds.modules.logging.module.database.MessageRecord;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.concurrent.CompletableFuture;

public class MessageManager extends Manager {

    private final LoggingModule module;

    private IRepository<MessageRecord> messageRepository;

    private MessageCache ramCache;


    private ISchedulerTask ramCleanupTask;

    private ISchedulerTask dbCleanupTask;

    public MessageManager(LoggingModule module) {
        this.module = module;
    }

    //
    // MANAGER
    //

    @Override
    protected void init0() {

        this.messageRepository = module.createRepository("messages", MessageRecord.class);

        ConfigurationNode config = module.getConfig();

        long ramTtlHours = config.node("cache", "ram_ttl_hours").getLong(24L);
        long ramTtlMillis = ramTtlHours * 60 * 60 * 1000L;

        this.ramCache = new MessageCache(ramTtlMillis);
        this.ramCleanupTask = module.getSbds().getScheduler().schedule(
                module,
                "logging_ram_cleanup",
                task -> ramCache.cleanupOldMessages(),
                600_000,
                600_000
        );

        long diskTtlHours = config.node("cache", "disk_ttl_hours").getLong(0L);
        if (diskTtlHours > 0) {

            long diskTtlMillis = diskTtlHours * 60 * 60 * 1000L;
            this.dbCleanupTask = module.getSbds().getScheduler().schedule(
                    module,
                    "logging_db_cleanup",
                    task -> cleanupDatabase(diskTtlMillis),
                    3600_000,
                    3600_000
            );

        }
    }


    @Override
    protected void shutdown0() {

        if (ramCleanupTask != null) {
            ramCleanupTask.tryCancel();
        }

        if (dbCleanupTask != null) {
            dbCleanupTask.tryCancel();
        }

        this.messageRepository = null;
        this.ramCache.clear();

    }

    //
    // TASKS
    //

    private void cleanupDatabase(long ttlMillis) {

        if (messageRepository == null) {
            return;
        }

        long thresholdTime = System.currentTimeMillis() - ttlMillis;

        messageRepository.queueSessionRequest(session ->
            session.createMutationQuery("DELETE FROM MessageRecord WHERE timestamp < :threshold")
                    .setParameter("threshold", thresholdTime)
                    .executeUpdate()
        );

    }

    //
    // MESSAGES
    //

    public void saveMessage(@NotNull MessageRecord record) {

        checkValid();

        ramCache.put(record);

        long guildId = record.getGuildId();
        var config = module.getSbds().getGuildConfigManager().getGuildConfig(module.getGuildConfig(), guildId);

        config.get("database_logging", Boolean.class, true).thenAcceptAsync(dbLoggingOpt -> {
            boolean isDbLoggingEnabled = dbLoggingOpt.orElse(false);
            if (isDbLoggingEnabled && messageRepository != null) {
                module.getDatabase().queueSave(record);
            }
        });

    }

    public CompletableFuture<@Nullable ILoggedMessage> getCachedMessage(long messageId) {

        checkValid();

        ILoggedMessage cachedMessage = ramCache.get(messageId);
        if (cachedMessage != null) {
            return CompletableFuture.completedFuture(cachedMessage);
        }

        if (messageRepository == null) {
            return CompletableFuture.completedFuture(null);
        }

        return messageRepository.getRecordById(messageId)
                .thenApply(record -> record);

    }

}