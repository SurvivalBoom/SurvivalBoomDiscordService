package net.survivalboom.sbds.api.modules;

import net.survivalboom.sbds.api.libraries.LibrarySatisfyConfiguration;
import net.survivalboom.sbds.api.modules.dependencies.LoadOrder;
import net.survivalboom.sbds.api.modules.dependencies.ModuleDependency;
import net.survivalboom.sbds.api.utils.CommonUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public final class ModuleMeta {

    private final String id;

    private final String name;

    private final String main;

    private final @Nullable String apiVersion;


    private final @Nullable String description;

    private final String version;


    private final @Nullable String website;

    private final List<String> authors;


    private final List<ModuleDependency> dependencies;

    private final @Nullable LibrarySatisfyConfiguration libraries;


    public ModuleMeta(
            @NotNull String name,
            @NotNull String main,
            @Nullable String apiVersion,
            @Nullable String description,
            @NotNull String version,
            @Nullable String website,
            @Nullable Collection<String> authors,
            @Nullable Collection<ModuleDependency> dependencies,
            @Nullable LibrarySatisfyConfiguration libraries
    ) {

        Objects.requireNonNull(name, "name == null");
        Objects.requireNonNull(main, "main == null");
        Objects.requireNonNull(version, "version == null");

        String id = name.toLowerCase();

        CommonUtils.checkStringExceptionally("id", id, IModuleManager.ALLOWED_ID_CHARACTERS);
        CommonUtils.checkStringExceptionally("name", name, IModuleManager.ALLOWED_NAME_CHARACTERS);

        this.name = name;
        this.id = id;
        this.main = main;
        this.apiVersion = apiVersion;

        this.description = description;
        this.version = version;

        this.website = website;
        this.authors = authors != null ? new ArrayList<>(authors) : new ArrayList<>();

        this.dependencies = dependencies != null ? new ArrayList<>(dependencies) : new ArrayList<>();
        this.libraries = libraries;

    }

    public @NotNull String getId() {
        return id;
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull String getMain() {
        return main;
    }

    public @Nullable String getApiVersion() {
        return apiVersion;
    }


    public @Nullable String getDescription() {
        return description;
    }

    public @NotNull String getVersion() {
        return version;
    }


    public @Nullable String getWebsite() {
        return website;
    }

    public @NotNull List<String> getAuthors() {
        return new ArrayList<>(authors);
    }


    public @NotNull List<ModuleDependency> getDependencies() {
        return new ArrayList<>(dependencies);
    }

    public @Nullable LibrarySatisfyConfiguration getLibraries() {
        return libraries;
    }


    public @NotNull Builder copy() {
        return new Builder(this);
    }


    //
    // BUILDER
    //

    public static @NotNull ModuleMeta fromStream(@NotNull InputStream stream) throws InvalidMetaException {

        ConfigurationNode node;
        try {
            node = YamlConfigurationLoader.builder()
                    .source(() -> new BufferedReader(new InputStreamReader(stream)))
                    .build()
                    .load();
        } catch (ConfigurateException e) {
            throw new InvalidMetaException("Failed to load ModuleMeta", e);
        }

        return fromSection(node);

    }

    public static @NotNull ModuleMeta fromSection(@NotNull ConfigurationNode section) throws InvalidMetaException {

        String name = section.node("name").getString();
        String main = section.node("main").getString();
        String apiVersion = section.node("api-version").getString();

        if (name == null) {
            throw new InvalidMetaException("Key `name` does not exist");
        }

        if (main == null) {
            throw new InvalidMetaException("Key `main` does not exist");
        }

        String description = section.node("description").getString();
        String version = section.node("version").getString();

        if (version == null) {
            throw new InvalidMetaException("Key `version` does not exist");
        }

        String website = section.node("website").getString();

        List<String> authors;
        ConfigurationNode authorsNode = section.node("authors");
        String author = authorsNode.getString();

        if (author != null) {
            authors = List.of(author);
        }

        else {

            try {
                authors = authorsNode.getList(String.class);
            } catch (SerializationException e) {
                throw new InvalidMetaException("Failed to load `authors` section", e);
            }

        }

        ConfigurationNode dependenciesSection = section.node("dependencies");
        List<ModuleDependency> dependencyList;
        try {
            dependencyList = ModuleDependency.fromMultiSection(dependenciesSection);
        }

        catch (IllegalArgumentException e) {
            throw new InvalidMetaException("Failed to load module dependencies: " + e.getMessage());
        }

        ConfigurationNode librariesSection = section.node("libraries");

        LibrarySatisfyConfiguration libraries;
        if (!librariesSection.virtual() && !librariesSection.empty()) {

            var librariesResult = LibrarySatisfyConfiguration.fromSection(librariesSection);
            for (var entry : librariesResult.declarationsFailed().entrySet()) {
                throw new InvalidMetaException("Invalid library declaration `" + entry.getKey() + "`", entry.getValue());
            }

            for (var entry : librariesResult.pinnedFailed().entrySet()) {
                throw new InvalidMetaException("Invalid pinned library declaration `" + entry.getKey() + "`", entry.getValue());
            }

            for (var entry : librariesResult.relocationsFailed().entrySet()) {
                throw new InvalidMetaException("Invalid library relocation `" + entry.getKey() + "`", entry.getValue());
            }

            libraries = librariesResult.result();

        }

        else {
            libraries = null;
        }

        try {
            return new ModuleMeta(name, main, apiVersion, description, version, website, authors, dependencyList, libraries);
        }

        catch (IllegalArgumentException e) {
            throw new InvalidMetaException(e.getMessage());
        }

    }

    public @NotNull Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String name;

        private String main;

        private @Nullable String apiVersion;


        private @Nullable String description;

        private String version;


        private @Nullable String website;

        private final List<String> authors = new ArrayList<>();


        private final List<ModuleDependency> dependencies = new ArrayList<>();

        private LibrarySatisfyConfiguration libraries;


        private Builder(Builder builder) {

            this.name = builder.name;
            this.main = builder.main;
            this.apiVersion = builder.apiVersion;

            this.description = builder.description;
            this.version = builder.version;

            this.website = builder.website;
            this.authors.addAll(builder.authors);

            this.dependencies.addAll(builder.dependencies);
            this.libraries = builder.libraries;

        }

        private Builder(ModuleMeta meta) {

            this.name = meta.name;
            this.main = meta.main;
            this.apiVersion = meta.apiVersion;

            this.description = meta.description;
            this.version = meta.version;

            this.website = meta.website;
            this.authors.addAll(meta.authors);

            this.dependencies.addAll(meta.dependencies);
            this.libraries = meta.libraries;

        }

        private Builder() {}

        // NAME //

        public @NotNull Builder setName(@NotNull String name) {
            this.name = name;
            return this;
        }

        public String getName() {
            return name;
        }

        // MAIN //

        public @NotNull Builder setMain(@NotNull String main) {
            this.main = main;
            return this;
        }

        public @NotNull Builder setMain(@NotNull Class<? extends ModuleMain> clazz) {
            this.main = clazz.getName();
            return this;
        }

        public String getMain() {
            return main;
        }

        // API VERSION //

        public @NotNull Builder setApiVersion(@Nullable String apiVersion) {
            this.apiVersion = apiVersion;
            return this;
        }

        public @Nullable String getApiVersion() {
            return apiVersion;
        }

        // DESCRIPTION //

        public @NotNull Builder setDescription(@Nullable String description) {
            this.description = description;
            return this;
        }

        public @Nullable String getDescription() {
            return description;
        }

        // VERSION //

        public @NotNull Builder setVersion(@NotNull String version) {
            this.version = version;
            return this;
        }

        public String getVersion() {
            return version;
        }

        // WEBSITE //

        public @NotNull Builder setWebsite(@Nullable String website) {
            this.website = website;
            return this;
        }

        public @Nullable String getWebsite() {
            return website;
        }

        // AUTHORS //

        public @NotNull Builder addAuthor(@NotNull String author) {
            this.authors.add(author);
            return this;
        }

        public @NotNull Builder setAuthors(@Nullable Collection<String> authors) {

            this.authors.clear();

            if (authors != null) {
                this.authors.addAll(authors);
            }

            return this;

        }

        public @NotNull List<String> getAuthors() {
            return new ArrayList<>(authors);
        }

        // DEPENDENCIES //

        public @NotNull Builder addDependency(@NotNull ModuleDependency dependency) {
            this.dependencies.add(dependency);
            return this;
        }

        public @NotNull Builder addDependency(@NotNull String id, boolean required, boolean joinClasspath, @NotNull LoadOrder order) {
            this.dependencies.add(new ModuleDependency(id, required, joinClasspath, order));
            return this;
        }

        public @NotNull Builder setDependencies(@Nullable Collection<ModuleDependency> dependencies) {

            this.dependencies.clear();

            if (dependencies != null) {
                this.dependencies.addAll(dependencies);
            }

            return this;

        }

        public @NotNull List<ModuleDependency> getDependencies() {
            return new ArrayList<>(dependencies);
        }

        // LIBRARIES //

        public @NotNull Builder setLibraries(@Nullable LibrarySatisfyConfiguration libraries) {
            this.libraries = libraries;
            return this;
        }

        public @NotNull LibrarySatisfyConfiguration getLibraries() {
            return libraries;
        }

        // BUILD //

        public @NotNull ModuleMeta build() {
            return new ModuleMeta(name, main, apiVersion, description, version, website, authors, dependencies, libraries);
        }


    }


    public static class InvalidMetaException extends Exception {

        public InvalidMetaException(String message) {
            super(message);
        }

        public InvalidMetaException(Throwable cause) {
            super(cause);
        }

        public InvalidMetaException(String message, Throwable cause) {
            super(message, cause);
        }

    }

}
