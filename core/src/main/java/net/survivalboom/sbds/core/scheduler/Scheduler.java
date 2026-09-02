package net.survivalboom.sbds.core.scheduler;

import net.survivalboom.sbds.api.modules.IModule;
import net.survivalboom.sbds.api.registrations.Registration;
import net.survivalboom.sbds.api.registrations.RegistrationManager;
import net.survivalboom.sbds.api.scheduler.IScheduler;
import net.survivalboom.sbds.api.scheduler.ISchedulerTask;
import net.survivalboom.sbds.api.utils.CommonUtils;
import net.survivalboom.sbds.api.utils.ThrowingConsumer;
import net.survivalboom.sbds.api.utils.ThrowingRunnable;
import net.survivalboom.sbds.core.SBDS;
import net.survivalboom.sbds.api.utils.valid.Manager;
import net.survivalboom.sbds.core.registration.InternalRegistrationManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.util.*;

public class Scheduler extends Manager implements IScheduler, RegistrationManager.Callback<ISchedulerTask> {

    protected static final Logger logger = LoggerFactory.getLogger(Scheduler.class.getSimpleName());


    private final SBDS sbds;

    protected final InternalRegistrationManager<ISchedulerTask> registry;

    public Scheduler(@NotNull SBDS sbds) {
        this.sbds = sbds;
        this.registry = new InternalRegistrationManager<>(this, this, sbds.getRegistrationRegistry());
    }

    //
    // MANAGER
    //

    @Override
    protected void init0() {
        registry.init();
    }

    @Override
    protected void shutdown0() {

        List<ISchedulerTask> tasks = getTasks();
        if (!tasks.isEmpty()) {
            logger.warn("There is {} tasks running. Seems like you did something wrong, you should stop all the tasks before shutting down the scheduler.", tasks.size());
        }

        registry.shutdown();

    }

    @Override
    public void unRegister(@NotNull Registration<ISchedulerTask> registration) {

        ISchedulerTask task = registration.object();

        if (task.isStopped()) {
            return;
        }

        String name = task.getRegistration().key().toString();
        logger.warn("> Waiting task `{}` to stop...", name);

        boolean killed = task.cancelAndWaitOrKill(5000, false);

        if (killed) {
            logger.warn("! Killing task `{}` due to timeout of 5 seconds.", name);
            CommonUtils.logThreadStackTrace(logger, Level.WARN, task.getThread());
        }

        else {
            logger.warn("- Stopped `{}`...", name);
        }

    }

    //
    // TASKS
    //

    @Override
    public @NotNull SchedulerTask schedule(
            @NotNull IModule imodule,
            @Nullable String name,
            @NotNull ThrowingConsumer<ISchedulerTask> consumer,
            int delay,
            int period
    ) {
        Objects.requireNonNull(imodule, "module == null");
        return schedule0(imodule, name, consumer, delay, period);
    }

    public @NotNull SchedulerTask schedule0(
            @Nullable IModule module,
            @Nullable String name,
            @NotNull ThrowingConsumer<ISchedulerTask> consumer,
            int delay,
            int period
    ) {

        Objects.requireNonNull(consumer, "consumer == null");
        checkValid();

        if (module != null) {
            sbds.getModuleManager().checkModuleEnabled(module, "Disabled module attempted to register a task");
        }

        String taskName = createName(name);

        SchedulerTask task = new SchedulerTask(consumer, delay, period, this);
        task.registration = registry.register0(module, taskName, task);
        task.launch();

        return task;

    }

    public @NotNull SchedulerTask schedule0(@Nullable IModule module, @Nullable String name, @NotNull ThrowingRunnable runnable, int delay, int period) {
        return schedule0(module, name, task -> runnable.run(), delay, period);
    }

    @Override
    public @NotNull List<ISchedulerTask> getTasks() {
        return registry.getRegisteredObjects();
    }

    //
    // MISC
    //

    private @NotNull String createName(@Nullable String name) {

        int number = CommonUtils.RANDOM.nextInt(9999);

        if (name == null) name = "task";

        return name + "_" + number;

    }

}
