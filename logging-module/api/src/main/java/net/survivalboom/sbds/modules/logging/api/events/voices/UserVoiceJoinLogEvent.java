package net.survivalboom.sbds.modules.logging.api.events.voices;

import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.survivalboom.sbds.api.events.EventCancellableBase;
import net.survivalboom.sbds.api.modules.ModuleMain;
import org.jetbrains.annotations.NotNull;

public class UserVoiceJoinLogEvent extends EventCancellableBase {

    private final GuildVoiceUpdateEvent jdaEvent;
    private final AudioChannelUnion channelJoined;

    public UserVoiceJoinLogEvent(@NotNull ModuleMain module, @NotNull GuildVoiceUpdateEvent jdaEvent, @NotNull AudioChannelUnion channelJoined) {
        super(module);
        this.jdaEvent = jdaEvent;
        this.channelJoined = channelJoined;
    }


    public @NotNull GuildVoiceUpdateEvent getJdaEvent() {
        return jdaEvent;
    }

    public @NotNull AudioChannelUnion getChannelJoined() {
        return channelJoined;
    }
}