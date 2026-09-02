package net.survivalboom.sbds.modules.changestatus;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.survivalboom.sbds.api.commands.Command;
import net.survivalboom.sbds.api.commands.console.ConsoleExecutionInfo;
import net.survivalboom.sbds.api.modules.ModuleMain;
import net.survivalboom.sbds.api.scheduler.ISchedulerTask;
import net.survivalboom.sbds.api.utils.CommonUtils;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.ArrayList;
import java.util.List;

public class ChangeStatusModule extends ModuleMain {

    private final List<ConfiguredStatus> statusList = new ArrayList<>();

    private ISchedulerTask task = null;

    @Override
    public void onEnable() {

        checkAndLoadConfig();

        int minutes = getConfig().node("change-interval").getInt();
        if (minutes < 5) {
            getLogger().warn("Invalid interval. Falling to default: 10 minutes.");
            minutes = 10;
        }

        try {
            var statuses = getConfig().node("statuses").getList(ConfiguredStatus.class);
            this.statusList.addAll(statuses);
        }

        catch (SerializationException e) {
            getLogger().error("Failed to load statuses! Womp womp...", e);
        }

        if (!statusList.isEmpty()) {
            getLogger().info("Loaded {} statuses; Change interval: {} minutes", statusList.size(), minutes);
        }

        registerCommand(
                Command.create("random-status")
                        .setDescription("Set a random status from the config")
                        .setConsoleExecutor(this::command)
                        .build()
        );

        task = schedule("change-status-task", this::changeStatus, 5000, minutes * 60 * 1000);

    }

    @Override
    public void onDisable() {

        statusList.clear();

        getSbds().getBot().getPresence().setPresence(OnlineStatus.IDLE, false);

        task.tryCancel();
        task = null;

    }

    private void changeStatus() {

        CommonUtils.waitUntil(getSbds()::isReady);

        if (statusList.isEmpty()) {
            return;
        }

        int index = CommonUtils.RANDOM.nextInt(statusList.size());
        ConfiguredStatus status = statusList.get(index);

        getSbds().getBot().getPresence().setPresence(status.status, Activity.of(status.activity, status.text));

        getLogger().info("Changed bot status to {} {} `{}`", status.status, status.activity, status.text);

    }

    private void command(@NotNull ConsoleExecutionInfo info) {
        changeStatus();
    }

    @ConfigSerializable
    private record ConfiguredStatus(@NotNull OnlineStatus status, @NotNull Activity.ActivityType activity, @NotNull String text) {

    }

}
