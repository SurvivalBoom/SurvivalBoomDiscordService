package net.survivalboom.sbds.core.commands.console;

import net.survivalboom.sbds.api.commands.ArgumentScope;
import net.survivalboom.sbds.api.commands.Command;
import net.survivalboom.sbds.api.commands.argument.Argument;
import net.survivalboom.sbds.api.commands.argument.ArgumentParseException;
import net.survivalboom.sbds.api.commands.argument.internal.SubCommandArgument;
import net.survivalboom.sbds.api.commands.console.ConsoleCommand;
import net.survivalboom.sbds.api.commands.console.ConsoleExecutionInfo;
import net.survivalboom.sbds.api.commands.console.IConsoleListener;
import net.survivalboom.sbds.api.utils.TypeMap;
import net.survivalboom.sbds.core.SBDS;
import net.survivalboom.sbds.core.commands.AbstractCommandManager;
import net.survivalboom.sbds.core.commands.cmds.common.StatusCommand;
import net.survivalboom.sbds.core.commands.string.StringCommandParser;
import net.survivalboom.sbds.core.commands.cmds.console.HelpCommand;
import net.survivalboom.sbds.core.commands.cmds.console.ShutdownCommand;
import net.survivalboom.sbds.core.commands.cmds.console.modules.ModulesCommand;
import net.survivalboom.sbds.core.scheduler.SchedulerTask;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ConsoleListener extends AbstractCommandManager implements IConsoleListener {

    private final Scanner scanner = new Scanner(System.in);

    private SchedulerTask task;


    public ConsoleListener(@NotNull SBDS sbds) {
        super("ConsoleListener", sbds, true);
    }


    @Override
    protected void init0() {

        registerCommand0(null, new ShutdownCommand().build(sbds, null));
        registerCommand0(null, new HelpCommand().build(sbds, null));
        registerCommand0(null, new ModulesCommand().build(sbds, null));

        registerCommand0(null, new StatusCommand(sbds).build(sbds, null));

        task = sbds.getScheduler().schedule0(null, "ConsoleListener", task -> this.consoleListener(), 0, 50);

    }

    @Override
    protected void shutdown0() {
        task.cancelForce();
        commands.clear();
        task = null;
    }


    private void consoleListener() {

        if (!sbds.isReady()) return;
        if (!scanner.hasNext()) return;

        String input = scanner.nextLine().strip();
        String prefix = StringCommandParser.getPrefix(input);

        RegisteredCommand registeredCommand = findByAlias(prefix);
        if (registeredCommand == null) {
            rootLogger.info("Unknown command. Type 'help' to view all available commands.");
            return;
        }

        Command command = registeredCommand.command();

        try {

            Argument.ArgumentResources resources = new Argument.ArgumentResources(sbds, TypeMap.empty(false));

            String string = input.substring(prefix.length()).strip();

            processSubcommand(prefix, string, input, command, resources);

        }

        catch (Throwable t) {
            logger.error("An internal error occurred while attempting to perform console command `{}`.", input, t);
        }


    }


    private void processSubcommand(@NotNull String prefix, @NotNull String input, @NotNull String fullInput, @NotNull Command command, @NotNull Argument.ArgumentResources resources) throws Throwable {

        StringCommandParser parser = new StringCommandParser(input, command, ArgumentScope.CONSOLE, resources);

        try {
            parser.parse();
        } catch (ArgumentParseException e) {
            rootLogger.error("An error occurred: {}", e.getMessage());
            return;
        }

        if (!parser.checkCount()) {

            int requiredArguments = command.requiredArguments().size();
            int currentArguments = parser.getArguments().size();

            String usage = command.usage() == null ? String.format("%s %s", prefix, String.join(" ", command.requiredArguments().stream().map(v -> "<" + v.name() + ">").toList())) : command.usage();

            rootLogger.info("Incorrect or incomplete command. Expected {} arguments, got {}. Usage: `{}`", requiredArguments, currentArguments, usage);

            return;

        }

        if (command.hasSubcommands()) {

            SubCommandArgument.SubCommand subcommand = parser.getArguments().get("subcommand", SubCommandArgument.SubCommand.class);

            Objects.requireNonNull(subcommand);

            String subInput = input.substring(subcommand.alias().length()).strip();

            processSubcommand(prefix, subInput, fullInput, subcommand.command(), resources);

            return;

        }

        TypeMap commandArguments = parser.getArguments();

        ConsoleExecutionInfo info = new ConsoleExecutionInfo(command, fullInput, prefix, commandArguments, rootLogger, sbds);

        ConsoleCommand consoleCommand = (ConsoleCommand) command.executor();

        consoleCommand.executes(info);

    }


}
