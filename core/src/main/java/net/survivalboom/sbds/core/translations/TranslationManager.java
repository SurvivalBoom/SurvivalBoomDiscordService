package net.survivalboom.sbds.core.translations;

import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.survivalboom.sbds.api.modules.IModule;
import net.survivalboom.sbds.api.translations.ITranslation;
import net.survivalboom.sbds.api.translations.ITranslationManager;
import net.survivalboom.sbds.api.translations.MessageLoadException;
import net.survivalboom.sbds.api.utils.CommonUtils;
import net.survivalboom.sbds.api.utils.Manager;
import net.survivalboom.sbds.core.SBDS;
import net.survivalboom.sbds.core.modules.Module;
import org.bspfsystems.yamlconfiguration.configuration.InvalidConfigurationException;
import org.bspfsystems.yamlconfiguration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TranslationManager extends Manager implements ITranslationManager {

    private static final Logger log = LoggerFactory.getLogger("TranslationManager");


    private final SBDS sbds;


    private final File dir;


    private final Map<String, Translation> translationMap = new HashMap<>();


    private Translation defaultTranslation = null;

    private Translation fallbackTranslation = null;


    public TranslationManager(@NotNull SBDS sbds) {
        this.sbds = sbds;
        this.dir = new File(sbds.getWorkingDir(), "translations");
    }


    @Override
    protected void init0() {

        // Створюємо директорію translations якщо її не існує.
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored <- fuck yourself.
            dir.mkdirs();

            Map<String, String> files = Map.of(
                    "translations/translation_en.yml", "translation_en.yml",
                    "translations/translation_ru.yml", "translation_ru.yml",
                    "translations/translation_uk.yml", "translation_uk.yml"
            );
            CommonUtils.checkFiles(TranslationManager.class, dir, files, null);

        }

        // Підвантажуємо усі файли перекладів.
        reload();

        if (!translationMap.isEmpty()) {
            log.info("Successfully loaded {} translations: \n- {}", translationMap.size(), String.join(", ", translationMap.keySet()));
        }

        // Завантажуємо стандартний та резервний переклади.

        String defaultTranslationName = sbds.getConfiguration().getString("translations.default", "null");
        defaultTranslation = getTranslation(defaultTranslationName);
        if (defaultTranslation == null) log.warn("Default translation with name `{}` not found.", defaultTranslationName);

        String fallbackTranslationName = sbds.getConfiguration().getString("translations.fallback", "null");
        fallbackTranslation = getTranslation(fallbackTranslationName);
        if (fallbackTranslation == null) log.warn("Fallback translation with name `{}` not found.", fallbackTranslationName);

    }

    @Override
    protected void shutdown0() {
        getTranslations0().forEach(Translation::invalid);
        translationMap.clear();
    }


    public synchronized void reload() {

        getTranslations0().forEach(Translation::invalid);
        translationMap.clear();

        for (File file : Objects.requireNonNull(dir.listFiles())) {

            Translation translation;

            try {
                translation = new Translation(file);
            }

            catch (Throwable t) {
                log.error("Failed to load translation `{}`.", file.getName(), t);
                continue;
            }

            translationMap.put(translation.getName(), translation);

        }

    }


    //
    // GETTERS
    //

    @Override
    public @Nullable Translation getTranslation(@NotNull String name) {
        checkValid();
        return translationMap.get(name);
    }

    @Override
    public @NotNull List<ITranslation> getTranslations() {
        return new ArrayList<>(translationMap.values());
    }


    public @NotNull List<Translation> getTranslations0() {
        return new ArrayList<>(translationMap.values());
    }


    @Override
    public @Nullable Translation defaultTranslation() {
        return defaultTranslation;
    }

    public void defaultTranslation(@Nullable Translation translation) {
        this.defaultTranslation = translation;
    }

    @Override
    public @Nullable Translation fallbackTranslation() {
        return fallbackTranslation;
    }

    public void fallbackTranslation(@Nullable Translation translation) {
        this.fallbackTranslation = translation;
    }


    @Override
    public void addModuleTranslations(@NotNull IModule imodule) {

        Module module = sbds.getModuleManager().checkModuleEnabled(imodule, "Disabled module attempted to add translations");

        File file = new File(module.getDataFolder(), "translations");
        if (!file.exists()) {
            imodule.getLogger().warn("Directory `translations` does not exist in module data folder.");
            return;
        }

        File[] files = file.listFiles();
        if (files == null) return;

        for (File f : files) {

            try {
                addModuleTranslation(module, f);
            }

            catch (Throwable t) {
                log.error("[{}] Failed to load module translation `{}`.", module.getName(), f.getName(), t);
            }

        }

    }

    @Override
    public @Nullable Translation findTranslationByLocale(@NotNull DiscordLocale locale) {
        return translationMap.values().stream().filter(t -> t.discordLocale().equals(locale)).findAny().orElse(null);
    }

    @Override
    public @NotNull Set<DiscordLocale> getAvailableLocales() {
        return translationMap.values().stream().map(Translation::discordLocale).collect(Collectors.toSet());
    }

    private void addModuleTranslation(@NotNull Module module, @NotNull File file) throws IOException, InvalidConfigurationException, MessageLoadException {

        YamlConfiguration yamlConfiguration = new YamlConfiguration();
        yamlConfiguration.load(file);

        String translationName = yamlConfiguration.getString("$name");
        if (translationName == null) throw new IllegalStateException("Yaml file does not contain `$name` key");

        Translation translation = getTranslation(translationName);
        if (translation == null) return;
//            throw new IllegalStateException("Unknown translation `" + translationName + "`");

        translation.addModuleTranslation(module, yamlConfiguration);

        module.getRegistration().add("ModuleTranslation-" + translationName, () -> translation.removeModuleTranslation(module));

    }


    public static @NotNull Translation convert(@NotNull ITranslation translation) {
        return (Translation) translation;
    }


}
