package net.survivalboom.sbds.core.modules;

import net.survivalboom.sbds.api.libraries.ILibrary;
import net.survivalboom.sbds.api.modules.IModule;
import net.survivalboom.sbds.api.modules.dependencies.ModuleDependency;
import net.survivalboom.sbds.api.utils.valid.Manager;
import net.survivalboom.sbds.core.libraries.LibrariesManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class ModulesClasspath extends Manager {

    private final ModuleManager moduleManager;

    private final LibrariesManager librariesManager;

    private final Map<String, Class<?>> recentClasses = new WeakHashMap<>();


    public ModulesClasspath(@NotNull ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
        this.librariesManager = moduleManager.getSbds().getLibrariesManager();;
    }

    //
    // MANAGER
    //

    @Override
    protected void init0() {

    }

    @Override
    protected void shutdown0() {
        purgeCache();
    }

    public void purgeCache() {
        recentClasses.clear();
    }

    //
    // REQUEST
    //

    public @Nullable Class<?> request(@NotNull String name, @NotNull Module module) {

        checkValid();
        moduleManager.checkModuleValid(module);

        // Шукаємо у кешу //

        if (recentClasses.containsKey(name)) {
            return recentClasses.get(name);
        }

        // Намагаємось виконати пошук класу по бібліотекам модуля //

        List<ILibrary> libraries = module.getLibraries();

        Class<?> clazz = null;
        for (ILibrary library : libraries) {
            clazz = librariesManager.requestClass(library, name, true);
        }

        if (clazz != null) {
            return clazz;
        }

        // Шукаємо запитуваний клас у всіх модулях, що приписані у залежностях. //
        // Отримуємо список усіх завантажених модулів та відсіюємо ті які не прописані у залежностях модуля що запитує клас.

        List<IModule> modules = moduleManager.getModules();
        List<ModuleDependency> dependencies = module.getMeta().getDependencies();

        modules.removeIf(m -> dependencies.stream().noneMatch(dependency -> dependency.id().equals(module.getName()) && dependency.joinClasspath()));

        for (IModule depModule : modules) {
            Module m = (Module) depModule;
            clazz = m.getClassLoader().getClass(name, false, false);
        }

        recentClasses.put(name, clazz); // Не забуваємо додати у кеш результат. Навіть якщо null.

        return clazz;

    }


}
