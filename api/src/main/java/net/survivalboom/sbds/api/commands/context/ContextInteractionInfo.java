package net.survivalboom.sbds.api.commands.context;

import net.dv8tion.jda.api.events.interaction.command.GenericContextInteractionEvent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.survivalboom.sbds.api.ISBDS;
import net.survivalboom.sbds.api.commands.Command;
import net.survivalboom.sbds.api.interaction.InteractionExecutionInfo;
import org.jetbrains.annotations.NotNull;

public abstract class ContextInteractionInfo<E extends GenericContextInteractionEvent<?>> extends InteractionExecutionInfo<E> {

    protected final IContextCommandManager.IRegisteredContextCommand rootCommand;

    protected final Command currentCommand;

    protected final String alias;

    public ContextInteractionInfo(
            @NotNull E event,
            @NotNull IContextCommandManager.IRegisteredContextCommand rootCommand,
            @NotNull Command currentCommand,
            @NotNull String alias,
            @NotNull ISBDS sbds
    ) {
        super(event, currentCommand.isEphemeral(), sbds);
        this.rootCommand = rootCommand;
        this.currentCommand = currentCommand;
        this.alias = alias;
    }

    // EDIT //

    @Override
    public @NotNull RestAction<?> editRaw(@NotNull String txt) {
        if (!event.isAcknowledged()) {
            throw new IllegalStateException("Cannot edit original message because no message was sent yet. Did you forget to call deferReply?");
        }
        return event.getHook().editOriginal(txt);
    }

    @Override
    public @NotNull RestAction<?> editRaw(@NotNull MessageCreateData data) {
        if (!event.isAcknowledged()) {
            throw new IllegalStateException("Cannot edit original message because no message was sent yet. Did you forget to call deferReply?");
        }
        return event.getHook().editOriginal(net.dv8tion.jda.api.utils.messages.MessageEditData.fromCreateData(data));
    }

    // SEND ONLY //

    @Override
    public @NotNull RestAction<?> sendRaw(@NotNull String txt) {
        if (event.isAcknowledged()) {
            return event.getHook().sendMessage(txt).setEphemeral(currentCommand.isEphemeral());
        }
        return event.reply(txt).setEphemeral(currentCommand.isEphemeral());
    }

    @Override
    public @NotNull RestAction<?> sendRaw(@NotNull MessageCreateData data) {
        if (event.isAcknowledged()) {
            return event.getHook().sendMessage(data).setEphemeral(currentCommand.isEphemeral());
        }
        return event.reply(data).setEphemeral(currentCommand.isEphemeral());
    }

    // REPLY //

    @Override
    public @NotNull RestAction<?> replyRaw(@NotNull String txt) {
        if (event.isAcknowledged()) {
            return editRaw(txt);
        } else {
            return sendRaw(txt);
        }
    }

    @Override
    public @NotNull RestAction<?> replyRaw(@NotNull MessageCreateData data) {
        if (event.isAcknowledged()) {
            return editRaw(data);
        } else {
            return sendRaw(data);
        }
    }

    public @NotNull IContextCommandManager.IRegisteredContextCommand rootCommand() {
        return rootCommand;
    }

    public @NotNull Command currentCommand() {
        return currentCommand;
    }

    public @NotNull String alias() {
        return alias;
    }

}
