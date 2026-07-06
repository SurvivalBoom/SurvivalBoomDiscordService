package net.survivalboom.sbds.core.libraries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;

public class DynamicClassLoader extends URLClassLoader {

    private final Logger log;


    private final Set<File> sources = new HashSet<>();

    private final Map<String, ClassSupplier> classSuppliers = new HashMap<>();

    private final Map<String, ResourceSupplier> resourceSuppliers = new HashMap<>();


    private boolean wasClosed = false;


    public DynamicClassLoader(@NotNull String name, @Nullable ClassLoader parent) {
        super(name, new URL[0], parent);
        this.log = Logger.getLogger(name);
    }

    public void resetSuppliers() {
        this.classSuppliers.clear();
        this.resourceSuppliers.clear();
    }

    //
    // SOURCES
    //

    public void addSource(@NotNull File file) {

        Objects.requireNonNull(file, "file == null");
        checkValid();

        if (sources.contains(file)) {
            throw new IllegalStateException("File `" + file.getPath() + "` already registered in this ClassLoader");
        }

        URL url;
        try {
            url = file.toURI().toURL();
        }

        catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        addURL(url);
        sources.add(file);

    }

    public @NotNull List<File> getSources() {
        return new ArrayList<>(sources);
    }

    //
    // CLASS SUPPLIERS
    //

    public @NotNull ClassSupplier addClassSupplier(@NotNull String name, @NotNull Function<String, Class<?>> supplier) {

        Objects.requireNonNull(name, "name == null");
        Objects.requireNonNull(supplier, "supplier == null");
        checkValid();

        if (classSuppliers.containsKey(name)) {
            throw new IllegalStateException("ClassSupplier with name `" + name + "` already exists");
        }

        ClassSupplier classSupplier = new ClassSupplier(name, null, supplier, false);
        classSuppliers.put(name, classSupplier);

        return classSupplier;

    }

    public @NotNull ClassSupplier addClassSupplier(@NotNull String name, @NotNull Predicate<String> predicate, @NotNull Function<String, Class<?>> supplier) {

        Objects.requireNonNull(name, "name == null");
        Objects.requireNonNull(predicate, "predicate == null");
        Objects.requireNonNull(supplier, "supplier == null");
        checkValid();

        if (classSuppliers.containsKey(name)) {
            throw new IllegalStateException("ClassSupplier with name `" + name + "` already exists");
        }

        ClassSupplier classSupplier = new ClassSupplier(name, predicate, supplier, false);
        classSuppliers.put(name, classSupplier);

        return classSupplier;

    }

    public @NotNull ClassSupplier addParentDelegateClassRule(@NotNull String name, @NotNull Predicate<String> predicate) {

        Objects.requireNonNull(name, "name == null");
        Objects.requireNonNull(predicate, "predicate == null");
        checkValid();

        if (classSuppliers.containsKey(name)) {
            throw new IllegalStateException("ClassSupplier with name `" + name + "` already exists");
        }

        ClassSupplier classSupplier = new ClassSupplier(name, predicate, null, true);
        classSuppliers.put(name, classSupplier);

        return classSupplier;

    }

    public void removeClassSupplier(@NotNull ClassSupplier supplier) {

        Objects.requireNonNull(supplier, "supplier == null");
        checkValid();

        String name = supplier.name;
        if (!classSuppliers.containsKey(name)) {
            throw new IllegalStateException("Supplier with name `" + name + "` does not exist");
        }

        classSuppliers.remove(name);

    }

    public @NotNull List<ClassSupplier> getClassSuppliers() {
        checkValid();
        return new ArrayList<>(classSuppliers.values());
    }

    public @Nullable ClassSupplier getClassSupplier(@NotNull String name) {
        checkValid();
        return classSuppliers.get(name);
    }

    //
    // RESOURCES SUPPLIERS
    //

    public @NotNull ResourceSupplier addResourceSupplier(@NotNull String name, @NotNull Function<String, List<URL>> supplier) {

        Objects.requireNonNull(name, "name == null");
        Objects.requireNonNull(supplier, "supplier == null");
        checkValid();

        if (resourceSuppliers.containsKey(name)) {
            throw new IllegalStateException("ResourceSupplier with name `" + name + "` already exists");
        }

        ResourceSupplier resourceSupplier = new ResourceSupplier(name, null, supplier, false);
        resourceSuppliers.put(name, resourceSupplier);

        return resourceSupplier;

    }

    public @NotNull ResourceSupplier addResourceSupplier(@NotNull String name, @NotNull Predicate<String> predicate, @NotNull Function<String, List<URL>> supplier) {

        Objects.requireNonNull(name, "name == null");
        Objects.requireNonNull(predicate, "predicate == null");
        Objects.requireNonNull(supplier, "supplier == null");
        checkValid();

        if (resourceSuppliers.containsKey(name)) {
            throw new IllegalStateException("ResourceSupplier with name `" + name + "` already exists");
        }

        ResourceSupplier resourceSupplier = new ResourceSupplier(name, predicate, supplier, false);
        resourceSuppliers.put(name, resourceSupplier);

        return resourceSupplier;

    }

    public @NotNull ResourceSupplier addParentDelegateResourceRule(@NotNull String name, @NotNull Predicate<String> predicate) {

        Objects.requireNonNull(name, "name == null");
        Objects.requireNonNull(predicate, "predicate == null");
        checkValid();

        if (resourceSuppliers.containsKey(name)) {
            throw new IllegalStateException("RresourceSupplier with name `" + name + "` already exists");
        }

        ResourceSupplier resourceSupplier = new ResourceSupplier(name, predicate, null, true);
        resourceSuppliers.put(name, resourceSupplier);

        return resourceSupplier;

    }

    public void removeResourceSupplier(@NotNull ResourceSupplier supplier) {

        Objects.requireNonNull(supplier, "supplier == null");
        checkValid();

        String name = supplier.name;
        if (resourceSuppliers.containsKey(name)) {
            throw new IllegalStateException("Supplier with name `" + name + "` does not exist");
        }

        resourceSuppliers.remove(name);

    }

    public @NotNull List<ResourceSupplier> getResourceSuppliers() {
        checkValid();
        return new ArrayList<>(resourceSuppliers.values());
    }

    public @Nullable ResourceSupplier getResourceSupplier(@NotNull String name) {
        checkValid();
        return resourceSuppliers.get(name);
    }

    //
    // CLASSES
    //

    @Override
    protected @NotNull Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {

        if (wasClosed) {
            throw new ClassNotFoundException("ClassLoader `" + this + "` was closed");
        }

        Class<?> clazz = getClass(name, true, true);

        if (clazz != null) {

            if (resolve) {
                resolveClass(clazz);
            }

//            String cn = clazz.getClassLoader() != null ? clazz.getClassLoader().getName() : "null";
//            System.out.println(name + " - " + cn);

            return clazz;

        }

        throw new ClassNotFoundException("Class `" + name + "` was not found in `" + this + "`");

    }

    public @Nullable Class<?> getClass(
            @NotNull String name,
            boolean suppliers,
            boolean parent
    ) {

        Objects.requireNonNull(name, "name == null");
        checkValid();

        // Ігноруємо усі вбудовані у JVM класи, оскільки навіщо нам їх взагалі тут шукати???
        // Нічого собі, раптово швидкість роботи бота виросла на 50%! Вау! Не може бути!
        if (name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("sun.") || name.startsWith("jdk.")) {

            try {
                return ClassLoader.getSystemClassLoader().loadClass(name);
            } catch (ClassNotFoundException e) {
                return null;
            }

        }

        // Шукаємо чи був цей клас вже завантажений цим об'єктом раніше. Якщо так, повертаємо його.

        Class<?> clazz = findLoadedClass(name);
        if (clazz != null) {
            return clazz;
        }

        ClassLoader parentClassLoader = getParent();

        // Перевіряємо parent delegate rules //

        if (suppliers) {

            boolean delegateToParent = classSuppliers.values().stream()
                    .anyMatch(supplier -> supplier.delegateToParent && supplier.predicate.test(name));


            if (delegateToParent && parentClassLoader != null) {

                try {
                    return parentClassLoader.loadClass(name);
                }

                catch (ClassNotFoundException ignored) {}

                catch (Exception e) {
                    log.severe("An exception was thrown in parent class loader `" + parentClassLoader + "` when tried to load class `" + name + "`.");
                    e.printStackTrace();
                }

            }

        }

        // Шукаємо клас у sources поточного class loader.

        try {
            clazz = findClass(name);
        }

        catch (ClassNotFoundException ignored) {}

        // Обробка ClassSuppliers //

        if (suppliers) {

            for (ClassSupplier supplier : classSuppliers.values()) {

                if (clazz != null) {
                    break;
                }

                if (supplier.delegateToParent) {
                    continue;
                }

                Predicate<String> predicate = supplier.predicate;
                if (predicate != null) {

                    try {

                        if (!predicate.test(name)) {
                            continue;
                        }

                    }

                    catch (Exception e) {
                        log.severe("An exception was thrown in ClassSupplier `" + supplier.name + "` predicate.");
                        e.printStackTrace();
                    }

                }

                var function = supplier.supplier;
                if (function != null) {

                    try {
                        clazz = function.apply(name);
                    }

                    catch (Exception e) {
                        log.severe("An exception was thrown in `" + supplier.name + "` when tried to load class `" + name + "`.");
                        e.printStackTrace();
                    }

                }

            }

        }

        if (clazz == null && parent && parentClassLoader != null) {

            try {
                clazz = parentClassLoader.loadClass(name);
            }

            catch (ClassNotFoundException ignored) {}

            catch (Exception e) {
                log.severe("An exception was thrown in parent class loader `" + parentClassLoader +  "` when tried to load class `" + name + "`.");
                e.printStackTrace();
            }

        }

        return clazz;

    }

    //
    // RESOURCES
    //

    @Override
    public Enumeration<URL> findResources(String name) {
        return Collections.enumeration(findResources(name, true, false));
    }

    public List<URL> findResources(@NotNull String name, boolean suppliers, boolean parent) {

        Objects.requireNonNull(name, "name == null");
        checkValid();

        ClassLoader parentClassLoader = getParent();

        // Оброблюємо переспрямовування до parent //

        if (suppliers) {

            boolean delegateToParent = resourceSuppliers.values().stream()
                    .anyMatch(supplier -> supplier.delegateToParent && supplier.predicate.test(name));

            if (delegateToParent) {

                try {
                    return Collections.list(parentClassLoader.getResources(name));
                }

                catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }

        }


        // Шукаємо ресурси у цьому ClassLoader //

        Enumeration<URL> enumeration;
        try {
            enumeration = super.findResources(name);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<URL> result = Collections.list(enumeration);

        // Шукаємо ресурси у ResourceSuppliers //

        if (suppliers) {
            for (ResourceSupplier supplier : resourceSuppliers.values()) {

                if (supplier.delegateToParent) {
                    continue;
                }

                Predicate<String> predicate = supplier.predicate;
                if (predicate != null) {

                    try {
                        if (!predicate.test(name)) {
                            continue;
                        }
                    } catch (Exception e) {
                        log.severe("An exception was thrown in ResourceSupplier `" + supplier.name + "` predicate.");
                        e.printStackTrace();
                    }

                }

                var function = supplier.supplier;
                if (function != null) {

                    try {
                        result.addAll(function.apply(name));
                    } catch (Exception e) {
                        log.severe("An exception was thrown in `" + supplier.name + "` when tried to load resource `" + name + "`.");
                        e.printStackTrace();
                    }

                }


            }

        }

        // Шукаємо ресурси у parent //

        if (parent) {

            try {
                result.addAll(Collections.list(parentClassLoader.getResources(name)));
            }

            catch (Exception e) {
                throw new RuntimeException(e);
            }

        }

        return result;

    }


    //
    // CLOSABLE
    //

    @Override
    public void close() {

        checkValid();

        this.wasClosed = true;

        try {
            super.close();
        }

        catch (IOException e) {
            log.severe("There was an exception thrown in URLClassLoader.close(). Maybe something went wrong. This error can cause memory leaks and weird behaviour!");
            e.printStackTrace();
        }

        this.sources.clear();
        this.classSuppliers.clear();

    }

    public boolean isClosed() {
        return wasClosed;
    }

    private void checkValid() {
        if (wasClosed) {
            throw new IllegalStateException("classloader was closed");
        }
    }

    //
    // MISC
    //

    @Override
    public String toString() {
        return String.format(
                "DynamicClassLoader{name=%s, sources=%s, class-suppliers=%s, resource-suppliers=%s}",
                getName(),
                sources.size(),
                classSuppliers.size(),
                resourceSuppliers.size()
        );
    }

    public record ClassSupplier(
            @NotNull String name,
            @Nullable Predicate<String> predicate,
            @Nullable Function<String, Class<?>> supplier,
            boolean delegateToParent
    ) {}

    public record ResourceSupplier(
            @NotNull String name,
            @Nullable Predicate<String> predicate,
            @Nullable Function<String, List<URL>> supplier,
            boolean delegateToParent
    ) {}

}
