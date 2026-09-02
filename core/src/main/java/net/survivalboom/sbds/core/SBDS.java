package net.survivalboom.sbds.core;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.survivalboom.sbds.api.ISBDS;
import net.survivalboom.sbds.api.SbdsProvider;
import net.survivalboom.sbds.api.database.guildconfig.IGuildConfigManager;
import net.survivalboom.sbds.api.database.members.IMemberDataManager;
import net.survivalboom.sbds.api.interaction.component.IComponentInteractionManager;
import net.survivalboom.sbds.api.utils.CommonUtils;
import net.survivalboom.sbds.api.utils.placeholders.IPlaceholderRegistry;
import net.survivalboom.sbds.api.utils.sixseven.DinosaurDeathException;
import net.survivalboom.sbds.core.commands.console.ConsoleListener;
import net.survivalboom.sbds.core.commands.context.ContextCommandManager;
import net.survivalboom.sbds.core.commands.slash.SlashCommandManager;
import net.survivalboom.sbds.core.commands.string.StringCommandManager;
import net.survivalboom.sbds.core.database.Database;
import net.survivalboom.sbds.core.database.guildconfig.GuildConfigManager;
import net.survivalboom.sbds.core.database.guilds.GuildDataManager;
import net.survivalboom.sbds.core.database.member.MemberDataManager;
import net.survivalboom.sbds.core.database.users.UserDataManager;
import net.survivalboom.sbds.core.events.EventManager;
import net.survivalboom.sbds.core.interaction.component.ComponentInteractionManager;
import net.survivalboom.sbds.core.interaction.command.CommandInteractionManager;
import net.survivalboom.sbds.core.interaction.modal.ModalInteractionManager;
import net.survivalboom.sbds.core.libraries.LibrariesManager;
import net.survivalboom.sbds.core.logging.LoggerFilter;
import net.survivalboom.sbds.core.messages.Messages;
import net.survivalboom.sbds.core.modules.ModuleManager;
import net.survivalboom.sbds.core.monitor.SystemMonitor;
import net.survivalboom.sbds.core.permissions.PermissionManager;
import net.survivalboom.sbds.core.registration.RegistrationRegistry;
import net.survivalboom.sbds.core.scheduler.Scheduler;
import net.survivalboom.sbds.core.service.ServiceProvider;
import net.survivalboom.sbds.core.translations.TranslationManager;
import net.survivalboom.sbds.core.utils.placeholders.PlaceholderRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.ChunkingFilter;

import java.io.File;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

public class SBDS implements ISBDS {

    private final Logger logger;

    private final ConfigurationNode configuration;

    private final File workingDir;

    private final LibrariesManager librariesManager;


    private final LoggerFilter loggerFilter;


    private final Scheduler scheduler;

    private final SystemMonitor systemMonitor;


    private final Database database;

    private final UserDataManager userDataManager;

    private final MemberDataManager memberDataManager;

    private final GuildDataManager guildDataManager;

    private final GuildConfigManager guildConfigManager;


    private final ModuleManager moduleManager;

    private final RegistrationRegistry registrationRegistry;

    private final ServiceProvider serviceProvider;

    private final EventManager eventManager;


    private final CommandInteractionManager commandInteractionManager;

    private final ConsoleListener consoleListener;

    private final SlashCommandManager slashCommandManager;

    private final StringCommandManager stringCommandManager;

    private final ContextCommandManager contextCommandManager;

    private final PermissionManager permissionManager;


    private final ComponentInteractionManager componentInteractionManager;

    private final ModalInteractionManager modalInteractionManager;


    private final TranslationManager translationManager;

    private final Messages messages;

    private final PlaceholderRegistry placeholderRegistry;


    private boolean started = false;

    private boolean ready = false;

    private boolean shutdownInitiated = false;

    private boolean shutDownCompleted = false;


    private final JDABuilder jdaBuilder;

    private static final EnumSet<GatewayIntent> ALL_GATEWAY_INTENTS = EnumSet.allOf(GatewayIntent.class);

    private JDA bot = null;


    public SBDS(
            @NotNull Logger logger,
            @NotNull LibrariesManager librariesManager,
            @NotNull ConfigurationNode configuration,
            @NotNull File workingDir,
            @NotNull String token
    ) {

        librariesManager.sbds = this;

        this.logger = logger;
        this.configuration = configuration;
        this.jdaBuilder = createJdaBuilder(token, configuration);
        this.workingDir = workingDir;

        this.librariesManager = librariesManager;

        this.registrationRegistry = new RegistrationRegistry(this);
        this.loggerFilter = new LoggerFilter(this);

        this.scheduler = new Scheduler(this);
        this.systemMonitor = new SystemMonitor(scheduler);

        this.database = new Database(this);
        this.userDataManager = new UserDataManager(this);
        this.memberDataManager = new MemberDataManager(this);
        this.guildDataManager = new GuildDataManager(this);
        this.guildConfigManager = new GuildConfigManager(this);

        this.eventManager = new EventManager(this);
        this.moduleManager = new ModuleManager(this);
        this.serviceProvider = new ServiceProvider(this);

        this.translationManager = new TranslationManager(this);
        this.messages = new Messages(this);
        this.placeholderRegistry = new PlaceholderRegistry(this);

        this.consoleListener = new ConsoleListener(this);
        this.permissionManager = new PermissionManager(this);
        this.commandInteractionManager = new CommandInteractionManager(this);
        this.contextCommandManager = new ContextCommandManager(this);
        this.slashCommandManager = new SlashCommandManager(this);
        this.stringCommandManager = new StringCommandManager(this);

        this.componentInteractionManager = new ComponentInteractionManager(this);
        this.modalInteractionManager = new ModalInteractionManager(this);

        SbdsProvider.internal_internal_internal_internal_internal_internal_set(this);

    }

    //
    // LIFECYCLE
    //

    public synchronized void run() throws InterruptedException {

        if (started) {
            throw new IllegalStateException("Already started");
        }

        started = true;

        registrationRegistry.init();
        loggerFilter.init();

        scheduler.init();
        systemMonitor.init();

        database.init();
        if (database.isFailed()) {
            throw new RuntimeException("Database initialization failed");
        }

        logger.info("Logging in...");

        try {
            bot = jdaBuilder.build();
        }

        catch (InvalidTokenException e) {
            logger.warn("Bot token is invalid.");
            throw new RuntimeException(e);
        }

        bot.awaitReady();

        logger.info("Logged successfully! ({}#{})", bot.getSelfUser().getName(), bot.getSelfUser().getDiscriminator());

        bot.getPresence().setPresence(OnlineStatus.DO_NOT_DISTURB, Activity.customStatus("Starting SBDS v" + BuildConstants.VERSION + "..."));

        userDataManager.init();
        memberDataManager.init();
        guildDataManager.init();
        guildConfigManager.init();

        translationManager.init();
        messages.init();
        placeholderRegistry.init();

        permissionManager.init();
        consoleListener.init();

        eventManager.init();

        commandInteractionManager.init();
        slashCommandManager.init();
        stringCommandManager.init();
        contextCommandManager.init();

        componentInteractionManager.init();
        modalInteractionManager.init();

        serviceProvider.init();

        moduleManager.init();

        bot.getPresence().setPresence(OnlineStatus.IDLE, Activity.customStatus("Running on SBDS v" + BuildConstants.VERSION + "🦖"));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdown();
            CommonUtils.waitUntil(() -> shutDownCompleted, 300000);
        }));

        logger.info("");
        logger.info("SurvivalBoom Discord Service successfully started!");
        logger.info("");

        ready = true;

        SbdsReadyEvent readyEvent = eventManager.callEvent0(new SbdsReadyEvent(this));
        if (readyEvent.isCancelled()) {
            logger.error("SbdsReadyEvent was cancelled! INITIATING SELF-DESTRUCTION PROTOCOL NOW!!!!");
            throw new DinosaurDeathException();
        }

        // Входимо у нескінченний цикл очікування запиту на вимкнення бота //
        CommonUtils.waitUntil(() -> shutdownInitiated, 0, 1000, null, null);

        ready = false;

        try {
            shutdown0();
        }

        catch (Throwable t) {
            logger.error("Failed to shutdown SBDS properly! This may cause data loss.", t);
        }

        started = false;

    }

    @Override
    public void shutdown() {
        this.shutdownInitiated = true;
    }

    private void shutdown0() {

        logger.info("");
        logger.info("Stopping SurvivalBoom Discord Service...");

        bot.getPresence().setPresence(OnlineStatus.DO_NOT_DISTURB, Activity.customStatus("Shutting down SBDS v" + BuildConstants.VERSION + "..."));

        moduleManager.shutdown();

        consoleListener.shutdown();
        slashCommandManager.shutdown();
        stringCommandManager.shutdown();
        permissionManager.shutdown();
        contextCommandManager.shutdown();
        commandInteractionManager.shutdown();

        componentInteractionManager.shutdown();
        modalInteractionManager.shutdown();

        placeholderRegistry.shutdown();
        translationManager.shutdown();
        messages.shutdown();

        eventManager.shutdown();

        guildConfigManager.shutdown();
        userDataManager.shutdown();
        memberDataManager.shutdown();
        guildDataManager.shutdown();
        database.shutdown();

        systemMonitor.shutdown();

        scheduler.shutdown();

        logger.info("Stopping bot...");

        bot.shutdown();
        bot = null;

        loggerFilter.shutdown();

        registrationRegistry.shutdown();

        logger.info("Bye bye!");

        this.shutDownCompleted = true;

    }

    //
    // GETTERS
    //

    @Override
    public @NotNull Logger getLogger() {
        return logger;
    }

    @Override
    public @NotNull ConfigurationNode getConfiguration() {
        return configuration;
    }

    @Override
    public @NotNull JDA getBot() {
        return bot;
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public @NotNull File getWorkingDir() {
        return workingDir;
    }


    @Override
    public @NotNull ModuleManager getModuleManager() {
        return moduleManager;
    }

    @Override
    public @NotNull RegistrationRegistry getRegistrationRegistry() {
        return registrationRegistry;
    }

    @Override
    public @NotNull ServiceProvider getServiceProvider() {
        return serviceProvider;
    }

    @Override
    public @NotNull EventManager getEventManager() {
        return eventManager;
    }

    @Override
    public @NotNull PermissionManager getPermissionManager() {
        return permissionManager;
    }

    @Override
    public @NotNull ConsoleListener getConsoleListener() {
        return consoleListener;
    }

    @Override
    public @NotNull SlashCommandManager getSlashCommandManager() {
        return slashCommandManager;
    }

    @Override
    public @NotNull ContextCommandManager getContextCommandManager() {
        return contextCommandManager;
    }

    @Override
    public @NotNull StringCommandManager getStringCommandManager() {
        return stringCommandManager;
    }

    @Override
    public @NotNull IComponentInteractionManager getComponentInteractionManager() {
        return componentInteractionManager;
    }


    @Override
    public @NotNull ModalInteractionManager getModalInteractionManager() {
        return modalInteractionManager;
    }

    @Override
    public @NotNull Database getDatabase() {
        return database;
    }

    @Override
    public @NotNull UserDataManager getUserDataManager() {
        return userDataManager;
    }

    @Override
    public @NotNull IMemberDataManager getMemberDataManager() {
        return memberDataManager;
    }

    @Override
    public @NotNull IGuildConfigManager getGuildConfigManager() {
        return guildConfigManager;
    }

    @Override
    public @NotNull GuildDataManager getGuildDataManager() {
        return guildDataManager;
    }

    @Override
    public @NotNull TranslationManager getTranslationManager() {
        return translationManager;
    }

    @Override
    public @NotNull Messages getMessages() {
        return messages;
    }

    @Override
    public @NotNull IPlaceholderRegistry getPlaceholderRegistry() {
        return placeholderRegistry;
    }

    @Override
    public @NotNull String getVersion() {
        return BuildConstants.VERSION;
    }

    @Override
    public @NotNull String getGitCommit() {
        return BuildConstants.GIT_COMMIT;
    }

    @Override
    public @NotNull String getCompiledBy() {
        return BuildConstants.COMPILED_BY;
    }

    @Override
    public @NotNull Scheduler getScheduler() {
        return scheduler;
    }

    @Override
    public @NotNull SystemMonitor getSystemMonitor() {
        return systemMonitor;
    }

    @Override
    public @NotNull LibrariesManager getLibrariesManager() {
        return librariesManager;
    }


    public @NotNull CommandInteractionManager getCommandInteractionManager() {
        return commandInteractionManager;
    }

    // JDA BUILDER //

    private @NotNull JDABuilder createJdaBuilder(@NotNull String token, @NotNull ConfigurationNode configuration) {

        Collection<GatewayIntent> intents = resolveGatewayIntents(configuration);

        JDABuilder builder = JDABuilder.createDefault(token, intents);

        // Без цього виникають баги за певних обставин, бо JDA кешував не всіх учасників
        if (intents.contains(GatewayIntent.GUILD_MEMBERS)) {
            builder.setMemberCachePolicy(MemberCachePolicy.ALL);
            builder.setChunkingFilter(ChunkingFilter.ALL);
        }

        return builder;

    }

    // GATEWAY INTENTS //

    private @NotNull Collection<GatewayIntent> resolveGatewayIntents(@NotNull ConfigurationNode configuration) {

        List<GatewayIntent> intents = resolveGatewayIntents0(configuration);
        if (intents == null || intents.isEmpty()) {
            return ALL_GATEWAY_INTENTS;
        }

        logger.info("Using configured gateway intents: {}", String.join(", ", intents.stream().map(GatewayIntent::toString).toList()));

        return intents;

    }

    private @Nullable List<GatewayIntent> resolveGatewayIntents0(@NotNull ConfigurationNode configuration) {

        ConfigurationNode node = configuration.node("bot").node("gateway-intents");
        if (node.virtual() || !node.isList()) {
            return null;
        }

        List<GatewayIntent> intents;
        try {
            intents = node.getList(GatewayIntent.class);
        }

        catch (SerializationException e) {
            logger.error("Failed to load Gateway intents. You broke the configuration file. Live with it. \n - {}", e.getMessage());
            Main.exit();
            return null;
        }

        return intents;

    }

    //
    // STATIC
    //

    public static @NotNull SBDS getInstance() {
        return sbds;
    }

    protected static SBDS sbds = null;

}
