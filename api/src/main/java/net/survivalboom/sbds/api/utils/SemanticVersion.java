package net.survivalboom.sbds.api.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record SemanticVersion(int major, int minor, int patch, @Nullable String release, @Nullable String original) implements Comparable<SemanticVersion> {

    private static final Pattern PATTERN = Pattern.compile(
            "^(?<major>\\d+)(?:\\.(?<minor>\\d+))?(?:\\.(?<patch>\\d+))?"
    );

    public SemanticVersion {

        if (major < 0) {
            throw new IllegalArgumentException("major < 0");
        }

        if (minor < 0) {
            throw new IllegalArgumentException("minor < 0");
        }

        if (patch < 0) {
            throw new IllegalArgumentException("patch < 0");
        }

    }

    @Override
    public int compareTo(@NotNull SemanticVersion o) {

        if (this.major != o.major) return Integer.compare(this.major, o.major);
        if (this.minor != o.minor) return Integer.compare(this.minor, o.minor);
        if (this.patch != o.patch) return Integer.compare(this.patch, o.patch);

        // Логика сравнения prerelease (помните, что версия БЕЗ prerelease СТАРШЕ, чем С prerelease)
        if (this.release == null && o.release != null) return 1;
        if (this.release != null) {

            if (o.release == null) {
                return -1;
            }

            return this.release.compareTo(o.release); // Грубое сравнение строк для альф/бет

        }

        return 0;

    }

    @Override
    public String toString() {

        if (original != null) {
            return original;
        }

        return major + "." + minor + "." + patch;

    }

    //
    // STATIC
    //

    public static @NotNull SemanticVersion fromString(@NotNull String string) {

        Objects.requireNonNull(string, "string == null");
        String trimmed = string.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException("String is blank!");
        }

        trimmed = killFuckingVersionRanges(trimmed);

        // 1. Ищем числовую часть (MAJOR.MINOR.PATCH) в начале строки
        Matcher matcher = PATTERN.matcher(trimmed);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid version format, no digits found at start: `" + string + "`");
        }

        int major = Integer.parseInt(matcher.group("major"));

        // Если MINOR или PATCH нет (как в "2.14"), ставим им 0
        String minorGroup = matcher.group("minor");
        int minor = (minorGroup != null) ? Integer.parseInt(minorGroup) : 0;

        String patchGroup = matcher.group("patch");
        int patch = (patchGroup != null) ? Integer.parseInt(patchGroup) : 0;

        // 2. Всё, что осталось ПОСЛЕ цифр — это суффикс (release/qualifier)
        String remainder = trimmed.substring(matcher.end()).trim();
        String release = null;

        if (!remainder.isEmpty()) {
            // Отрезаем ведущие точки или дефисы (например, ".Final" превращаем в "Final", "-alpha-1" в "alpha-1")
            if (remainder.startsWith(".") || remainder.startsWith("-")) {
                remainder = remainder.substring(1).trim();
            }
            if (!remainder.isEmpty()) {
                release = remainder;
            }
        }

        return new SemanticVersion(major, minor, patch, release, string);

    }

    private static String killFuckingVersionRanges(String rawVersion) {

        if (rawVersion == null) {
            return null;
        }

        // Если это диапазон вроде [2.17.1, 3) или [1.0.0]
        if (rawVersion.startsWith("[") || rawVersion.startsWith("(")) {
            // Убираем скобки и делим по запятой
            String clean = rawVersion.substring(1, rawVersion.length() - 1);
            String[] parts = clean.split(",");

            // Берем левую границу (минимальную версию)
            String baseVersion = parts[0].trim();

            // Если левая граница пустая (например, (,2.0.0]), берем правую
            if (baseVersion.isEmpty() && parts.length > 1) {
                baseVersion = parts[1].trim();
            }

            return baseVersion;
        }

        return rawVersion;
    }

}
