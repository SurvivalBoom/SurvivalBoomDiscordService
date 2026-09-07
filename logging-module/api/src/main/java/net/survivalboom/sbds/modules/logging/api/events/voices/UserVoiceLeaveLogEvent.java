package net.survivalboom.sbds.modules.logging.api.events.voices;

import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.survivalboom.sbds.api.events.EventBase;
import net.survivalboom.sbds.api.modules.ModuleMain;
import org.jetbrains.annotations.NotNull;

public class UserVoiceLeaveLogEvent extends EventBase {

    private final GuildVoiceUpdateEvent jdaEvent;
    private final AudioChannelUnion channelLeft;

    public UserVoiceLeaveLogEvent(@NotNull ModuleMain module, @NotNull GuildVoiceUpdateEvent jdaEvent, @NotNull AudioChannelUnion channelLeft) {
        super(module);
        this.jdaEvent = jdaEvent;
        this.channelLeft = channelLeft;
    }


    public @NotNull GuildVoiceUpdateEvent getJdaEvent() {
        return jdaEvent;
    }

    public @NotNull AudioChannelUnion getChannelLeft() {
        return channelLeft;
    }
}