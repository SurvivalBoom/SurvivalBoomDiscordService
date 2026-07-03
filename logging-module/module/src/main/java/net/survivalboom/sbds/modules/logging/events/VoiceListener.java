package net.survivalboom.sbds.modules.logging.events;

import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.survivalboom.sbds.api.events.EventHandler;
import net.survivalboom.sbds.api.events.EventListener;
import net.survivalboom.sbds.modules.logging.LoggingModule;
import net.survivalboom.sbds.modules.logging.logging.LogManager;
import org.jetbrains.annotations.NotNull;

public class VoiceListener implements EventListener {

    private final LoggingModule module;

    public VoiceListener(@NotNull LoggingModule module) {
        this.module = module;
    }

    @EventHandler
    public void onVoiceUpdate(GuildVoiceUpdateEvent event) {
        long guildId = event.getGuild().getIdLong();
        var channelJoined = event.getChannelJoined();
        var channelLeft = event.getChannelLeft();
        var member = event.getMember();

        if (channelLeft == null && channelJoined != null) {
            LogManager.dispatch(module, guildId,
                    "events.voice", "events.voice.join",
                    "logging.message.voice.join",
                    "username", member.getEffectiveName(),
                    "user_id", member.getId(),
                    "user", member,
                    "channel", channelJoined,
                    "channel_name", channelJoined.getName()
            );
        }

        else if (channelJoined == null && channelLeft != null) {
            LogManager.dispatch(module, guildId,
                    "events.voice", "events.voice.leave",
                    "logging.message.voice.leave",
                    "username", member.getEffectiveName(),
                    "user_id", member.getId(),
                    "user", member,
                    "channel", channelLeft,
                    "channel_name", channelLeft.getName()
            );
        }

        else if (channelLeft != null && channelJoined != null) {
            LogManager.dispatch(module, guildId,
                    "events.voice", "events.voice.move",
                    "logging.message.voice.move",
                    "username", member.getEffectiveName(),
                    "user_id", member.getId(),
                    "user", member,
                    "old_channel", channelLeft,
                    "new_channel", channelJoined
            );
        }
    }
}