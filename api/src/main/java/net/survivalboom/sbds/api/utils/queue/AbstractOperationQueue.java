package net.survivalboom.sbds.api.utils.queue;

import net.survivalboom.sbds.api.scheduler.ISchedulerTask;
import net.survivalboom.sbds.api.utils.CommonUtils;
import net.survivalboom.sbds.api.utils.valid.Manager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;

public abstract class AbstractOperationQueue<obj, it extends AbstractOperationQueue<obj, it>> extends Manager {

    protected final String name;

    protected final Consumer<it> consumer;

    protected final Logger log;


    protected final int delay;

    protected ISchedulerTask task;

    protected final Map<String, Set<obj>> queue = new HashMap<>();

    protected long lastAppend = 0;



    public AbstractOperationQueue(
            @NotNull Consumer<it> consumer,
            @NotNull String name,
            int delay
    ) {

        Objects.requireNonNull(consumer, "consumer == null");
        Objects.requireNonNull(name, "name == null");

        this.consumer = consumer;
        this.name = name;
        this.delay = delay;

        this.log = LoggerFactory.getLogger(name + "-PushQueue");

    }

    //
    // MANAGER
    //

    @Override
    protected void init0() {
        task = schedule0();
    }

    @Override
    protected void shutdown0() {
        task.tryCancel();
        task = null;
    }

    protected abstract ISchedulerTask schedule0();

    @SuppressWarnings("unchecked")
    protected void task() {

        if (queue.isEmpty()) {
            return;
        }

        long time = System.currentTimeMillis();
        if (lastAppend + delay > time) {
            return;
        }

        try {
            consumer.accept((it) this);
        }

        catch (Throwable t) {
            log.error("Failed to execute an update on {} objects in the queue!", queue.size(), t);
        }

        this.queue.clear();

    }

    //
    // QUEUE
    //

    public void append(@NotNull String operation, @NotNull obj obj) {

        Objects.requireNonNull(obj, "obj == null");
        Objects.requireNonNull(operation, "operation == null");
        checkValid();

        this.queue.computeIfAbsent(operation, k -> new HashSet<>()).add(obj);
        this.lastAppend = System.currentTimeMillis();

    }

    public @NotNull Map<String, Set<obj>> getQueue() {
        checkValid();
        return CommonUtils.deepCopy(this.queue);
    }

}
