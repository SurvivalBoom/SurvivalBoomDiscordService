package net.survivalboom.sbds.core.commands.context;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.GenericContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.survivalboom.sbds.api.commands.Command;
import net.survivalboom.sbds.api.commands.CommandExecutor;
import net.survivalboom.sbds.api.commands.context.*;
import net.survivalboom.sbds.api.commands.slash.SlashExecutionInfo;
import net.survivalboom.sbds.api.events.EventHandler;
import net.survivalboom.sbds.api.events.EventListener;
import net.survivalboom.sbds.api.permissions.Permission;
import net.survivalboom.sbds.api.registrations.Registration;
import net.survivalboom.sbds.core.SBDS;
import net.survivalboom.sbds.core.commands.AbstractCommandManager;
import net.survivalboom.sbds.core.interaction.command.CommandInteractionManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ContextCommandManager extends AbstractCommandManager<IContextCommandManager.IRegisteredContextCommand, IContextCommandManager> implements IContextCommandManager, EventListener {

    private final CommandInteractionManager commandInteractionManager;

    public ContextCommandManager(@NotNull SBDS sbds) {
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
    }

    @Override
    protected void shutdown0() {
        sbds.getEventManager().unregisterEvents(this);
        super.shutdown0();
    }

    @Override
    protected @NotNull IContextCommandManager.IRegisteredContextCommand createCommandReg(@NotNull Command command) {
        return new RegisteredContextCommand(this, command);
    }

    @Override
    public void onRegister(@NotNull Registration<IRegisteredContextCommand> registration) {

        RegisteredContextCommand reg = (RegisteredContextCommand) registration.object();
        Command command = reg.getCommand();
        CommandExecutor executor = command.getExecutor();

        if (executor == null) {
            return;
        }

        if (!(executor instanceof ContextCommandExecutor<?>)) {
            throw new IllegalArgumentException("Command `" + command.getName() + "` does not have executor for a context command");
        }

        List<net.dv8tion.jda.api.interactions.commands.Command.Type> types = new ArrayList<>();
        if (executor instanceof UserContextCommandExecutor) {
            types.add(net.dv8tion.jda.api.interactions.commands.Command.Type.USER);
        }

        if (executor instanceof MessageContextCommandExecutor) {
            types.add(net.dv8tion.jda.api.interactions.commands.Command.Type.MESSAGE);
        }

        commandInteractionManager.registerCommand(reg, types);

    }

    @Override
    public void unRegister(@NotNull Registration<IRegisteredContextCommand> registration) {
        commandInteractionManager.unregisterCommand(registration.object());
    }

    //
    // HANDLER
    //

    @EventHandler
    public void onEvent(@NotNull GenericContextInteractionEvent<?> event) {

        if (!sbds.isReady()) {
            return;
        }

        try {

            String name = event.getName();

            logger.info("User &b{} &rexecuted context-command &b/{}", event.getUser().getEffectiveName(), name);

            IRegisteredContextCommand registeredContextCommand = registry.getRegisteredObjects().stream()
                    .filter(c -> c.getCommand().getName().equals(name))
                    .findAny()
                    .orElse(null);

            if (registeredContextCommand == null) {
                logger.warn("Received unknown context command `{}` execution request.", name);
                return;
            }

            Command command = registeredContextCommand.getCommand();

            if (command.isDeferReply()) {
                event.deferReply(command.isEphemeral()).queue();
            }

            var executor = registeredContextCommand.getCommand().getExecutor();
            switch (event.getCommandType()) {

                case USER -> {

                    UserContextInteractionEvent event0 = (UserContextInteractionEvent) event;
                    UserContextCommandExecutor executor0 = (UserContextCommandExecutor) executor;
                    UserContextInteractionInfo info = new UserContextInteractionInfo(event0, registeredContextCommand, command, name, sbds);

                    if (!permissionCheck(info)) {
                        return;
                    }

                    if (executor0 != null) {
                        executor0.execute(info);
                    }

                }

                case MESSAGE -> {

                    MessageContextInteractionEvent event0 = (MessageContextInteractionEvent) event;
                    MessageContextCommandExecutor executor0 = (MessageContextCommandExecutor) executor;
                    MessageContextInteractionInfo info = new MessageContextInteractionInfo(event0, registeredContextCommand, command, name, sbds);

                    if (!permissionCheck(info)) {
                        return;
                    }

                    if (executor0 != null) {
                        executor0.execute(info);
                    }

                }

            }

        }

        catch (Throwable t) {
            logger.error("An internal error occurred while attempting to perform context command.", t);
            sbds.getMessages().reply(event, "sbds.error", event.getUser())
                    .withPlaceholders("exception", t)
                    .queue();
        }

    }

    private boolean permissionCheck(@NotNull ContextInteractionInfo<?> info) {

        Permission permission = info.currentCommand().getPermission();
        GenericContextInteractionEvent<?> event = info.event();

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

    public static class RegisteredContextCommand extends RegisteredInteractionCommand<IRegisteredContextCommand, IContextCommandManager> implements IRegisteredContextCommand {

        public RegisteredContextCommand(@NotNull IContextCommandManager manager, @NotNull Command command) {
            super(manager, command);
        }

    }

}
