package net.survivalboom.sbds.api.service;

import net.survivalboom.sbds.api.modules.IModule;
import net.survivalboom.sbds.api.modules.ModuleMain;
import net.survivalboom.sbds.api.registrations.Registration;
import net.survivalboom.sbds.api.utils.NamespacedKey;
import net.survivalboom.sbds.api.utils.valid.IManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface IServiceProvider extends IManager {

    // REG //

    <T> @NotNull IRegisteredService<T> registerService(@NotNull IModule module, @NotNull Class<T> clazz, @NotNull T service);

    default <T> @NotNull IRegisteredService<T> registerService(@NotNull ModuleMain main, @NotNull Class<T> clazz, @NotNull T service) {
        return registerService(main.getModule(), clazz, service);
    }

    // UNREG //

    boolean unregisterService(@NotNull IRegisteredService<?> reg);

    // GET //

    @Nullable <T> T getService(@NotNull Class<T> clazz);

    <T> @Nullable IRegisteredService<T> getRegisteredService(@NotNull Class<T> clazz);

    @Nullable IRegisteredService<?> getRegisteredService(@NotNull NamespacedKey key);

    default @Nullable IRegisteredService<?> getRegisteredService(@NotNull String key) {
        return getRegisteredService(NamespacedKey.fromString(key));
    }

    @NotNull List<IRegisteredService<?>> getRegistry();

    // RECORD //

    interface IRegisteredService<T> {

        @NotNull IServiceProvider getManager();

        @NotNull Registration<IRegisteredService<T>> getRegistration();

        @NotNull Class<T> getClazz();

        @NotNull T getObject();

    }

}
