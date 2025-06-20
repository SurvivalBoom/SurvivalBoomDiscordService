package net.survivalboom.sbds.core;

import net.survivalboom.sbds.api.utils.CommonUtils;
import net.survivalboom.sbds.core.libraries.JarLoader;
import net.survivalboom.sbds.core.libraries.simple.SimpleLibrariesDownloader;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class Main {

    private static boolean started = false;

    public static void main(String[] args) throws Throwable {

        if (started) throw new RuntimeException("Ну ты долбоеб, да? Я склоняюсь к мысли что да");
        started = true;

        File workingDir = CommonUtils.getJarFile(Main.class).getParentFile();

        JarLoader jarLoader = new JarLoader(Main.class.getClassLoader());
        Thread.currentThread().setContextClassLoader(jarLoader);

        checkInitialLibraries(workingDir, jarLoader);
        launch(workingDir, jarLoader);

        System.exit(0);

    }

    private static void checkInitialLibraries(@NotNull File workingDir, @NotNull JarLoader jarLoader) {

        System.out.println("Loading initial libraries...");

        SimpleLibrariesDownloader librariesDownloader = new SimpleLibrariesDownloader(new File(workingDir, "libraries"), jarLoader);

        // Logging
        librariesDownloader.download("https://repo1.maven.org/maven2/", "org.slf4j", "slf4j-api", "2.0.16");
        librariesDownloader.download("https://repo1.maven.org/maven2/", "ch.qos.logback", "logback-core", "1.5.16");
        librariesDownloader.download("https://repo1.maven.org/maven2/", "ch.qos.logback", "logback-classic", "1.5.16");

        // configuration
        librariesDownloader.download("https://repo1.maven.org/maven2/", "org.yaml", "snakeyaml", "2.3");
        librariesDownloader.download("https://repo1.maven.org/maven2/", "org.bspfsystems", "yamlconfiguration", "2.0.1");
        librariesDownloader.download("https://repo1.maven.org/maven2/", "org.json", "json", "20240303");

    }

    private static void launch(@NotNull File workingDir, @NotNull JarLoader jarLoader) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {

        Class<?> bootstrapClass = jarLoader.findClass("net.survivalboom.sbds.core.SbdsBootstrap");

        Constructor<?> constructor = bootstrapClass.getDeclaredConstructors()[0];
        Object bootstrapInstance = constructor.newInstance(workingDir, jarLoader);

        bootstrapClass.getMethod("launch").invoke(bootstrapInstance);

    }

    public static void exit() {
        CommonUtils.sleep(10000);
        System.exit(0);
    }

}
