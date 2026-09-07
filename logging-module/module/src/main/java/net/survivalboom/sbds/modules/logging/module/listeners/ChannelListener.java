package net.survivalboom.sbds.modules.logging.module.listeners;

import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateNameEvent;
import net.survivalboom.sbds.api.events.EventHandler;
import net.survivalboom.sbds.modules.logging.api.events.channels.ChannelCreateLogEvent;
import net.survivalboom.sbds.modules.logging.api.events.channels.ChannelDeleteLogEvent;
import net.survivalboom.sbds.modules.logging.api.events.channels.ChannelUpdateNameLogEvent;
import net.survivalboom.sbds.modules.logging.module.LoggingModule;
import org.jetbrains.annotations.NotNull;

public class ChannelListener extends AbstractLogListener {

    public ChannelListener(@NotNull LoggingModule module) {
        super(module);
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onChannelCreate(ChannelCreateEvent event) {
        fetchModeratorAndReason(event.getGuild(), ActionType.CHANNEL_CREATE, event.getChannel().getIdLong(), (moderator, reason) -> {

            ChannelCreateLogEvent customEvent = new ChannelCreateLogEvent(module, event, moderator, reason);
            module.callEvent(customEvent);

            Long modId = moderator != null ? moderator.getIdLong() : null;
            String payload = String.format("Type: %s | Name: %s", event.getChannel().getType().name(), event.getChannel().getName());

            module.getLogDataManager().create(
                    event.getGuild().getIdLong(),
                    event.getChannel().getIdLong(),
                    modId,
                    "CHANNEL_CREATE",
                    customEvent.getReason(),
                    payload
            );
        });
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onChannelDelete(ChannelDeleteEvent event) {
        fetchModeratorAndReason(event.getGuild(), ActionType.CHANNEL_DELETE, event.getChannel().getIdLong(), (moderator, reason) -> {

            ChannelDeleteLogEvent customEvent = new ChannelDeleteLogEvent(module, event, moderator, reason);
            module.callEvent(customEvent);

            Long modId = moderator != null ? moderator.getIdLong() : null;
            String payload = String.format("Type: %s | Name: %s", event.getChannel().getType().name(), event.getChannel().getName());

            module.getLogDataManager().create(
                    event.getGuild().getIdLong(),
                    event.getChannel().getIdLong(),
                    modId,
                    "CHANNEL_DELETE",
                    customEvent.getReason(),
                    payload
            );
        });
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onChannelNameUpdate(ChannelUpdateNameEvent event) {
        fetchModeratorAndReason(event.getGuild(), ActionType.CHANNEL_UPDATE, event.getChannel().getIdLong(), (moderator, reason) -> {

            ChannelUpdateNameLogEvent customEvent = new ChannelUpdateNameLogEvent(module, event, moderator, reason);
            module.callEvent(customEvent);

            if (customEvent.isCancelled()) {
                if (event.getChannel() instanceof GuildChannel guildChannel) {
                    guildChannel.getManager().setName(event.getOldValue())
                            .reason(customEvent.getCancelReason())
                            .queue(null, ex -> {});
                }
                return;
            }

            Long modId = moderator != null ? moderator.getIdLong() : null;
            String payload = String.format("Old: %s | New: %s", event.getOldValue(), event.getNewValue());

            module.getLogDataManager().create(
                    event.getGuild().getIdLong(),
                    event.getChannel().getIdLong(),
                    modId,
                    "CHANNEL_NAME_UPDATE",
                    customEvent.getReason(),
                    payload
            );
        });
    }
}