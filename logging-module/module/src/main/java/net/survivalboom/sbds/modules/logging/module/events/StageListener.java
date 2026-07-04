package net.survivalboom.sbds.modules.logging.module.events;

import net.dv8tion.jda.api.events.stage.StageInstanceCreateEvent;
import net.dv8tion.jda.api.events.stage.StageInstanceDeleteEvent;
import net.dv8tion.jda.api.events.stage.update.StageInstanceUpdateTopicEvent;
import net.survivalboom.sbds.api.events.EventHandler;
import net.survivalboom.sbds.api.events.EventListener;
import net.survivalboom.sbds.modules.logging.module.LoggingModule;
import net.survivalboom.sbds.modules.logging.module.logging.LogManager;
import org.jetbrains.annotations.NotNull;

public class StageListener implements EventListener {

    private final LoggingModule module;

    public StageListener(@NotNull LoggingModule module) {
        this.module = module;
    }

    @EventHandler
    public void onStageStart(StageInstanceCreateEvent event) {
        String stageChannelMention = "<#" + event.getChannel().getId() + ">";
        String topic = event.getInstance().getTopic();

        LogManager.dispatch(module, event.getGuild().getIdLong(),
                "events.stage", "events.stage.start",
                "logging.message.stage.start",
                "stage_channel", stageChannelMention,
                "topic", topic
        );
    }

    @EventHandler
    public void onStageEnd(StageInstanceDeleteEvent event) {
        String stageChannelMention = "<#" + event.getChannel().getId() + ">";
        String topic = event.getInstance().getTopic();

        LogManager.dispatch(module, event.getGuild().getIdLong(),
                "events.stage", "events.stage.end",
                "logging.message.stage.end",
                "stage_channel", stageChannelMention,
                "topic", topic
        );
    }

    @EventHandler
    public void onStageTopicUpdate(StageInstanceUpdateTopicEvent event) {
        String stageChannelMention = "<#" + event.getChannel().getId() + ">";
        String oldTopic = event.getOldValue();
        String newTopic = event.getNewValue();

        LogManager.dispatch(module, event.getGuild().getIdLong(),
                "events.stage", "events.stage.topic",
                "logging.message.stage.topic",
                "stage_channel", stageChannelMention,
                "old_topic", oldTopic,
                "new_topic", newTopic
        );
    }
}