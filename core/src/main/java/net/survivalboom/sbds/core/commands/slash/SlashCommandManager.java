package net.survivalboom.sbds.core.commands.slash;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.survivalboom.sbds.api.commands.Command;
import net.survivalboom.sbds.api.commands.CommandArgument;
import net.survivalboom.sbds.api.commands.CommandExecutor;
import net.survivalboom.sbds.api.commands.argument.ArgumentAutoCompleteContext;
import net.survivalboom.sbds.api.commands.argument.ArgumentParsingContext;
import net.survivalboom.sbds.api.commands.argument.misc.SubCommandArgument;
import net.survivalboom.sbds.api.commands.slash.ISlashCommandManager;
import net.survivalboom.sbds.api.commands.slash.SlashCommandExecutor;
import net.survivalboom.sbds.api.commands.slash.SlashExecutionInfo;
import net.survivalboom.sbds.api.events.EventHandler;
import net.survivalboom.sbds.api.events.EventListener;
import net.survivalboom.sbds.api.messages.parsers.LinkedTextParser;
import net.survivalboom.sbds.api.permissions.Permission;
import net.survivalboom.sbds.api.registrations.Registration;
import net.survivalboom.sbds.api.utils.typemap.TypeMap;
import net.survivalboom.sbds.api.utils.typemap.UnmodifiableTypeMap;
import net.survivalboom.sbds.core.BuildConstants;
import net.survivalboom.sbds.core.SBDS;
import net.survivalboom.sbds.core.commands.AbstractCommandManager;
import net.survivalboom.sbds.core.commands.cmds.common.StatusCommand;
import net.survivalboom.sbds.core.commands.parser.SlashCommandParser;
import net.survivalboom.sbds.core.interaction.command.CommandInteractionManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SlashCommandManager extends AbstractCommandManager<SlashCommandManager.IRegisteredSlashCommand, ISlashCommandManager> implements ISlashCommandManager, EventListener {

    private final CommandInteractionManager commandInteractionManager;

    public SlashCommandManager(@NotNull SBDS sbds) {
        super(sbds);
        this.commandInteractionManager = sbds.getCommandInteractionManager();
    }

    //
    // MANAGER
    //

    @Override
    protected void init0() {

        super.init0();

        sbds.getEventManager().registerEvents0(null, this);

        registerCommand0(null, new StatusCommand().build());

    }

    @Override
    protected void shutdown0() {
        sbds.getEventManager().unregisterEvents(this);
        super.shutdown0();
    }

    @Override
    protected @NotNull SlashCommandManager.IRegisteredSlashCommand createCommandReg(@NotNull Command command) {
        return new RegisteredSlashCommand(this, command);
    }

    @Override
    public void onRegister(@NotNull Registration<IRegisteredSlashCommand> registration) {

        RegisteredSlashCommand reg = (RegisteredSlashCommand) registration.object();
        Command command = registration.object().getCommand();
        CommandExecutor executor = command.getExecutor();

        if (executor != null && !(executor instanceof SlashCommandExecutor)) {
            throw new IllegalArgumentException("Command `" + command.getName() + "` does not have executor for a slash command");
        }

         reg.commandData = commandInteractionManager.registerCommand(registration.object(), List.of(net.dv8tion.jda.api.interactions.commands.Command.Type.SLASH));

    }

    @Override
    public void unRegister(@NotNull Registration<IRegisteredSlashCommand> registration) {
        commandInteractionManager.unregisterCommand(registration.object());
    }

    //
    // HANDLER
    //

    @EventHandler
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        commandInteractionManager.updateGuild(event.getGuild());
    }

    @EventHandler
    public void onCommand(@NotNull SlashCommandInteractionEvent event) {

        if (!sbds.isReady()) {
            return;
        }

        String commandName = event.getName();
        String fullCommandStr = event.getFullCommandName();

        logger.info("User &b{} &rexecuted slash-command &b/{} {}", event.getUser().getEffectiveName(), fullCommandStr, String.join(" ", event.getOptions().stream().map(OptionMapping::getAsString).toList()));

        IRegisteredSlashCommand registeredCommand = getByAlias(commandName);
        if (registeredCommand == null) {
            logger.warn("Something went wrong! Slash command /{} does not exist in SlashCommandManager!", fullCommandStr);
            return;
        }

        Command baseCommand = registeredCommand.getCommand();

        try {

            Command command = getCommand(baseCommand, event.getFullCommandName(), event.getName());

            if (!event.isAcknowledged() && command.isDeferReply()) {
                event.deferReply(command.isEphemeral()).queue();
            }

            SlashCommandExecutor executor = (SlashCommandExecutor) command.getExecutor();

            if (executor == null) {
                logger.error("Command executor of /{} is null. You did something wrong?", event.getFullCommandName());
                messages.reply(event, "sbds.error", event.getUser())
                        .withPlaceholders("exception", "Command executor is null")
                        .queue();
                return;
            }

            TypeMap arguments;

            try {
                arguments = SlashCommandParser.parse(registeredCommand, command, event);
            }

            catch (SlashCommandParser.ArgumentParsingException e) {

                var argument = e.getArgument();

                String argTranslated;
                if (command.getTranslationKey() != null) {
                    argTranslated = "$[" + command.getTranslationKey() + "." + argument.name() + ".name" + "]";
                }

                else {
                    argTranslated = argument.name();
                }

                messages.reply(event, "sbds.slash-invalid-argument", event.getUser())
                        .withPlaceholders(
                                "argument", argTranslated,
                                "message", e.getCause().getMessage().replace("`", "")
                        )
                        .queue();

                return;

            }

            SlashExecutionInfo info = new SlashExecutionInfo(event, registeredCommand, command, commandName, arguments, command.isEphemeral());

            if (!permissionCheck(info)) {
                return;
            }

            executor.executes(info);

        }

        catch (Throwable t) {

            String place = event.getGuild() != null ? event.getGuild().getName() + ":" + event.getUser().getName() : event.getUser().getName();

            logger.error("[{}] An internal error occurred while attempting to perform slash command /{}", place, event.getFullCommandName(), t);

            try {

                messages.reply(event, "sbds.error", event.getUser())
                        .withPlaceholders("exception", t.toString())
                        .queue();

            }

            catch (Throwable tt) {

                String msg = """ 
                **SurvivalBoom Discord Service** *v{v}*
                A low-level fatal error occurred in SurvivalBoom Discord Service while attempting to process your request!
                This is an internal error. Looks like something went completely wrong!
                `{e}`
                """.replace("{v}", BuildConstants.VERSION).replace("{e}", tt.toString());

                if (event.isAcknowledged()) {
                    event.getHook().editOriginal(msg).queue();
                }

                else {
                    event.reply(msg).queue();
                }

                throw tt;

            }

        }

    }

    @EventHandler
    public void onAutoComplete(@NotNull CommandAutoCompleteInteractionEvent event) {

        try {

            String baseCmdName = event.getName();
            String fullCmdName = event.getFullCommandName();

            IRegisteredSlashCommand registeredCommand = getByAlias(baseCmdName);
            if (registeredCommand == null) {
                logger.warn("[{}] Received autocompletion request for command &b/{}&r, but command does not exist.", event.getUser().getName(), fullCmdName);
                event.replyChoices(List.of(
                        new net.dv8tion.jda.api.interactions.commands.Command.Choice("ua.timurishche.DinosaurDeathException", 1),
                        new net.dv8tion.jda.api.interactions.commands.Command.Choice("java.lang.OutOfMemoryError", 2),
                        new net.dv8tion.jda.api.interactions.commands.Command.Choice("RAWR-R-R!!!!", 3),
                        new net.dv8tion.jda.api.interactions.commands.Command.Choice("Slash command `" + fullCmdName + "` does not exist", 4)
                )).queue();
                return;
            }

            Command command = getCommand(registeredCommand.getCommand(), fullCmdName, baseCmdName);

            CommandArgument commandArgument = command.getArgument(event.getFocusedOption().getName());
            Objects.requireNonNull(commandArgument, "commandArgument == null");

            Map<String, Object> arguments = new HashMap<>();
            for (OptionMapping option : event.getOptions()) {

                String name = option.getName();
                String value = option.getAsString();

                CommandArgument argument = command.getArgument(name);
                if (argument == null) {
                    continue;
                }

                ArgumentParsingContext argumentParsingContext = new ArgumentParsingContext(
                        registeredCommand,
                        command,
                        argument
                );

                Object result;
                try {
                    result = argument.argument().parse(value, argumentParsingContext);
                } catch (Exception e) {
                    continue;
                }

                arguments.put(name, result);

            }

            ArgumentAutoCompleteContext context = new ArgumentAutoCompleteContext(
                    registeredCommand, command, commandArgument, event.getFocusedOption().getValue(), UnmodifiableTypeMap.ofMap(arguments), event
            );

            List<net.dv8tion.jda.api.interactions.commands.Command.Choice> result = commandArgument.argument().onArgumentAutoComplete(context);
            if (result == null || result.isEmpty()) {
                return;
            }

            LinkedTextParser parser = LinkedTextParser.builder(sbds.getMessages(), event.getUser()).build();

            List<net.dv8tion.jda.api.interactions.commands.Command.Choice> resultFinal = result.stream()
                    .map(choice -> new net.dv8tion.jda.api.interactions.commands.Command.Choice(
                            parser.parse(choice.getName()),
                            parser.parse(choice.getAsString())
                    ))
                    .toList();

            event.replyChoices(resultFinal).queue();

        }

        catch (Throwable t) {
            logger.error("[{}] An internal error occurred while attempting to autocomplete command &b/{}", event.getUser().getName(), event.getFullCommandName(), t);
            event.replyChoices(List.of(
                    new net.dv8tion.jda.api.interactions.commands.Command.Choice("ua.timurishche.DinosaurDeathException", 1),
                    new net.dv8tion.jda.api.interactions.commands.Command.Choice("java.lang.OutOfMemoryError", 2),
                    new net.dv8tion.jda.api.interactions.commands.Command.Choice("RAWR-R-R!!!!", 3),
                    new net.dv8tion.jda.api.interactions.commands.Command.Choice("An internal error occurred while attempting to generate suggestions.", 4)
            )).queue();
        }

    }

    private @NotNull Command getCommand(@NotNull Command baseCommand, @NotNull String eventFullInput, @NotNull String eventBaseInput) {

        boolean hasSubCommands = baseCommand.getArguments().stream()
                .anyMatch(argument -> argument.argument() instanceof SubCommandArgument);

        if (!hasSubCommands) {
            return baseCommand;
        }

        String fullInput = eventFullInput.substring(eventBaseInput.length()).trim();
        String[] parts = fullInput.split(" ");

        Command command = baseCommand;
        for (String part : parts) {

            List<Command> subcommands = command.getArguments().stream()
                    .filter(argument -> argument.argument() instanceof SubCommandArgument)
                    .flatMap(argument -> ((SubCommandArgument) argument.argument()).getSubcommands().stream())
                    .toList();

            Command subcommand = subcommands.stream()
                    .filter(sc -> sc.getName().equals(part) || sc.getAliases().contains(part))
                    .findAny()
                    .orElse(null);

            if (subcommand == null) {
                break;
            }

            command = subcommand;

        }

        return command;

    }

    private boolean permissionCheck(@NotNull SlashExecutionInfo info) {

        Permission permission = info.currentCommand().getPermission();
        SlashCommandInteraction event = info.interaction();

        Member member = event.getMember();
        if (member != null && permission != null) {

            boolean hasPermission = permissionManager.hasPermission(member,  permission);
            if (!hasPermission) {
                messages.reply(event,"sbds.no-permission", event.getUser())
                        .withPlaceholders("permission", permission.permission())
                        .queue();
                return false;
            }

        }

        return true;

    }


    public static class RegisteredSlashCommand extends AbstractCommandManager.RegisteredInteractionCommand<IRegisteredSlashCommand, ISlashCommandManager> implements IRegisteredSlashCommand {

        public RegisteredSlashCommand(
                @NotNull ISlashCommandManager manager,
                @NotNull Command command
        ) {
            super(manager, command);
        }

    }

}
