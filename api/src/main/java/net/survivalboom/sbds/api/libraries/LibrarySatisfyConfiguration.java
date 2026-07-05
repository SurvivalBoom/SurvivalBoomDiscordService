package net.survivalboom.sbds.api.libraries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.*;

public class LibrarySatisfyConfiguration {

    private final Set<LibraryDeclaration> libraries = new HashSet<>();

    private final Set<LibraryDeclaration> pinnedLibraries = new HashSet<>();

    private final Map<ArtifactAddress, LibraryDeclaration> relocations = new HashMap<>();

    public LibrarySatisfyConfiguration(
            @NotNull Collection<LibraryDeclaration> libraries,
            @Nullable Map<ArtifactAddress, LibraryDeclaration> relocations,
            @Nullable Collection<LibraryDeclaration> pinnedLibraries
    ) {

        Objects.requireNonNull(libraries, "libraries == null");
        this.libraries.addAll(libraries);

        if (relocations != null) {
            this.relocations.putAll(relocations);
        }

        if (pinnedLibraries != null) {
            this.pinnedLibraries.addAll(pinnedLibraries);
        }

    }


    public @NotNull List<LibraryDeclaration> getLibraries() {
        return new ArrayList<>(libraries);
    }

    public @NotNull List<LibraryDeclaration> getPinnedLibraries() {
        return new ArrayList<>(pinnedLibraries);
    }

    public @NotNull Map<ArtifactAddress, LibraryDeclaration> getRelocations() {
        return new HashMap<>(relocations);
    }

    public boolean isEmpty() {
        return libraries.isEmpty();
    }

    //
    // STATIC
    //

    public static @NotNull MassLoadResult fromSection(@NotNull ConfigurationNode section) {

        if (section.isList()) {

            var result = LibraryDeclaration.fromMultiSection(section);

            LibrarySatisfyConfiguration configuration = new LibrarySatisfyConfiguration(
                    result.loaded(),
                    null,
                    null
            );

            return new MassLoadResult(configuration, new HashMap<>(), result.failed(), new HashMap<>());

        }

        ConfigurationNode relocationsSection = section.node("relocations");
        Map<String, Exception> relocationsErrors = new HashMap<>();
        Map<ArtifactAddress, LibraryDeclaration> relocations = new HashMap<>();

        for (ConfigurationNode node : relocationsSection.childrenList()) {

            String str = node.getString();
            if (str != null) {

                String[] args = str.split("->");
                if (args.length != 2) {
                    relocationsErrors.put(str, new IllegalArgumentException("Invalid relocation `" + str + "`"));
                    continue;
                }

                String originRaw = args[0].trim();
                String destinationRaw = args[1].trim();

                ArtifactAddress origin;
                try {
                    origin = ArtifactAddress.fromGradleString(originRaw);
                }

                catch (IllegalArgumentException e) {
                    relocationsErrors.put(originRaw, e);
                    continue;
                }

                LibraryDeclaration destination;
                try {
                    ArtifactAddress addr = ArtifactAddress.fromGradleString(destinationRaw);
                    destination = new LibraryDeclaration(addr, null);
                }

                catch (IllegalArgumentException e) {
                    relocationsErrors.put(destinationRaw, e);
                    continue;
                }

                relocations.put(origin, destination);
                continue;

            }

            String targetRaw = node.node("target").getString("null");

            ArtifactAddress target;
            LibraryDeclaration destination;
            try {
                target = ArtifactAddress.fromGradleString(targetRaw);
                destination = LibraryDeclaration.fromSection(node);
            }

            catch (IllegalArgumentException e) {
                relocationsErrors.put(String.valueOf(node.key()), e);
                continue;
            }

            relocations.put(target, destination);

        }

        ConfigurationNode pinnedSection = section.node("pinned");
        var pinnedResult = LibraryDeclaration.fromMultiSection(pinnedSection);

        ConfigurationNode declarationsSection = section.node("dependencies");
        var declarationsResult = LibraryDeclaration.fromMultiSection(declarationsSection);

        LibrarySatisfyConfiguration configuration = new LibrarySatisfyConfiguration(
                declarationsResult.loaded(),
                relocations,
                pinnedResult.loaded()
        );

        return new MassLoadResult(configuration, relocationsErrors, declarationsResult.failed(), pinnedResult.failed());

    }

    public record MassLoadResult(
            @NotNull LibrarySatisfyConfiguration result,
            @NotNull Map<String, Exception> relocationsFailed,
            @NotNull Map<String, Exception> declarationsFailed,
            @NotNull Map<String, Exception> pinnedFailed
    ) {}


}
