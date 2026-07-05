package net.survivalboom.sbds.core.commands.cmds.console.libraries;

import net.survivalboom.sbds.api.commands.argument.primitive.StringArgument;
import net.survivalboom.sbds.api.commands.base.ArgumentMethod;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.base.CommandClass;
import net.survivalboom.sbds.api.commands.console.ConsoleCommandExecutor;
import net.survivalboom.sbds.api.commands.console.ConsoleExecutionInfo;
import net.survivalboom.sbds.api.libraries.ArtifactAddress;
import net.survivalboom.sbds.api.libraries.ILibrariesManager;
import net.survivalboom.sbds.api.libraries.ILibrary;
import net.survivalboom.sbds.api.libraries.IPomData;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@CommandClass(name = "info", description = "Show library information")
public class LibrariesInfoCommand extends CommandBase implements ConsoleCommandExecutor {

    @Override
    public void executes(@NotNull ConsoleExecutionInfo info) throws Throwable {

        String libraryRaw = info.arguments().getCast("library", String.class).orElseThrow();

        ILibrariesManager librariesManager = info.sbds().getLibrariesManager();
        ILibrary library = librariesManager.getLoadedLibrary(libraryRaw);
        if (library == null) {
            info.logger().error("No library with address `{}` was found.", libraryRaw);
            return;
        }

        IPomData pom = library.getPomData();
        IPomData parent = pom.getParent();

        ArtifactAddress address = pom.getAddress();

        Map<ArtifactAddress, ILibrary> libraries = librariesManager.getLoadedLibraries();
        List<ILibrary> dependencies = library.getDependencies();

        List<ILibrary> requiredBy = libraries.values().stream()
                .filter(lib -> lib.getDependencies().contains(library))
                .toList();

        List<ILibrary> similar = libraries.entrySet()
                .stream()
                .filter(entry -> {
                    ArtifactAddress addr = entry.getKey();
                    return addr.group().equals(address.group()) && addr.artifact().equals(address.artifact());
                })
                .map(Map.Entry::getValue)
                .toList();

        info.logger().info("--- --- < Library Info > --- ---");
        info.logger().info("> Address: {}", address);
        info.logger().info("> Source: {}", pom.getSourceRepository());
        info.logger().info(" ");
        info.logger().info("> ClassLoader: {}", library.getClassLoader().getName());
        info.logger().info("> File: {}", library.getFile().getName());
        info.logger().info("");
        info.logger().info("> Parent: {}", parent != null ? parent.getAddress() : null);

        if (!dependencies.isEmpty()) {

            info.logger().info(" ");
            info.logger().info("> Dependencies:");
            for (ILibrary dep : dependencies) {
                info.logger().info("* {}", dep.getPomData().getAddress());
            }

        }

        if (!requiredBy.isEmpty()) {

            info.logger().info(" ");
            info.logger().info("> Required by:");
            for (ILibrary lib : requiredBy) {
                info.logger().info("* {}", lib.getPomData().getAddress());
            }

        }

        if (!similar.isEmpty()) {

            info.logger().info(" ");
            info.logger().info("> Other versions:");
            for (ILibrary lib : similar) {
                info.logger().info(
                        "* {} - From - {}",
                        lib.getPomData().getAddress(),
                        String.join(", ", libraries.values()
                                .stream()
                                .filter(l -> l.getDependencies().contains(lib))
                                .map(l -> l.getPomData().getAddress().toString()).toList()
                        )
                );
            }

        }

        info.logger().info(" ");
        info.logger().info("--- --- --- ---  --- --- --- ---");

    }

    @ArgumentMethod
    public StringArgument library() {
        return new StringArgument();
    }

}
