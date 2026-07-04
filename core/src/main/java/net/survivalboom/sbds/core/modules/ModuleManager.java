package net.survivalboom.sbds.core.modules;

import net.survivalboom.sbds.api.libraries.ILibrary;
import net.survivalboom.sbds.api.libraries.LibraryDeclaration;
import net.survivalboom.sbds.api.libraries.LibrarySatisfyConfiguration;
import net.survivalboom.sbds.api.modules.*;
import net.survivalboom.sbds.api.modules.dependencies.LoadOrder;
import net.survivalboom.sbds.api.modules.dependencies.ModuleDependency;
import net.survivalboom.sbds.core.SBDS;
import net.survivalboom.sbds.api.utils.valid.Manager;
import net.survivalboom.sbds.core.libraries.DynamicClassLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class ModuleManager extends Manager implements IModuleManager {

    private static final Logger log = LoggerFactory.getLogger("ModuleManager");

    private final SBDS sbds;

    private final File modulesDir;

    private final Map<String, Module> modules = new HashMap<>();

    private final ModulesClasspath modulesClasspath;


    public ModuleManager(@NotNull SBDS sbds) {
        this.sbds = sbds;
        this.modulesClasspath = new ModulesClasspath(this);
        this.modulesDir = new File(sbds.getWorkingDir(), "modules");
    }

    //
    // MANAGER
    //

    @Override
    protected void init0() {

        modulesClasspath.init();

        log.info("Loading modules...");
        //noinspection ResultOfMethodCallIgnored
        modulesDir.mkdirs();

        //noinspection DataFlowIssue
        List<File> modulesFiles = Arrays.stream(modulesDir.listFiles())
                .filter(file -> file.getName().endsWith(".jar"))
                .toList();

        if (modulesFiles.isEmpty()) {
            log.info("No modules found! Skipping...");
            return;
        }

        Map<String, ModuleMetaLoadResult> modulesMetas = new HashMap<>();
        for (File file : modulesFiles) {

            ModuleMetaLoadResult result;
            try {
                result = loadModuleMeta(file);
            }

            catch (ModuleMeta.InvalidMetaException e) {
                log.error("Invalid module file `{}`. Skipping...", file.getName(), e);
                continue;
            }

            if (modulesMetas.containsKey(result.meta().getId())) {
                log.warn("Refusing to load module file `{}`. Module with id `{}` already exist.", file.getName(), result.meta().getId());
                continue;
            }

            modulesMetas.put(result.meta().getId(), result);

        }

        SortingResult sortingResult = sortModulesByDependencies(modulesMetas.values());
        if (!sortingResult.skipped().isEmpty()) {
            log.error("The following modules were skipped due to circular dependencies or loop propagation: {}",
                    String.join(", ", sortingResult.skipped().stream()
                            .map(meta -> meta.meta().getId())
                            .toList())
            );
        }

        List<ModuleMetaLoadResult> sortedMetas = sortingResult.sorted();

        if (!sortedMetas.isEmpty()) {

            log.info("Found {} valid modules. Loading them... \n - {}",
                    sortedMetas.size(),
                    String.join(", ", sortedMetas.stream()
                            .map(meta -> meta.meta().getName())
                            .toList())
            );

        }

        List<IModule> successfullyLoaded = new ArrayList<>();
        for (ModuleMetaLoadResult meta : sortedMetas) {

            String name = meta.meta().getName();
            log.info("Loading module {}...", name);

            try {
                IModule module = createModule(meta.meta(), meta.file(), null);
                successfullyLoaded.add(module);
            }

            catch (ModuleUnsatisfiedLibraryException e) {
                log.error("Failed to download library `{}`. Refusing to load.", e.getLibrary(), e.getCause());
            }

            catch (ModuleUnsatisfiedDependencyException e) {
                log.error("Module `{}` requires `{}` as a dependency. No module with that id was found. Refusing to load.", name, e.getDependency().id());
            }

            catch (ModuleLoadingException e) {
                log.error("Module `{}` failed to load!", name, e);
            }

            catch (ModuleRefusedException e) {
                log.error("Module `{}` refused to load! Maybe it's mad on you?", name, e);
            }

        }

        for (IModule module : successfullyLoaded) {

            try {
                enableModule(module);
            }

            catch (ModuleStateCallbackException e) {
                log.error("An exception was thrown when attempted to enable module `{}`.", module, e);
            }

            catch (ModuleRefusedException e) {
                log.error("Module `{}` refused to start! Maybe it's mad on you?", module.getName(), e);
            }

        }

    }

    @Override
    protected void shutdown0() {

        if (!modules.isEmpty()) {
            log.info("Disabling modules...");
        }

        List<IModule> currentModules = getModules();
        List<IModule> sortedModules = sortActiveModulesByDependencies(currentModules);

        for (int i = sortedModules.size() - 1; i >= 0; i--) {

            IModule module = sortedModules.get(i);

            if (module.isEnabled()) {

                try {
                    disableModule(module);
                }

                catch (ModuleStateCallbackException e) {
                    log.error("An exception was thrown in {}.onDisable().", module.getName(), e);
                }

            }

            try {
                unloadModule(module);
            }

            catch (ModuleDependantException e) {
                log.error("Catastrophic failure!", e);
            }

            catch (ModuleStateCallbackException e) {
                log.error("An exception was thrown in {}.onUnload().", module.getName(), e);
            }

        }

        modulesClasspath.shutdown();

    }

    //
    // MODULES
    //

    // LOAD/UNLOAD //

    @Override
    public synchronized @NotNull Module loadModule(@NotNull File file) throws ModuleLoadingException, ModuleRefusedException, ModuleUnsatisfiedDependencyException, ModuleUnsatisfiedLibraryException {

        // Завантажуємо ModuleMeta із .jar файлу //

        ModuleMetaLoadResult result;
        try {
            result = loadModuleMeta(file);
        }

        catch (ModuleMeta.InvalidMetaException e) {
            throw new ModuleLoadingException(e);
        }

        // Створюємо модуль //

        return createModule(result.meta(), result.file(), null);

    }

    @SuppressWarnings("unchecked")
    public synchronized @NotNull Module createModule(@NotNull ModuleMeta meta, @Nullable ModuleFile file, @Nullable File dataDir) throws ModuleLoadingException, ModuleRefusedException, ModuleUnsatisfiedDependencyException, ModuleUnsatisfiedLibraryException {

        Objects.requireNonNull(meta, "meta == null");
        checkValid();

        // Перевіряємо чи модуль з такою назвою вже існує //
        if (modules.containsKey(meta.getId())) {
            throw new ModuleLoadingException("Module with name `" + meta.getId() + "` already loaded");
        }

        // Перевіряємо на правильність теку із даними модуля //

        if (dataDir == null) {
            dataDir = new File(modulesDir, meta.getName());
        }

        if (dataDir.isFile()) {
            throw new ModuleLoadingException("Module data directory `" + dataDir.getPath() + "` is not a directory");
        }

        // Перевіряємо залежності модуля //

        for (ModuleDependency dependency : meta.getDependencies()) {

            String dpId = dependency.id();
            boolean required = dependency.required();

            if (required && !modules.containsKey(dpId)) {
                throw new ModuleUnsatisfiedDependencyException(dependency, "Dependency `" + dpId + "` is not present! Load the dependency first.");
            }

        }

        // Завантажуємо бібліотеки модуля //

        LibrarySatisfyConfiguration libraries = meta.getLibraries();
        List<ILibrary> loadedLibraries = null;
        if (libraries != null && !libraries.isEmpty()) {

            var result = sbds.getLibrariesManager().satisfy(libraries);

            for (var entry : result.failed().entrySet()) {

                LibraryDeclaration library = entry.getKey();
                Exception cause = entry.getValue();

                throw new ModuleUnsatisfiedLibraryException(library, "Failed to download module library `" + library + "`", cause);
            }

            loadedLibraries = new ArrayList<>();
            loadedLibraries.addAll(result.downloaded());
            loadedLibraries.addAll(result.skipped());

        }

        // Створюємо модуль //

        Module module = new Module(meta, file, dataDir, loadedLibraries, this);

        DynamicClassLoader classLoader = module.getClassLoader();
        classLoader.addClassSupplier("MODULES-CLASSPATH", cl -> modulesClasspath.request(cl, module));

        if (file != null) {
            classLoader.addSource(file.file());
        }

        modules.put(meta.getId(), module);
        modulesClasspath.purgeCache();

        // Шукаємо головний клас модуля.
        String mainClassName = meta.getMain();

        Class<? extends ModuleMain> clazz;
        try {
            clazz = (Class<? extends ModuleMain>) module.getClassLoader().getClass(mainClassName, false, false);
        }

        catch (Throwable t) {
            modules.remove(meta.getId());
            throw new ModuleLoadingException("Failed to load module main class `" + mainClassName + "`. A fatal error occurred.", t);
        }

        if (clazz == null) {
            modules.remove(meta.getId());
            throw new ModuleLoadingException("Module main class `" + mainClassName + "` not found in module ClassLoader");
        }

        // Шукаємо конструктор за яким ми зможемо створити об'єкт.
        Constructor<? extends ModuleMain> constructor;
        try {
            constructor = clazz.getDeclaredConstructor();
        }

        catch (NoSuchMethodException e) {
            modules.remove(meta.getId());
            throw new ModuleLoadingException("No public no-args constructor found in class `" + mainClassName + "`");
        }

        // Створюємо об'єкт головного класа модуля.
        ModuleMain main;
        try {
            main = constructor.newInstance();
        }

        catch (Throwable e) {
            modules.remove(meta.getId());
            throw new ModuleLoadingException("Failed to create instance of module main class", e);
        }

        module.moduleMain = main;
        main.init(module, sbds);

        // Після створення об'єкта головного класу модуля, виконуємо onLoad()
        try {
            main.onLoad();
        }

        catch (ModuleRefusedException e) {
            modules.remove(meta.getId());
            throw e;
        }

        catch (Throwable e) {
            modules.remove(meta.getId());
            throw new ModuleLoadingException("An exception occurred in onLoad()", e);
        }

        return module;

    }

    @Override
    public @NotNull ModuleMetaLoadResult loadModuleMeta(@NotNull File file) throws ModuleMeta.InvalidMetaException {

        checkValid();

        // Перевіряємо файл на правильність //

        Objects.requireNonNull(file, "file == null");

        if (!file.exists()) {
            throw new IllegalArgumentException("File `" + file.getPath() + "` does not exist");
        }

        if (!file.isFile()) {
            throw new IllegalArgumentException("File `" + file.getPath() + "` is not a file");
        }

        if (!file.getName().endsWith(".jar")) {
            throw new IllegalArgumentException("File `" + file.getPath() + "` is not a jar file");
        }

        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("Invalid file");
        }

        // Намагаємось відкрити jar файл та прочитати його вміст //

        JarFile jarFile;
        try {
            jarFile = new JarFile(file);
        }

        catch (IOException e) {
            throw new ModuleMeta.InvalidMetaException(e);
        }

        ZipEntry entry = jarFile.getEntry("module.yml");
        if (entry == null) {
            throw new ModuleMeta.InvalidMetaException("Module meta file `module.yml` not found in target jar file `" + file.getPath() + "`");
        }

        ModuleMeta meta;
        try (InputStream stream = jarFile.getInputStream(entry)) {
            meta = ModuleMeta.fromStream(stream);
        }

        catch (IOException e) {
            throw new ModuleMeta.InvalidMetaException("Failed to load module meta from `module.yml`", e);
        }

        return new ModuleMetaLoadResult(meta, new ModuleFile(file, jarFile));

    }

    @Override
    public synchronized void unloadModule(@NotNull IModule module) throws ModuleStateCallbackException, ModuleDependantException {

        checkModuleValid(module);

        if (module.isEnabled()) {
            throw new IllegalStateException("Module must be disabled first in order to unload");
        }

        IModule dependent = modules.values().stream()
                .filter(m -> m.getMeta().getDependencies().stream().anyMatch(dep -> dep.id().equals(module.getName())))
                .findAny()
                .orElse(null);

        if (dependent != null) {
            throw new ModuleDependantException(dependent, "Could not unload module. Found module `" + dependent.getName() + "` depends on this module. You must unload it first in order to unload this module.");
        }

        try {
            module.getMain().onUnload();
        }

        catch (Throwable e) {
            throw new ModuleStateCallbackException(e);
        }

        finally {
            this.modules.remove(module.getMeta().getId());
            modulesClasspath.purgeCache();
        }

    }

    // ENABLE/DISABLE //

    @Override
    public synchronized void enableModule(@NotNull IModule imodule) throws ModuleStateCallbackException, ModuleRefusedException {

        checkModuleValid(imodule);

        if (imodule.isEnabled()) {
            throw new IllegalStateException("Module is not disabled");
        }

        log.info("Enabling {}...", imodule.getName());

        Module module = (Module) imodule;
        module.enabled = true;

        try {
            imodule.getMain().onEnable();
        }

        catch (ModuleRefusedException e) {
            module.enabled = false;
            throw e;
        }

        catch (Throwable e) {
            module.enabled = false;
            sbds.getRegistrationRegistry().removeModuleRegistrations(imodule);
            throw new ModuleStateCallbackException(e);
        }

    }

    @Override
    public synchronized void disableModule(@NotNull IModule imodule) throws ModuleStateCallbackException {

        checkModuleValid(imodule);

        if (!imodule.isEnabled()) {
            throw new IllegalStateException("Module is not enabled");
        }

        Module module = (Module) imodule;

        log.info("Disabling {}...", module.getName());

        try {
            imodule.getMain().onDisable();
        }

        catch (Throwable e) {
            throw new ModuleStateCallbackException(e);
        }

        finally {
            module.enabled = false;
            sbds.getRegistrationRegistry().removeModuleRegistrations(imodule);
        }

    }

    //
    // GETTERS
    //

    @Override
    public @Nullable IModule getModule(@NotNull String name) {
        return modules.get(name);
    }


    //
    // GETTERS
    //

    public @NotNull SBDS getSbds() {
        return sbds;
    }

    public @NotNull List<IModule> getModules() {
        checkValid();
        return new ArrayList<>(getModules0());
    }

    @Override
    public @NotNull File getModulesDir() {
        return modulesDir;
    }

    public @NotNull List<Module> getModules0() {
        checkValid();
        return new ArrayList<>(modules.values());
    }

    public @NotNull ModulesClasspath getModulesClassesSharingManager() {
        checkValid();
        return modulesClasspath;
    }

    //
    // INTERNAL
    //

    @Override
    public @NotNull IModule checkModuleValid(@NotNull IModule module) {

        Objects.requireNonNull(module, "module == null");
        checkValid();

        if (!(module instanceof Module m)) {
            throw new IllegalArgumentException("IModule object is instance of `" + module.getClass().getName() + "` not a net.survivalboom.sbds.core.modules.Module object! Are you trying to break SBDS?");
        }

        if (!modules.containsValue(m)) {
            throw new IllegalArgumentException("Module object is not registered in the ModuleManager. Is module unloaded?");
        }

        if (!m.isValid()) {
            throw new IllegalArgumentException("Module object is registered in ModuleManager, but Method#valid returned false. Did you break something?");
        }

        return m;

    }

    @Override
    public @NotNull IModule checkModuleEnabled(@NotNull IModule imodule, @Nullable String message) {

        Module module = (Module) checkModuleValid(imodule);
        if (!module.isEnabled()) {
            String msg = message != null ? message : "Module must be enabled";
            throw new IllegalArgumentException(msg);
        }

        return module;

    }

    //
    // MODULES SORTING
    //

    // УВАГА! Нижче починається AI SLOP! //

    private SortingResult sortModulesByDependencies(Collection<ModuleMetaLoadResult> input) {
        Map<String, ModuleMetaLoadResult> registry = new HashMap<>();
        for (ModuleMetaLoadResult res : input) {
            registry.put(res.meta().getId(), res);
        }

        List<ModuleMetaLoadResult> sorted = new ArrayList<>();
        List<ModuleMetaLoadResult> skipped = new ArrayList<>();

        // Состояния для DFS: 0 - не посещен, 1 - в процессе (серый), 2 - успешно обработан (черный), -1 - ошибка/цикл
        Map<String, Integer> states = new HashMap<>();

        // Строим граф зависимостей (какие модули должны быть ДО текущего)
        Map<String, List<String>> dependsOn = new HashMap<>();
        for (ModuleMetaLoadResult current : input) {
            String currentId = current.meta().getId();
            dependsOn.computeIfAbsent(currentId, k -> new ArrayList<>());

            for (ModuleDependency dep : current.meta().getDependencies()) {
                String depId = dep.id();
                if (!registry.containsKey(depId)) continue;

                if (dep.order() == LoadOrder.AFTER) { //
                    dependsOn.get(currentId).add(depId);
                } else if (dep.order() == LoadOrder.BEFORE) { //
                    dependsOn.computeIfAbsent(depId, k -> new ArrayList<>()).add(currentId);
                }
            }
        }

        // Запускаем DFS для каждого модуля
        for (String id : registry.keySet()) {
            int state = states.getOrDefault(id, 0);
            if (state == 0) {
                dfsSort(id, registry, dependsOn, states, sorted, skipped);
            }
        }

        return new SortingResult(sorted, skipped);
    }

    private boolean dfsSort(String id,
                            Map<String, ModuleMetaLoadResult> registry,
                            Map<String, List<String>> dependsOn,
                            Map<String, Integer> states,
                            List<ModuleMetaLoadResult> sorted,
                            List<ModuleMetaLoadResult> skipped) {

        int state = states.getOrDefault(id, 0);

        // Обнаружен цикл!
        if (state == 1) {
            log.error("Circular dependency detected involving module `{}`!", id);
            states.put(id, -1);
            skipped.add(registry.get(id));
            return false;
        }

        // Если модуль уже был обработан ранее
        if (state == 2) return true;
        if (state == -1) return false;

        // Входим в модуль (делаем «серым»)
        states.put(id, 1);

        List<String> dependencies = dependsOn.getOrDefault(id, Collections.emptyList());
        boolean hasCircularDependency = false;

        for (String depId : dependencies) {
            // Если хотя бы одна зависимость упала в цикл, текущий модуль тоже не может быть загружен
            if (!dfsSort(depId, registry, dependsOn, states, sorted, skipped)) {
                hasCircularDependency = true;
            }
        }

        // Если сам модуль или его зависимости цикличны
        if (hasCircularDependency || states.get(id) == -1) {
            if (states.get(id) != -1) { // Если его скипнули из-за родителей, а не из-за себя напрямую
                log.error("Module `{}` skipped because one of its dependencies has a circular loop.", id);
                states.put(id, -1);
                skipped.add(registry.get(id));
            }
            return false;
        }

        // Успешно обработан (делаем «черным»)
        states.put(id, 2);
        sorted.add(registry.get(id));
        return true;
    }

    private record SortingResult(
            @NotNull List<ModuleMetaLoadResult> sorted,
            @NotNull List<ModuleMetaLoadResult> skipped
    ) {}


    private List<IModule> sortActiveModulesByDependencies(List<IModule> input) {
        Map<String, IModule> registry = new HashMap<>();
        for (IModule mod : input) {
            registry.put(mod.getMeta().getId(), mod);
        }

        List<IModule> result = new ArrayList<>();
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();

        // Строим граф зависимостей (какие модули должны быть ДО текущего)
        Map<String, List<String>> dependsOn = new HashMap<>();
        for (IModule current : input) {
            String currentId = current.getMeta().getId();
            dependsOn.computeIfAbsent(currentId, k -> new ArrayList<>());

            for (ModuleDependency dep : current.getMeta().getDependencies()) {
                String depId = dep.id();

                // Если зависимого модуля уже нет в памяти, игнорируем связь
                if (!registry.containsKey(depId)) continue;

                if (dep.order() == LoadOrder.AFTER) {
                    dependsOn.get(currentId).add(depId);
                } else if (dep.order() == LoadOrder.BEFORE) {
                    dependsOn.computeIfAbsent(depId, k -> new ArrayList<>()).add(currentId);
                }
            }
        }

        // Запускаем DFS
        for (String id : registry.keySet()) {
            if (!visited.contains(id)) {
                dfsSortActive(id, registry, dependsOn, visiting, visited, result);
            }
        }

        return result;
    }

    private void dfsSortActive(String id,
                               Map<String, IModule> registry,
                               Map<String, List<String>> dependsOn,
                               Set<String> visiting,
                               Set<String> visited,
                               List<IModule> result) {

        if (visiting.contains(id)) {
            log.error("Circular dependency detected during shutdown for module `{}`! Order might be broken.", id);
            return;
        }

        if (!visited.contains(id)) {
            visiting.add(id);

            List<String> dependencies = dependsOn.getOrDefault(id, Collections.emptyList());
            for (String depId : dependencies) {
                dfsSortActive(depId, registry, dependsOn, visiting, visited, result);
            }

            visiting.remove(id);
            visited.add(id);
            result.add(registry.get(id));
        }
    }

}
