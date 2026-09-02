package net.survivalboom.sbds.api.scheduler;

import net.survivalboom.sbds.api.modules.IModule;
import net.survivalboom.sbds.api.modules.ModuleMain;
import net.survivalboom.sbds.api.utils.ThrowingConsumer;
import net.survivalboom.sbds.api.utils.ThrowingRunnable;
import net.survivalboom.sbds.api.utils.valid.IManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface IScheduler extends IManager {

    @NotNull ISchedulerTask schedule(
            @NotNull IModule module,
            @Nullable String name,
            @NotNull ThrowingConsumer<ISchedulerTask> consumer,
            int delay,
            int period
    );

    default @NotNull ISchedulerTask schedule(
            @NotNull IModule module,
            @NotNull ThrowingConsumer<ISchedulerTask> consumer,
            int delay,
            int period
    ) {
        return schedule(module, null, consumer, delay, period);
    }

    default @NotNull ISchedulerTask schedule(
            @NotNull IModule module,
            @Nullable String name,
            @NotNull ThrowingRunnable runnable,
            int delay,
            int period
    ) {
        return schedule(module, name, task -> runnable.run(), delay, period);
    }

    default @NotNull ISchedulerTask schedule(
            @NotNull IModule module,
            @NotNull ThrowingRunnable runnable,
            int delay,
            int period
    ) {
        return schedule(module, null, runnable, delay, period);
    }

    default @NotNull ISchedulerTask schedule(
            @NotNull ModuleMain main,
            @Nullable String name,
            @NotNull ThrowingConsumer<ISchedulerTask> consumer,
            int delay,
            int period
    ) {
        return schedule(main.getModule(), name, consumer, delay, period);
    }

    default @NotNull ISchedulerTask schedule(
            @NotNull ModuleMain main,
            @NotNull ThrowingConsumer<ISchedulerTask> consumer,
            int delay,
            int period
    ) {
        return schedule(main.getModule(), consumer, delay, period);
    }

    default @NotNull ISchedulerTask schedule(
            @NotNull ModuleMain main,
            @Nullable String name,
            @NotNull ThrowingRunnable runnable,
            int delay,
            int period
    ) {
        return schedule(main.getModule(), name, runnable, delay, period);
    }

    default @NotNull ISchedulerTask schedule(@NotNull ModuleMain main, @NotNull ThrowingRunnable runnable, int delay, int period) {
        return schedule(main.getModule(), runnable, delay, period);
    }

    default @NotNull ISchedulerTask schedule(@NotNull IModule module, @NotNull ThrowingRunnable runnable) {
        return schedule(module, runnable, 0, 0);
    }

    default @NotNull ISchedulerTask schedule(@NotNull ModuleMain main, @NotNull ThrowingRunnable runnable) {
        return schedule(main.getModule(), runnable, 0, 0);
    }

    @NotNull List<ISchedulerTask> getTasks();

}
