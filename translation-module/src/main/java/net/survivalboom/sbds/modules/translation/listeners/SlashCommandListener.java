package net.survivalboom.sbds.modules.translation.listeners;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.survivalboom.sbds.api.database.IDatabase;
import net.survivalboom.sbds.api.database.users.IUserData;
import net.survivalboom.sbds.api.database.users.IUserRepositoryHandler;
import net.survivalboom.sbds.api.events.EventHandler;
import net.survivalboom.sbds.api.events.Listener;
import net.survivalboom.sbds.api.translations.ITranslation;
import net.survivalboom.sbds.api.translations.ITranslationManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlashCommandListener implements Listener {

    private static final Logger log = LoggerFactory.getLogger(SlashCommandListener.class);
    private final ITranslationManager translationManager;

    private final IUserRepositoryHandler repository;

    public SlashCommandListener(@NotNull ITranslationManager translationManager, @NotNull IDatabase database) {
        this.translationManager = translationManager;
        this.repository = database.getRepositoryHandler("sbds:users", IUserRepositoryHandler.class);
    }

    @EventHandler
    public void onSlashCommand(SlashCommandInteractionEvent event) {

        IUserData userData = repository.createUser(event.getUser());
        if (userData.translation() != null) return;

        DiscordLocale locale = event.getUserLocale();
        ITranslation translation = translationManager.findTranslationByLocale(locale);
        if (translation == null) return;

        userData.translation(translation);
        userData.save();

        log.info("Successfully set `{}` for `{}` based on user's discord locale `{}`.", translation.getName(), event.getUser().getEffectiveName(), locale);

    }

}
