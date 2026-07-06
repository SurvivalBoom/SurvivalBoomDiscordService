package net.survivalboom.sbds.core;

import net.survivalboom.sbds.api.libraries.LibrarySatisfyConfiguration;
import net.survivalboom.sbds.api.utils.CommonUtils;
import net.survivalboom.sbds.core.libraries.DynamicClassLoader;
import net.survivalboom.sbds.core.libraries.LibrariesManager;
import net.survivalboom.sbds.core.libraries.simple.SimpleLibrariesDownloader;
import net.survivalboom.sbds.core.logging.LoggerLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SbdsBootstrap {

    private final Logger logger;

    private final File workingDir;


    private final LibrariesManager librariesManager;

    private final SimpleLibrariesDownloader simpleLibrariesDownloader;


    private ConfigurationNode configuration;


    private String token;


    public SbdsBootstrap(
            @NotNull File workingDir,
            @NotNull SimpleLibrariesDownloader downloader,
            @NotNull DynamicClassLoader rootClassLoader
    ) {

        this.logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        this.workingDir = workingDir;

        this.simpleLibrariesDownloader = downloader;

        this.librariesManager = new LibrariesManager(new File(workingDir, "libraries"), rootClassLoader);

    }


    public void launch() {

        LoggerLayout.setup();
        LoggerLayout.INSTANCE.setColorsSupport(true);

        logger.info("");
        logger.info("    ____              _           _____                ");
        logger.info("   / __/_ _______  __(_)  _____ _/ / _ )___  ___  __  _ ");
        logger.info("  _\\ \\/ // / __/ |/ / / |/ / _ `/ / _  / _ \\/ _ \\/  '  /");
        logger.info(" /___/\\_,_/_/  |___/_/|___/\\_,_/_/____/\\___/\\___/_/_/_/");
        logger.info("");
        logger.info("SurvivalBoom Network 2026 | SurvivalBoom Discord Service");
        logger.info("                    By TIMURishche \uD83E\uDD96");
        logger.info("");
        logger.info("                    Version {}", BuildConstants.VERSION);
        logger.info("");

        try {

            checkFiles();
            loadConfiguration();

            checkLibraries();

            sbdsRun();

        }

        catch (Throwable t) {
            logger.error("Fatal error occurred. Exiting in 10 seconds...", t);
            Main.exit();
        }


    }

    private void checkFiles() {

        logger.info("Checking files...");

        try {
            CommonUtils.checkFiles(Main.class, workingDir, Map.of("settings.yml", "settings.yml"), null);
        }

        catch (Throwable t) {
            logger.error("Failed to create required files", t);
            Main.exit();
        }

    }

    private boolean loadConfiguration() throws IOException {

        logger.info("Loading configuration...");

        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(new File(workingDir, "settings.yml").toPath())
                .build();

        try {

            configuration = loader.load();
            token = loadToken(new File(workingDir, "token"));

        }

        catch (Throwable t) {
            logger.error("Failed to load configuration. Exiting...", t);
            Main.exit();
            return true;
        }

        if (token == null) {

            logger.warn("Token file is empty. Please provide a discord bot token. Exiting in 10 seconds...");

            new File(workingDir, "token").createNewFile();

            Main.exit();
            return true;

        }

        return false;

    }

    private void checkLibraries() {

        logger.info("Loading libraries...");

        librariesManager.init();
        librariesManager.importFromSimpleLibrariesDownloader(simpleLibrariesDownloader);

        ConfigurationNode section = configuration.node("libraries");
        if (section.virtual()) {
            logger.warn("Libraries section does not exist or is empty. No libraries will be downloaded, SBDS may crash!");
            return;
        }

        LibrarySatisfyConfiguration.MassLoadResult request = LibrarySatisfyConfiguration.fromSection(section);
        boolean failure = false;

        Map<String, Exception> failed = new HashMap<>();
        failed.putAll(request.relocationsFailed());
        failed.putAll(request.declarationsFailed());
        failed.putAll(request.pinnedFailed());

        var result = librariesManager.satisfy(request.result());
        if (!result.failed().isEmpty()) {

            for (var entry : result.failed().entrySet()) {
                logger.error("Failed to load library `{}`. An exception occurred.", entry.getKey(), entry.getValue());
            }

            failure = true;

        }

        if (!failed.isEmpty()) {

            for (var entry : failed.entrySet()) {
                logger.error("Found an invalid LibraryDeclaration `{}`.", entry.getKey(), entry.getValue());
            }

            failure = true;

        }

        if (failure) {
            logger.error("Some libraries were failed to download. Refusing to start.");
            throw new RuntimeException();
        }

        librariesManager.setupSbdsLibraries(result);

    }

    private @NotNull SBDS sbdsRun() throws InterruptedException {

        logger.info("Starting SBDS...");

        SBDS sbds = new SBDS(logger, librariesManager, configuration, workingDir, token);
        SBDS.sbds = sbds;

        sbds.run();

        return sbds;

    }

    //
    // UTILS
    //

    private static @Nullable String loadToken(@NotNull File file) throws IOException {

        if (!file.exists()) file.createNewFile();

        List<String> lines = Files.readAllLines(file.toPath());
        if (lines.isEmpty()) return null;

        return lines.getFirst();

    }

}
