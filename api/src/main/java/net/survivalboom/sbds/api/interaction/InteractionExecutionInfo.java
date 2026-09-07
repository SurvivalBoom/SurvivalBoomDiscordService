package net.survivalboom.sbds.api.interaction;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.callbacks.IDeferrableCallback;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import net.survivalboom.sbds.api.ISBDS;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class InteractionExecutionInfo<event extends GenericInteractionCreateEvent> extends ExecutionInfo implements InteractionHolder {

    protected final event event;

    protected final boolean ephemeral;

    public InteractionExecutionInfo(
            @NotNull event event,
            boolean ephemeral,
            @NotNull ISBDS sbds
    ) {
        super(sbds);
        this.event = event;
        this.ephemeral = ephemeral;
    }

    public @NotNull event event() {
        return event;
    }

    public boolean isEphemeral() {
        return ephemeral;
    }

    @Override
    public @NotNull Object source() {
        return event;
    }


    @Override
    public @Nullable Member member() {
        return event.getMember();
    }

    @Override
    public @NotNull User user() {
        return event.getUser();
    }

    @Override
    public @Nullable Guild guild() {
        return event.getGuild();
    }

    @Override
    public Channel channel() {
        return event.getChannel();
    }

    // EDIT //

    @Override
    public @NotNull RestAction<?> editRaw(@NotNull MessageCreateData data) {

        if (event instanceof net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback editCallback) {
            if (!editCallback.isAcknowledged()) {
                return editCallback.editMessage(MessageEditData.fromCreateData(data));
            }
        }

        if (!(event instanceof IDeferrableCallback callback)) {
            throw new IllegalStateException("No edit method applicable to `" + this + "`");
        }

        if (!callback.isAcknowledged()) {
            throw new IllegalStateException("Cannot edit original message because no message was sent yet. Did you forget to call deferReply?");
        }

        return callback.getHook().editOriginal(MessageEditData.fromCreateData(data));
    }

    @Override
    public @NotNull RestAction<?> editRaw(@NotNull String txt) {

        if (event instanceof net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback editCallback) {
            if (!editCallback.isAcknowledged()) {
                return editCallback.editMessage(txt);
            }
        }

        if (!(event instanceof IDeferrableCallback callback)) {
            throw new IllegalStateException("No edit method applicable to `" + this + "`");
        }

        if (!callback.isAcknowledged()) {
            throw new IllegalStateException("Cannot edit original message because no message was sent yet. Did you forget to call deferReply?");
        }

        return callback.getHook().editOriginal(txt);
    }

    // SEND ONLY //

    @Override
    public @NotNull RestAction<?> sendRaw(@NotNull String txt) {

        if (!(event instanceof IReplyCallback callback)) {
            throw new IllegalStateException("No reply method applicable to `" + this + "`");
        }

        return callback.reply(txt).setEphemeral(ephemeral);

    }

    @Override
    public @NotNull RestAction<?> sendRaw(@NotNull MessageCreateData data) {

        if (!(event instanceof IReplyCallback callback)) {
            throw new IllegalStateException("No reply method applicable to `" + this + "`");
        }

        return callback.reply(data).setEphemeral(ephemeral);

    }

    // REPLY (INTELLIGENT) //

    @Override
    public @NotNull RestAction<?> replyRaw(@NotNull String txt) {

        if (ephemeral) {
            return sendRaw(txt);
        }

        if (!(event instanceof IReplyCallback)) {
            throw new IllegalStateException("No reply method applicable to `" + this + "`");
        }

        if (event.isAcknowledged()) {
            return editRaw(txt);
        }

        else {
            return sendRaw(txt);
        }

    }

    @Override
    public @NotNull RestAction<?> replyRaw(@NotNull MessageCreateData data) {

        if (ephemeral) {
            return sendRaw(data);
        }

        if (!(event instanceof IReplyCallback)) {
            throw new IllegalStateException("No reply method applicable to `" + this + "`");
        }

        if (event.isAcknowledged()) {
            return editRaw(data);
        }

        else {
            return sendRaw(data);
        }

    }

    // COMPONENTS //

    @Override
    public void invalidateInputs() {

        if (isEphemeral()) {
            if (event instanceof IDeferrableCallback callback) {
                callback.getHook().editOriginalComponents().queue(s -> {}, e -> {});
            }
            return;
        }

        if (!(event instanceof IDeferrableCallback callback)) {
            throw new IllegalStateException("No edit method applicable to `" + this + "`");
        }

        Message message = callback.getHook().retrieveOriginal().complete();
        if (message == null) {
            throw new IllegalStateException("No message sent yet");
        }

        invalidateInputs0(message);

    }

}
