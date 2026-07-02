package net.survivalboom.sbds.api.utils;

import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.survivalboom.sbds.api.utils.placeholders.Placeholders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.event.Level;
import org.spongepowered.configurate.ConfigurationNode;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CommonUtils {

    public static String STACK_TRACE_FORMAT = "    at {class}.{method}({file}:{line}) ~[{classloader}}:{module}]";

    public static final Random RANDOM = new Random();


    //
    // FILES
    //

    public static void checkFiles(@NotNull Class<?> origin, @NotNull File workingDir, @Nullable Logger logger, String... args) {
        Map<String, String> map = mapOf((Object[]) args);
        checkFiles(origin, workingDir, map, logger);
    }

    public static void checkFiles(@NotNull Class<?> origin, @NotNull File workingDir, @NotNull Map<String, String> files, @Nullable Logger logger) {

        Objects.requireNonNull(origin, "origin == null");
        Objects.requireNonNull(workingDir, "workingDir == null");
        Objects.requireNonNull(files, "files == null");

        if (workingDir.isFile()) {
            throw new IllegalArgumentException(String.format("File at %s is a file!", workingDir.getPath()));
        }

        try {

            if (!workingDir.exists()) workingDir.mkdirs();

            // Получаем путь к JAR файлу
            File jarFile = new File(origin.getProtectionDomain().getCodeSource().getLocation().toURI());

            // Открываем JAR как ZipFile
            try (ZipFile zipFile = new ZipFile(jarFile)) {

                for (Map.Entry<String, String> entry : files.entrySet()) {

                    String jarPath = entry.getKey();
                    String destinationPath = entry.getValue();
                    File destFile = new File(workingDir, destinationPath);

                    if (jarPath.endsWith("/")) {
                        // Если ключ заканчивается на "/", это папка — копируем её содержимое рекурсивно
                        copyDirectoryFromJar(zipFile, jarPath, destFile, logger);
                    }

                    else {

                        // Если это отдельный файл, копируем его напрямую
                        if (!destFile.exists()) {
                            copyFileFromJar(zipFile, jarPath, destFile, logger);
                        }

                    }

                }

            }

        }

        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void copyFileFromJar(ZipFile zipFile, String jarPath, File destFile, @Nullable Logger logger) throws IOException {

        ZipEntry entry = zipFile.getEntry(jarPath);
        if (entry == null) throw new IOException("File " + jarPath + " not found in JAR.");

        destFile.getParentFile().mkdirs();

        try (InputStream stream = zipFile.getInputStream(entry)) {
            Files.copy(stream, destFile.toPath());
            if (logger != null) logger.info("Created {}...", destFile.getName());
        }

    }

    private static void copyDirectoryFromJar(ZipFile zipFile, String jarDirPath, File destDir, @Nullable Logger logger) throws IOException {

        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        while (entries.hasMoreElements()) {

            ZipEntry entry = entries.nextElement();
            String entryName = entry.getName();

            if (!entryName.startsWith(jarDirPath)) continue;

            String relativePath = entryName.substring(jarDirPath.length());
            File destFile = new File(destDir, relativePath);

            if (destFile.exists()) continue;

            if (entry.isDirectory()) destFile.mkdirs();
            else copyFileFromJar(zipFile, entryName, destFile, logger);

        }
    }

    public static @NotNull File getJarFile(@NotNull Class<?> clazz) {

        String jarPath;

        try {
            jarPath = clazz.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        }

        catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        String decodedPath = URLDecoder.decode(jarPath, StandardCharsets.UTF_8);

        return new File(decodedPath);

    }

    //
    // TEXT
    //

    public static @Nullable Character checkString(@NotNull String string, @NotNull String allowed) {

        for (char c : string.toCharArray()) {

            if (allowed.indexOf(c) == -1) {
                return c;
            }

        }

        return null;

    }

    public static void checkStringExceptionally(@NotNull String name, @NotNull String string, @NotNull String allowed) {

        Character c = checkString(string, allowed);
        if (c != null) {
            throw new IllegalArgumentException("Illegal input `" + string + "` for `" + name + "`. Illegal character `" + c + "`. Valid characters: `" + allowed + "`");
        }

    }

    //
    // LOGGING
    //

    public static void logThreadStackTrace(@NotNull Logger logger, @NotNull Level level, @NotNull Thread thread) {

        String msg = "Thread dump of: " + thread.getName() + "\n";

        msg += stackTraceToString(thread.getStackTrace());

        logger.atLevel(level).log(msg);

    }

    public static @NotNull String stackTraceToString(@NotNull StackTraceElement[] stackTraceElements) {

        StringBuilder builder = new StringBuilder();

        for (StackTraceElement element : stackTraceElements) {
            builder.append(stackTraceElementToString(element));
            builder.append("\n");
        }

        return builder.toString();

    }

    public static String stackTraceElementToString(@NotNull StackTraceElement element) {

        String module = element.getModuleVersion();
        String classLoader = element.getClassLoaderName();

        Placeholders placeholders = new Placeholders();
        placeholders.add("class", element.getClassName());
        placeholders.add("method", element.getMethodName());
        placeholders.add("file", Objects.requireNonNullElse(element.getFileName(), "?"));
        placeholders.add("line", element.getLineNumber());
        placeholders.add("classloader", classLoader == null ? "?" : classLoader);
        placeholders.add("module", module == null ? "?" : module);

        return placeholders.parse(STACK_TRACE_FORMAT);

    }

    //
    // WAIT
    //

    public static void sleep(int millis) {
        if (millis <= 0) return;
//        if (Bukkit.isPrimaryThread()) throw new IllegalStateException("Do not attempt to freeze main thread! Fuck yourself!");
        try { Thread.sleep(millis); }
        catch (InterruptedException ignored) {}
        catch (Exception e) { throw new RuntimeException(e); }
    }

    public static void waitUntil(@NotNull Supplier<Boolean> supplier, int timeOutInMillis, int checkIntervalMillis, @Nullable Runnable onCheck) {

        long startTime = System.currentTimeMillis();

        while (!supplier.get()) {

            if (onCheck != null) {
                onCheck.run();
            }

            long currentTime = System.currentTimeMillis();
            if (timeOutInMillis > 0 && (currentTime - startTime) > timeOutInMillis) {
                throw new RuntimeException("waitUntil method time out");
            }

            sleep(checkIntervalMillis);

        }

    }

    public static void waitUntil(@NotNull Supplier<Boolean> supplier, int timeOutMillis) {
        waitUntil(supplier, timeOutMillis, 10, null);
    }

    public static void waitUntil(@NotNull Supplier<Boolean> supplier) {
        waitUntil(supplier, 0, 10, null);
    }

    //
    // Option Mapping
    //

    public static @NotNull Object getValueFromMapping(@NotNull OptionMapping mapping) {

        OptionType type = mapping.getType();

        return switch (type) {

            case STRING -> mapping.getAsString();

            case INTEGER -> mapping.getAsInt();

            case BOOLEAN -> mapping.getAsBoolean();

            case USER -> mapping.getAsUser();

            case CHANNEL -> mapping.getAsChannel();

            case ROLE -> mapping.getAsRole();

            case MENTIONABLE -> mapping.getAsMentionable();

            case NUMBER -> mapping.getAsDouble();

            case ATTACHMENT -> mapping.getAsAttachment();

            default -> throw new RuntimeException("Invalid object type `" + type + "`");

        };

    }

    //
    // ENUM
    //

    public static @Nullable <E extends Enum<E>> E getEnumValue(@NotNull Class<E> clazz, @Nullable String value) {

        if (value == null) return null;


        try {
            return Enum.valueOf(clazz, value);
        }

        catch (IllegalArgumentException e) {
            return null;
        }

    }


    //
    // REFLECTION
    //

    public static @NotNull StackTraceElement getMethodCaller(int depth) {

        StackTraceElement[] elements = Thread.currentThread().getStackTrace();

        depth += 2;

        return elements[depth];

    }

    public static @Nullable Object invokeMethod(@NotNull Object origin, @NotNull Method method, Object... resources) {

        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] arguments = new Object[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> paramType = parameterTypes[i];
            Object matchedResource = null;

            // Перебираем массив ресурсов и ищем подходящий по типу
            for (Object resource : resources) {
                if (resource == null) continue;

                // Если тип параметра можно присвоить из класса ресурса (paramType = ResourceClass)
                if (paramType.isAssignableFrom(resource.getClass())) {
                    matchedResource = resource;
                    break; // Нашли первое подходящее совпадение
                }
            }

            if (matchedResource != null) {
                arguments[i] = matchedResource;
            } else {
                throw new IllegalArgumentException(
                        String.format("Failed to invoke %s.%s. No resource found for parameter type: %s",
                                method.getDeclaringClass().getSimpleName(), method.getName(), paramType.getName())
                );
            }
        }

        if (!method.canAccess(origin)) {
            method.setAccessible(true);
        }

        try {
            return method.invoke(origin, arguments);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    //
    // MAP
    //

    @SuppressWarnings("unchecked")
    public static @NotNull <K, V> Map<K, V> mapOf(Object... args) {

        Map<K, V> map = new HashMap<>();

        if ((args.length % 2) != 0) {
            throw new IllegalArgumentException("Invalid count of arguments " + args.length);
        }

        int index = 0;
        while (index < args.length) {

            Object key = args[index];
            Object value = args[index + 1];

            map.put((K) key, (V) value);

            index += 2;

        }

        return map;

    }

    @SuppressWarnings("unchecked")
    public static <T> T deepCopy(T object) {

        // 1. Обработка Map
        if (object instanceof Map<?, ?>) {
            Map<Object, Object> sourceMap = (Map<Object, Object>) object;
            Map<Object, Object> copyMap = new LinkedHashMap<>(); // Сохраняем порядок вставки
            for (Map.Entry<Object, Object> entry : sourceMap.entrySet()) {
                copyMap.put(deepCopy(entry.getKey()), deepCopy(entry.getValue()));
            }

            return (T) copyMap;

        }

        // 2. Обработка List (включая List<Map>)
        else if (object instanceof List<?>) {
            List<Object> sourceList = (List<Object>) object;
            List<Object> copyList = new ArrayList<>(sourceList.size());
            for (Object item : sourceList) {
                copyList.add(deepCopy(item));
            }

            return (T) copyList;

        }

        // 3. Обработка Set (опционально, для полноты)
        else if (object instanceof Set<?>) {

            Set<Object> sourceSet = (Set<Object>) object;
            Set<Object> copySet = new LinkedHashSet<>();
            for (Object item : sourceSet) {
                copySet.add(deepCopy(item));
            }

            return (T) copySet;

        }

        // 4. Базовый случай: примитивы, String, неизменяемые объекты или null
        // В Java String и обертки примитивов неизменяемы, их копировать не нужно.
        return object;

    }

    public static @NotNull Map<String, Object> compressDeepMap(@NotNull Map<String, Object> source) {
        Objects.requireNonNull(source, "source == null");
        return compressDeepMap0(source, new HashMap<>(), "");
    }

    @SuppressWarnings("unchecked")
    private static @NotNull Map<String, Object> compressDeepMap0(@NotNull Map<String, Object> source, @NotNull Map<String, Object> map, @NotNull String path) {

        for (var entry : map.entrySet()) {

            String key = entry.getKey();
            Object value = entry.getValue();

            String newPath = path + key + ".";

            if (value instanceof Map<?,?> m) {

                Map<String, Object> mm;
                try {
                    mm = (Map<String, Object>) m;
                }

                catch (ClassCastException ignored) {
                    continue;
                }

                compressDeepMap0(mm, map, newPath);

            }

            else if (value instanceof Collection<?> undefinedCollection) {

                Collection<Map<String, Object>> collection;

                try {
                    collection = (Collection<Map<String, Object>>) undefinedCollection;
                }

                catch (ClassCastException ignored) {
                    continue;
                }

                var list = List.copyOf(collection);
                for (int i = 0; i < list.size(); i++) {
                    var m = list.get(i);
                    compressDeepMap0(m, map, newPath + i + ".");
                }

            }

            else {
                map.put(newPath, value);
            }

        }

        return map;

    }

    //
    // YAML
    //

    public static @NotNull Properties getPropertiesFromYaml(@NotNull ConfigurationNode section) {

        Properties properties = new Properties();
        properties.putAll(getStringMapFromYaml(section));

        return properties;

    }

    public static @NotNull Map<String, String> getStringMapFromYaml(@NotNull ConfigurationNode section) {
        return loadPropertiesMap(section, new HashMap<>());
    }

    private static Map<String, String> loadPropertiesMap(@NotNull ConfigurationNode node, @NotNull Map<String, String> map) {

        for (ConfigurationNode child : node.childrenMap().values()) {

            if (!child.isMap()) {

                String path = String.join(".", Arrays.stream(child.path().array()).map(Object::toString).toList());

                String string = child.getString();
                if (string == null) {
                    continue;
                }

                map.put(path, string);

                continue;

            }

            loadPropertiesMap(child, map);

        }

        return map;

    }


    //
    // TIME
    //

    private static final Pattern periodPattern = Pattern.compile("([0-9]+)([shdwmy])");

    public static @Nullable String durationToString(Duration duration) {

        if (duration == null) {
            return null;
        }

        long seconds = duration.getSeconds();
        long absSeconds = Math.abs(seconds);

        String positive = String.format(
                "%d:%02d:%02d",
                absSeconds / 3600,
                (absSeconds % 3600) / 60,
                absSeconds % 60);

        return seconds < 0 ? "-" + positive : positive;

    }

    public static @Nullable Duration getDurationFromStr(String string) {

        if (string == null) {
            return null;
        }

        string = string.toLowerCase(Locale.ENGLISH);

        Matcher matcher = periodPattern.matcher(string);
        Duration duration = Duration.ZERO;

        while (matcher.find()) {

            int num = Integer.parseInt(matcher.group(1));
            String typ = matcher.group(2);

            Duration d = switch (typ) {

                case "s" -> Duration.ofSeconds(num);

                case "h" -> Duration.ofHours(num);

                case "d" -> Duration.ofDays(num);

                case "w" -> Duration.ofDays(num * 7L);

                case "m" -> Duration.ofDays(num * 30L);

                case "y" -> Duration.ofDays(num * 365L);

                default -> null;

            };

            if (d == null) {
                return null;
            }

            duration = duration.plus(d);

        }

        return duration;

    }

    //
    // HASH
    //

    public static long longHash(Object... args) {

        if (args == null || args.length == 0) {
            return 0L;
        }

        // Начальное значение (seed) на основе золотого сечения для минимизации коллизий
        long h = 0x9E3779B97F4A7C15L;

        for (Object arg : args) {
            long value;

            if (arg == null) {
                value = 0L;
            } else if (arg instanceof Number) {
                // Если это Long, Integer и т.д., берем их примитивное значение
                value = ((Number) arg).longValue();
            } else {
                // Для строк и других объектов используем hashCode
                // Мы "растягиваем" 32-битный hashCode, чтобы он лучше перемешивался
                value = arg.hashCode();
            }

            // Качественное перемешивание (Mixer из MurmurHash3)
            value = (value ^ (value >>> 33)) * -0xae502812aa7333L;
            value = (value ^ (value >>> 33)) * -0x3b3146010f6d7dL;
            value = value ^ (value >>> 33);

            // Комбинируем текущий хэш с новым значением
            h ^= value + 0x9e3779b9 + (h << 6) + (h >> 2);
        }

        return h;

    }

    //
    // COLOR
    //

    public static @Nullable Color parseColor(@Nullable String hex) {

        if (hex == null) {
            return null;
        }

        int resultRed, resultGreen, resultBlue;
        try {
            resultRed = Integer.valueOf(hex.substring(0, 2), 16);
            resultGreen = Integer.valueOf(hex.substring(2, 4), 16);
            resultBlue = Integer.valueOf(hex.substring(4, 6), 16);
        }

        catch (NumberFormatException e) {
            return null;
        }

        return new Color(resultRed, resultGreen, resultBlue);

    }

    //
    // STRING
    //

    public static @NotNull String[] splitString(@NotNull String string, @NotNull String regex) {

        String[] parts = string.split(regex);
        if (parts.length == 0) {
            return new String[]{string};
        }

        return parts;

    }

    //
    // REPEAT
    //

    public static <T> RepeatResult<T> tryRepeat(@NotNull ThrowingSupplier<T> supplier, int attempts, int sleep) {

        if (attempts < 1) {
            throw new IllegalArgumentException("attempts < 1");
        }

        List<Throwable> errors = new ArrayList<>();
        int index = 0;
        T value = null;

        while (index <= attempts) {

            try {
                value = supplier.getThrowing();
                break;
            }

            catch (Throwable t) {
                errors.add(t);
            }

            index++;
            sleep(sleep);

        }

        return new RepeatResult<>(errors, Optional.ofNullable(value), index);

    }

    public record RepeatResult<T>(@NotNull List<Throwable> errors, @NotNull Optional<T> result, int attempts) {}


    /**
     * Объединяет коллекцию CompletableFuture в один CompletableFuture,
     * который возвращает список успешно выполненных результатов, игнорируя null.
     */
    public static <T> CompletableFuture<List<T>> sequenceAsync(Collection<CompletableFuture<T>> futures) {
        // Создаем массив для CompletableFuture.allOf
        CompletableFuture<?>[] futuresArray = futures.toArray(new CompletableFuture[0]);

        // Ждем выполнения всех задач, затем собираем результаты
        return CompletableFuture.allOf(futuresArray)
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .filter(Objects::nonNull) // Твой фильтр на null
                        .collect(Collectors.toList())
                );
    }


}
