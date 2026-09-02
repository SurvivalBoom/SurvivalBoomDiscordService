package net.survivalboom.sbds.core.registration;

import net.survivalboom.sbds.api.modules.IModule;
import net.survivalboom.sbds.api.registrations.IRegistrationRegistry;
import net.survivalboom.sbds.api.utils.valid.Manager;
import net.survivalboom.sbds.api.utils.NamespacedKey;
import net.survivalboom.sbds.core.SBDS;
import net.survivalboom.sbds.api.registrations.Registration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class RegistrationRegistry extends Manager implements IRegistrationRegistry {

    private static final Logger log = LoggerFactory.getLogger(RegistrationRegistry.class);

    private final Map<NamespacedKey, Registration<?>> registrationMap = new ConcurrentHashMap<>();

    private final SBDS sbds;


    public RegistrationRegistry(@NotNull SBDS sbds) {
        this.sbds = sbds;
    }


    @Override
    protected void init0() {

    }

    @Override
    protected void shutdown0() {
        getRegistrations().forEach(this::removeRegistration);
    }

    //
    // REGISTRATION
    //

    @Override
    public <T> @NotNull Registration<T> register(@NotNull IModule module, @NotNull T object, @NotNull Consumer<Registration<T>> unregisterAction, @NotNull String... names) {
        Objects.requireNonNull(module, "module == null");
        return register0(module, object, unregisterAction, names);
    }

    @Override
    public <T> @NotNull Registration<T> register(@NotNull IModule module, @NotNull T object, @NotNull Consumer<Registration<T>> unregisterAction, @NotNull Collection<String> names) {
        Objects.requireNonNull(module, "module == null");
        return register0(module, object, unregisterAction, names);
    }


    public <T> @NotNull Registration<T> register0(@Nullable IModule module, @NotNull T object, @NotNull Consumer<Registration<T>> unregisterAction, @NotNull String... names) {
        return register0(module, object, unregisterAction, List.of(names));
    }

    public <T> @NotNull Registration<T> register0(@Nullable IModule module, @NotNull T object, @NotNull Consumer<Registration<T>> unregisterAction, @NotNull Collection<String> names) {

        Objects.requireNonNull(names, "names == null");
        Objects.requireNonNull(object, "object == null");
        checkValid();

        List<String> nameList;
        if (names instanceof List<String> nms) {
            nameList = nms;
        }

        else {
            nameList = new ArrayList<>(names);
        }

        if (names.isEmpty()) {
            throw new IllegalArgumentException("No name provided");
        }

        String name = nameList.getLast();
        String regName = String.join(".", names);

        NamespacedKey key = createKey(module, name);
        NamespacedKey regKey = createKey(module, regName);

        if (module != null) {
            sbds.getModuleManager().checkModuleEnabled(module, "Disabled module `" + module.getName() + "` tried to register `" + regName + "`");
        }

        if (registrationMap.containsKey(regKey)) {
            throw new IllegalStateException("Registration with name `" + regName + "` already exists");
        }

        Registration<T> reg = new Registration<>(name, key, module, regKey, object, unregisterAction);
        registrationMap.put(regKey, reg);

        return reg;

    }

    private @NotNull NamespacedKey createKey(@Nullable IModule module, @NotNull String name) {

        if (module == null) {
            return NamespacedKey.sbds(name);
        }

        return NamespacedKey.fromModule(module, name);

    }

    //
    // UNREGISTRATION
    //

    public <T> boolean removeRegistration(@NotNull Registration<T> registration) {

        Objects.requireNonNull(registration, "registration == null");
        checkValid();

        NamespacedKey regKey = registration.regKey();

        if (!registrationMap.containsKey(regKey)) {
            return false;
        }

        try {
            registration.unregisterAction().accept(registration);
        }

        catch (Exception e) {
            log.error("Failed to execute an unregister action for `{}`. This may cause errors.", regKey, e);
        }

        registrationMap.remove(regKey);

        return true;

    }

    public @NotNull List<Registration<?>> removeModuleRegistrations(@NotNull IModule module) {

        checkValid();

        var regs = getModuleRegistrations(module);

        regs.forEach(this::removeRegistration);

        return regs;

    }

    public <T> boolean unregister(@NotNull Registration<T> reg) {
        checkValid();
        return registrationMap.remove(reg.regKey()) != null;
    }

    //
    // GETTERS
    //

    public @Nullable Registration<?> getRegistration(@NotNull NamespacedKey key) {
        checkValid();
        return registrationMap.get(key);
    }

    @Override
    public @NotNull List<Registration<?>> getRegistrations() {
        return new ArrayList<>(registrationMap.values());
    }

}
