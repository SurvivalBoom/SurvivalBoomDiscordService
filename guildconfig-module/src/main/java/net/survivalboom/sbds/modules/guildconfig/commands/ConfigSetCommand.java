package net.survivalboom.sbds.modules.guildconfig.commands;

import net.dv8tion.jda.api.interactions.commands.Command;
import net.survivalboom.sbds.api.commands.argument.discord.UserArgument;
import net.survivalboom.sbds.api.commands.argument.discord.channel.TextChannelArgument;
import net.survivalboom.sbds.api.commands.argument.discord.channel.VoiceChannelArgument;
import net.survivalboom.sbds.api.commands.argument.primitive.BooleanArgument;
import net.survivalboom.sbds.api.commands.argument.primitive.IntegerArgument;
import net.survivalboom.sbds.api.commands.argument.primitive.StringArgument;
import net.survivalboom.sbds.api.commands.argument.sbds.GuildConfigArgument;
import net.survivalboom.sbds.api.commands.base.ArgumentMethod;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.base.CommandClass;
import net.survivalboom.sbds.api.commands.slash.SlashCommandExecutor;
import net.survivalboom.sbds.api.commands.slash.SlashExecutionInfo;
import net.survivalboom.sbds.api.database.guildconfig.GuildConfigField;
import net.survivalboom.sbds.api.database.guildconfig.IGuildConfig;
import net.survivalboom.sbds.api.database.guildconfig.IGuildConfigTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

@CommandClass(name = "set", description = "Set an option value for this guild", translationKey = "guildconfig.command.config.set", permission = "guilconfig.command.config.set")
public class ConfigSetCommand extends CommandBase implements SlashCommandExecutor {

    @Override
    public void executes(@NotNull SlashExecutionInfo info) {

        IGuildConfigTemplate template = info.arguments().getCast("config", IGuildConfigTemplate.class).orElseThrow();
        String key = info.arguments().getCast("key", String.class).orElseThrow();

        var args = new ArrayList<>(info.arguments().values());
        args.remove(template);
        args.remove(key);

        if (args.isEmpty()) {
            info.reply("guildconfig.command.config.set.empty").queue();
            return;
        }

        IGuildConfig config = template.obtainConfig(info.guild());

        GuildConfigField field = config.getField(key);
        if (field == null || field.internal()) {
            info.reply("guildconfig.command.config.set.invalid-key")
                    .withPlaceholders("option", template.getKey() + ":" + key)
                    .queue();
            return;
        }

        Object value = args.getFirst();

        if (value instanceof String string && string.equals("null")) {
            value = null;
        }

        if (field.type() == TimeZone.class && value instanceof String tzStr) {
            if (!Arrays.asList(TimeZone.getAvailableIDs()).contains(tzStr)) {
                String fieldTranslated = template.getTranslationKey() != null ? field.createTranslationKey(template) : template.getKey() + ":" + field.key();
                info.reply("guildconfig.command.config.set.invalid-value")
                        .withPlaceholders(
                                "option", field,
                                "option.translated", fieldTranslated,
                                "value", tzStr
                        )
                        .queue();
                return;
            }
            value = TimeZone.getTimeZone(tzStr);
        }

        String fieldTranslated = template.getTranslationKey() != null ? field.createTranslationKey(template) : template.getKey() + ":" + field.key();
        Object valueTranslated = value != null ? value : field.defaultValue();

        if (valueTranslated instanceof TimeZone tz) {
            valueTranslated = tz.getID();
        } else if (valueTranslated == null) {
            valueTranslated = "$[guildconfig.values.empty]";
        }

        if (!field.isValueAllowed(value)) {
            info.reply("guildconfig.command.config.set.invalid-value")
                    .withPlaceholders(
                            "option", field,
                            "option.translated", fieldTranslated,
                            "value", valueTranslated
                    )
                    .queue();
            return;
        }

        config.set(key, value).join();

        info.reply("guildconfig.command.config.set.success")
                .withPlaceholders(
                        "option", field,
                        "option.translated", template.getTranslationKey() != null ? field.createTranslationKey(template) : field.key(),
                        "value", valueTranslated
                )
                .queue();

    }

    //
    // ARGUMENTS
    //

    @ArgumentMethod
    public GuildConfigArgument config() {
        return new GuildConfigArgument();
    }

    @ArgumentMethod(index = 1)
    public StringArgument key() {
        return new StringArgument(context -> {

            IGuildConfigTemplate template = context.arguments().getCast("config", IGuildConfigTemplate.class).orElse(null);
            if (template == null) {
                return null;
            }

            return template.getFields().values().stream()
                    .filter(field -> !field.internal())
                    .map(field -> new Command.Choice(field.createTranslationKey(template), field.key()))
                    .toList();

        });
    }

    @ArgumentMethod(index = 2, required = false)
    public TextChannelArgument textchannel() {
        return new TextChannelArgument();
    }

    @ArgumentMethod(index = 2, required = false)
    public VoiceChannelArgument voicechannel() {
        return new VoiceChannelArgument();
    }

    @ArgumentMethod(index = 2, required = false)
    public UserArgument user() {
        return new UserArgument();
    }

    @ArgumentMethod(index = 2, required = false)
    public StringArgument string() {
        return new StringArgument();
    }

    @ArgumentMethod(index = 2, required = false)
    public StringArgument timezone() {
        return new StringArgument(context -> List.of(
                new Command.Choice("UTC", "UTC"),
                new Command.Choice("Europe/Kyiv", "Europe/Kyiv"),
                new Command.Choice("Europe/Warsaw", "Europe/Warsaw"),
                new Command.Choice("Europe/London", "Europe/London"),
                new Command.Choice("Europe/Berlin", "Europe/Berlin"),
                new Command.Choice("Europe/Paris", "Europe/Paris"),
                new Command.Choice("America/New_York", "America/New_York"),
                new Command.Choice("America/Chicago", "America/Chicago"),
                new Command.Choice("America/Los_Angeles", "America/Los_Angeles"),
                new Command.Choice("America/Sao_Paulo", "America/Sao_Paulo"),
                new Command.Choice("Asia/Istanbul", "Asia/Istanbul"),
                new Command.Choice("Asia/Dubai", "Asia/Dubai"),
                new Command.Choice("Asia/Kolkata", "Asia/Kolkata"),
                new Command.Choice("Asia/Shanghai", "Asia/Shanghai"),
                new Command.Choice("Asia/Tokyo", "Asia/Tokyo"),
                new Command.Choice("Australia/Sydney", "Australia/Sydney"),
                new Command.Choice("Pacific/Auckland", "Pacific/Auckland")
        ));
    }

    @ArgumentMethod(index = 2, required = false)
    public IntegerArgument integer() {
        return new IntegerArgument();
    }

    @ArgumentMethod(index = 2, required = false)
    public BooleanArgument bool() {
        return new BooleanArgument();
    }

}