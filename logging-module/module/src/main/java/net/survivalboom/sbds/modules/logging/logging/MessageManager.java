package net.survivalboom.sbds.modules.logging.logging;

import net.survivalboom.sbds.api.database.IRepository;
import net.survivalboom.sbds.api.scheduler.ISchedulerTask;
import net.survivalboom.sbds.modules.logging.LoggingModule;
import net.survivalboom.sbds.modules.logging.api.ILoggedMessage;
import net.survivalboom.sbds.modules.logging.database.MessageRecord;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.concurrent.CompletableFuture;

public class MessageManager {

    private final LoggingModule module;
    private IRepository<MessageRecord> messageRepository;
    private final MessageCache ramCache;

    private ISchedulerTask ramCleanupTask;
    private ISchedulerTask dbCleanupTask;

    public MessageManager(LoggingModule module) {
        this.module = module;

        ConfigurationNode config = module.getConfig();

        long ramTtlHours = config.node("cache", "ram_ttl_hours").getLong(24L);
        long ramTtlMillis = ramTtlHours * 60 * 60 * 1000L;

        this.ramCache = new MessageCache(ramTtlMillis);
    }

    public void init() {
        this.messageRepository = module.getDatabase().createRepository(module, "messages", MessageRecord.class);

        this.ramCleanupTask = module.getSbds().getScheduler().schedule(
                module,
                "logging_ram_cleanup",
                task -> ramCache.cleanupOldMessages(),
                600_000,
                600_000
        );

        ConfigurationNode config = module.getConfig();
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

    private void cleanupDatabase(long ttlMillis) {
        if (messageRepository == null) return;

        long thresholdTime = System.currentTimeMillis() - ttlMillis;

        messageRepository.queueSessionRequest(session -> {
            session.createMutationQuery("DELETE FROM MessageRecord WHERE timestamp < :threshold")
                    .setParameter("threshold", thresholdTime)
                    .executeUpdate();
        });
    }

    public void shutdown() {
        if (ramCleanupTask != null) ramCleanupTask.tryCancel();
        if (dbCleanupTask != null) dbCleanupTask.tryCancel();
        this.messageRepository = null;
        this.ramCache.clear();
    }

    public void saveMessage(MessageRecord record) {
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

    public CompletableFuture<ILoggedMessage> getCachedMessage(long messageId) {
        ILoggedMessage cachedMessage = ramCache.get(messageId);
        if (cachedMessage != null) {
            return CompletableFuture.completedFuture(cachedMessage);
        }

        if (messageRepository == null) {
            return CompletableFuture.completedFuture(null);
        }

        return messageRepository.getRecordById(messageId)
                .thenApply(record -> (ILoggedMessage) record);
    }
}