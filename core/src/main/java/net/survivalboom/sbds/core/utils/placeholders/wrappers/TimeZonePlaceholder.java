package net.survivalboom.sbds.core.utils.placeholders.wrappers;

import net.survivalboom.sbds.api.utils.placeholders.IPlaceholders;
import net.survivalboom.sbds.api.utils.placeholders.Placeholders;
import org.jetbrains.annotations.NotNull;

import java.util.TimeZone;

public class TimeZonePlaceholder implements IPlaceholders {

    private final TimeZone timeZone;

    public TimeZonePlaceholder(@NotNull TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    @Override
    public @NotNull Placeholders placeholders() {
        return Placeholders.of("", timeZone.toZoneId());
    }

}
