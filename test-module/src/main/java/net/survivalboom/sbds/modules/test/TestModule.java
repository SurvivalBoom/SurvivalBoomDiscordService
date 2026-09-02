package net.survivalboom.sbds.modules.test;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.survivalboom.sbds.api.modules.ModuleMain;
import net.survivalboom.sbds.modules.test.commands.*;
import net.survivalboom.sbds.modules.test.database.TestRecord;
import net.survivalboom.sbds.modules.test.events.EventListenerTest;
import net.survivalboom.sbds.modules.test.listeners.MessageReplier;

public class TestModule extends ModuleMain {

    @Override
    public void onEnable() {

        addModuleTranslations2("translation_uk.yml");

        registerCommand(new BanPrototypeCommand());
        registerCommand(new EphemeralCommand());
        registerCommand(new LongRespondingCommand());

        registerCommand(new TestMessageContext());
        registerCommand(new TestUserContext());

        registerCommand(new TestModalCommand());

        createGuildConfig(builder ->
            builder
                .setTranslation("testmodule.config")
                .addField("replier", TextChannel.class, null)
        );

        registerEvents(new MessageReplier(this));
        registerEvents(new EventListenerTest(this));

        getSbds().getDatabase().createRepository(this, TestRecord.class);

        getLogger().info("Бугага! Мєня включілі! Вам всім кабздєц!");

    }

    @Override
    public void onDisable() {
        getModule().getLogger().info("Нєєєєт!!! Міня віключілііі!!!");
    }

}