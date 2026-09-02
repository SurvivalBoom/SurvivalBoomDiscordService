package net.survivalboom.sbds.core.scheduler;

import net.survivalboom.sbds.api.registrations.Registration;
import net.survivalboom.sbds.api.scheduler.ISchedulerTask;
import net.survivalboom.sbds.api.utils.CommonUtils;
import net.survivalboom.sbds.api.utils.ThrowingConsumer;
import net.survivalboom.sbds.core.modules.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.util.Objects;
import java.util.function.Consumer;

public class SchedulerTask implements ISchedulerTask {

    private final Scheduler scheduler;

    private final Logger logger;

    protected Registration<ISchedulerTask> registration;


    private final ThrowingConsumer<ISchedulerTask> consumer;

    private final int delay;

    private final int period;



    private boolean run = true;

    private boolean stopped = false;

    private Thread thread;



    public SchedulerTask(
            @NotNull ThrowingConsumer<ISchedulerTask> consumer,
            int delay,
            int period,
            @NotNull Scheduler scheduler
    ) {

        this.consumer = consumer;
        this.delay = delay;
        this.period = period;

        this.scheduler = scheduler;
        this.logger = Scheduler.logger;

    }

    public void launch() {

        if (thread != null || stopped) {
            throw new IllegalStateException("Already launched");
        }

        Objects.requireNonNull(registration);

        thread = Thread.startVirtualThread(this::run);
        thread.setName(registration.regKey().toString());

    }

    private void run() {

        // Даем задержку перед запуском задачи, проверяя отмену
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < delay) {

            // Если задача отменена, сразу выходим
            if (!run) {
                stop();
                return;
            }

            CommonUtils.sleep(100); // Проверяем флаг отмены каждую секунду

        }

        // Если период не задан (меньше или равен 0), сразу выполняем задачу один раз
        if (period <= 0) {
            execute();
            stop();
            return;
        }

        // Запускаем цикл, пока задача не отменена
        while (run) {

            execute(); // Выполняем задачу

            if (!run) break; // Проверяем, была ли отменена задача, и если да, то выходим из цикла

            // Для немедленной отмены нужно использовать проверку флага внутри sleep
            startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < period) {

                if (!run) break; // Если задача отменена, прерываем цикл

                // Прерываем сон, если задача отменена
                CommonUtils.sleep(100); // Проверяем флаг каждую секунду

            }

        }

        // Завершаем выполнение задачи
        stop();

    }

    private void execute() {

        try {
            consumer.accept(this);
        }

        catch (Throwable t) {
            logger.error("An exception was thrown in task `{}`.", registration.key(), t);
            CommonUtils.sleep(1000);
        }

    }

    private void stop() {
        run = false;
        stopped = true;
        thread = null;
        scheduler.registry.unregister(this);
    }

    @Override
    public void cancel() {
        if (stopped) return;
        run = false;
    }

    @Override
    public void kill() {

        if (stopped) {
            return;
        }

        thread.interrupt();

        stop();

    }

    @Override
    public boolean cancelAndWaitOrKill(int timeout, boolean log) {

        if (stopped) {
            return false;
        }

        cancel();

        try {
            waitForCancel(timeout);
        }

        catch (RuntimeException e) {

            if (log) {
                logger.warn("Task `{}` was killed due to timeout of {} milliseconds.", registration.key(), timeout);
                CommonUtils.logThreadStackTrace(logger, Level.WARN, thread);
            }

            kill();

            return true;

        }

        return false;

    }

    @Override
    public void waitForCancel(int timeout) {

        if (stopped) {
            return;
        }

        CommonUtils.waitUntil(this::isStopped, timeout);

    }


    @Override
    public boolean isRunning() {
        return !stopped;
    }

    @Override
    public boolean isCancelled() {
        return stopped || !run;
    }

    @Override
    public boolean isStopped() {
        return stopped;
    }

    @Override
    public int getDelay() {
        return delay;
    }

    @Override
    public int getPeriod() {
        return period;
    }

    @Override
    public @NotNull Thread getThread() {
        if (thread == null) throw new IllegalStateException("Task is not running");
        return thread;
    }

    @Override
    public @NotNull Registration<ISchedulerTask> getRegistration() {
        return registration;
    }

}
