package net.survivalboom.sbds.modules.test.commands;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.base.CommandClass;
import net.survivalboom.sbds.api.commands.context.MessageContextCommandExecutor;
import net.survivalboom.sbds.api.commands.context.MessageContextInteractionInfo;
import org.jetbrains.annotations.NotNull;

@CommandClass(name = "test-context-message", description = "A command to test SBDS context commands", ephemeral = true)
public class TestMessageContext extends CommandBase implements MessageContextCommandExecutor {

    @Override
    public void executes(@NotNull MessageContextInteractionInfo info) {

        Message message = info.event().getTarget();

        String id = message.getId();
        User author = message.getAuthor();
        String text = message.getContentRaw();

        info.reply("testmodule.context.message")
                .withPlaceholders(
                        "id", id,
                        "author", author,
                        "text", text
                )
                .queue();

    }

}
