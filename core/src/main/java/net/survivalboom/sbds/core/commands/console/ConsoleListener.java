package net.survivalboom.sbds.core.commands.console;

import net.survivalboom.sbds.api.commands.ArgumentScope;
import net.survivalboom.sbds.api.commands.Command;
import net.survivalboom.sbds.api.commands.CommandArgument;
import net.survivalboom.sbds.api.commands.CommandExecutor;
import net.survivalboom.sbds.api.commands.argument.ArgumentParseException;
import net.survivalboom.sbds.api.commands.argument.ArgumentParsingContext;
import net.survivalboom.sbds.api.commands.argument.misc.SubCommandArgument;
import net.survivalboom.sbds.api.commands.console.ConsoleCommandExecutor;
import net.survivalboom.sbds.api.commands.console.ConsoleExecutionInfo;
import net.survivalboom.sbds.api.commands.console.IConsoleListener;
import net.survivalboom.sbds.api.registrations.Registration;
import net.survivalboom.sbds.core.SBDS;
import net.survivalboom.sbds.core.commands.AbstractCommandManager;
import net.survivalboom.sbds.core.commands.cmds.common.StatusCommand;
import net.survivalboom.sbds.core.commands.cmds.console.ServersCommand;
import net.survivalboom.sbds.core.commands.cmds.console.SuicideCommand;
import net.survivalboom.sbds.core.commands.cmds.console.database.DatabaseCommand;
import net.survivalboom.sbds.core.commands.cmds.console.guildconfig.GuildConfigCommand;
import net.survivalboom.sbds.core.commands.cmds.console.permission.PermissionCommand;
import net.survivalboom.sbds.core.commands.cmds.console.registration.RegistrationCommand;
import net.survivalboom.sbds.core.commands.parser.StringCommandParser;
import net.survivalboom.sbds.core.commands.cmds.console.HelpCommand;
import net.survivalboom.sbds.core.commands.cmds.console.ShutdownCommand;
import net.survivalboom.sbds.core.commands.cmds.console.modules.ModulesCommand;
import net.survivalboom.sbds.core.scheduler.SchedulerTask;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ConsoleListener extends AbstractCommandManager<IConsoleListener.IRegisteredConsoleCommand, IConsoleListener> implements IConsoleListener {

    private final Scanner scanner = new Scanner(System.in);

    private SchedulerTask task;


    public ConsoleListener(@NotNull SBDS sbds) {
        super(sbds);
    }


    @Override
    protected void init0() {

        super.init0();

        registerCommand0(null, new SuicideCommand());
        registerCommand0(null, new ShutdownCommand());
        registerCommand0(null, new HelpCommand());
        registerCommand0(null, new ModulesCommand());
        registerCommand0(null, new RegistrationCommand());

        registerCommand0(null, new DatabaseCommand());
        registerCommand0(null, new GuildConfigCommand());
        registerCommand0(null, new PermissionCommand());

        registerCommand0(null, new StatusCommand());
        registerCommand0(null, new ServersCommand());

        task = sbds.getScheduler().schedule0(null, "ConsoleListener", task -> this.consoleListener(), 0, 50);

    }

    @Override
    protected void shutdown0() {

        task.cancelAndWaitOrKill(100, false);
        task = null;

        super.shutdown0();

    }


    @Override
    public void onRegister(@NotNull Registration<IRegisteredConsoleCommand> registration) {

        Command command = registration.object().getCommand();
        CommandExecutor executor = command.getExecutor();

        if (executor == null) {
            return;
        }

        if (!(executor instanceof ConsoleCommandExecutor)) {
            throw new IllegalArgumentException("Command `" + command.getName() + "` does not have executor for a console command");
        }

    }


    @Override
    protected @NotNull ConsoleListener.RegisteredConsoleCommand createCommandReg(@NotNull Command command) {
        return new RegisteredConsoleCommand(this, command);
    }


    private void consoleListener() {

        if (!scanner.hasNext() || !sbds.isReady()) {
            return;
        }

        String input = scanner.nextLine().strip();

        try {
            processCommand(input);
        }

        catch (Throwable t) {
            logger.error("Ooopsies! A fatal internal error occurred while attempting to process input `{}`. OutOfMemoryError?", input);
        }

    }

    private void processCommand(@NotNull String input) {

        String prefix = StringCommandParser.getPrefix(input);

        var cmdReg = getByAlias(prefix);
        if (cmdReg == null) {
            rootLogger.info("Unknown command. Type 'help' to view all available commands.");
            return;
        }

        Command command = cmdReg.getCommand();
        String string = input.substring(prefix.length()).strip();

        try {

            var result = StringCommandParser.parseInput(string, command, ArgumentScope.CONSOLE, argument -> new ArgumentParsingContext(cmdReg, command, argument));
            var toExecute = new ArrayList<>(result.foundSubcommands());
            toExecute.addFirst(new SubCommandArgument.SubCommand(command, prefix));

            for (SubCommandArgument.SubCommand execute : toExecute) {

                ConsoleExecutionInfo info = new ConsoleExecutionInfo(cmdReg, execute.command(), input, execute.alias(), result.arguments(), rootLogger);

                CommandExecutor executor = execute.command().getExecutor();
                if (!(executor instanceof ConsoleCommandExecutor consoleCommandExecutor)) {
                    continue;
                }

                consoleCommandExecutor.executes(info);

            }

        }

        catch (StringCommandParser.ArgumentParsingException e) {

            String argumentName = e.getArgument().name();
            String inputRaw = e.getInput();

            Throwable cause = e.getCause();
            if (!(cause instanceof ArgumentParseException argumentParseException)) {
                logger.error("An error occurred while attempting to parse argument `{}`: `{}`.", argumentName, inputRaw, cause);
                return;
            }

            logger.error("Invalid input `{}` for argument `{}`: {}", inputRaw, argumentName, argumentParseException.getMessage());

        }

        catch (StringCommandParser.NotEnoughArgumentsException e) {
            logger.error("Incomplete command. Expected {} arguments, got {}. Usage: `{}`", e.expected.size(), e.got.size(), createUsage(prefix, e.expected, e.got));
        }

        catch (Throwable t) {
            logger.error("An error occurred while attempting to perform console command `{}`.", input, t);
        }

    }

    public static String createUsage(@NotNull String prefix, @NotNull List<CommandArgument> args, @NotNull Map<CommandArgument, String> parsed) {

        List<String> strings = new ArrayList<>();
        for (CommandArgument argument : args) {

            String str;
            if (argument.isSubCommand()) {

                str = parsed.get(argument);
                if (str != null) {
                    strings.add(str);
                    continue;
                }

                str = String.join("/", ((SubCommandArgument) argument.argument()).getSubcommands().stream().map(Command::getName).toList());

            }

            else {
                str = argument.name();
            }

            if (argument.required()) {
                strings.add("<" + str + ">");
            }

            else {
                strings.add("[" + str + "]");
            }

        }

        return prefix + " " + String.join(" ", strings);

    }

    public static class RegisteredConsoleCommand extends RegisteredCommand<IRegisteredConsoleCommand, IConsoleListener> implements IRegisteredConsoleCommand {

        public RegisteredConsoleCommand(@NotNull ConsoleListener manager, @NotNull Command command) {
            super(manager, command);
        }

    }


}
