package net.survivalboom.sbds.api.commands.slash;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import net.survivalboom.sbds.api.commands.Command;
import net.survivalboom.sbds.api.commands.CommandExecutionInfo;
import net.survivalboom.sbds.api.interaction.InteractionHolder;
import net.survivalboom.sbds.api.utils.typemap.TypeMap;
import org.jetbrains.annotations.NotNull;

public class SlashExecutionInfo extends CommandExecutionInfo<ISlashCommandManager.IRegisteredSlashCommand, ISlashCommandManager> implements InteractionHolder {

    protected final SlashCommandInteraction interaction;

    protected final boolean ephemeral;

    public SlashExecutionInfo(
            @NotNull SlashCommandInteraction interaction,
            @NotNull ISlashCommandManager.IRegisteredSlashCommand rootCommand,
            @NotNull Command currentCommand,
            @NotNull String alias,
            @NotNull TypeMap arguments,
            boolean ephemeral
    ) {
        super(rootCommand, currentCommand, alias, arguments);
        this.interaction = interaction;
        this.ephemeral = ephemeral;
    }

    @Override
    public @NotNull Object source() {
        return interaction;
    }

    @Override
    public boolean isEphemeral() {
        return ephemeral;
    }

    public @NotNull SlashCommandInteraction interaction() {
        return interaction;
    }

    @Override
    public Guild guild() {
        return interaction.getGuild();
    }

    @Override
    public @NotNull User user() {
        return interaction.getUser();
    }

    @Override
    public Member member() {
        return interaction.getMember();
    }

    @Override
    public Channel channel() {
        return interaction.getChannel();
    }

    // EDIT //

    @Override
    public @NotNull RestAction<?> editRaw(@NotNull String txt) {
        if (!interaction.isAcknowledged()) {
            throw new IllegalStateException("Cannot edit original message because no message was sent yet. Did you forget to call deferReply?");
        }
        return interaction.getHook().editOriginal(txt);
    }

    @Override
    public @NotNull RestAction<?> editRaw(@NotNull MessageCreateData data) {
        if (!interaction.isAcknowledged()) {
            throw new IllegalStateException("Cannot edit original message because no message was sent yet. Did you forget to call deferReply?");
        }
        return interaction.getHook().editOriginal(MessageEditData.fromCreateData(data));
    }

    // SEND ONLY //

    @Override
    public @NotNull RestAction<?> sendRaw(@NotNull String txt) {

        if (interaction.isAcknowledged()) {
            return interaction.getHook().sendMessage(txt).setEphemeral(currentCommand.isEphemeral());
        }

        return interaction.reply(txt).setEphemeral(currentCommand.isEphemeral());

    }

    @Override
    public @NotNull RestAction<?> sendRaw(@NotNull MessageCreateData data) {

        if (interaction.isAcknowledged()) {
            return interaction.getHook().sendMessage(data).setEphemeral(currentCommand.isEphemeral());
        }

        return interaction.reply(data).setEphemeral(currentCommand.isEphemeral());

    }

    // REPLY (INTELLIGENT) //

    @Override
    public @NotNull RestAction<?> replyRaw(@NotNull String txt) {
        if (interaction.isAcknowledged()) {
            return editRaw(txt);
        } else {
            return sendRaw(txt);
        }
    }

    @Override
    public @NotNull RestAction<?> replyRaw(@NotNull MessageCreateData data) {
        if (interaction.isAcknowledged()) {
            return editRaw(data);
        } else {
            return sendRaw(data);
        }
    }

    // COMPONENT //

    @Override
    public void invalidateInputs() {

        if (isEphemeral()) {
            return;
        }

        Message message = interaction.getHook().retrieveOriginal().complete();
        if (message == null) {
            throw new IllegalStateException("No message sent yet");
        }

        invalidateInputs0(message);


    }

}
