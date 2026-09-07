package net.survivalboom.sbds.modules.logging.module;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.survivalboom.sbds.api.modules.ModuleMain;
import net.survivalboom.sbds.modules.logging.api.ILoggingModule;
import net.survivalboom.sbds.modules.logging.api.storage.ILogDataManager;
import net.survivalboom.sbds.modules.logging.api.storage.ILogRecordData;
import net.survivalboom.sbds.modules.logging.module.kostily.InviteTracker;
import net.survivalboom.sbds.modules.logging.module.listeners.*;
import net.survivalboom.sbds.modules.logging.module.storage.LogDataManager;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LoggingModule extends ModuleMain implements ILoggingModule {

    private LogDataManager logDataManager;
    private InviteTracker inviteTracker;

    @Override
    public void onEnable() {

        checkAndLoadConfig();
        /*
        addModuleTranslations2(
                "translation_uk.yml",
                "translation_en.yml",
                "translation_ru.yml"
        );
         */

        this.logDataManager = new LogDataManager(this);
        this.logDataManager.init();

        this.inviteTracker = new InviteTracker(this);
        this.inviteTracker.init();

        registerEvents(this.inviteTracker);
        registerEvents(new MemberListener(this, this.inviteTracker));
        registerEvents(new MessageListener(this));
        registerEvents(new ChannelListener(this));
        registerEvents(new VoiceListener(this));
        registerEvents(new StageListener(this));

        registerService(ILoggingModule.class, this);
    }

    @Override
    public void onDisable() {
        this.inviteTracker.shutdown();
        this.logDataManager.shutdown();
    }

    @Override
    public @NotNull ILogDataManager getLogDataManager() {
        return logDataManager;
    }

    @Override
    public @NotNull CompletableFuture<List<ILogRecordData>> getUserHistory(@NotNull Guild guild, @NotNull User user) {
        return logDataManager.getHistory(guild, user);
    }
}