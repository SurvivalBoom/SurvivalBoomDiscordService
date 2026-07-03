package net.survivalboom.sbds.api.database.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.TimeZone;

@Converter(autoApply = true)
public class TimeZoneConverter implements AttributeConverter<TimeZone, String>, TypeSerializer<TimeZone> {

    @Override
    public String convertToDatabaseColumn(TimeZone timeZone) {
        if (timeZone == null) return null;
        return timeZone.getID();
    }

    @Override
    public TimeZone convertToEntityAttribute(String id) {
        if (id == null || id.isBlank()) return null;
        return TimeZone.getTimeZone(id);
    }

    @Override
    public TimeZone deserialize(@NotNull Type type, @NotNull ConfigurationNode node) throws SerializationException {

        String id = node.getString();
        if (id == null || id.isBlank()) {
            throw new SerializationException("Timezone ID cannot be empty");
        }

        if (!Arrays.asList(TimeZone.getAvailableIDs()).contains(id)) {
            throw new SerializationException("Unknown timezone ID: `" + id + "`. Please use IANA format, e.g. 'Europe/Kyiv'");
        }

        return TimeZone.getTimeZone(id);
    }

    @Override
    public void serialize(@NotNull Type type, @Nullable TimeZone obj, @NotNull ConfigurationNode node) throws SerializationException {

        if (obj == null) {
            node.set(null);
        } else {
            node.set(obj.getID());
        }

    }

}