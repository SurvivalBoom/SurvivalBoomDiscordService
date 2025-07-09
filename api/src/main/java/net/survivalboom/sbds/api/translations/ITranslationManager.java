package net.survivalboom.sbds.api.translations;

import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.survivalboom.sbds.api.modules.IModule;
import net.survivalboom.sbds.api.modules.ModuleMain;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public interface ITranslationManager {

    @Nullable ITranslation getTranslation(@NotNull String name);

    @NotNull List<ITranslation> getTranslations();


    @Nullable ITranslation defaultTranslation();

    @Nullable ITranslation fallbackTranslation();

    default void addModuleTranslations(@NotNull ModuleMain moduleMain) {
        addModuleTranslations(moduleMain.getModule());
    }

    void addModuleTranslations(@NotNull IModule module);


    @Nullable ITranslation findTranslationByLocale(@NotNull DiscordLocale locale);

    @NotNull Set<DiscordLocale> getAvailableLocales();

}
