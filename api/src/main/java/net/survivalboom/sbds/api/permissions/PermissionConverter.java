package net.survivalboom.sbds.api.permissions;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

@Converter(autoApply = true)
public class PermissionConverter implements AttributeConverter<String, Permission>, TypeSerializer<Permission> {

    @Override
    public Permission convertToDatabaseColumn(String attribute) {
        return Permission.fromString(attribute);
    }

    @Override
    public String convertToEntityAttribute(Permission dbData) {
        return dbData.toString();
    }

    @Override
    public Permission deserialize(@NotNull Type type, @NotNull ConfigurationNode node) throws SerializationException {

        try {
            return Permission.fromString(node.getString());
        }

        catch (Exception e) {
            throw new SerializationException(e.getMessage());
        }

    }

    @Override
    public void serialize(@NotNull Type type, @Nullable Permission obj, @NotNull ConfigurationNode node) throws SerializationException {

        if (obj == null) {
            node.set(null);
        }

        else {
            node.set(obj.toString());
        }

    }

}
