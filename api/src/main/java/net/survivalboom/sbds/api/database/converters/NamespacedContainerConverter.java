package net.survivalboom.sbds.api.database.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import net.survivalboom.sbds.api.database.IDatabase;
import net.survivalboom.sbds.api.utils.container.INamespacedDataContainer;
import net.survivalboom.sbds.api.utils.NamespacedKey;
import net.survivalboom.sbds.api.utils.container.NamespacedDataContainer;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.jackson.JacksonConfigurationLoader;

@Converter(autoApply = true)
public class NamespacedContainerConverter implements AttributeConverter<INamespacedDataContainer, String> {

    @Override
    public String convertToDatabaseColumn(INamespacedDataContainer container) {

        if (container == null) {
            return null;
        }

        ConfigurationNode rootNode = IDatabase.createConfigurateLoader().build().createNode();

        for (var entry : container.getAsMap().entrySet()) {

            var key = entry.getKey();
            var node = entry.getValue();

            rootNode.node(key).mergeFrom(node);

        }

        String out;
        try {
            out = JacksonConfigurationLoader.builder().buildAndSaveString(rootNode);
        }

        catch (ConfigurateException e) {
            throw new RuntimeException("Something went wrong! Failed to save NamespacedDataContainer to JSON! This may be an internal error, or you just messed up with objects");
        }

        return out;

    }

    @Override
    public INamespacedDataContainer convertToEntityAttribute(String dbData) {

        if (dbData == null || dbData.isBlank()) {
            return new NamespacedDataContainer();
        }

        ConfigurationNode rootNode;
        try {
            
            rootNode = IDatabase.createConfigurateLoader().buildAndLoadString(dbData);

            NamespacedDataContainer container = new NamespacedDataContainer();
            for (var entry : rootNode.childrenMap().entrySet()) {

                NamespacedKey namespacedKey = NamespacedKey.fromString((String) entry.getKey());
                ConfigurationNode node = entry.getValue();

                container.obtainNode(namespacedKey).mergeFrom(node);

            }

            return container;

        }
        
        catch (ConfigurateException e) {
            throw new RuntimeException("Something went wrong! Failed to load data. Looks like you fucked up with the JSON format. Raw data `" + dbData + "`");
        }

    }

}
