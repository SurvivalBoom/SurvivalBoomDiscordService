package net.survivalboom.sbds.core.commands.cmds.console.libraries;

import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.base.CommandClass;
import net.survivalboom.sbds.api.commands.console.ConsoleCommandExecutor;
import net.survivalboom.sbds.api.commands.console.ConsoleExecutionInfo;
import net.survivalboom.sbds.api.libraries.ILibrariesManager;
import net.survivalboom.sbds.api.libraries.ILibrary;
import org.jetbrains.annotations.NotNull;

@CommandClass(name = "list", description = "Show a list of loaded libraries")
public class LibrariesListCommand extends CommandBase implements ConsoleCommandExecutor {

    @Override
    public void executes(@NotNull ConsoleExecutionInfo info) throws Throwable {

        ILibrariesManager librariesManager = info.sbds().getLibrariesManager();

        info.logger().info("--- --- --- < Libraries > --- --- ---");

        for (ILibrary library : librariesManager.getLoadedLibraries().values()) {
            info.logger().info("> {} -> {}", library.getPomData().getAddress(), library.getFile().getName());
        }

        info.logger().info("--- --- --- --- -- -- --- --- --- ---");

    }

}
