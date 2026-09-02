package net.survivalboom.sbds.api.database.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import net.dv8tion.jda.api.entities.Role;
import net.survivalboom.sbds.api.SbdsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

@Converter(autoApply = true)
public class RoleConverter implements AttributeConverter<Role, Long>, TypeSerializer<Role> {

    @Override
    public Long convertToDatabaseColumn(Role role) {
        if (role == null) {
            return null;
        }
        return role.getIdLong();
    }

    @Override
    public Role convertToEntityAttribute(Long id) {
        if (id == null) {
            return null;
        }
        return SbdsProvider.getInstance().getBot().getRoleById(id);
    }

    @Override
    public Role deserialize(@NotNull Type type, @NotNull ConfigurationNode node) throws SerializationException {
        long id = node.getLong();
        if (id == 0) {
            throw new SerializationException("Invalid id of role `" + node.getString() + "`");
        }

        Role role = SbdsProvider.getInstance().getBot().getRoleById(id);
        if (role == null) {
            throw new SerializationException("Unknown role with id `" + id + "`");
        }

        return role;
    }

    @Override
    public void serialize(@NotNull Type type, @Nullable Role obj, @NotNull ConfigurationNode node) throws SerializationException {
        if (obj == null) {
            node.set(null);
        } else {
            node.set(obj.getIdLong());
        }
    }

}
