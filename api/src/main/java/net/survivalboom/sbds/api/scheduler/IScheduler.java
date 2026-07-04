package net.survivalboom.sbds.api.scheduler;

import net.survivalboom.sbds.api.modules.IModule;
import net.survivalboom.sbds.api.modules.ModuleMain;
import net.survivalboom.sbds.api.utils.valid.IManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public interface IScheduler extends IManager {

    @NotNull ISchedulerTask schedule(
            @NotNull IModule module,
            @Nullable String name,
            @NotNull Consumer<ISchedulerTask> consumer,
            int delay,
            int period
    );

    default @NotNull ISchedulerTask schedule(
            @NotNull IModule module,
            @NotNull Consumer<ISchedulerTask> consumer,
            int delay,
            int period
    ) {
        return schedule(module, null, consumer, delay, period);
    }

    default @NotNull ISchedulerTask schedule(
            @NotNull IModule module,
            @Nullable String name,
            @NotNull Runnable runnable,
            int delay,
            int period
    ) {
        return schedule(module, name, task -> runnable.run(), delay, period);
    }

    default @NotNull ISchedulerTask schedule(
            @NotNull IModule module,
            @NotNull Runnable runnable,
            int delay,
            int period
    ) {
        return schedule(module, null, runnable, delay, period);
    }

    default @NotNull ISchedulerTask schedule(
            @NotNull ModuleMain main,
            @Nullable String name,
            @NotNull Consumer<ISchedulerTask> consumer,
            int delay,
            int period
    ) {
        return schedule(main.getModule(), name, consumer, delay, period);
    }

    default @NotNull ISchedulerTask schedule(
            @NotNull ModuleMain main,
            @NotNull Consumer<ISchedulerTask> consumer,
            int delay,
            int period
    ) {
        return schedule(main.getModule(), consumer, delay, period);
    }

    default @NotNull ISchedulerTask schedule(
            @NotNull ModuleMain main,
            @Nullable String name,
            @NotNull Runnable runnable,
            int delay,
            int period
    ) {
        return schedule(main.getModule(), name, runnable, delay, period);
    }

    default @NotNull ISchedulerTask schedule(@NotNull ModuleMain main, @NotNull Runnable runnable, int delay, int period) {
        return schedule(main.getModule(), runnable, delay, period);
    }

    default @NotNull ISchedulerTask schedule(@NotNull IModule module, @NotNull Runnable runnable) {
        return schedule(module, runnable, 0, 0);
    }

    default @NotNull ISchedulerTask schedule(@NotNull ModuleMain main, @NotNull Runnable runnable) {
        return schedule(main.getModule(), runnable, 0, 0);
    }

    @NotNull List<ISchedulerTask> getTasks();

}
