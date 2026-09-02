package net.survivalboom.sbds.modules.music.commands;

import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.survivalboom.sbds.api.commands.ArgumentScope;
import net.survivalboom.sbds.api.commands.argument.Argument;
import net.survivalboom.sbds.api.commands.argument.discord.channel.VoiceChannelArgument;
import net.survivalboom.sbds.api.commands.argument.primitive.GreedyStringArgument;
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
import net.survivalboom.sbds.api.utils.CommonUtils;
import net.survivalboom.sbds.api.utils.placeholders.Placeholders;
import net.survivalboom.sbds.modules.music.MusicModule;
import net.survivalboom.sbds.modules.music.music.MusicManager;
import net.survivalboom.sbds.modules.music.music.GuildPlayer;
import net.survivalboom.sbds.modules.music.music.MusicTrack;
import net.survivalboom.sbds.modules.music.music.TrackLoadException;
import net.survivalboom.sbds.modules.music.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;

@CommandClass(
        name = "play",
        description = "Finds a track from your query and connects a new bot to your channel",
        translationKey = "music.command.play",
        permission = "music.command.play",
        defaultPermission = true
)
public class PlayCommand extends CommandBase implements SlashCommandExecutor, StringCommandExecutor, ConsoleCommandExecutor {

    private final MusicManager manager;

    public PlayCommand(@NotNull MusicModule module) {
        this.manager = module.getMusicManager();
    }

    @Override
    public void executes(@NotNull SlashExecutionInfo info) {
        executes0(info);
    }

    @Override
    public void executes(@NotNull StringExecutionInfo info) {
        executes0(info);
    }

    private void executes0(@NotNull InteractionHolder info) {

        Member member = info.member();
        String query = info.arguments().getCast("query", String.class).orElseThrow();

        // Шукаємо плеєр, який відповідає за цей сервер. Якщо немає, створюємо новий //
        GuildPlayer player = Utils.getInteractionPlayer(manager, info, true, false);
        if (player == null) {
            return;
        }

        // Перевіряємо чи користувач може взаємодіяти із плеєром //
        if (Utils.checkInteractionDenied(manager, info, player, false)) {
            return;
        }

        GuildVoiceState voiceState = member.getVoiceState();
        AudioChannel channel = Objects.requireNonNull(voiceState).getChannel();
        Objects.requireNonNull(channel);

        // Шукаємо треки за запитом //

        List<MusicTrack> tracks = searchTracks(info, query, player);
        if (tracks == null) {
            return;
        }

        // Запускаємо плеєр, завантажуємо треки //

        boolean newBot = !player.isActive();
        if (newBot) {
            player.connect(channel);
        }

        player.addTracks(tracks);

        // Готуємо плейсхолдери та відправляємо повідомлення, відповідно до того що ми зробили //

        User botUser = player.getBot().getBot().getSelfUser();
        MusicTrack addedTrack = tracks.getFirst();
        MusicTrack playingTrack = player.getCurrentPlaying();

        Placeholders placeholders = Placeholders.of(
                "bot", botUser,
                "count", tracks.size(),
                "playlist", Utils.createTracksString(player.getPlaylist(), 10),
                "playlist.size", player.getPlaylistSize(),
                "playing", playingTrack
        );

        if (tracks.size() > 1) {
            placeholders.add("added", Utils.createTracksString(tracks, 10));
        }

        else {
            placeholders.add("added", addedTrack);
        }

        if (newBot) {
            info.reply("music.command.play.connected").withPlaceholders(placeholders).queue();
        }

        else {

            if (tracks.size() > 1) {
                placeholders.add("count", tracks.size());
                info.reply("music.command.play.playlist-added").withPlaceholders(placeholders).queue();
            }

            else {
                info.reply("music.command.play.playlist-added-single").withPlaceholders(placeholders).queue();
            }

        }


    }

    private boolean isUrl(@NotNull String string) {

        if (string.startsWith("https://") || string.startsWith("http://")) {

            try {
                new URI(string);
                return true;
            }

            catch (URISyntaxException e) {
                return false;
            }

        }

        return false;

    }

    private @Nullable List<MusicTrack> searchTracks(@NotNull InteractionHolder info, @NotNull String query, @NotNull GuildPlayer player) {

        boolean isUrl = isUrl(query);

        if (isUrl) {
            info.reply("music.command.play.loading-tracks").withPlaceholders("url", query).queue();
        }
        else {
            info.reply("music.command.play.searching-tracks").withPlaceholders("query", query).queue();
        }

        CommonUtils.RepeatResult<List<MusicTrack>> result = CommonUtils.tryRepeat(() -> player.searchTracks(isUrl ? query : "ytsearch:" + query), 5, 500);
        if (result.result().isEmpty()) {
            info.reply("music.command.play.load-failed").withPlaceholders("error", result.errors().getFirst().toString()).queue();
            return null;
        }

        List<MusicTrack> tracks = result.result().get();
        if (tracks.isEmpty()) {
            info.reply("music.command.play.no-tracks-found").withPlaceholders("query", query).queue();
            return null;
        }

        if (!isUrl) {
            return List.of(tracks.getFirst());
        }

        return tracks;

    }

    @Override
    public void executes(@NotNull ConsoleExecutionInfo info) {

        String query = info.arguments().getCast("query", String.class).orElseThrow();
        AudioChannelUnion channel = info.arguments().getCast("channel", AudioChannelUnion.class).orElseThrow();

        // Шукаємо плеєр, який відповідає за цей сервер. Якщо немає, створюємо новий //

        GuildPlayer player = Utils.getConsolePlayer(manager, info, channel, true);
        if (player == null) {
            return;
        }

        // Шукаємо треки за запитом й завантажуємо у плеєр //

        List<MusicTrack> tracks = searchTracks(info, query, player);
        if (tracks == null) {
            return;
        }

        // Запускаємо плеєр та завантажуємо треки //

        boolean newBot = !player.isActive();
        if (newBot) {
            player.connect(channel);
            player.setIdleDisconnect(false);
        }

        player.addTracks(tracks);

        // Готуємо плейсхолдери та відправляємо повідомлення, відповідно до того що ми зробили //

        User botUser = player.getBot().getBot().getSelfUser();
        MusicTrack addedTrack = tracks.getFirst();

        if (newBot) {
            info.logger().info("Connected `{}` to `#{}`.", botUser.getEffectiveName(), channel.getName());
        }

        else {

            if (tracks.size() > 1) {
                info.logger().info("Added `{}` tracks to playlist!", tracks.size());
            }

            else {
                info.logger().info("Added `{}` to playlist!", addedTrack.getTitle());
            }

        }


    }

    private @Nullable List<MusicTrack> searchTracks(@NotNull ConsoleExecutionInfo info, @NotNull String query, @NotNull GuildPlayer player) {

        boolean isUrl = isUrl(query);

        if (isUrl) info.logger().info("Loading tracks from link `{}`...", query);
        else info.logger().info("Searching tracks for query `{}`...", query);

        CommonUtils.RepeatResult<List<MusicTrack>> result = CommonUtils.tryRepeat(() -> player.searchTracks(isUrl ? query : "ytsearch:" + query), 5, 500);
        if (result.result().isEmpty()) {
            return null;
        }

        List<MusicTrack> tracks = result.result().get();
        if (tracks.isEmpty()) {
            info.logger().error("No results found for `{}`.", query);
            return null;
        }

        if (!isUrl) {
            return List.of(tracks.getFirst());
        }

        return tracks;

    }

    @ArgumentMethod(description = "Channel with bot", scope = ArgumentScope.CONSOLE)
    public Argument<?> channel() {
        return new VoiceChannelArgument();
    }

    @ArgumentMethod(description = "URL or search query", index = 1)
    public Argument<?> query() {
        return new GreedyStringArgument();
    }

}
