package net.survivalboom.sbds.api;

import net.dv8tion.jda.api.JDA;
import net.survivalboom.sbds.api.commands.slash.ISlashCommandManager;
import net.survivalboom.sbds.api.commands.console.IConsoleListener;
import net.survivalboom.sbds.api.commands.string.IStringCommandManager;
import net.survivalboom.sbds.api.database.IDatabase;
import net.survivalboom.sbds.api.database.guildconfig.IGuildConfigManager;
import net.survivalboom.sbds.api.database.guilds.IGuildDataManager;
import net.survivalboom.sbds.api.database.members.IMemberDataManager;
import net.survivalboom.sbds.api.database.users.IUserDataManager;
import net.survivalboom.sbds.api.events.EventCancellableBase;
import net.survivalboom.sbds.api.events.IEventManager;
import net.survivalboom.sbds.api.interaction.component.IComponentInteractionManager;
import net.survivalboom.sbds.api.commands.context.IContextCommandManager;
import net.survivalboom.sbds.api.interaction.modal.IModalInteractionManager;
import net.survivalboom.sbds.api.libraries.ILibrariesManager;
import net.survivalboom.sbds.api.messages.IMessages;
import net.survivalboom.sbds.api.modules.IModuleManager;
import net.survivalboom.sbds.api.monitoring.ISystemMonitor;
import net.survivalboom.sbds.api.permissions.IPermissionManager;
import net.survivalboom.sbds.api.utils.placeholders.IPlaceholderRegistry;
import net.survivalboom.sbds.api.registrations.IRegistrationRegistry;
import net.survivalboom.sbds.api.scheduler.IScheduler;
import net.survivalboom.sbds.api.service.IServiceProvider;
import net.survivalboom.sbds.api.translations.ITranslationManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;

import java.io.File;

public interface ISBDS {

    @NotNull String getVersion();


    @NotNull IScheduler getScheduler();

    @NotNull ISystemMonitor getSystemMonitor();


    @NotNull IModuleManager getModuleManager();

    @NotNull IGuildConfigManager getGuildConfigManager();

    @NotNull IRegistrationRegistry getRegistrationRegistry();

    @NotNull IServiceProvider getServiceProvider();

    @NotNull ILibrariesManager getLibrariesManager();


    @NotNull IEventManager getEventManager();


    @NotNull IPermissionManager getPermissionManager();

    @NotNull IConsoleListener getConsoleListener();

    @NotNull ISlashCommandManager getSlashCommandManager();

    @NotNull IContextCommandManager getContextCommandManager();

    @NotNull IStringCommandManager getStringCommandManager();


    @NotNull IComponentInteractionManager getComponentInteractionManager();

    @NotNull IModalInteractionManager getModalInteractionManager();


    @NotNull IDatabase getDatabase();

    @NotNull IUserDataManager getUserDataManager();

    @NotNull IMemberDataManager getMemberDataManager();

    @NotNull IGuildDataManager getGuildDataManager();


    @NotNull ITranslationManager getTranslationManager();

    @NotNull IMessages getMessages();

    @NotNull IPlaceholderRegistry getPlaceholderRegistry();


    @NotNull File getWorkingDir();

    @NotNull ConfigurationNode getConfiguration();

    @NotNull Logger getLogger();

    @NotNull JDA getBot();


    boolean isReady();

    boolean isStarted();


    void shutdown();


    // Ну что, пахімічім?
    // https://www.youtube.com/watch?v=j5u4Z1ItvCQ
    class SbdsReadyEvent extends EventCancellableBase {

        public SbdsReadyEvent(@NotNull ISBDS sbds) {
            super(sbds);
        }

    }

}
