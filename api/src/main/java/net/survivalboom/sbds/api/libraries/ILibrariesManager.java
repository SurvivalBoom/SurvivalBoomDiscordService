package net.survivalboom.sbds.api.libraries;

import net.survivalboom.sbds.api.utils.valid.IManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Менеджер бібілотек SurvivalBoom Discord Service.
 * Займається завантаженням необхідних бібліотек та бібліотек для модулів.
 * Також відіграє важливу роль у взаємодії ClassLoader бібілотек (пошук класів, ресурсів)
 */
public interface ILibrariesManager extends IManager {

    /**
     * Стандартне посилання на репозиторії Maven.
     */
    String MAVEN_REPO_URL = "https://repo1.maven.org/maven2/";

    /**
     * Шаблон для плейсхолдерів з maven properties.
     */
    String MAVEN_PROPERTIES_LAYOUT = "${$p$}";


    /**
     * @return Повертає головний ClassLoader для SurvivalBoom Discord Service.
     */

    @NotNull ClassLoader getRootClassLoader();

    //
    // LIBRARIES
    //

    // DOWNLOAD //

    @NotNull MassLibraryDownloadResult satisfy(@NotNull LibrarySatisfyConfiguration configuration);

    /**
     * Завантажує вказану бібліотеку на диск.
     * Ви можете вказати закріплені артефакти, щоб бібліотека використовувала саме вказану версію бібліотек для своїх залежностей.
     * Якщо jar файл бібліотеки вже існує, але бібліотека ще не була завантажена, буде використаний такий файл замість завантаження з репозиторія.
     * @param pom POM файл бібліотеки
     * @param findOptimal Увімкнути пошук схожих по версії бібліотек замість завантаження нових.
     * @return Завантажена бібліотека.
     * @throws LibraryDownloadException Якщо щось пішло не так під час завантаження бібліотеки.
     * @throws IllegalStateException Якщо така бібліотека вже завантажена.
     */
    @NotNull ILibrary downloadLibrary(
            @NotNull IPomData pom,
            @Nullable Collection<LibraryDeclaration> pinnedArtifacts,
            @Nullable Map<ArtifactAddress, LibraryDeclaration> relocations,
            boolean findOptimal
    ) throws LibraryDownloadException;

    // GETTERS //

    @Nullable ILibrary getLoadedLibrary(@NotNull ArtifactAddress address);

    default @Nullable ILibrary getLoadedLibrary(@NotNull IPomData pom) {
        return getLoadedLibrary(pom.getAddress());
    }

    default @Nullable ILibrary getLoadedLibrary(@NotNull String gradleString) {
        return getLoadedLibrary(ArtifactAddress.fromGradleString(gradleString));
    }

    @NotNull Map<ArtifactAddress, ILibrary> getLoadedLibraries();

    //
    // POM
    //

    // RETRIEVE //

    @NotNull IPomData retrievePom(@NotNull String repository, @NotNull ArtifactAddress address) throws PomResolutionException;

    default @NotNull IPomData retrievePom(@NotNull Collection<String> repositories, @NotNull ArtifactAddress address) throws PomResolutionException {

        if (repositories.isEmpty()) {
            throw new IllegalArgumentException("repositories are empty");
        }

        PomResolutionException lastException = null;
        for (String repo : repositories) {

            IPomData pom;

            try {
                pom = retrievePom(repo, address);
            }

            catch (PomResolutionException e) {
                lastException = e;
                continue;
            }

            return pom;

        }

        throw lastException;

    }

    default @NotNull IPomData retrievePom(@NotNull LibraryDeclaration declaration) throws PomResolutionException {

        Objects.requireNonNull(declaration, "declaration == null");

        ArtifactAddress address = declaration.address();
        String repository = declaration.source();

        if (repository == null) {
            repository = MAVEN_REPO_URL;
        }

        return retrievePom(repository, address);

    }

    default @NotNull IPomData retrievePom(@NotNull String repository, @NotNull String gradleString) throws PomResolutionException {
        return retrievePom(repository, ArtifactAddress.fromGradleString(gradleString));
    }

    // GETTERS //

    @Nullable IPomData getLoadedPom(@NotNull ArtifactAddress address);

    default @Nullable IPomData getLoadedPom(@NotNull String gradleString) {
        return getLoadedPom(ArtifactAddress.fromGradleString(gradleString));
    }

    @NotNull Map<ArtifactAddress, IPomData> getLoadedPoms();


    record MassLibraryDownloadResult(
            @NotNull LibrarySatisfyConfiguration request,
            @NotNull List<ILibrary> downloaded,
            @NotNull List<ILibrary> skipped,
            @NotNull Map<LibraryDeclaration, Exception> failed
    ) {

        public @NotNull List<ILibrary> list() {

            List<ILibrary> list = new ArrayList<>(downloaded);
            list.addAll(skipped);

            return list;

        }

    }

}
