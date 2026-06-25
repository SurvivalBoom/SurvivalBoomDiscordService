package net.survivalboom.sbds.core.commands.string;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.survivalboom.sbds.api.ISBDS;
import net.survivalboom.sbds.api.commands.ArgumentScope;
import net.survivalboom.sbds.api.commands.Command;
import net.survivalboom.sbds.api.commands.CommandExecutor;
import net.survivalboom.sbds.api.commands.argument.ArgumentParseException;
import net.survivalboom.sbds.api.commands.argument.ArgumentParsingContext;
import net.survivalboom.sbds.api.commands.argument.misc.SubCommandArgument;
import net.survivalboom.sbds.api.commands.string.IStringCommandManager;
import net.survivalboom.sbds.api.commands.string.StringCommandExecutor;
import net.survivalboom.sbds.api.commands.string.StringExecutionInfo;
import net.survivalboom.sbds.api.database.guildconfig.IGuildConfigManager;
import net.survivalboom.sbds.api.events.EventHandler;
import net.survivalboom.sbds.api.events.EventListener;
import net.survivalboom.sbds.api.permissions.Permission;
import net.survivalboom.sbds.api.registrations.Registration;
import net.survivalboom.sbds.core.SBDS;
import net.survivalboom.sbds.core.commands.AbstractCommandManager;
import net.survivalboom.sbds.core.commands.cmds.common.StatusCommand;
import net.survivalboom.sbds.core.commands.console.ConsoleListener;
import net.survivalboom.sbds.core.commands.parser.StringCommandParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class StringCommandManager extends AbstractCommandManager<IStringCommandManager.IRegisteredStringCommand, IStringCommandManager> implements IStringCommandManager, EventListener {

    private final IGuildConfigManager guildConfigManager;

    public StringCommandManager(@NotNull SBDS sbds) {
        super(sbds);
        this.guildConfigManager = sbds.getGuildConfigManager();
    }

    //
    // MANAGER
    //

    @Override
    protected void init0() {

        super.init0();

        sbds.getEventManager().registerEvents0(null, this);

        registerCommand0(null, new StatusCommand()).setDMGlobal(true);

    }

    @Override
    protected void shutdown0() {
        sbds.getEventManager().unregisterEvents(this);
        super.shutdown0();
    }

    @Override
    public void onRegister(@NotNull Registration<IRegisteredStringCommand> registration) {

        Command command = registration.object().getCommand();
        CommandExecutor executor = command.getExecutor();
        if (executor == null) {
            return;
        }

        if (!(executor instanceof StringCommandExecutor)) {
            throw new IllegalArgumentException("Command `" + command.getName() + "` does not have executor for a string command");
        }

    }

    @Override
    protected @NotNull IStringCommandManager.IRegisteredStringCommand createCommandReg(@NotNull Command command) {
        return new RegisteredStringCommand(this, command);
    }


    @EventHandler
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {

        User author = event.getAuthor();
        Guild guild = event.isFromGuild() ? event.getGuild() : null;
        if (author.isBot() || author.isSystem()) {
            return;
        }

        Message message = event.getMessage();
        String content = message.getContentRaw();

        // Визначаємо префікс для поточного місця виконання команди //

        String prefix;
        if (event.isFromGuild()) {
            // TODO: Цей шматок коду буде блокувати увесь потік подій, що не є певно добре, але це буде відбуватись всього раз для кожного серверу тож певно можна поки лишити так.
            prefix = guildConfigManager.getTemplate("sbds")
                    .obtainConfig(guild)
                    .get("prefix", String.class)
                    .join()
                    .orElseThrow();
        }

        else {
            prefix = STRING_COMMAND_PREFIX;
        }

        // Пропускаємо повідомлення, якщо не починається з нашого префіксу.
        if (!content.startsWith(prefix)) {
            return;
        }

        // Визначаємо команду яку було виконано //
        String string = content.substring(prefix.length()).strip();
        String rootCmdName = StringCommandParser.getPrefix(string);

        if (string.isBlank()) {
            return;
        }

        String argsRaw = string.substring(rootCmdName.length()).strip();

        IRegisteredStringCommand stringCommand = getByAlias(rootCmdName);
        if (stringCommand == null) {
            message.addReaction(Emoji.fromUnicode("❓")).queue();
            return;
        }

        // Визначаємо де було виконано команду та чи дозволено виконувати цю команду тут //

        MessageChannelUnion channel = event.getChannel();
        boolean isDM = channel instanceof PrivateChannel;

        if (isDM && !stringCommand.isDMGlobal() && !stringCommand.getUserRegistrations().contains(author)) {
            return;
        }

        if (!isDM && !stringCommand.isGuildGlobal() && !stringCommand.getGuildRegistrations().contains(guild)) {
            return;
        }

        // Виконуємо команду //

        Command command = stringCommand.getCommand();

        if (isDM) {
            logger.info("[DM] User &b{} &rused command &e{}", author.getEffectiveName(), content);
        }

        else {
            logger.info("[{}:{}] User &b{} &rused command &e{}", guild.getName(), channel.getName(), author.getEffectiveName(), content);
        }

        Message resp = null;
        try {

            resp = command.isDeferReply() ? (Message) messages.reply(message, "sbds.loading", author).send().complete() : null;

            var result = StringCommandParser.parseInput(argsRaw, command, ArgumentScope.STRING, argument -> new ArgumentParsingContext(stringCommand, command, argument));
            var toExecute = new ArrayList<>(result.foundSubcommands());
            toExecute.addFirst(new SubCommandArgument.SubCommand(command, rootCmdName));

            if (!isDM) {
                Permission permission = checkPermission(toExecute, guild, author);
                if (permission != null) {
                    sbds.getMessages().reply(message, "sbds.no-permission", author)
                            .withPlaceholders("permission", permission.permission())
                            .queue();
                    return;
                }
            }

            boolean hasReply = false;
            for (SubCommandArgument.SubCommand execute : toExecute) {

                StringExecutionInfo info = new StringExecutionInfo(message, stringCommand, command, rootCmdName, result.arguments());
                StringCommandExecutor executor = (StringCommandExecutor) execute.command().getExecutor();

                info.response(resp);

                if (executor != null) {
                    executor.executes(info);
                }

                if (info.wasReplied()) {
                    hasReply = true;
                }

            }

            if (!hasReply) {

                if (resp != null) {
                    resp.delete().queue();
                }

                sbds.getMessages().reply(message, "sbds.interaction-no-response", author).queue();
                logger.error("Command did not respond to the request. Did something go wrong?");

            }

        }

        catch (StringCommandParser.ArgumentParsingException e) {

            String argumentName = e.getArgument().name();
            String inputRaw = e.getInput();

            if (resp != null) resp.delete().queue();

            Throwable cause = e.getCause();
            if (!(cause instanceof ArgumentParseException argumentParseException)) {
                sbds.getMessages().reply(message, "sbds.string-argument-error", author)
                        .withPlaceholders("argument", argumentName, "input", inputRaw, "exception", cause.toString().replace("`", ""))
                        .queue();
                logger.error("An error occurred while attempting to parse argument &e{} &rwith input &a{}&r,", argumentName, inputRaw, cause);
                return;
            }

            sbds.getMessages().reply(message, "sbds.string-argument-invalid", author)
                    .withPlaceholders("argument", argumentName, "input", inputRaw, "message", argumentParseException.getMessage().replace("`", ""))
                    .queue();

        }

        catch (StringCommandParser.NotEnoughArgumentsException e) {
            if (resp != null) resp.delete().queue();
            sbds.getMessages().reply(message, "sbds.string-not-enough-arguments", author)
                    .withPlaceholders("expected", e.expected.size(), "got", e.got.size(), "usage", ConsoleListener.createUsage(rootCmdName, e.expected, e.got))
                    .queue();
        }

        catch (Throwable t) {
            if (resp != null) resp.delete().queue();
            logger.error("An exception occurred while attempted to execute string command &b{}", content, t);
            sbds.getMessages().reply(message, "sbds.error", event.getAuthor())
                    .withPlaceholders("exception", t.toString().replace("`", ""))
                    .queue();
        }

    }

    private @Nullable Permission checkPermission(@NotNull Collection<SubCommandArgument.SubCommand> collection, @NotNull Guild guild, @NotNull User user) {

        for (var subcommand : collection) {

            Permission permission = subcommand.command().getPermission();
            if (permission == null) {
                continue;
            }

            boolean hasPermission = sbds.getPermissionManager().hasPermission(guild, user, permission);
            if (!hasPermission) {
                return permission;
            }

        }

        return null;

    }


    // SIX SEVEN! SEX SEVEN! SIX SEVEN! //
    @EventHandler
    public void onReady(@NotNull ISBDS.SbdsReadyEvent event) {
        event.setCancelled(true);
    }


    public static class RegisteredStringCommand extends AbstractCommandManager.RegisteredCommand<IRegisteredStringCommand, IStringCommandManager> implements IRegisteredStringCommand {

        private boolean guildGlobal = true;

        private final Set<Guild> guilds = new HashSet<>();


        private boolean dmGlobal = false;

        private final Set<User> users = new HashSet<>();


        public RegisteredStringCommand(@NotNull IStringCommandManager manager, @NotNull Command command) {
            super(manager, command);
        }

        //
        // GUILD
        //

        // GUILD GLOBAL //

        @Override
        public @NotNull IRegisteredStringCommand setGuildGlobal(boolean v) {
            this.guildGlobal = v;
            return this;
        }

        @Override
        public boolean isGuildGlobal() {
            return guildGlobal;
        }

        // PER GUILD REGISTRATION //

        @Override
        public @NotNull IRegisteredStringCommand setGuildRegistrations(@Nullable Collection<Guild> guilds) {

            this.guilds.clear();

            if (guilds != null) {
                this.guilds.addAll(guilds);
            }

            return this;

        }

        @Override
        public @NotNull IRegisteredStringCommand addGuildRegistration(@NotNull Guild guild) {
            this.guilds.add(guild);
            return this;
        }

        @Override
        public @NotNull IRegisteredStringCommand removeGuildRegistration(@NotNull Guild guild) {
            this.guilds.remove(guild);
            return this;
        }

        @Override
        public @NotNull List<Guild> getGuildRegistrations() {
            return new ArrayList<>(guilds);
        }

        //
        // DM
        //

        // DM GLOBAL //

        @Override
        public @NotNull IRegisteredStringCommand setDMGlobal(boolean v) {
            this.dmGlobal = v;
            return this;
        }

        @Override
        public boolean isDMGlobal() {
            return dmGlobal;
        }

        // PER USER REGISTRATION //

        @Override
        public @NotNull IRegisteredStringCommand setUserRegistrations(@Nullable Collection<User> users) {

            this.users.clear();

            if (users != null) {
                this.users.addAll(users);
            }

            return this;

        }

        @Override
        public @NotNull IRegisteredStringCommand addUserRegistration(@NotNull User user) {
            this.users.add(user);
            return this;
        }

        @Override
        public @NotNull IRegisteredStringCommand removeUserRegistration(@NotNull User user) {
            this.users.remove(user);
            return this;
        }

        @Override
        public @NotNull List<User> getUserRegistrations() {
            return new ArrayList<>(users);
        }

    }

}
