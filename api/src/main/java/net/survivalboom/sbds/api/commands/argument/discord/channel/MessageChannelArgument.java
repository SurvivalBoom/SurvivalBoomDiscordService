package net.survivalboom.sbds.api.commands.argument.discord.channel;

import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

public class MessageChannelArgument extends ChannelArgument<MessageChannel> {

    public MessageChannelArgument() {
        super(MessageChannel.class);
    }

}
