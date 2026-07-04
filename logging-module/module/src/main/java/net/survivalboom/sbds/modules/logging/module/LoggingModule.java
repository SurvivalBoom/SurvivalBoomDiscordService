package net.survivalboom.sbds.modules.logging.module;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.survivalboom.sbds.modules.logging.module.events.*;
import net.survivalboom.sbds.api.modules.ModuleMain;
import net.survivalboom.sbds.modules.logging.api.ILoggedMessage;
import net.survivalboom.sbds.modules.logging.api.ILoggingModule;
import net.survivalboom.sbds.modules.logging.module.logging.MessageManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


import java.util.concurrent.CompletableFuture;

public class LoggingModule extends ModuleMain implements ILoggingModule {

    // Окрема подяка розробникам дискорда за всрате API де не можна отримати половину інформації про евент і треба танцвюати з бубном.
    private final MessageManager messageManager = new MessageManager(this);;

    @Override
    public void onEnable() {

        checkAndLoadConfig();
        addModuleTranslations2(
                "translation_uk.yml",
                "translation_en.yml",
                "translation_ru.yml"
        );

        messageManager.init();

        setupGuildConfig();

        // Як же круто писати цей модуль коли API ще не дописано і не має документації
        // YOU ARE DEPRECATED <-- No, you are! KUUUURRRRWAAAA - TIMURishche

        registerService(ILoggingModule.class, this);

        registerEvents(new MessageReceiveListener(this));
        registerEvents(new DeleteListener(this));
        registerEvents(new EditListener(this));
        registerEvents(new MemberListener(this));
        registerEvents(new StageListener(this));
        registerEvents(new VoiceListener(this));

        getLogger().info("Logging Module has been enabled.");

    }

    @Override
    public void onDisable() {
        messageManager.shutdown();
        getLogger().info("Logging Module has been disabled.");
    }

    private void setupGuildConfig() {
        createGuildConfig(builder -> {
            builder.setTranslation("logging.config");

            builder.addField("enabled", Boolean.class, false);
            builder.addField("channel", TextChannel.class, null);

            builder.addField("events.message", Boolean.class, true);
            builder.addField("events.message.edit", Boolean.class, true);
            builder.addField("events.message.delete", Boolean.class, true);

            builder.addField("events.member", Boolean.class, true);
            builder.addField("events.member.join", Boolean.class, true);
            builder.addField("events.member.leave", Boolean.class, true);
            builder.addField("events.member.nickname", Boolean.class, true);
            builder.addField("events.member.role_add", Boolean.class, true);
            builder.addField("events.member.role_remove", Boolean.class, true);

            builder.addField("events.voice", Boolean.class, true);
            builder.addField("events.voice.join", Boolean.class, true);
            builder.addField("events.voice.leave", Boolean.class, true);
            builder.addField("events.voice.move", Boolean.class, true);

            builder.addField("events.stage", Boolean.class, true);
            builder.addField("events.stage.start", Boolean.class, true);
            builder.addField("events.stage.end", Boolean.class, true);
            builder.addField("events.stage.topic", Boolean.class, true);

            // Сєкрєтікі
            builder.addField("database_logging", Boolean.class, false, true);
        });
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    @Override
    public @NotNull CompletableFuture<@Nullable ILoggedMessage> getCachedMessage(long messageId) {
        return messageManager.getCachedMessage(messageId);
    }

}
