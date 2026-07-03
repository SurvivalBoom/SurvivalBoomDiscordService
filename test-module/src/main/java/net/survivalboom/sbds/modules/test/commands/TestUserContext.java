package net.survivalboom.sbds.modules.test.commands;

import net.dv8tion.jda.api.entities.User;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.base.CommandClass;
import net.survivalboom.sbds.api.commands.context.UserContextCommandExecutor;
import net.survivalboom.sbds.api.commands.context.UserContextInteractionInfo;
import org.jetbrains.annotations.NotNull;

@CommandClass(name = "test-user", description = "A command to test SBDS context commands", ephemeral = true)
public class TestUserContext extends CommandBase implements UserContextCommandExecutor {

    @Override
    public void executes(@NotNull UserContextInteractionInfo info) {
        User user = info.user();
        info.reply("testmodule.context.user").withPlaceholders("user", user).queue();
    }

}
