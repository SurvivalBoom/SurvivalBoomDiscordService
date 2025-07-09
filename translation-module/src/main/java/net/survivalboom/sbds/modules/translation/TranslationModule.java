package net.survivalboom.sbds.modules.translation;

import net.survivalboom.sbds.api.database.IDatabase;
import net.survivalboom.sbds.api.modules.ModuleMain;
import net.survivalboom.sbds.api.translations.ITranslationManager;
import net.survivalboom.sbds.modules.translation.commands.TranslationCommand;
import net.survivalboom.sbds.modules.translation.listeners.SlashCommandListener;

import java.util.Map;

public class TranslationModule extends ModuleMain {

    @Override
    public void onEnable() throws Throwable {

        ITranslationManager translationManager = getSbds().getTranslationManager();
        IDatabase database = getDatabase();

        checkFiles(Map.of(
                "translations/translation_uk.yml", "translations/translation_uk.yml",
                "translations/translation_ru.yml", "translations/translation_ru.yml",
                "translations/translation_en.yml", "translations/translation_en.yml"
        ));
        addModuleTranslations();

        TranslationCommand command = new TranslationCommand(database, translationManager);
        registerConsoleCommand(command);
        registerSlashCommand(command);

        registerEvents(new SlashCommandListener(translationManager, database));

    }

}
