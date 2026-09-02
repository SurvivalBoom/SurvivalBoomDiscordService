package net.survivalboom.sbds.api.commands.base;

import net.survivalboom.sbds.api.ISBDS;
import net.survivalboom.sbds.api.SbdsProvider;
import net.survivalboom.sbds.api.commands.Command;
import net.survivalboom.sbds.api.commands.CommandArgument;
import net.survivalboom.sbds.api.commands.CommandExecutor;
import net.survivalboom.sbds.api.commands.argument.Argument;
import net.survivalboom.sbds.api.permissions.Permission;
import net.survivalboom.sbds.api.utils.CommonUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.*;

public abstract class CommandBase implements CommandExecutor {

    private boolean initialized = false;

    public @NotNull Command build() {

        CommandClass info = this.getClass().getAnnotation(CommandClass.class);
        if (info == null) {
            throw new InvalidCommandException("Annotation `@CommandClass` is not present in CommandBase class `" + getClass().getSimpleName() + "`");
        }

        String name = info.name();
        List<String> aliases = List.of(info.aliases());

        String description = info.description().isBlank() ? null : info.description();
        String translationKey = info.translationKey().isBlank() ? null : info.translationKey() ;

        String permissionStr = info.permission();
        Permission permission = !permissionStr.isBlank() ? new Permission(permissionStr, info.defaultPermission()) : null;

        boolean deferReply = info.deferReply();
        boolean ephemeral = info.ephemeral();

        List<CommandArgument> arguments = scanForArguments(translationKey);

        return new Command(
                name,
                this,
                arguments,
                aliases,
                description,
                translationKey,
                deferReply,
                ephemeral,
                permission
        );

    }

    private @NotNull List<CommandArgument> scanForArguments(String translationKey) {

        List<CommandArgument> out = new ArrayList<>();
        for (Method method : getClass().getDeclaredMethods()) {

            if (!method.isAnnotationPresent(ArgumentMethod.class)) {
                continue;
            }

            ArgumentMethod argumentInfo = method.getAnnotation(ArgumentMethod.class);

            String name = method.getName();

            Argument<?> argument;
            try {
                argument = CommonUtils.invokeMethod(this, method, SbdsProvider.getInstance());
            }

            catch (Exception e) {
                throw new InvalidCommandException("Invalid command argument `" + name + "`", e);
            }

            if (argument == null) {
                throw new InvalidCommandException("Received null in argument method `" + method.getName() + "`");
            }

            String argumentTranslationKey = translationKey != null ? translationKey + "." + method.getName() : null;
            out.add(new CommandArgument(
                    name,
                    argumentInfo.description(),
                    argumentTranslationKey,
                    List.of(argumentInfo.scope()),
                    argument,
                    argumentInfo.index(),
                    argumentInfo.required()
            ));

        }

        out.sort(Comparator.comparing(CommandArgument::index));

        if (!initialized) {
            init(SbdsProvider.getInstance());
            this.initialized = true;
        }

        return out;

    }

    protected void init(@NotNull ISBDS sbds) {

    }

}
