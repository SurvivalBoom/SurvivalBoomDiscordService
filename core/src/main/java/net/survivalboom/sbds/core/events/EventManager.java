package net.survivalboom.sbds.core.events;

import net.dv8tion.jda.api.events.GenericEvent;
import net.survivalboom.sbds.api.events.*;
import net.survivalboom.sbds.api.events.EventListener;
import net.survivalboom.sbds.api.modules.IModule;
import net.survivalboom.sbds.api.registrations.Registration;
import net.survivalboom.sbds.api.utils.CommonUtils;
import net.survivalboom.sbds.api.utils.ThrowingConsumer;
import net.survivalboom.sbds.core.SBDS;
import net.survivalboom.sbds.api.utils.valid.Manager;
import net.survivalboom.sbds.core.registration.InternalRegistrationManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

public class EventManager extends Manager implements net.dv8tion.jda.api.hooks.EventListener, IEventManager {

    private static final Logger logger = LoggerFactory.getLogger(EventManager.class.getSimpleName());


    private final SBDS sbds;

    private final InternalRegistrationManager<IRegisteredEventHandler> registry;


    public EventManager(@NotNull SBDS sbds) {
        this.sbds = sbds;
        this.registry = new InternalRegistrationManager<>(this, null, sbds.getRegistrationRegistry());
    }

    //
    // MANAGER
    //

    @Override
    protected void init0() {
        registry.init();
        sbds.getBot().addEventListener(this);
    }

    @Override
    protected void shutdown0() {
        sbds.getBot().removeEventListener(this);
        registry.shutdown();
    }

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        onEvent((Object) event);
    }

    private void onEvent(@NotNull Object event) {

        if (!sbds.isReady()) {
            return;
        }

        // TODO Оптимізувати із кешуванням. Поки що нехай буде так.
        List<IRegisteredEventHandler> eventHandlers = registry.getRegisteredObjects().stream()
                .filter(handler -> handler.getEventClass().isAssignableFrom(event.getClass()))
                .sorted(Comparator.comparing(IRegisteredEventHandler::getPriority).reversed())
                .toList();

        boolean cancelled = false;
        for (var handler : eventHandlers) {

            if (handler.isIgnoringCancelled() && cancelled) {
                continue;
            }

            try {
                rawrrr(handler.getEventHandler(), event);
            }

            catch (Throwable t) {
                logger.error("Could not pass &e{} &rto &e{}&r. An exception was thrown.", event.getClass().getSimpleName(), Objects.requireNonNullElse(handler.getRegistration().module(), "SBDS"), t);
            }

            if (event instanceof ICancellable cancellable) {
                cancelled = cancellable.isCancelled();
            }

        }

    }

    @SuppressWarnings("unchecked") // <- Сасі хуй і нє псіхуй.
    private <T> void rawrrr(@NotNull ThrowingConsumer<?> consumer, @NotNull T you) throws Throwable {
        var dragonGayPorn = (ThrowingConsumer<T>) consumer;
        dragonGayPorn.accept(you);
    }

    @Override
    public <T extends EventBase> @NotNull T callEvent(@NotNull T event) {

        Objects.requireNonNull(event, "event == null");
        checkValid();

        IModule module = event.getModule();
        Objects.requireNonNull(module, "module == null");

        sbds.getModuleManager().checkModuleEnabled(module, "Disabled module tried to call an event");

        onEvent(event);

        return event;

    }

    public <T extends EventBase> @NotNull T callEvent0(@NotNull T event) {

        Objects.requireNonNull(event, "event == null");
        checkValid();

        onEvent(event);

        return event;

    }

    //
    // EVENT HANDLERS
    //

    // REGISTER //

    @Override
    public @NotNull <T> IRegisteredEventHandler registerEvent(
            @NotNull IModule module,
            @NotNull Class<T> clazz,
            @NotNull ThrowingConsumer<T> consumer,
            @NotNull EventPriority priority,
            boolean ignoreCancelled
    ) {
        Objects.requireNonNull(module, "module == null");
        return registerEvent0(module, clazz, consumer, priority, ignoreCancelled);
    }

    public @NotNull <T> IRegisteredEventHandler registerEvent0(
            @Nullable IModule module,
            @NotNull Class<T> clazz,
            @NotNull ThrowingConsumer<T> consumer,
            @NotNull EventPriority priority,
            boolean ignoreCancelled
    ) {

        Objects.requireNonNull(clazz, "clazz == null");
        Objects.requireNonNull(consumer, "consumer == null");
        checkValid();

        if (module != null) {
            sbds.getModuleManager().checkModuleEnabled(module, "Disabled module tried to register an event handler");
        }

        String name = UUID.randomUUID().toString();

        RegisteredEventHandler registeredEventHandler = new RegisteredEventHandler(clazz, consumer, null, priority, ignoreCancelled, this);
        registeredEventHandler.registration = registry.register0(module, name, registeredEventHandler);

        return registeredEventHandler;

    }


    @Override
    public @NotNull List<IRegisteredEventHandler> registerEvents(@NotNull IModule module, @NotNull EventListener listener) {
        Objects.requireNonNull(module, "module == null");
        return registerEvents0(module, listener);
    }

    @SuppressWarnings({"rawtypes"}) // <-- Дінаху сука бля.
    public @NotNull List<IRegisteredEventHandler> registerEvents0(@Nullable IModule module, @NotNull EventListener listener) {

        if (module != null) {
            sbds.getModuleManager().checkModuleEnabled(module, "Disabled module tried to register an event listener");
        }

        Class<? extends EventListener> clazz = listener.getClass();

        boolean alreadyRegistered = getEventHandlers().stream()
                .anyMatch(handler -> handler.getOriginClass() != null && handler.getOriginClass().equals(clazz));

        if (alreadyRegistered) {
            throw new IllegalStateException("Clazz `" + clazz.getName() + "` already registered");
        }

        Method[] methods = clazz.getMethods();

        List<IRegisteredEventHandler> result = new ArrayList<>();
        for (Method method : methods) {

            EventHandler info = method.getAnnotation(EventHandler.class);
            if (info == null) {
                continue;
            }

            Parameter[] parameters = method.getParameters();
            if (parameters.length != 1) {
                continue;
            }

            Parameter parameter = parameters[0];
            Class eventClass = parameter.getType();

            ThrowingConsumer<?> consumer = obj -> {

                try {
                    method.invoke(listener, obj);
                }

                // Нам потрібно щоб в консоль висиралась саме причина цієї помилки, а не сама InvocationTargetException.
                catch (InvocationTargetException e) {
                    throw e.getCause();
                }

            };

            String name = String.valueOf(CommonUtils.RANDOM.nextInt(999999));

            RegisteredEventHandler registeredEventHandler = new RegisteredEventHandler(eventClass, consumer, clazz, info.priority(), info.ignoreCancelled(), this);
            registeredEventHandler.registration = registry.register0(module, name, registeredEventHandler);

            result.add(registeredEventHandler);

        }

        return result;

    }

    // UNREGISTER //

    @Override
    public boolean unregisterEvent(@NotNull IRegisteredEventHandler reg) {
        return registry.unregister(reg) != null;
    }

    @Override
    public @NotNull List<IRegisteredEventHandler> unregisterEvents(@NotNull EventListener listener) {

        Objects.requireNonNull(listener, "listener == null");
        checkValid();

        Class<? extends EventListener> clazz = listener.getClass();

        List<IRegisteredEventHandler> toUnregister = getEventHandlers().stream()
                .filter(reg -> reg.getOriginClass() != null && reg.getOriginClass().equals(clazz))
                .toList();

        for (IRegisteredEventHandler handler : toUnregister) {
            unregisterEvent(handler);
        }

        return toUnregister;

    }

    // GET //

    @Override
    public @NotNull List<IRegisteredEventHandler> getEventHandlers() {
        return registry.getRegisteredObjects();
    }

    //
    // GAY, GAY, GAY, GAY!
    //

    public static class RegisteredEventHandler implements IRegisteredEventHandler {

        private final EventManager manager;

        private Registration<IRegisteredEventHandler> registration;


        private final Class<?> eventClass;

        private final ThrowingConsumer<?> eventHandler;


        private final @Nullable Class<? extends EventListener> originClass;


        private final EventPriority priority;

        private final boolean ignoringCancelled;


        public RegisteredEventHandler(
                @NotNull Class<?> eventClass,
                @NotNull ThrowingConsumer<?> eventHandler,
                @Nullable Class<? extends EventListener> originClass,
                @NotNull EventPriority priority,
                boolean ignoringCancelled,
                @NotNull EventManager manager
        ) {

            this.eventClass = eventClass;
            this.eventHandler = eventHandler;

            this.originClass = originClass;

            this.priority = priority;
            this.ignoringCancelled = ignoringCancelled;
            this.manager = manager;

        }


        @Override
        public @NotNull IEventManager getManager() {
            return manager;
        }

        @Override
        public @NotNull Class<?> getEventClass() {
            return eventClass;
        }

        @Override
        public @NotNull ThrowingConsumer<?> getEventHandler() {
            return eventHandler;
        }

        @Override
        public @Nullable Class<? extends EventListener> getOriginClass() {
            return originClass;
        }

        @Override
        public @NotNull EventPriority getPriority() {
            return priority;
        }

        @Override
        public boolean isIgnoringCancelled() {
            return ignoringCancelled;
        }

        @Override
        public @NotNull Registration<IRegisteredEventHandler> getRegistration() {
            return registration;
        }

    }



}
