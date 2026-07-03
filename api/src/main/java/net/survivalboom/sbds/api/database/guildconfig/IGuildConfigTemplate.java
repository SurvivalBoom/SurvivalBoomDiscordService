package net.survivalboom.sbds.api.database.guildconfig;

import net.dv8tion.jda.api.entities.Guild;
import net.survivalboom.sbds.api.registrations.Registration;
import net.survivalboom.sbds.api.utils.valid.IValid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface IGuildConfigTemplate extends IValid {

    @NotNull IGuildConfigManager getManager();

    @NotNull Registration<IGuildConfigTemplate> getRegistration();

    @NotNull String getKey();

    @Nullable String getTranslationKey();

    @NotNull String createTranslationKey();


    @Nullable GuildConfigField getField(@NotNull String key);

    @NotNull Map<String, GuildConfigField> getFields();


    @NotNull IGuildConfig obtainConfig(long guildId);

    default @NotNull IGuildConfig obtainConfig(@NotNull Guild guild) {
        return obtainConfig(guild.getIdLong());
    }

}
