package net.survivalboom.sbds.api.utils.queue;

import net.survivalboom.sbds.api.modules.IModule;
import net.survivalboom.sbds.api.modules.ModuleMain;
import net.survivalboom.sbds.api.scheduler.ISchedulerTask;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;

public class PushQueue<obj> extends AbstractPushQueue<obj, PushQueue<obj>> {

    private final IModule module;

    public PushQueue(
            @NotNull IModule module,
            @NotNull Consumer<PushQueue<obj>> consumer,
            @NotNull String name,
            int delay
    ) {
        super(consumer, name, delay);
        Objects.requireNonNull(module, "module == null");
        this.module = module;
    }

    public PushQueue(
            @NotNull ModuleMain module,
            @NotNull Consumer<PushQueue<obj>> consumer,
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
