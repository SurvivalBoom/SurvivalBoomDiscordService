package net.survivalboom.sbds.modules.logging.api.events.voices;

import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.survivalboom.sbds.api.events.EventCancellableBase;
import net.survivalboom.sbds.api.modules.ModuleMain;
import org.jetbrains.annotations.NotNull;

public class UserVoiceMoveLogEvent extends EventCancellableBase {

    private final GuildVoiceUpdateEvent jdaEvent;
    private final AudioChannelUnion channelLeft;
    private final AudioChannelUnion channelJoined;


    public UserVoiceMoveLogEvent(@NotNull ModuleMain module, @NotNull GuildVoiceUpdateEvent jdaEvent, @NotNull AudioChannelUnion channelLeft, @NotNull AudioChannelUnion channelJoined) {
        super(module);
        this.jdaEvent = jdaEvent;
        this.channelLeft = channelLeft;
        this.channelJoined = channelJoined;
    }


    public @NotNull GuildVoiceUpdateEvent getJdaEvent() {
        return jdaEvent;
    }

    public @NotNull AudioChannelUnion getChannelLeft() {
        return channelLeft;
    }

    public @NotNull AudioChannelUnion getChannelJoined() {
        return channelJoined;
    }
}