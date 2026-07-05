package net.survivalboom.sbds.core.libraries;

import net.survivalboom.sbds.api.ISBDS;
import net.survivalboom.sbds.api.libraries.*;
import net.survivalboom.sbds.api.modules.IModule;
import net.survivalboom.sbds.api.modules.IModuleManager;
import net.survivalboom.sbds.api.utils.SemanticVersion;
import net.survivalboom.sbds.api.utils.placeholders.Placeholders;
import net.survivalboom.sbds.api.utils.valid.Manager;
import net.survivalboom.sbds.core.libraries.simple.SimpleLibrariesDownloader;
import net.survivalboom.sbds.core.libraries.simple.SimpleLibrary;
import net.survivalboom.sbds.core.modules.Module;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.xml.XmlConfigurationLoader;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class LibrariesManager extends Manager implements ILibrariesManager {

    private static final Logger log = LoggerFactory.getLogger(LibrariesManager.class.getSimpleName());


    private final DynamicClassLoader rootClassLoader;

    private final File librariesDir;

    private final HttpClient httpClient;


    private final Map<ArtifactAddress, IPomData> cachedPoms = new HashMap<>();

    private final Map<ArtifactAddress, ILibrary> libraryMap = new HashMap<>();

    private final List<LibraryDeclaration> globalPinned = new ArrayList<>();


    private final List<ILibrary> sbdsLibraries = new ArrayList<>();

    public ISBDS sbds = null; // Я знаю що це безглуздно, але пішло нахуй! Має бути setter, замість public. ХРЮ ХРЮ ХРЮ! СВІНТУС!


    public LibrariesManager(
            @NotNull File librariesDir,
            @NotNull DynamicClassLoader rootClassLoader
    ) {

        this.librariesDir = librariesDir;
        this.rootClassLoader = rootClassLoader;

        this.httpClient = HttpClient.newBuilder().build();

    }

    //
    // MANAGER
    //

    @Override
    protected void init0() {
        loadLibrariesFromDisk();
    }

    @Override
    protected void shutdown0() {

    }

    //
    // SETUP
    //

    public void setupSbdsLibraries(
            @NotNull MassLibraryDownloadResult libraries
    ) {

        // Налаштовуємо root class loader //

        rootClassLoader.resetSuppliers();
        rootClassLoader.addClassSupplier("ROOT", this::rootClassRequest);
        rootClassLoader.addResourceSupplier("SPI", n -> n.startsWith("META-INF/services/"), this::findGlobalSPIMetaInf);

        // Встановлюємо закріплені бібліотеки //

        this.globalPinned.addAll(libraries.request().getPinnedLibraries());
        this.sbdsLibraries.addAll(libraries.list());

    }

    private void loadLibrariesFromDisk() {

        List<File> pomFiles = Arrays.stream(librariesDir.listFiles())
                .filter(file -> file.isFile() && file.getName().endsWith(".pom"))
                .toList();

        for (File file : pomFiles) {

            String fileName = file.getName().replace(".pom", "");

            try {

                ArtifactAddress address = ArtifactAddress.fromGradleString(fileName, ArtifactAddress.DEFAULT_FILESYSTEM_SEPARATOR);

                // Можливо у поточний момент POM вже завантажений
                if (cachedPoms.containsKey(address)) {
                    continue;
                }

                IPomData pom = loadPomFromDisk(address);

                Objects.requireNonNull(pom, "POM was not imported. Possibly .url file does not exist.");

            }

            catch (Throwable t) {
                log.error("Failed to import POM `{}` from disk. Did you break something?", file.getName(), t);
            }

        }

    }

    // IMPORT LIBRARIES FROM SimpleLibrariesManager //

    public void importFromSimpleLibrariesDownloader(@NotNull SimpleLibrariesDownloader downloader) {

        checkValid();
        Objects.requireNonNull(downloader, "downloader == null");

        for (var lib : downloader.getLibrariesInstalled()) {
            importSimpleLibrary(lib);
        }

    }

    private @NotNull ILibrary importSimpleLibrary(@NotNull SimpleLibrary simpleLibrary) {

        List<ILibrary> dependencies = simpleLibrary.dependencies().stream()
                .map(this::importSimpleLibrary)
                .toList();

        String group = simpleLibrary.group();
        String artifact = simpleLibrary.artifact();
        String version = simpleLibrary.version();

        ArtifactAddress address = ArtifactAddress.create(group, artifact, version);

        // Якщо ми вже завантажили таку бібліотеку, повертаємо її.
        ILibrary library = getLoadedLibrary(address);
        if (library != null) {
            return library;
        }

        File file = simpleLibrary.file();
        DynamicClassLoader classLoader = simpleLibrary.classLoader();
        String repository = simpleLibrary.repository();

        // Отримуємо повний POM файл бібліотеки.
        IPomData pom;
        try {
            pom = retrievePom(repository, address);
        }

        catch (PomResolutionException e) {
            throw new RuntimeException(e);
        }

        library = new Library(pom, file, classLoader, dependencies);
        libraryMap.put(address, library);
        sbdsLibraries.add(library);

        configureLibraryClassLoader(library, classLoader);

        return library;

    }

    //
    // LIBRARIES
    //

    // DOWNLOAD //

    @Override
    public @NotNull ILibrariesManager.MassLibraryDownloadResult satisfy(@NotNull LibrarySatisfyConfiguration request) {

        Objects.requireNonNull(request, "request == null");
        checkValid();

        MassLibraryDownloadResult result = new MassLibraryDownloadResult(request, new ArrayList<>(), new ArrayList<>(), new HashMap<>());

        var pinned = request.getPinnedLibraries();
        var reloacations = request.getRelocations();

        for (var declaration : request.getLibraries()) {

            ArtifactAddress address = declaration.address();
            String repository = declaration.source();
            if (repository == null) {
                repository = MAVEN_REPO_URL;
            }

            IPomData pom;
            try {
                pom = retrievePom(repository, address);
            }

            catch (PomResolutionException e) {
                result.failed().put(declaration, e);
                continue;
            }

            ILibrary library = getLoadedLibrary(pom);
            if (library == null) {

                try {
                    library = downloadLibrary(pom, pinned, reloacations, true);
                }

                catch (LibraryDownloadException e) {
                    result.failed().put(declaration, e);
                    continue;
                }

            }

            result.downloaded().add(library);

        }

        return result;

    }

    @Override
    public @NotNull ILibrary downloadLibrary(
            @NotNull IPomData pom,
            @Nullable Collection<LibraryDeclaration> pinned,
            @Nullable Map<ArtifactAddress, LibraryDeclaration> relocations,
            boolean findOptimal
    ) throws LibraryDownloadException {

        Objects.requireNonNull(pom, "pom == null");
        checkValid();

        String repository = pom.getSourceRepository();
        ArtifactAddress address = pom.getAddress();

        if (libraryMap.containsKey(address)) {
            throw new IllegalStateException("Library `" + address + "` already exists");
        }

        ILibrary lib = findOptimalLoadedLibrary(address);
        if (lib != null) {
            return lib;
        }

        String url = address.createRepositoryAddress(repository, "jar");
        File file = new File(librariesDir, address.toGradleString(ArtifactAddress.DEFAULT_FILESYSTEM_SEPARATOR) + ".jar");

        List<LibraryDeclaration> pinned0 = new ArrayList<>(globalPinned);
        if (pinned != null) {
            pinned0.addAll(pinned);
        }

        //
        // DEPENDENCIES
        //

        List<ILibrary> dependencies = new ArrayList<>();
        for (IPomData dependencyPom : pom.getDependencies()) {

            boolean modified = false;

            ArtifactAddress pomAddress = dependencyPom.getAddress();
            LibraryDeclaration toModify = null;

            // RELOCATIONS //

            if (relocations != null) {
                toModify = relocations.get(pomAddress);
            }

            // PINNED //

            if (toModify == null) {
                toModify = pinned0.stream()
                        .filter(l -> l.address().group().equals(pomAddress.group()) && l.address().artifact().equals(pomAddress.artifact()))
                        .findAny()
                        .orElse(null);
            }

            if (toModify != null) {
                modified = true;
            }

            // LOAD MODIFIED LIB //

            if (toModify != null) {

                try {
                    dependencyPom = retrievePom(toModify);
                }

                catch (PomResolutionException e) {
                    throw new LibraryDownloadException("Failed to resolve modified artifact `" + toModify + "`");
                }

            }

            // LOAD LIB //

            ILibrary dependency = null;
            if (!modified && findOptimal) {
                dependency = findOptimalLoadedLibrary(dependencyPom.getAddress());
            }

            if (dependency == null) {
                dependency = getLoadedLibrary(dependencyPom);
            }

            if (dependency == null) {

                try {
                    dependency = downloadLibrary(dependencyPom, pinned0, relocations, findOptimal);
                }

                catch (LibraryDownloadException e) {
                    throw new LibraryDownloadException("Failed to download dependency `" + pom.getAddress() + "`");
                }

            }

            dependencies.add(dependency);

        }

        // DOWNLOADED //

        if (!file.exists()) {

            log.info("Downloading library from {}", url);

            try (InputStream in = new URI(url).toURL().openStream()) {
                Files.copy(in, file.toPath());
            }

            catch (Exception e) {
                throw new LibraryDownloadException("Failed to download library `" + address + "`", e);
            }

        }

        DynamicClassLoader classLoader = new DynamicClassLoader(pom.getAddress().toGradleString(), null);
        classLoader.addSource(file);

        Library library = new Library(pom, file, classLoader, dependencies);
        libraryMap.put(address, library);

        configureLibraryClassLoader(library, classLoader);

        return library;

    }

    private @Nullable ILibrary findOptimalLoadedLibrary(@NotNull ArtifactAddress address) {
        return libraryMap.values().stream()
                .filter(lib -> lib.getPomData().getAddress().group().equals(address.group()) && lib.getPomData().getAddress().artifact().equals(address.artifact()))
                .filter(lib -> lib.getPomData().getAddress().version().major() == address.version().major())
                .min(Comparator.comparing(l -> l.getPomData().getAddress().version()))
                .orElse(null);
    }

    // GETTERS //

    @Override
    public @Nullable ILibrary getLoadedLibrary(@NotNull ArtifactAddress address) {
        checkValid();
        return libraryMap.get(address);
    }

    @Override
    public @NotNull Map<ArtifactAddress, ILibrary> getLoadedLibraries() {
        checkValid();
        return new HashMap<>(libraryMap);
    }

    //
    // POM
    //

    @Override
    @Blocking
    public @NotNull IPomData retrievePom(@NotNull String repository, @NotNull ArtifactAddress address) throws PomResolutionException {

        Objects.requireNonNull(repository, "repository == null");
        Objects.requireNonNull(address, "address == null");
        checkValid();

        // Спочатку намагаємось дістати POM із кешу або диску //

        IPomData pom = getLoadedPom(address);
        if (pom != null) {
            return pom;
        }

        try {
            pom = loadPomFromDisk(address);
        }

        catch (Throwable t) {
            log.error("Failed to load POM of `{}` from disk.", address, t);
        }

        if (pom != null) {
            return pom;
        }

        // Завантажуємо POM з інтернету //

        String url = address.createRepositoryAddress(repository, "pom");

        log.info("Retrieving POM from {}", url);

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        }

        catch (Exception e) {
            throw new PomResolutionException("POM resolution failed. Failed to retrieve POM from repository. " + url, e);
        }

        int status = response.statusCode();
        if (status != 200) {
            throw new PomResolutionException("POM resolution failed. Server responded with code " + status + " " + url);
        }

        String string = response.body();

        PomData data;
        try {
            data = createPomFromString(repository, address, string);
        }

        catch (PomResolutionException e) {

            if (e.getMessage().contains("Invalid POM file")) {
                log.error("Received an invalid POM file. Printing contents below: \n{}", string);
            }

            throw new PomResolutionException("Failed to resolve `" + address + "`", e);

        }

        catch (Exception e) {
            throw new PomResolutionException("An internal error occurred! Maybe a bug?", e);
        }

        // Зберігаємо POM на диску //

        try {
            savePomToDisk(repository, data);
        }

        catch (Throwable t) {
            log.error("Failed to save POM `{}` to the disk.", address, t);
        }

        cachedPoms.put(address, data);

        return data;

    }

    @Override
    public @Nullable IPomData getLoadedPom(@NotNull ArtifactAddress address) {
        checkValid();
        return cachedPoms.get(address);
    }

    @Override
    public @NotNull Map<ArtifactAddress, IPomData> getLoadedPoms() {
        checkValid();
        return new HashMap<>(cachedPoms);
    }

    private @Nullable IPomData loadPomFromDisk(@NotNull ArtifactAddress address) throws IOException, PomResolutionException {

        if (cachedPoms.containsKey(address)) {
            throw new IllegalStateException("Already exists `" + address + "`");
        }

        String addressRaw = address.toGradleString(ArtifactAddress.DEFAULT_FILESYSTEM_SEPARATOR);
        String pomFileName = addressRaw + ".pom";
        String urlFileName = addressRaw + ".url";

        File pomFile = new File(librariesDir, pomFileName);
        File urlFile = new File(librariesDir, urlFileName);

        if (!pomFile.exists() || !pomFile.isFile() || !urlFile.exists() || !urlFile.isFile()) {
            return null;
        }

        String pomDataRaw = Files.readString(pomFile.toPath());
        String url = Files.readString(urlFile.toPath());

        IPomData pomData = createPomFromString(url, address, pomDataRaw);
        cachedPoms.put(address, pomData);

        return pomData;

    }

    private void savePomToDisk(@NotNull String repository, @NotNull IPomData pomData) throws IOException {

        String addressRaw = pomData.getAddress().toGradleString(ArtifactAddress.DEFAULT_FILESYSTEM_SEPARATOR);
        String pomFileName = addressRaw + ".pom";
        String urlFileName = addressRaw + ".url";

        File pomFile = new File(librariesDir, pomFileName);
        File urlFile = new File(librariesDir, urlFileName);

        XmlConfigurationLoader loader = XmlConfigurationLoader.builder()
                .path(pomFile.toPath())
                .build();

        loader.save(pomData.getData());
        Files.writeString(urlFile.toPath(), repository, StandardOpenOption.CREATE);

    }


    //
    // INTERNAL
    //

    private @NotNull PomData createPomFromString(@NotNull String repository, @NotNull ArtifactAddress address, @NotNull String string) throws PomResolutionException {

        ConfigurationNode pomData;
        try {
            pomData = XmlConfigurationLoader.builder().buildAndLoadString(string);
        } catch (ConfigurateException e) {
            throw new PomResolutionException("Invalid POM file. Could not parse XML", e);
        }

        ConfigurationNode projectData = pomData.node("project");
        if (projectData.virtual()) {
            projectData = pomData;
        }

        //
        // Завантажуємо properties з поточного pom.xml //
        //

        Placeholders properties = new Placeholders();
        createMergePomProperties(address, null, properties);

        Map<String, String> propertiesMap = new HashMap<>();
        for (var entry : projectData.node("properties").childrenMap().entrySet()) {

            String key = (String) entry.getKey();
            String value = entry.getValue().getString();

            propertiesMap.put(key, value);

        }

        properties.addAll(propertiesMap);
        properties.selfParse(100, MAVEN_PROPERTIES_LAYOUT);

        //
        // Дістаємо перелік репозиторіїв //
        //

        List<String> repositories = new ArrayList<>();
        repositories.add(MAVEN_REPO_URL);

        for (var section : матьЕбал(projectData, "repositories")) {

            String url = section.node("url").getString();
            if (url == null) {
                continue;
            }

            repositories.add(properties.parse(url));

        }

        //
        // Визначаємо parent цього POM //
        //

        ConfigurationNode parentSection = projectData.node("parent");
        IPomData parent = null;
        if (!parentSection.virtual()) {

            ArtifactAddress parentAddress;
            try {
                parentAddress = ArtifactAddress.fromPom(parentSection, properties);
            }

            catch (IllegalArgumentException e) {
                throw new PomResolutionException("Invalid POM parent: `" + e.getMessage() + "`");
            }

            try {
                parent = retrievePom(repositories, parentAddress);
            }

            catch (PomResolutionException e) {
                throw new PomResolutionException("Failed to resolve parent `" + parentAddress + "`", e);
            }

        }

        // Додаємо деякі системні змінні.
        if (parent != null) {
            createMergePomProperties(address, parent, properties);
        }

        //
        // Створюємо глобально properties рекурсивно з усіх parent //
        //

        if (parent != null) {
            properties = createGlobalPlaceholdersRecursive(parent).addAll(properties);
            properties.selfParse(100, MAVEN_PROPERTIES_LAYOUT);
        }

        //
        // Завантажуємо BOM //
        //

        List<ArtifactAddress> bombSources = new ArrayList<>();
        List<ArtifactAddress> bombArtifacts = new ArrayList<>();

        for (var section : матьЕбал(projectData, "dependencyManagement", "dependencies")) {

            ArtifactAddress bomAddress;
            try {
                bomAddress = ArtifactAddress.fromPom(section, properties);
            }

            catch (IllegalArgumentException e) {
                throw new PomResolutionException("An invalid BOM at index " + section.key() + " -> " + e.getMessage());
            }

            boolean isSource = Objects.equals(section.node("scope").getString(), "import");

            if (isSource) {
                bombSources.add(bomAddress);
            }

            else {
                bombArtifacts.add(bomAddress);
            }

        }

        //
        // Нарешті, завантажуємо залежності //
        //

        List<IPomData> dependencies = new ArrayList<>();
        for (ConfigurationNode node : матьЕбал(projectData, "dependencies")) {

            IPomData dependency = resolveDependency(node, repositories, parent, bombSources, bombArtifacts, properties);
            if (dependency == null) {
                continue;
            }

            dependencies.add(dependency);

        }

        return new PomData(repository, address, projectData, repositories, propertiesMap, parent, bombSources, bombArtifacts, dependencies);

    }

    private @Nullable IPomData resolveDependency(
            @NotNull ConfigurationNode section,
            @NotNull List<String> repositories,
            @Nullable IPomData parent,
            @NotNull List<ArtifactAddress> bombSources,
            @NotNull List<ArtifactAddress> bombArtifacts,
            @NotNull Placeholders properties
    ) throws PomResolutionException {

        String scope = section.node("scope").getString();
        boolean optional = section.node("optional").getBoolean();

        if (scope != null && !scope.equals("compile") && !scope.equals("runtime") || optional) {
            return null;
        }

        IncompleteArtifactAddress incompleteArtifactAddress;
        try {
            incompleteArtifactAddress = IncompleteArtifactAddress.fromPom(section, properties);
        }

        catch (IllegalArgumentException e) {
            throw new PomResolutionException("An invalid dependency at index " + section.key() + " -> " + e.getMessage());
        }

        ArtifactAddress completeDependencyAddress;
        try {
            completeDependencyAddress = findCompleteArtifact(repositories, incompleteArtifactAddress, parent, bombSources, bombArtifacts, properties);
        }

        catch (PomResolutionException e) {
            throw new PomResolutionException("Dependency `" + incompleteArtifactAddress + "` has no version defined. Tried tp find version in BOM, but an exception occurred", e);
        }

        if (completeDependencyAddress == null) {
            throw new PomResolutionException("Dependency `" + incompleteArtifactAddress + "` has no version defined. Tried to find version in BOM, no results found");
        }

        IPomData dependency;
        try {
            dependency = retrievePom(repositories, completeDependencyAddress);
        }

        catch (PomResolutionException e) {
            throw new PomResolutionException("Failed to resolve dependency `" + completeDependencyAddress + "`", e);
        }

        return dependency;

    }

    // Розробники Configurate кончені підараси, я їбав їх у рот, пізду та сраку.
    // Не можна було блять зробити просто щоб section.childrenList() видавав усі ключі??? Треба блять обов'язково childrenMap і isList()???
    // Кончені довбойоби блять.
    private @NotNull List<? extends ConfigurationNode> матьЕбал(@NotNull ConfigurationNode node, @NotNull String... target) {

        ConfigurationNode section = node.node((Object[]) target);
        if (section.virtual()) {
            return new ArrayList<>();
        }

        if (section.isList()) {
            return section.childrenList();
        }

        var sex = new ArrayList<>(section.childrenMap().values());
        if (sex.isEmpty()) {
            return new ArrayList<>();
        }

        return List.of(sex.getFirst());

    }


    private @Nullable ArtifactAddress findCompleteArtifact(
            @NotNull List<String> repositories,
            @NotNull IncompleteArtifactAddress address,
            @Nullable IPomData parent,
            @Nullable List<ArtifactAddress> bombSources,
            @Nullable List<ArtifactAddress> bombArtifacts,
            @NotNull Placeholders properties
    ) throws PomResolutionException {

        if (address.version.isPresent()) {
            return new ArtifactAddress(address.group, address.artifact, address.version.get());
        }

        ArtifactAddress result = null;

        // Спочатку шукаємо артефакт у деклараціях артефактів BOM.
        if (bombArtifacts != null) {
            result = bombArtifacts.stream()
                    .filter(artifact -> artifact.group().equals(address.group()) && artifact.artifact().equals(address.artifact()))
                    .map(artifact -> artifact.applyProperties(properties))
                    .findAny()
                    .orElse(null);
        }

        // Якщо не знайшли, рекурсивно пробігаємось по BOMb sources.
        if (result == null && bombSources != null) {

            for (ArtifactAddress source : bombSources) {

                IPomData pom;
                try {
                    pom = retrievePom(repositories, source);
                }

                catch (PomResolutionException e) {
                    throw new PomResolutionException("Failed to resolve BOM source `" + source + "`");
                }

                result = findCompleteArtifact(pom.getDeclaredRepositories(), address, null, pom.getBOMbSources(), pom.getBOMbArtifacts(), properties);
                if (result != null) {
                    break;
                }

            }

        }

        // Якщо все ще не знайшли, дивимось у parent
        if (result == null && parent != null) {
            result = findCompleteArtifact(parent.getDeclaredRepositories(), address, parent.getParent(), parent.getBOMbSources(), parent.getBOMbArtifacts(), properties);
        }

        return result;

    }

    private @NotNull Placeholders createGlobalPlaceholdersRecursive(@NotNull IPomData pom) {

        Placeholders result = new Placeholders();
        IPomData current = pom;

        while (current != null) {

            Placeholders properties = new Placeholders();
            createMergePomProperties(pom.getAddress(), pom.getParent(), properties);

            for (var entry : current.getProperties().entrySet()) {

                String key = entry.getKey();
                String value = entry.getValue();

                if (result.contains(key)) {
                    continue;
                }

                result.add(key, value);

            }

            result.selfParse(100, MAVEN_PROPERTIES_LAYOUT);
            current = current.getParent();

        }

        return result;

    }

    private void createMergePomProperties(
            @NotNull ArtifactAddress address,
            @Nullable IPomData parent,
            @NotNull Placeholders properties
    ) {

        String version = address.version().toString();
        properties.add("project.version", version);
        properties.add("version", version);

        String groupId = address.group();
        properties.add("project.groupId", groupId);
        properties.add("groupId", groupId);

        String artifactId = address.artifact();
        properties.add("project.artifactId", artifactId);
        properties.add("artifactId", artifactId);

        if (parent != null) {

            String parentVersion = parent.getAddress().version().toString();
            properties.add("parent.version", parentVersion);
            properties.add("project.parent.version", parentVersion);

            String parentGroupId = address.group();
            properties.add("project.parent.groupId", parentGroupId);
            properties.add("parent.groupId", parentGroupId);

            String parentArtifactId = address.artifact();
            properties.add("project.parent.artifactId", parentArtifactId);
            properties.add("parent.artifactId", parentArtifactId);

        }

        properties.selfParse(100, MAVEN_PROPERTIES_LAYOUT);

    }

    private record IncompleteArtifactAddress(@NotNull String group, @NotNull String artifact, @NotNull Optional<SemanticVersion> version) {

        public static @NotNull IncompleteArtifactAddress fromPom(@NotNull ConfigurationNode section, @Nullable Placeholders properties) {

            String group = section.node("groupId").getString();
            String artifact = section.node("artifactId").getString();
            String versionRaw = section.node("version").getString();

            if (group == null) {
                throw new IllegalArgumentException("Invalid POM section, groupId not found");
            }

            if (artifact == null) {
                throw new IllegalArgumentException("Invalid POM section, artifactId not found");
            }

            if (properties != null) {

                group = properties.parse(group, ILibrariesManager.MAVEN_PROPERTIES_LAYOUT);
                artifact = properties.parse(artifact, ILibrariesManager.MAVEN_PROPERTIES_LAYOUT);

                if (versionRaw != null) {
                    versionRaw = properties.parse(versionRaw, ILibrariesManager.MAVEN_PROPERTIES_LAYOUT);
                }

            }

            SemanticVersion version = null;
            if (versionRaw != null) {

                try {
                    version = SemanticVersion.fromString(versionRaw);
                }

                catch (Exception e) {
                    throw new IllegalArgumentException("Invalid POM: " + e.getMessage());
                }

            }

            return new IncompleteArtifactAddress(group, artifact, Optional.ofNullable(version));

        }

    }

    //
    // CLASSLOADER
    //

    // Переналаштовуємо ClassLoader бібліотеки.
    // 1. Повністю очищаємо усі налаштування, які встановив SimpleLibrariesDownloader.
    // 2. Налаштовуємо вже повноцінну систему доступів до класів.
    private void configureLibraryClassLoader(@NotNull ILibrary library, @NotNull DynamicClassLoader classLoader) {
        classLoader.resetSuppliers();
        classLoader.addClassSupplier("dependencies", name -> requestClass(library, name, false));
        classLoader.addResourceSupplier("SPI", n -> n.startsWith("META-INF/services/"), this::findGlobalSPIMetaInf);
    }

    public @Nullable Class<?> requestClass(@NotNull ILibrary library, @NotNull String name, boolean inside) {
        return requestClass0(library, name, inside, new HashSet<>());
    }

    private @Nullable Class<?> requestClass0(@NotNull ILibrary library, @NotNull String name, boolean inside, @NotNull Set<ILibrary> visited) {

        // Защита от бесконечного цикла (StackOverflowError) между А и Б
        if (!visited.add(library)) {
            return null;
        }

        DynamicClassLoader libraryClassLoader = (DynamicClassLoader) library.getClassLoader();

        // 1. Ищем локально в самом JAR-нике библиотеки
        if (inside) {
            Class<?> result = libraryClassLoader.getClass(name, false, false);
            if (result != null) {
                return result;
            }
        }

        // 2. Ищем в зависимостях
        List<ILibrary> maxDependencies = library.getDependencies();

        // Подтягиваем связанные через settings.yml / обратные зависимости
        List<ILibrary> connectedLibs = libraryMap.values().stream()
                .filter(lib -> lib.getDependencies().contains(library))
                .toList();
        maxDependencies.addAll(connectedLibs);

        for (ILibrary dependency : maxDependencies) {
            Class<?> result = requestClass0(dependency, name, true, visited);
            if (result != null) {
                return result;
            }
        }

        return null;

    }

    private @Nullable Class<?> rootClassRequest(@NotNull String name) {

        for (ILibrary library : sbdsLibraries) {

            Class<?> clazz = requestClass(library, name, true);
            if (clazz != null) {
                return clazz;
            }

        }

        if (sbds == null) {
            return null;
        }

        IModuleManager moduleManager = sbds.getModuleManager();
        if (!moduleManager.isValid()) {
            return null;
        }

        for (IModule module : sbds.getModuleManager().getModules()) {

            Module module0 = (Module) module;
            DynamicClassLoader classLoader = module0.getClassLoader();

            Class<?> clazz = classLoader.getClass(name, false, false);
            if (clazz != null) {
                return clazz;
            }

        }

        return null;

    }

    private List<URL> findGlobalSPIMetaInf(@NotNull String name) {

        List<URL> result = new ArrayList<>();
        for (var lib : this.libraryMap.values()) {
            DynamicClassLoader classLoader = (DynamicClassLoader) lib.getClassLoader();
            result.addAll(classLoader.findResources(name, false, false));
        }

        return result;

    }

    //
    // MISC
    //

    @Override
    public @NotNull ClassLoader getRootClassLoader() {
        return rootClassLoader;
    }

}
