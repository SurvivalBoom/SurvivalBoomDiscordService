package net.survivalboom.sbds.api.database.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import net.survivalboom.sbds.api.SbdsProvider;
import net.survivalboom.sbds.api.translations.ITranslation;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

@Converter(autoApply = true)
public class TranslationConverter implements AttributeConverter<ITranslation, String>, TypeSerializer<ITranslation> {

    @Override
    public String convertToDatabaseColumn(ITranslation attribute) {

        if (attribute == null) {
            return null;
        }

        return attribute.getRegistration().key().toString();

    }

    @Override
    public ITranslation convertToEntityAttribute(String dbData) {

        if (dbData == null) {
            return null;
        }

        return SbdsProvider.getInstance().getTranslationManager().getTranslation(dbData);

    }

    @Override
    public ITranslation deserialize(@NotNull Type type, @NotNull ConfigurationNode node) throws SerializationException {

        String key = node.getString();
        if (key == null) {
            return null;
        }

        try {
            return SbdsProvider.getInstance().getTranslationManager().getTranslation(key);
        }

        catch (Exception e) {
            throw new SerializationException("Unknown translation `" + key + "`");
        }

    }

    @Override
    public void serialize(@NotNull Type type, @Nullable ITranslation obj, @NotNull ConfigurationNode node) throws SerializationException {

        if (obj == null) {
            node.set(node);
        }

        else {
            node.set(obj.getName());
        }

    }

}
