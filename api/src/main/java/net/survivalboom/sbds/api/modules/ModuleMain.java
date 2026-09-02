package net.survivalboom.sbds.api.modules;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.survivalboom.sbds.api.ISBDS;
import net.survivalboom.sbds.api.commands.Command;
import net.survivalboom.sbds.api.commands.CommandExecutor;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.console.ConsoleCommandExecutor;
import net.survivalboom.sbds.api.commands.console.IConsoleListener;
import net.survivalboom.sbds.api.commands.context.ContextCommandExecutor;
import net.survivalboom.sbds.api.commands.context.IContextCommandManager;
import net.survivalboom.sbds.api.commands.slash.ISlashCommandManager;
import net.survivalboom.sbds.api.commands.slash.SlashCommandExecutor;
import net.survivalboom.sbds.api.commands.string.IStringCommandManager;
import net.survivalboom.sbds.api.commands.string.StringCommandExecutor;
import net.survivalboom.sbds.api.database.DataRecord;
import net.survivalboom.sbds.api.database.IDatabase;
import net.survivalboom.sbds.api.database.IRepository;
import net.survivalboom.sbds.api.database.guildconfig.IGuildConfigManager;
import net.survivalboom.sbds.api.database.guildconfig.IGuildConfigTemplate;
import net.survivalboom.sbds.api.database.guilds.IGuildDataManager;
import net.survivalboom.sbds.api.database.members.IMemberDataManager;
import net.survivalboom.sbds.api.database.users.IUserDataManager;
import net.survivalboom.sbds.api.events.EventBase;
import net.survivalboom.sbds.api.events.EventListener;
import net.survivalboom.sbds.api.events.EventPriority;
import net.survivalboom.sbds.api.events.IEventManager;
import net.survivalboom.sbds.api.interaction.component.ComponentInteractionInfo;
import net.survivalboom.sbds.api.interaction.component.IComponentInteractionManager;
import net.survivalboom.sbds.api.interaction.modal.IModalInteractionManager;
import net.survivalboom.sbds.api.interaction.modal.ModalInteractionInfo;
import net.survivalboom.sbds.api.interaction.modal.ModalTemplate;
import net.survivalboom.sbds.api.messages.IMessages;
import net.survivalboom.sbds.api.monitoring.ISystemMonitor;
import net.survivalboom.sbds.api.permissions.IPermissionManager;
import net.survivalboom.sbds.api.permissions.Permission;
import net.survivalboom.sbds.api.registrations.IRegistrationRegistry;
import net.survivalboom.sbds.api.scheduler.IScheduler;
import net.survivalboom.sbds.api.scheduler.ISchedulerTask;
import net.survivalboom.sbds.api.service.IServiceProvider;
import net.survivalboom.sbds.api.translations.ITranslationManager;
import net.survivalboom.sbds.api.utils.CommonUtils;
import net.survivalboom.sbds.api.utils.ThrowingConsumer;
import net.survivalboom.sbds.api.utils.ThrowingRunnable;
import net.survivalboom.sbds.api.utils.placeholders.IPlaceholderRegistry;
import net.survivalboom.sbds.api.utils.placeholders.IPlaceholders;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class ModuleMain {

    private ISBDS sbds;

    private IModule module;

    //
    // LIFECYCLE (will be overridden in module's class)
    //

    @ApiStatus.OverrideOnly
    public void onLoad() throws Exception {}

    @ApiStatus.OverrideOnly
    public void onUnload() throws Exception {}


    @ApiStatus.OverrideOnly
    public void onEnable() throws Exception {}

    @ApiStatus.OverrideOnly
    public void onDisable() throws Exception {}

    //
    // INIT
    //

    @ApiStatus.Internal
    public void init(@NotNull IModule module, @NotNull ISBDS sbds) {

        if (this.module != null) {
            throw new RuntimeException("Сука, ну написано же для таких долбоебов как ты 'Внутреннее API'. Ты слишком тупой чтобы использовать его, понимаешь? Не для тебя оно было сделано.");
        }

        this.module = module;
        this.sbds = sbds;

    }

    //
    // GETTERS
    //

    public @NotNull IModule getModule() {
        return module;
    }

    public @NotNull ISBDS getSbds() {
        return sbds;
    }

    // MODULE INFO //

    public @NotNull String getId() {
        return getModule().getId();
    }

    public @NotNull String getName() {
        return getModule().getName();
    }

    public @NotNull String getVersion() {
        return getModule().getVersion();
    }

    public @Nullable ModuleFile getFile() {
        return getModule().getFile();
    }

    public @NotNull File getDataFolder() {
        return getModule().getDataFolder();
    }

    public @NotNull Logger getLogger() {
        return getModule().getLogger();
    }

    public @NotNull ModuleMeta getMeta() {
        return getModule().getMeta();
    }

    // SERVICES //

    public @NotNull IScheduler getScheduler() {
        return getSbds().getScheduler();
    }

    public @NotNull ISystemMonitor getSystemMonitor() {
        return getSbds().getSystemMonitor();
    }


    public @NotNull IModuleManager getModuleManager() {
        return getSbds().getModuleManager();
    }

    public @NotNull IGuildConfigManager getGuildConfigManager() {
        return getSbds().getGuildConfigManager();
    }

    public @NotNull IRegistrationRegistry getRegistrationRegistry() {
        return getSbds().getRegistrationRegistry();
    }

    public @NotNull IServiceProvider getServiceProvider() {
        return getSbds().getServiceProvider();
    }


    public @NotNull IEventManager getEventManager() {
        return getSbds().getEventManager();
    }

    public @NotNull IPermissionManager getPermissionManager() {
        return getSbds().getPermissionManager();
    }


    public @NotNull IConsoleListener getConsoleListener() {
        return getSbds().getConsoleListener();
    }

    public @NotNull ISlashCommandManager getSlashCommandManager() {
        return getSbds().getSlashCommandManager();
    }

    public @NotNull IContextCommandManager getContextCommandManager() {
        return getSbds().getContextCommandManager();
    }

    public @NotNull IStringCommandManager getStringCommandManager() {
        return getSbds().getStringCommandManager();
    }


    public @NotNull IComponentInteractionManager getComponentInteractionManager() {
        return getSbds().getComponentInteractionManager();
    }

    public @NotNull IModalInteractionManager getModalInteractionManager() {
        return getSbds().getModalInteractionManager();
    }


    public @NotNull IDatabase getDatabase() {
        return getSbds().getDatabase();
    }

    public @NotNull IUserDataManager getUserDataManager() {
        return getSbds().getUserDataManager();
    }

    public @NotNull IGuildDataManager getGuildDataManager() {
        return getSbds().getGuildDataManager();
    }

    public @NotNull IMemberDataManager getMemberDataManager() {
        return getSbds().getMemberDataManager();
    }


    public @NotNull ITranslationManager getTranslationManager() {
        return getSbds().getTranslationManager();
    }

    public @NotNull IMessages getMessages() {
        return getSbds().getMessages();
    }

    public @NotNull IPlaceholderRegistry getPlaceholderRegistry() {
        return getSbds().getPlaceholderRegistry();
    }

    //
    // REGISTRATIONS
    //

    // DATABASE //

    public <T extends DataRecord> IRepository<T> createRepository(@NotNull Class<T> clazz) {
        return getDatabase().createRepository(this, clazz);
    }

    public <T extends DataRecord> IRepository<T> getRepository(@NotNull String name, @NotNull Class<T> clazz) {
        return getDatabase().getRepository(name, clazz);
    }

    // SERVICE PROVIDER //

    public <T> IServiceProvider.IRegisteredService<T> registerService(@NotNull Class<T> clazz, T object) {
        return getServiceProvider().registerService(this, clazz, object);
    }

    public <T> T getService(@NotNull Class<T> clazz) {
        return getServiceProvider().getService(clazz);
    }

    public <T> IServiceProvider.IRegisteredService<T> getRegisteredService(@NotNull Class<T> clazz) {
        return getServiceProvider().getRegisteredService(clazz);
    }

    // EVENTS //

    public <T> IEventManager.@NotNull IRegisteredEventHandler registerEvent(@NotNull Class<T> clazz, @NotNull ThrowingConsumer<T> consumer, @NotNull EventPriority priority, boolean ignoreCancelled) {
        return getEventManager().registerEvent(this, clazz, consumer, priority, ignoreCancelled);
    }

    public void registerEvents(@NotNull EventListener listener) {
        getEventManager().registerEvents(this, listener);
    }

    // COMMANDS //

    public void registerCommand(@NotNull Command command) {

        CommandExecutor executor = command.getExecutor();

        if (executor instanceof ConsoleCommandExecutor) {
            registerConsoleCommand(command);
        }

        if (executor instanceof SlashCommandExecutor) {
            registerSlashCommand(command);
        }

        if (executor instanceof ContextCommandExecutor) {
            registerContextCommand(command);
        }

        if (executor instanceof StringCommandExecutor) {
            registerStringCommand(command);
        }

    }

    public void registerCommand(@NotNull CommandBase command) {

        if (command instanceof ConsoleCommandExecutor) {
            registerConsoleCommand(command);
        }

        if (command instanceof SlashCommandExecutor) {
            registerSlashCommand(command);
        }

        if (command instanceof ContextCommandExecutor) {
            registerContextCommand(command);
        }

        if (command instanceof StringCommandExecutor) {
            registerStringCommand(command);
        }

    }

   // CONSOLE COMMANDS //

    public @NotNull IConsoleListener.IRegisteredConsoleCommand registerConsoleCommand(@NotNull Command command) {
        return sbds.getConsoleListener().registerCommand(this, command);
    }

    public @NotNull IConsoleListener.IRegisteredConsoleCommand registerConsoleCommand(@NotNull CommandBase command) {
        return sbds.getConsoleListener().registerCommand(this, command);
    }

    // SLASH COMMANDS //

    public @NotNull ISlashCommandManager.IRegisteredSlashCommand registerSlashCommand(@NotNull Command command) {
        return sbds.getSlashCommandManager().registerCommand(this, command);
    }

    public @NotNull ISlashCommandManager.IRegisteredSlashCommand registerSlashCommand(@NotNull CommandBase command) {
        return sbds.getSlashCommandManager().registerCommand(this, command);
    }

    // CONTEXT COMMANDS //

    public @NotNull IContextCommandManager.IRegisteredContextCommand registerContextCommand(@NotNull Command command) {
        return sbds.getContextCommandManager().registerCommand(this, command);
    }

    public @NotNull IContextCommandManager.IRegisteredContextCommand registerContextCommand(@NotNull CommandBase command) {
        return sbds.getContextCommandManager().registerCommand(this, command);
    }

    // STRING COMMANDS //

    public @NotNull IStringCommandManager.IRegisteredStringCommand registerStringCommand(@NotNull Command command) {
        return sbds.getStringCommandManager().registerCommand(this, command);
    }

    public @NotNull IStringCommandManager.IRegisteredStringCommand registerStringCommand(@NotNull CommandBase command) {
        return sbds.getStringCommandManager().registerCommand(this, command);
    }

    // COMPONENTS //

    public <T extends GenericComponentInteractionCreateEvent> IComponentInteractionManager.@NotNull IRegisteredListener<T> registerComponent(
            @NotNull String name,
            @NotNull Class<T> clazz,
            @NotNull Consumer<ComponentInteractionInfo<T>> consumer,
            @Nullable Permission permission
    ) {
        return getComponentInteractionManager().registerListener(this, name, clazz, consumer, permission);
    }

    public <T extends GenericComponentInteractionCreateEvent> IComponentInteractionManager.@NotNull IRegisteredListener<T> registerComponent(
            @NotNull String name,
            @NotNull Class<T> clazz,
            @NotNull Consumer<ComponentInteractionInfo<T>> consumer
    ) {
        return getComponentInteractionManager().registerListener(this, name, clazz, consumer, null);
    }

    // BUTTONS //

    public @NotNull IComponentInteractionManager.IRegisteredListener<ButtonInteractionEvent> registerButton(
            @NotNull String name,
            @NotNull Consumer<ComponentInteractionInfo<ButtonInteractionEvent>> executor,
            @Nullable Permission permission
    ) {
        return getComponentInteractionManager().registerButton(this, name, executor, permission);
    }

    public @NotNull IComponentInteractionManager.IRegisteredListener<ButtonInteractionEvent> registerButton(
            @NotNull String name,
            @NotNull Consumer<ComponentInteractionInfo<ButtonInteractionEvent>> executor
    ) {
        return getComponentInteractionManager().registerButton(this, name, executor, null);
    }

    // ENTITY SELECT //

    public @NotNull IComponentInteractionManager.IRegisteredListener<EntitySelectInteractionEvent> registerEntityDropdown(
            @NotNull String name,
            @NotNull Consumer<ComponentInteractionInfo<EntitySelectInteractionEvent>> executor,
            @Nullable Permission permission
    ) {
        return getComponentInteractionManager().registerEntityDropdown(this, name, executor, permission);
    }

    public @NotNull IComponentInteractionManager.IRegisteredListener<EntitySelectInteractionEvent> registerEntityDropdown(
            @NotNull String name,
            @NotNull Consumer<ComponentInteractionInfo<EntitySelectInteractionEvent>> executor
            ) {
        return getComponentInteractionManager().registerEntityDropdown(this, name, executor, null);
    }

    // STRING SELECT //

    public @NotNull IComponentInteractionManager.IRegisteredListener<StringSelectInteractionEvent> registerStringDropdown(
            @NotNull String name,
            @NotNull Consumer<ComponentInteractionInfo<StringSelectInteractionEvent>> executor,
            @Nullable Permission permission
    ) {
        return getComponentInteractionManager().registerStringDropdown(this, name, executor, permission);
    }

    public @NotNull IComponentInteractionManager.IRegisteredListener<StringSelectInteractionEvent> registerStringDropdown(
            @NotNull String name,
            @NotNull Consumer<ComponentInteractionInfo<StringSelectInteractionEvent>> executor
            ) {
        return getComponentInteractionManager().registerStringDropdown(this, name, executor, null);
    }

    // MODAL //

    public @NotNull IModalInteractionManager.IRegisteredModal registerModal(@NotNull String name, @NotNull ModalTemplate template, @Nullable Consumer<ModalInteractionInfo> executor) {
        return getModalInteractionManager().registerModal(this, name, template, executor);
    }

    public @NotNull IModalInteractionManager.IRegisteredModal registerModal(@NotNull String name, @NotNull ModalTemplate template) {
        return getModalInteractionManager().registerModal(this, name, template);
    }


    public @NotNull IModalInteractionManager.IRegisteredModal registerModal(@NotNull String name, @NotNull Consumer<ModalTemplate.Builder> builder, @NotNull Consumer<ModalInteractionInfo> executor) {
        return getModalInteractionManager().registerModal(this, name, builder, executor);
    }

    public @NotNull IModalInteractionManager.IRegisteredModal registerModal(@NotNull String name, @NotNull Consumer<ModalTemplate.Builder> builder) {
        return getModalInteractionManager().registerModal(this, name, builder);
    }

    // PLACEHOLDERS //

    public <V> IPlaceholderRegistry.@NotNull IRegisteredPlaceholderProvider<V> registerProvider(@NotNull Class<V> clazz, @NotNull Function<V, IPlaceholders> function) {
        return getPlaceholderRegistry().registerProvider(this, clazz, function);
    }

    // TRANSLATIONS //

    public void addModuleTranslations() {
        addModuleTranslations("translations");
    }

    public void addModuleTranslations2(@NotNull String... files) {

        Map<String, String> map = new HashMap<>();
        for (String file : files) {
            map.put("translations/" + file, "translations/" + file);
        }

        checkFiles(map);
        addModuleTranslations();

    }

    public void addModuleTranslations(@NotNull String directory) {
        sbds.getTranslationManager().importModuleMessages(this, directory);
    }

    // SCHEDULER //

    public @NotNull ISchedulerTask schedule(@Nullable String name, @NotNull ThrowingConsumer<ISchedulerTask> task, int delay, int period) {
        return getScheduler().schedule(this, name, task, delay, period);
    }

    public @NotNull ISchedulerTask schedule(@NotNull ThrowingConsumer<ISchedulerTask> task, int delay, int period) {
        return getScheduler().schedule(this, task, delay, period);
    }

    public @NotNull ISchedulerTask schedule(@Nullable String name, @NotNull ThrowingRunnable task, int delay, int period) {
        return getScheduler().schedule(this, name, task, delay, period);
    }

    public @NotNull ISchedulerTask schedule(@NotNull ThrowingRunnable task, int delay, int period) {
        return getScheduler().schedule(this, task, delay, period);
    }

    public @NotNull ISchedulerTask schedule(@NotNull ThrowingRunnable runnable) {
        return getScheduler().schedule(this, runnable);
    }

    // EVENT HANDLER //

    public <T extends EventBase> @NotNull T callEvent(@NotNull T event) {
        return getEventManager().callEvent(event);
    }

    //
    // MODULE FILES
    //

    public void checkFiles(@NotNull Map<String, String> map) {
        CommonUtils.checkFiles(this.getClass(), getModule().getDataFolder(), map, null);
    }

    public void checkFiles(@NotNull String... files) {
        CommonUtils.checkFiles(this.getClass(), getModule().getDataFolder(), null, files);
    }

    public void checkFiles2(@NotNull String... files) {

        Map<String, String> map = new HashMap<>();
        for (String file : files) {
            map.put(file, file);
        }

        checkFiles(map);

    }

    //
    // CONFIG
    //

    public @NotNull ConfigurationNode loadConfig(@NotNull File file) {
        return getModule().loadConfig(file);
    }

    public @NotNull ConfigurationNode loadConfig() {
        return getModule().loadConfig();
    }


    public void saveConfig(@NotNull File file) {
        getModule().saveConfig(file);
    }

    public void saveConfig() {
        getModule().saveConfig();
    }


    public @NotNull ConfigurationNode checkAndLoadConfig(@NotNull String fileName) {
        return getModule().checkAndLoadConfig(fileName);
    }

    public @NotNull ConfigurationNode checkAndLoadConfig() {
        return getModule().checkAndLoadConfig();
    }


    public @NotNull ConfigurationNode getConfig() {
        return getModule().getConfig();
    }

    //
    // GUILD CONFIG
    //

    public @NotNull IGuildConfigTemplate createGuildConfig(@NotNull Consumer<IGuildConfigManager.IGuildConfigBuilder> consumer) {
        return module.createGuildConfig(consumer);
    }

    public IGuildConfigTemplate getGuildConfig() {
        return module.getGuildConfig();
    }

}
