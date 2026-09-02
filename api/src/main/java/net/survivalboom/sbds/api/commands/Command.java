package net.survivalboom.sbds.api.commands;

import net.survivalboom.sbds.api.commands.argument.Argument;
import net.survivalboom.sbds.api.commands.argument.misc.SubCommandArgument;
import net.survivalboom.sbds.api.commands.console.ConsoleCommandExecutor;
import net.survivalboom.sbds.api.commands.console.ConsoleExecutionInfo;
import net.survivalboom.sbds.api.commands.context.ContextCommandExecutor;
import net.survivalboom.sbds.api.commands.context.ContextInteractionInfo;
import net.survivalboom.sbds.api.commands.slash.SlashCommandExecutor;
import net.survivalboom.sbds.api.commands.slash.SlashExecutionInfo;
import net.survivalboom.sbds.api.commands.string.StringCommandExecutor;
import net.survivalboom.sbds.api.commands.string.StringExecutionInfo;
import net.survivalboom.sbds.api.permissions.Permission;
import net.survivalboom.sbds.api.utils.ThrowingConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Command {

    // COMMAND BASE //

    private final String name;

    private final @Nullable CommandExecutor executor;

    private final List<CommandArgument> arguments = new ArrayList<>();

    private final List<String> aliases = new ArrayList<>();

    // COMMAND INFO //

    private final @Nullable String description;

    private final @Nullable String translationKey;

    // INTERACTION //

    private final boolean deferReply;

    private final boolean ephemeral;

    // PERMISSION //

    private final @Nullable Permission permission;


    public Command(
            @NotNull String name,
            @Nullable CommandExecutor executor,
            @Nullable Collection<CommandArgument> arguments,
            @Nullable Collection<String> aliases,

            @Nullable String description,
            @Nullable String translationKey,

            boolean deferReply,
            boolean ephemeral,

            @Nullable Permission permission
    ) {

        Objects.requireNonNull(name, "name == null");

        this.name = name;
        this.executor = executor;

        this.description = description;
        this.translationKey = translationKey;

        this.deferReply = deferReply;
        this.ephemeral = ephemeral;

        this.permission = permission;

        if (arguments != null) {
            this.arguments.addAll(arguments);
        }

        this.arguments.sort(Comparator.comparing(arg -> {

            int index = arg.index();

            if (!arg.required()) {
                index += 100;
            }

            return index;

        }));

        if (aliases != null) {
            this.aliases.addAll(aliases);
        }

    }

    // COMMAND BASE //

    public @NotNull String getName() {
        return name;
    }

    public @NotNull List<String> getAliases() {
        return new ArrayList<>(aliases);
    }

    public @Nullable CommandExecutor getExecutor() {
        return executor;
    }

    // ARGUMENTS //

    public @NotNull List<CommandArgument> getArguments() {
        return new ArrayList<>(arguments);
    }

    public @NotNull List<CommandArgument> getArguments(@NotNull Collection<ArgumentScope> scopes) {
        return arguments.stream()
                .filter(arg -> arg.scopes().stream().anyMatch(scopes::contains))
                .collect(Collectors.toList());
    }

    public @NotNull List<CommandArgument> getArguments(ArgumentScope @NotNull... scopes) {
        return getArguments(List.of(scopes));
    }

    public @Nullable CommandArgument getArgument(@NotNull String name) {
        return arguments.stream().filter(arg -> arg.name().equals(name)).findAny().orElse(null);
    }

    public @NotNull List<CommandArgument> getSubCommands() {
        return arguments.stream().filter(arg -> arg.argument() instanceof SubCommandArgument).toList();
    }

    // INTERACTION //

    public boolean isDeferReply() {
        return deferReply;
    }

    public boolean isEphemeral() {
        return ephemeral;
    }

    // COMMAND INFO //

    public @Nullable String getDescription() {
        return description;
    }

    public @Nullable String getTranslationKey() {
        return translationKey;
    }

    // PERMISSION //

    public @Nullable Permission getPermission() {
        return permission;
    }

    // COPY //

    public @NotNull Builder copy() {
        return new Builder(this);
    }


    //
    // BUILDER
    //

    public static @NotNull Builder create(@NotNull String name) {
        return new Builder(name);
    }

    public static class Builder {

        // COMMAND BASE //

        private final String name;

        private @Nullable CommandExecutor executor;

        private final List<CommandArgument> arguments = new ArrayList<>();

        private final List<String> aliases = new ArrayList<>();

        // COMMAND INFO //

        private @Nullable String description;

        private @Nullable String translationKey;

        // INTERACTION //

        private boolean deferReply = true;

        private boolean ephemeral = false;

        // PERMISSION //

        private @Nullable Permission permission;


        private Builder(@NotNull String name) {
            Objects.requireNonNull(name);
            this.name = name;
        }

        private Builder(Builder builder) {

            this.name = builder.name;
            this.executor = builder.executor;

            this.arguments.addAll(builder.arguments);
            this.aliases.addAll(builder.aliases);

            this.description = builder.description;
            this.translationKey = builder.translationKey;

            this.deferReply = builder.deferReply;
            this.ephemeral = builder.ephemeral;

            this.permission = builder.permission;

        }

        private Builder(Command command) {

            this.name = command.name;
            this.executor = command.executor;

            this.arguments.addAll(command.arguments);
            this.aliases.addAll(command.aliases);

            this.description = command.description;
            this.translationKey = command.translationKey;

            this.deferReply = command.deferReply;
            this.ephemeral = command.ephemeral;

            this.permission = command.permission;

        }

        // NAME //

        public String getName() {
            return name;
        }

        // EXECUTOR //

        public @NotNull Builder setExecutor(@Nullable CommandExecutor executor) {
            this.executor = executor;
            return this;
        }

        public @NotNull Builder setConsoleExecutor(@NotNull ThrowingConsumer<ConsoleExecutionInfo> executor) {
            this.executor = new ConsoleCommandExecutor() {
                @Override
                public void executes(@NotNull ConsoleExecutionInfo info) throws Throwable {
                    executor.accept(info);
                }
            };
            return this;
        }

        public @NotNull Builder setStringExecutor(@NotNull ThrowingConsumer<StringExecutionInfo> executor) {
            this.executor = new StringCommandExecutor() {
                @Override
                public void executes(@NotNull StringExecutionInfo info) throws Throwable {
                    executor.accept(info);
                }
            };
            return this;
        }

        public @NotNull Builder setSlashExecutor(@NotNull ThrowingConsumer<SlashExecutionInfo> executor) {
            this.executor = new SlashCommandExecutor() {
                @Override
                public void executes(@NotNull SlashExecutionInfo info) throws Throwable {
                    executor.accept(info);
                }
            };
            return this;
        }

        public @NotNull Builder setContextExecutor(@NotNull ThrowingConsumer<ContextInteractionInfo<?>> executor) {
            this.executor = (ContextCommandExecutor<ContextInteractionInfo<?>>) executor::accept;
            return this;
        }

        public CommandExecutor getExecutor() {
            return executor;
        }

        // ARGUMENTS //

        public @NotNull Builder setArguments(@Nullable Collection<CommandArgument> arguments) {

            this.arguments.clear();

            if (arguments != null) {
                this.arguments.addAll(arguments);
            }

            return this;

        }

        public @NotNull Builder addArgument(@NotNull CommandArgument argument) {
            Objects.requireNonNull(argument, "argument == null");
            this.arguments.add(argument);
            return this;
        }

        public @NotNull Builder addArgument(@NotNull String name, @NotNull Consumer<CommandArgument.Builder> consumer) {

            var builder = CommandArgument.create(name);
            consumer.accept(builder);

            CommandArgument argument = builder.build();
            this.arguments.add(argument);

            return this;

        }

        public @NotNull Builder addArgument(@NotNull String name, @NotNull Argument<?> argument) {
            addArgument(name, builder -> builder.setArgument(argument).setIndex(arguments.size()));
            return this;
        }

        public @NotNull List<CommandArgument> getArguments() {
            return arguments;
        }

        // SUBCOMMANDS //

        public @NotNull Builder addSubCommand(@NotNull Collection<Command> commands) {

            SubCommandArgument argument = new SubCommandArgument(commands);
            addArgument("subcommand" + arguments.size(), argument);

            return this;

        }

        public @NotNull Builder addSubCommand(@NotNull Command... commands) {

            SubCommandArgument argument = new SubCommandArgument(commands);
            addArgument("subcommand" + arguments.size(), argument);

            return this;

        }

        // ALIASES //

        public @NotNull Builder setAliases(@Nullable Collection<String> aliases) {

            this.aliases.clear();

            if (aliases != null) {
                this.aliases.addAll(aliases);
            }

            return this;

        }

        public @NotNull Builder addAlias(@NotNull String alias) {

            Objects.requireNonNull(alias, "alias == null");
            this.aliases.add(alias);

            return this;

        }

        public @NotNull List<String> getAliases() {
            return aliases;
        }

        // DESCRIPTION //

        public @NotNull Builder setDescription(@Nullable String description) {
            this.description = description;
            return this;
        }

        public String getDescription() {
            return description;
        }

        // TRANSLATION KEY //

        public @NotNull Builder setTranslation(@Nullable String translationKey) {
            this.translationKey = translationKey;
            return this;
        }

        public String getTranslationKey() {
            return translationKey;
        }

        // DEFER REPLY //

        public @NotNull Builder setDeferReply(boolean value) {
            this.deferReply = value;
            return this;
        }

        public boolean isDeferReply() {
            return deferReply;
        }

        // EPHEMERAL //

        public @NotNull Builder setEphemeral(boolean value) {
            this.ephemeral = value;
            return this;
        }

        public boolean isEphemeral() {
            return ephemeral;
        }

        // PERMISSION //

        public @NotNull Builder setPermission(@Nullable Permission permission) {
            this.permission = permission;
            return this;
        }

        public Permission getPermission() {
            return permission;
        }

        // BUILD //

        public @NotNull Command build() {
            return new Command(name, executor, arguments, aliases, description, translationKey, deferReply, ephemeral, permission);
        }

        public @NotNull Builder copy() {
            return new Builder(this);
        }

    }

}
