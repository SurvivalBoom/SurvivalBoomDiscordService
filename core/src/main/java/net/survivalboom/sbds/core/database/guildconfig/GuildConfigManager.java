package net.survivalboom.sbds.core.database.guildconfig;

import net.survivalboom.sbds.api.database.guildconfig.GuildConfigField;
import net.survivalboom.sbds.api.database.guildconfig.IGuildConfig;
import net.survivalboom.sbds.api.database.guildconfig.IGuildConfigManager;
import net.survivalboom.sbds.api.database.guildconfig.IGuildConfigTemplate;
import net.survivalboom.sbds.api.database.guilds.IGuildDataManager;
import net.survivalboom.sbds.api.modules.IModule;
import net.survivalboom.sbds.api.registrations.Registration;
import net.survivalboom.sbds.api.registrations.RegistrationManager;
import net.survivalboom.sbds.api.translations.ITranslation;
import net.survivalboom.sbds.api.utils.NamespacedKey;
import net.survivalboom.sbds.api.utils.valid.Manager;
import net.survivalboom.sbds.core.SBDS;
import net.survivalboom.sbds.core.registration.InternalRegistrationManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class GuildConfigManager extends Manager implements IGuildConfigManager, RegistrationManager.Callback<IGuildConfigTemplate> {

    private final InternalRegistrationManager<IGuildConfigTemplate> registry;

    private final Map<Long, Map<IGuildConfigTemplate, IGuildConfig>> guildConfigCache = new WeakHashMap<>();

    protected final SBDS sbds;

    protected final IGuildDataManager guildDataManager;


    public GuildConfigManager(@NotNull SBDS sbds) {
        this.sbds = sbds;
        this.registry = new InternalRegistrationManager<>(this,null, sbds.getRegistrationRegistry());
        this.guildDataManager = sbds.getGuildDataManager();
    }


    @Override
    protected void init0() {

        registry.init();

        registerTemplate0(null, builder ->
            builder
                .setTranslation("sbds.config")
                .addField("prefix", String.class, "!")
                .addField("translation", ITranslation.class, null)
                .addField("timezone", TimeZone.class, TimeZone.getDefault())
        );

    }

    @Override
    protected void shutdown0() {
        registry.shutdown();
    }

    @Override
    public void unRegister(@NotNull Registration<IGuildConfigTemplate> registration) {
        IGuildConfigTemplate template = registration.object();
        guildConfigCache.values().forEach(map -> map.remove(template)); // Видаляємо з кешу відвантажений шаблон конфігурації.
    }

    //
    // GUILD CONFIG TEMPLATE
    //

    @Override
    public @NotNull IGuildConfigTemplate registerTemplate(@NotNull IModule module, @NotNull Collection<GuildConfigField> fields, @Nullable String translationKey) {
        Objects.requireNonNull(module, "module == null");
        return registerTemplate0(module, fields, translationKey);
    }

    @Override
    public @NotNull IGuildConfigTemplate registerTemplate(@NotNull IModule module, @NotNull Consumer<IGuildConfigBuilder> builder) {
        Objects.requireNonNull(module, "module == null");
        return registerTemplate0(module, builder);
    }

    public @NotNull IGuildConfigTemplate registerTemplate0(@Nullable IModule module, @NotNull Consumer<IGuildConfigBuilder> builder) {

        Builder b = new Builder();
        builder.accept(b);

        return registerTemplate0(module, b.fields, b.translationKey);

    }

    public @NotNull IGuildConfigTemplate registerTemplate0(@Nullable IModule module, @NotNull Collection<GuildConfigField> fields, @Nullable String translationKey) {

        checkValid();

        if (fields.isEmpty()) {
            throw new IllegalArgumentException("Fields cannot be empty");
        }

        if (getTemplate(module) != null) {
            throw new IllegalArgumentException("Template already registered");
        }

        Map<String, GuildConfigField> map = fields.stream()
                .collect(Collectors.toMap(GuildConfigField::key, field -> field, (field1, field2) -> field1));

        GuildConfigTemplate template = new GuildConfigTemplate(map, translationKey, this);
        template.registration = registry.register0(module, "config", template);

        return template;

    }

    @Override
    public void unregisterGuildConfig(@NotNull IGuildConfigTemplate template) {
        checkValid();
        registry.unregister(template);
    }

    @Override
    public @Nullable IGuildConfigTemplate getTemplate(@Nullable IModule module) {
        return registry.getRegistrations().stream()
                .filter(reg -> Objects.equals(reg.module(), module))
                .map(Registration::object)
                .findAny()
                .orElse(null);
    }

    @Override
    public @Nullable IGuildConfigTemplate getTemplate(@NotNull String key) {
        return registry.getRegistrationAsObject(NamespacedKey.create(key, "config"));
    }

    @Override
    public @NotNull List<IGuildConfigTemplate> getTemplates() {
        return registry.getRegisteredObjects();
    }

    //
    // GUILD CONFIG
    //

    // GET //

    @Override
    public @NotNull IGuildConfig getGuildConfig(@NotNull IGuildConfigTemplate template, long guildId) {

        checkValid();

        GuildConfigTemplate template0 = (GuildConfigTemplate) template;
        if (registry.getObjectRegistration(template0) == null) {
            throw new IllegalArgumentException("Template is not registered");
        }

        return guildConfigCache.computeIfAbsent(guildId, k -> new WeakHashMap<>())
                .computeIfAbsent(template, k -> new GuildConfig(guildId, template0, this));

    }

    @Override
    public @NotNull List<IGuildConfig> getGuildConfigs(long guildId) {
        return getTemplates().stream()
                .map(template -> getGuildConfig(template, guildId))
                .collect(Collectors.toList());
    }

    // DEFAULT //

    @Override
    public @NotNull IGuildConfigTemplate getSbdsConfig() {
        return Objects.requireNonNull(getTemplate("sbds:config"), "sbds template == null; Congratulations! You totally fucked up!");
    }

    //
    // DE-BUILDER
    //

    public static class Builder implements IGuildConfigBuilder {

        private String translationKey = null;

        private final List<GuildConfigField> fields = new ArrayList<>();

        // TRANSLATION //

        @Override
        public @NotNull IGuildConfigBuilder setTranslation(@Nullable String translationKey) {
            this.translationKey = translationKey;
            return this;
        }

        @Override
        public String getTranslation() {
            return translationKey;
        }

        // FIELDS //

        @Override
        public @NotNull <T> IGuildConfigBuilder addField(@NotNull String key, @NotNull Class<T> type, @Nullable T defaultValue, boolean internal) {
            this.fields.add(new GuildConfigField(key, type, defaultValue, internal));
            return this;
        }

        @Override
        public @NotNull IGuildConfigBuilder addFields(@NotNull Collection<GuildConfigField> fields) {
            this.fields.addAll(fields);
            return this;
        }

        @Override
        public @Nullable IGuildConfigBuilder addFields(GuildConfigField @NotNull ... fields) {
            return addFields(List.of(fields));
        }

        @Override
        public @NotNull IGuildConfigBuilder setFields(@Nullable Collection<GuildConfigField> fields) {

            this.fields.clear();

            if (fields != null) {
                this.fields.addAll(fields);
            }

            return this;

        }

        @Override
        public @NotNull List<GuildConfigField> getFields() {
            return new ArrayList<>(fields);
        }

    }

}
