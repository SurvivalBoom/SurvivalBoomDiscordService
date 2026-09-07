package net.survivalboom.sbds.modules.logging.module.listeners;

import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateVoiceStatusEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.survivalboom.sbds.api.events.EventHandler;
import net.survivalboom.sbds.modules.logging.api.events.voices.*;
import net.survivalboom.sbds.modules.logging.module.LoggingModule;
import org.jetbrains.annotations.NotNull;

public class VoiceListener extends AbstractLogListener {

    public VoiceListener(@NotNull LoggingModule module) {
        super(module);
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onVoiceUpdate(GuildVoiceUpdateEvent event) {
        if (event.getMember().getUser().isBot()) return;

        boolean isJoin = event.getChannelLeft() == null && event.getChannelJoined() != null;
        boolean isLeave = event.getChannelLeft() != null && event.getChannelJoined() == null;
        boolean isMove = event.getChannelLeft() != null && event.getChannelJoined() != null;

        if (isJoin) {
            UserVoiceJoinLogEvent customEvent = new UserVoiceJoinLogEvent(module, event, event.getChannelJoined());
            module.callEvent(customEvent);

            if (customEvent.isCancelled()) {
                event.getGuild().kickVoiceMember(event.getMember()).queue(null, ex -> {});
                return;
            }

            module.getLogDataManager().create(
                    event.getGuild().getIdLong(),
                    event.getMember().getIdLong(),
                    null,
                    "VOICE_JOIN",
                    null,
                    event.getChannelJoined().getName()
            );
        }
        else if (isLeave) {
            UserVoiceLeaveLogEvent customEvent = new UserVoiceLeaveLogEvent(module, event, event.getChannelLeft());
            module.callEvent(customEvent);

            module.getLogDataManager().create(
                    event.getGuild().getIdLong(),
                    event.getMember().getIdLong(),
                    null,
                    "VOICE_LEAVE",
                    null,
                    event.getChannelLeft().getName()
            );
        }
        else if (isMove) {
            UserVoiceMoveLogEvent customEvent = new UserVoiceMoveLogEvent(module, event, event.getChannelLeft(), event.getChannelJoined());
            module.callEvent(customEvent);

            if (customEvent.isCancelled()) {
                event.getGuild().moveVoiceMember(event.getMember(), event.getChannelLeft()).queue(null, ex -> {});
                return;
            }

            String payload = String.format("Old: %s | New: %s", event.getChannelLeft().getName(), event.getChannelJoined().getName());
            module.getLogDataManager().create(
                    event.getGuild().getIdLong(),
                    event.getMember().getIdLong(),
                    null,
                    "VOICE_MOVE",
                    null,
                    payload
            );
        }
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onVoiceStatusUpdate(ChannelUpdateVoiceStatusEvent event) {
        fetchModeratorAndReason(event.getGuild(), ActionType.VOICE_CHANNEL_STATUS_UPDATE, event.getChannel().getIdLong(), (moderator, reason) -> {

            String oldTopic = event.getOldValue();
            String newTopic = event.getNewValue();

            if (newTopic == null || newTopic.isBlank()) {
                return;
            }

            VoiceChannelStatusUpdateLogEvent customEvent = new VoiceChannelStatusUpdateLogEvent(module, event, moderator, reason);
            module.callEvent(customEvent);

            Long modId = moderator != null ? moderator.getIdLong() : null;
            String payload = String.format("Old: %s | New: %s", event.getOldValue(), event.getNewValue());

            module.getLogDataManager().create(
                    event.getGuild().getIdLong(),
                    event.getChannel().getIdLong(),
                    modId,
                    "VOICE_STATUS_UPDATE",
                    customEvent.getReason(),
                    payload
            );
        });
    }
}