package net.survivalboom.sbds.modules.logging.module.listeners;

import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.events.stage.StageInstanceCreateEvent;
import net.dv8tion.jda.api.events.stage.StageInstanceDeleteEvent;
import net.dv8tion.jda.api.events.stage.update.StageInstanceUpdateTopicEvent;
import net.survivalboom.sbds.api.events.EventHandler;
import net.survivalboom.sbds.modules.logging.api.events.stages.StageEndLogEvent;
import net.survivalboom.sbds.modules.logging.api.events.stages.StageStartLogEvent;
import net.survivalboom.sbds.modules.logging.api.events.stages.StageTopicUpdateLogEvent;
import net.survivalboom.sbds.modules.logging.module.LoggingModule;
import org.jetbrains.annotations.NotNull;

public class StageListener extends AbstractLogListener {

    public StageListener(@NotNull LoggingModule module) {
        super(module);
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onStageStart(StageInstanceCreateEvent event) {
        fetchModeratorAndReason(event.getGuild(), ActionType.STAGE_INSTANCE_CREATE, event.getChannel().getIdLong(), (moderator, reason) -> {

            StageStartLogEvent customEvent = new StageStartLogEvent(module, event, moderator, reason);
            module.callEvent(customEvent);

            Long modId = moderator != null ? moderator.getIdLong() : null;
            String payload = "Topic: " + event.getInstance().getTopic();

            module.getLogDataManager().create(
                    event.getGuild().getIdLong(),
                    event.getChannel().getIdLong(),
                    modId,
                    "STAGE_START",
                    customEvent.getReason(),
                    payload
            );
        });
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onStageEnd(StageInstanceDeleteEvent event) {
        fetchModeratorAndReason(event.getGuild(), ActionType.STAGE_INSTANCE_DELETE, event.getChannel().getIdLong(), (moderator, reason) -> {

            StageEndLogEvent customEvent = new StageEndLogEvent(module, event, moderator, reason);
            module.callEvent(customEvent);

            Long modId = moderator != null ? moderator.getIdLong() : null;
            String payload = "Topic: " + event.getInstance().getTopic();

            module.getLogDataManager().create(
                    event.getGuild().getIdLong(),
                    event.getChannel().getIdLong(),
                    modId,
                    "STAGE_END",
                    customEvent.getReason(),
                    payload
            );
        });
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onStageTopicUpdate(StageInstanceUpdateTopicEvent event) {
        fetchModeratorAndReason(event.getGuild(), ActionType.STAGE_INSTANCE_UPDATE, event.getChannel().getIdLong(), (moderator, reason) -> {

            StageTopicUpdateLogEvent customEvent = new StageTopicUpdateLogEvent(module, event, moderator, reason);
            module.callEvent(customEvent);

            Long modId = moderator != null ? moderator.getIdLong() : null;
            String payload = String.format("Old: %s | New: %s", event.getOldValue(), event.getNewValue());

            module.getLogDataManager().create(
                    event.getGuild().getIdLong(),
                    event.getChannel().getIdLong(),
                    modId,
                    "STAGE_TOPIC_UPDATE",
                    customEvent.getReason(),
                    payload
            );
        });
    }
}