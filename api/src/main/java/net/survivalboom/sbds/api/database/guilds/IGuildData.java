package net.survivalboom.sbds.api.database.guilds;

import net.dv8tion.jda.api.entities.Guild;
import net.survivalboom.sbds.api.utils.container.INamespacedDataContainer;
import net.survivalboom.sbds.api.translations.ITranslation;
import net.survivalboom.sbds.api.utils.valid.IValid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public interface IGuildData extends IValid {

    @NotNull IGuildDataManager getManager();

    // GUILD //

    @NotNull Guild getGuild();

    // TRANSLATION //

    @Nullable ITranslation getTranslation();

    // DATA //

    @NotNull INamespacedDataContainer container();

    void save();

    @NotNull CompletableFuture<Void> delete();

}
