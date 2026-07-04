package net.survivalboom.sbds.api.utils.queue;

import net.survivalboom.sbds.api.modules.IModule;
import net.survivalboom.sbds.api.modules.ModuleMain;
import net.survivalboom.sbds.api.scheduler.ISchedulerTask;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class OperationQueue<obj> extends AbstractOperationQueue<obj, OperationQueue<obj>> {

    private final IModule module;

    public OperationQueue(
            @NotNull IModule module,
            @NotNull Consumer<OperationQueue<obj>> consumer,
            @NotNull String name,
            int delay
    ) {
        super(consumer, name, delay);
        this.module = module;
    }

    public OperationQueue(
            @NotNull ModuleMain module,
            @NotNull Consumer<OperationQueue<obj>> consumer,
            @NotNull String name,
            int delay
    ) {
        this(module.getModule(), consumer, name, delay);
    }

    @Override
    protected ISchedulerTask schedule0() {
        return module.getManager().getSbds().getScheduler().schedule(module, name, this::task, 1000, 500);
    }

}
