package net.survivalboom.sbds.core.interaction.command;

import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.localization.LocalizationFunction;
import net.survivalboom.sbds.api.commands.Command;
import net.survivalboom.sbds.api.commands.CommandArgument;
import net.survivalboom.sbds.api.commands.argument.misc.SubCommandArgument;
import net.survivalboom.sbds.api.messages.template.IMessageTemplate;
import net.survivalboom.sbds.api.messages.template.TextMessageTemplate;
import net.survivalboom.sbds.api.translations.CommandTranslationScope;
import net.survivalboom.sbds.api.translations.ITranslation;
import net.survivalboom.sbds.core.translations.TranslationManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class CommandLocalizator {

    private static final Logger log = LoggerFactory.getLogger(CommandLocalizator.class.getSimpleName());

    private static final Pattern OPTION_PATTERN = Pattern.compile("^[a-zа-яіїєґ-]+$");

    private static final Set<String> WARNED_KEYS = ConcurrentHashMap.newKeySet();

    private static final Set<String> WARNED_MISSING_PER_FILE = ConcurrentHashMap.newKeySet();

    private final TranslationManager translationManager;


    public CommandLocalizator(@NotNull TranslationManager translationManager) {
        this.translationManager = translationManager;
    }


    private @Nullable TranslationResult getLocalizationKey(@NotNull Command rootCommand, @NotNull String request) {

        String[] parts = request.split("\\.");
        if (parts.length < 2) {
            return null;
        }

        Command current = rootCommand;
        int index = 0;

        while (index < parts.length) {

            String part = parts[index];

            if (part.equals("options")) {
                break;
            }

            if (!part.equals(current.getName())) {

                Command next = current.getArguments().stream()
                        .filter(argument -> argument.argument() instanceof SubCommandArgument)
                        .flatMap(argument -> ((SubCommandArgument) argument.argument()).getSubcommands().stream())
                        .filter(c -> c.getName().equals(part))
                        .findAny().orElse(null);

                if (next == null) {
                    break;
                }

                current = next;

            }

            index++;

        }

        if (index == parts.length - 1 && parts[index].equals("description")) {
            return current.getTranslationKey() != null ? of(current.getTranslationKey() + ".description", request) : null;
        }

        if (index < parts.length && parts[index].equals("options")) {

            index++;
            if (index >= parts.length) return null;

            String argName = parts[index++];
            CommandArgument argument = current.getArguments().stream()
                    .filter(a -> a.name().equals(argName))
                    .findAny().orElse(null);

            if (argument == null || argument.translationKey() == null) return null;

            if (index < parts.length && parts[index].equals("choices")) {
                index++;
                if (index >= parts.length) return null;

                String choiceName = parts[index];
                return of(argument.translationKey() + ".choices." + choiceName, request);
            }

            if (index < parts.length) {
                String type = parts[index];
                return of(argument.translationKey() + "." + type, request);
            }

            return null;

        }

        return null;
    }


    public @NotNull LocalizationFunction createLocalizationFunction(@NotNull Command command) {

        return key -> {

            Map<DiscordLocale, String> map = new HashMap<>();

            TranslationResult result = getLocalizationKey(command, key);
            if (result != null) {

                String translationKey = result.key();

                for (ITranslation translation : translationManager.getTranslations()) {

                    String translationName = translation.getRegistration().key().toString();
                    DiscordLocale locale = translation.getDiscordLocale();

                    if (locale == null) continue;

                    IMessageTemplate template = translation.getMessage(translationKey);

                    if (template == null) {
                        String warnId = translationName + ":" + translationKey;
                        if (WARNED_MISSING_PER_FILE.add(warnId)) {
                            log.warn("Missing translation for `{}` in `{}`.", translationKey, translationName);
                        }
                        continue;
                    }

                    if (!(template instanceof TextMessageTemplate txt)) {
                        log.warn("Translation for `{}` in `{}` must be a plain text.", translationKey, translationName);
                        continue;
                    }

                    String str = txt.getContent();
                    if (result.scope == CommandTranslationScope.OPTION_NAME && !checkOptionRegex(str)) {
                        log.error("Invalid option name `{}` ({}) from `{}`. Must match the regex `^[a-zа-яіїєґ-]+$` and be between 1 and 32.", str, translationKey, translationName);
                        continue;
                    }

                    if (str.isEmpty() || str.length() > 100) {
                        log.error("Invalid translation `{}` ({}) from `{}`. Must be between 1 and 100.", str, translationKey, translationName);
                        continue;
                    }

                    map.put(locale, str);

                }

                if (map.isEmpty()) {
                    if (WARNED_KEYS.add(translationKey)) {
                        log.warn("No translations found for `{}` in ANY file ({}).", translationKey, key);
                    }
                }

            }

            return map;

        };

    }

    private static TranslationResult of(@NotNull String sbdsTranslationKey, @NotNull String key) {

        boolean name = key.endsWith("name");
        boolean choice = key.contains("choices");
        boolean option = !choice && key.contains("options");

        CommandTranslationScope scope;
        if (choice) {
            scope = CommandTranslationScope.CHOICE_NAME;
        }

        else if (option) {
            scope = name ? CommandTranslationScope.OPTION_NAME : CommandTranslationScope.OPTION_DESCRIPTION;
        }

        else {
            scope = name ? CommandTranslationScope.COMMAND_NAME : CommandTranslationScope.COMMAND_DESCRIPTION;
        }

        return new TranslationResult(sbdsTranslationKey, scope);

    }

    private static boolean checkOptionRegex(String str) {
        return OPTION_PATTERN.matcher(str).matches() && !str.isEmpty() && str.length() <= 31;
    }

    record TranslationResult(@NotNull String key, @NotNull CommandTranslationScope scope) {}

}