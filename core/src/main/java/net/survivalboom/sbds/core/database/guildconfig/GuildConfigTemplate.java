package net.survivalboom.sbds.core.database.guildconfig;

import net.dv8tion.jda.api.entities.Guild;
import net.survivalboom.sbds.api.database.guildconfig.GuildConfigField;
import net.survivalboom.sbds.api.database.guildconfig.IGuildConfig;
import net.survivalboom.sbds.api.database.guildconfig.IGuildConfigManager;
import net.survivalboom.sbds.api.database.guildconfig.IGuildConfigTemplate;
import net.survivalboom.sbds.api.registrations.Registration;
import net.survivalboom.sbds.api.utils.valid.Valid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class GuildConfigTemplate extends Valid implements IGuildConfigTemplate {

    private final GuildConfigManager manager;

    private final Map<String, GuildConfigField> fieldMap;

    protected Registration<IGuildConfigTemplate> registration;

    private final String translationKey;

    public GuildConfigTemplate(
            @NotNull Map<String, GuildConfigField> map,
            @Nullable String translationKey,
            @NotNull GuildConfigManager manager
    ) {
        this.fieldMap = map;
        this.translationKey = translationKey;
        this.manager = manager;
    }

    @Override
    public @NotNull IGuildConfigManager getManager() {
        return manager;
    }

    @Override
    public @NotNull Registration<IGuildConfigTemplate> getRegistration() {
        return registration;
    }

    @Override
    public @NotNull String getKey() {
        return registration.key().prefix();
    }

    @Override
    public @Nullable String getTranslationKey() {
        return translationKey;
    }

    @Override
    public @NotNull String createTranslationKey() {
        return String.format("$[%s.label]", getTranslationKey());
    }

    @Override
    public @Nullable GuildConfigField getField(@NotNull String key) {
        return fieldMap.get(key);
    }

    @Override
    public @NotNull Map<String, GuildConfigField> getFields() {
        return new HashMap<>(fieldMap);
    }

    @Override
    public @NotNull IGuildConfig obtainConfig(long guildId) {
        checkValid();
        return manager.getGuildConfig(this, guildId);
    }

}
