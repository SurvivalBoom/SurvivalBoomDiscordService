package net.survivalboom.sbds.core.database.guilds;

import net.dv8tion.jda.api.entities.Guild;
import net.survivalboom.sbds.api.utils.container.INamespacedDataContainer;
import net.survivalboom.sbds.api.database.guilds.IGuildData;
import net.survivalboom.sbds.api.database.guilds.IGuildDataManager;
import net.survivalboom.sbds.api.translations.ITranslation;
import net.survivalboom.sbds.api.utils.valid.Valid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class GuildData extends Valid implements IGuildData {

    private final GuildDataManager manager;

    private final GuildDataRecord record;

    private final Guild guild;


    public GuildData(
            @NotNull GuildDataRecord record,
            @NotNull GuildDataManager manager
    ) {

        this.record = record;
        this.manager = manager;

        this.guild = manager.getSbds().getBot().getGuildById(record.getGuildId());

    }


    @Override
    public @NotNull IGuildDataManager getManager() {
        return manager;
    }

    // GUILD //

    @Override
    public @NotNull Guild getGuild() {
        return guild;
    }

    // TRANSLATION //

    @Override
    public @NotNull ITranslation getTranslation() {
        checkValid();

        var configTemplate = manager.getSbds().getGuildConfigManager().getTemplate("sbds:config");
        if (configTemplate != null) {
            var config = manager.getSbds().getGuildConfigManager().getGuildConfig(configTemplate, guild.getIdLong());

            String langKey = config.get("language", String.class, true).join().orElse("sbds:english");

            ITranslation translation = manager.getSbds().getTranslationManager().getTranslation(langKey);
            if (translation != null) {
                return translation;
            }
        }

        ITranslation defaultTranslation = manager.getSbds().getTranslationManager().getDefaultTranslation();
        if (defaultTranslation != null) {
            return defaultTranslation;
        }

        ITranslation fallbackTranslation = manager.getSbds().getTranslationManager().getFallbackTranslation();
        if (fallbackTranslation != null) {
            return fallbackTranslation;
        }

        throw new IllegalStateException("Critical error: No translations available in TranslationManager!");
    }

    // DATABASE //

    public @NotNull GuildDataRecord getRecord() {
        return record;
    }

    @Override
    public @NotNull INamespacedDataContainer container() {
        checkValid();
        return record.getData();
    }

    @Override
    public void save() {
        checkValid();
        manager.save(this);
    }

    @Override
    public @NotNull CompletableFuture<Void> delete() {
        checkValid();
        return manager.delete(this);
    }

    //
    // VALID
    //

    @Override
    protected void setValid(boolean v) {
        super.setValid(v);
    }

}