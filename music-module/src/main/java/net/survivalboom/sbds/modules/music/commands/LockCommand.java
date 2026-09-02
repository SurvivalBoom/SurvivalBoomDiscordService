package net.survivalboom.sbds.modules.music.commands;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.survivalboom.sbds.api.ISBDS;
import net.survivalboom.sbds.api.commands.ArgumentScope;
import net.survivalboom.sbds.api.commands.argument.Argument;
import net.survivalboom.sbds.api.commands.argument.discord.channel.VoiceChannelArgument;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.base.CommandClass;
import net.survivalboom.sbds.api.commands.base.ArgumentMethod;
import net.survivalboom.sbds.api.commands.console.ConsoleCommandExecutor;
import net.survivalboom.sbds.api.commands.console.ConsoleExecutionInfo;
import net.survivalboom.sbds.api.commands.slash.SlashCommandExecutor;
import net.survivalboom.sbds.api.commands.slash.SlashExecutionInfo;
import net.survivalboom.sbds.api.commands.string.StringCommandExecutor;
import net.survivalboom.sbds.api.commands.string.StringExecutionInfo;
import net.survivalboom.sbds.api.interaction.InteractionHolder;
import net.survivalboom.sbds.api.permissions.Permission;
import net.survivalboom.sbds.modules.music.MusicModule;
import net.survivalboom.sbds.modules.music.music.MusicManager;
import net.survivalboom.sbds.modules.music.music.GuildPlayer;
import net.survivalboom.sbds.modules.music.utils.Utils;
import org.jetbrains.annotations.NotNull;

@CommandClass(
        name = "music-lock",
        description = "Locks current music bot for staff usage only",
        translationKey = "music.command.lock",
        permission = "music.command.lock"
)
public class LockCommand extends CommandBase implements SlashCommandExecutor, StringCommandExecutor, ConsoleCommandExecutor {

    private final MusicModule module;

    private final MusicManager manager;

    public LockCommand(@NotNull MusicModule module) {
        this.module = module;
        this.manager = module.getMusicManager();
    }

    @Override
    protected void init(@NotNull ISBDS sbds) {

        sbds.getComponentInteractionManager().registerListener(
                module,
                "lock",
                ButtonInteractionEvent.class,
                click -> executes0(click, true),
                new Permission("music.command.lock", true)
        );

    }

    @Override
    public void executes(@NotNull SlashExecutionInfo info) {
        executes0(info, false);
    }

    @Override
    public void executes(@NotNull StringExecutionInfo info) throws Throwable {
        executes0(info, false);
    }

    private void executes0(@NotNull InteractionHolder info, boolean ephemeral) {

        GuildPlayer player = Utils.getInteractionPlayer(manager, info, false, ephemeral);
        if (player == null) {
            return;
        }

        boolean state = !player.hasAdminLock();
        player.setAdminLock(state);

        User botUser = player.getBot().getBot().getSelfUser();

        String str = state ? "music.command.lock.locked" : "music.command.lock.unlocked";
        info.reply(str)
                .withPlaceholders("bot", botUser)
                .setEphemeral(ephemeral)
                .queue();

    }

    @Override
    public void executes(@NotNull ConsoleExecutionInfo info) {

        AudioChannelUnion channel = info.arguments().getCast("channel", AudioChannelUnion.class).orElseThrow();

        GuildPlayer player = Utils.getConsolePlayer(manager, info, channel, false);
        if (player == null) {
            return;
        }

        boolean newState = !player.hasAdminLock(); // toggle lock
        player.setAdminLock(newState);

        String msg = newState ? "Music bot is now &blocked &rfor staff-only use." : "Music bot is now &bunlocked &rfor all users.";
        info.logger().info(msg);

    }

    @ArgumentMethod(description = "Channel with bot", scope = ArgumentScope.CONSOLE)
    public Argument<?> channel() {
        return new VoiceChannelArgument();
    }

}
