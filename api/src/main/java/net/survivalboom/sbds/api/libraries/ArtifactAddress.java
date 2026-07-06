package net.survivalboom.sbds.api.libraries;

import net.survivalboom.sbds.api.utils.SemanticVersion;
import net.survivalboom.sbds.api.utils.placeholders.Placeholders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.Objects;

public record ArtifactAddress(
        @NotNull String group,
        @NotNull String artifact,
        @NotNull SemanticVersion version
) {

    public ArtifactAddress {

        Objects.requireNonNull(group, "group == null");
        Objects.requireNonNull(artifact, "artifact == null");
        Objects.requireNonNull(version, "version == null");

    }

    public @NotNull String toGradleString() {
        return toGradleString(DEFAULT_GRADLE_SEPARATOR);
    }

    public @NotNull String toGradleString(@NotNull String separator) {
        return group + separator + artifact + separator + version;
    }

    public @NotNull String createRepositoryAddress(@NotNull String repository, @NotNull String fileType) {

        if (!repository.endsWith("/")) {
            repository += "/";
        }

        return repository + group.replace(".", "/") + "/" + artifact + "/" + version + "/" + artifact + "-" + version + "." + fileType;

    }

    public @NotNull ArtifactAddress applyProperties(@NotNull Placeholders properties) {

        String group = properties.parse(this.group);
        String artifact = properties.parse(this.artifact);
        String version = properties.parse(this.version.toString());

        return create(group, artifact, version);

    }

    @Override
    public String toString() {
        return toGradleString();
    }

    //
    // STATIC
    //

    public static final String DEFAULT_FILESYSTEM_SEPARATOR = "$";

    public static final String DEFAULT_GRADLE_SEPARATOR = ":";


    public static @NotNull ArtifactAddress create(
            @NotNull String group,
            @NotNull String artifact,
            @NotNull String version
    ) {
        return new ArtifactAddress(group, artifact, SemanticVersion.fromString(version));
    }

    public static @NotNull ArtifactAddress fromGradleString(@NotNull String string) {
        return fromGradleString(string, DEFAULT_GRADLE_SEPARATOR);
    }

    public static @NotNull ArtifactAddress fromGradleString(@NotNull String string, @NotNull String separator) {

        Objects.requireNonNull(string, "string == null");
        Objects.requireNonNull(separator, "separator == null");

        String[] parts = string.split("\\" + separator);
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid format; Separator: `" + separator + "`; Expected length: 3, Got " + parts.length + "; " + string);
        }

        String group = parts[0];
        String artifact = parts[1];
        String version = parts[2];

        return create(group, artifact, version);

    }

    public static @NotNull ArtifactAddress fromPom(@NotNull ConfigurationNode section, @Nullable Placeholders properties) {

        String group = section.node("groupId").getString();
        String artifact = section.node("artifactId").getString();
        String version = section.node("version").getString();

        if (group == null) {
            throw new IllegalArgumentException("Invalid POM section, groupId not found");
        }

        if (artifact == null) {
            throw new IllegalArgumentException("Invalid POM section, artifactId not found");
        }

        if (version == null) {
            throw new IllegalArgumentException("Invalid POM section, version not found");
        }


        if (properties != null) {

            group = properties.parse(group, ILibrariesManager.MAVEN_PROPERTIES_LAYOUT);
            artifact = properties.parse(artifact, ILibrariesManager.MAVEN_PROPERTIES_LAYOUT);
            version = properties.parse(version, ILibrariesManager.MAVEN_PROPERTIES_LAYOUT);

        }

        return create(group, artifact, version);

    }

}
