package net.survivalboom.sbds.modules.logging.api.storage;

import net.survivalboom.sbds.api.utils.valid.IValid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

public interface ILogRecordData extends IValid {

    long getId();

    long getGuildId();

    long getUserId(); // Користувач, над яким виконано дію (або який її виконав)

    @Nullable Long getModeratorId(); // Модератор, якщо дію виконано кимось іншим

    @NotNull String getAction(); // Наприклад: "MEMBER_JOIN", "NICKNAME_UPDATE"

    @Nullable String getReason(); // Причина з Audit Log (якщо є)

    @Nullable String getPayload(); // Додаткові дані (наприклад: "Old: Vasya | New: Petya")

    @NotNull Instant getTime();

}