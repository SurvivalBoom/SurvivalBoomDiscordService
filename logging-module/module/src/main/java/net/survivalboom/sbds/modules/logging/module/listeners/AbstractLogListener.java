package net.survivalboom.sbds.modules.logging.module.listeners;

import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.survivalboom.sbds.api.events.EventListener;
import net.survivalboom.sbds.modules.logging.module.LoggingModule;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.BiConsumer;

// Список костилів номер 666:

// Шановні розробники дискорду!
// Я бажаю вам щоб цей сраний журнал аудиту снився вам у самих жахливих снах, а потім прийшов наяву і спалив усі ваші офіси
public abstract class AbstractLogListener implements EventListener {

    protected final LoggingModule module;

    public AbstractLogListener(@NotNull LoggingModule module) {
        this.module = module;
    }

    // Сраний говнокостиль через те, що дискорд не показує деяку інформацію в евентах, а пише її лише у журнал аудиту сервера.
    protected void fetchModeratorAndReason(
            @NotNull Guild guild,
            @NotNull ActionType type,
            long targetId,
            @NotNull BiConsumer<User, String> callback
    ) {
        // А це костиль через те, що CompletableFuture гавно
        module.schedule("audit_fetch_" + targetId, () -> {
            // Запускаємо цей сраний костиль синхронно, але всередині окремого потоку в шкедулері

            try {
                // РЕТРІЕВЕ АУДІТ ЛОХС, ГЄ ГЄ ГЄ ГЄ ГЄ ВЩІАВАЩВАВАЩІ
                List<AuditLogEntry> logs = guild.retrieveAuditLogs()
                        .type(type)
                        .limit(5)
                        .complete();

                AuditLogEntry entry = logs.stream()
                        .filter(e -> e.getTargetIdLong() == targetId)
                        .filter(e -> (OffsetDateTime.now().toEpochSecond() - e.getTimeCreated().toEpochSecond()) < 10)
                        .findFirst()
                        .orElse(null);

                if (entry != null) {
                    User actor = entry.getUser();

                    if (actor != null && actor.getIdLong() == targetId) {
                        callback.accept(null, null);

                    } else {
                        callback.accept(actor, entry.getReason());
                    }

                } else {
                    callback.accept(null, null);
                }

            } catch (Exception ex) {
                // Памілка
                callback.accept(null, null);

                throw new RuntimeException("Failed to fetch audit log for target " + targetId, ex);
            }

        }, 1000, 0);
    }
}