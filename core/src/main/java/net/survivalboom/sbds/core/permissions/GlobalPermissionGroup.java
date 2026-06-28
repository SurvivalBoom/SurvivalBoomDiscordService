package net.survivalboom.sbds.core.permissions;

import net.survivalboom.sbds.api.modules.IModule;
import net.survivalboom.sbds.api.permissions.IGlobalGroupPermissionsPool;
import net.survivalboom.sbds.api.permissions.IGlobalPermissionGroup;
import net.survivalboom.sbds.api.permissions.IPermissionManager;
import net.survivalboom.sbds.api.permissions.Permission;
import net.survivalboom.sbds.api.registrations.Registration;
import net.survivalboom.sbds.api.registrations.RegistrationManager;
import net.survivalboom.sbds.api.utils.NamespacedKey;
import net.survivalboom.sbds.core.registration.InternalRegistrationManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class GlobalPermissionGroup extends AbstractPermissionHolder implements IGlobalPermissionGroup, RegistrationManager.Callback<IGlobalGroupPermissionsPool> {

    private final PermissionManager manager;

    private final InternalRegistrationManager<IGlobalGroupPermissionsPool> registry;

    protected Registration<IGlobalPermissionGroup> registration;


    public GlobalPermissionGroup(@NotNull String name, @NotNull PermissionManager manager) {
        super(manager);

        this.manager = manager;
        this.registry = new InternalRegistrationManager<>(this, name, this, manager.getSbds().getRegistrationRegistry());

        registry.init();

    }

    @Override
    public @NotNull IPermissionManager getManager() {
        return manager;
    }

    @Override
    public @NotNull Registration<IGlobalPermissionGroup> getRegistration() {
        return registration;
    }

    @Override
    public @NotNull String getName() {
        return registration.key().key();
    }

    //
    // POOLS
    //

    // CREATE //

    @Override
    public @NotNull IGlobalGroupPermissionsPool createPool(@NotNull IModule module, @NotNull String name) {
        Objects.requireNonNull(module, "module == null");
        return createPool0(module, name);
    }

    public @NotNull GlobalGroupPermissionPool createPool0(@Nullable IModule module, @NotNull String name) {

        Objects.requireNonNull(name, "name == null");
        checkValid();

        GlobalGroupPermissionPool pool = new GlobalGroupPermissionPool(this);
        pool.registration = registry.register0(module, name, pool);

        return pool;

    }

    // REMOVE //

    @Override
    public void removePool(@NotNull IGlobalGroupPermissionsPool pool) {

        checkValid();

        if (registry.unregister(pool) != null) {
            throw new IllegalArgumentException("Invalid pool `" + pool + "`");
        }

    }

    @Override
    public void unRegister(@NotNull Registration<IGlobalGroupPermissionsPool> registration) {
        recalculateFullCache();
    }

    // GET //

    @Override
    public @Nullable IGlobalGroupPermissionsPool getPool(@NotNull NamespacedKey key) {
        checkValid();
        return registry.getRegistrationAsObject(key);
    }

    @Override
    public @NotNull List<IGlobalGroupPermissionsPool> getPools() {
        checkValid();
        return new ArrayList<>(registry.getRegisteredObjects());
    }

    //
    // MISC
    //

    private void recalculateFullCache() {

        Map<String, Permission> out = getPools().stream()
                .flatMap(pool -> pool.getPermissions().entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        this.permissionMap.clear();
        this.permissionMap.putAll(out);

    }

}
