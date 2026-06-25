package net.survivalboom.sbds.core.commands.cmds.console;

import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.base.CommandClass;
import net.survivalboom.sbds.api.commands.console.ConsoleCommandExecutor;
import net.survivalboom.sbds.api.commands.console.ConsoleExecutionInfo;
import org.jetbrains.annotations.NotNull;

@CommandClass(name = "suicide", description = "Commit a suicide")
public class SuicideCommand extends CommandBase implements ConsoleCommandExecutor {

    @Override
    public void executes(@NotNull ConsoleExecutionInfo info) throws Throwable {

        var manager = info.sbds().getRegistrationRegistry();
        var regs = manager.getRegistrations();
        for (var reg : regs) {
            info.logger().info("Commiting suicide to `{}`.", reg.regKey());
            manager.removeRegistration(reg);
        }

        info.logger().info("You commited suicide! Congratulations!");

    }

}
