package net.survivalboom.sbds.modules.music.music;

import dev.arbjerg.lavalink.client.Link;
import dev.arbjerg.lavalink.client.event.TrackEndEvent;
import dev.arbjerg.lavalink.client.player.*;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.survivalboom.sbds.api.scheduler.ISchedulerTask;
import net.survivalboom.sbds.api.utils.CommonUtils;
import net.survivalboom.sbds.modules.music.MusicModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class GuildPlayer {

    private static final Logger log = LoggerFactory.getLogger(GuildPlayer.class);

    private final MusicModule musicModule;

    private final MusicBot bot;

    private final LavalinkPlayer lavalinkPlayer;

    private final Link lavalink;


    private final Guild guild;

    private final Member botGuildMember;

    private AudioChannelUnion currentChannel = null;


    private final List<MusicTrack> playlist = new ArrayList<>();

    private int playingIndex = 0;

    private boolean playing = false;

    private boolean paused = false;


    private LoopMode loop = LoopMode.DISABLED;

    private boolean idleDisconnect = true;

    private boolean adminLock = false;


    private ISchedulerTask task;

    private boolean waitingForTracks = true;


    public GuildPlayer(
            @NotNull MusicBot bot,
            @NotNull Guild guild,
            @NotNull Link lavalink,
            @NotNull LavalinkPlayer lavalinkPlayer
    ) {

        this.guild = guild;
        this.bot = bot;

        this.botGuildMember = Objects.requireNonNull(bot.getBot().getGuildById(guild.getId())).getSelfMember();

        this.lavalink = lavalink;
        this.lavalinkPlayer = lavalinkPlayer;

        this.musicModule = bot.getManager().getModule();

    }

    public @NotNull MusicBot getBot() {
        return bot;
    }

    public @NotNull Guild getGuild() {
        return guild;
    }

    public @Nullable AudioChannelUnion getConnectedChannel() {
        return currentChannel;
    }

    //
    // TRACKS SEARCHING
    //

    public @NotNull List<MusicTrack> searchTracks(@NotNull String query) throws TrackLoadException {

        Objects.requireNonNull(query, "query == null");

        LavalinkLoadResult result = lavalink.loadItem(query)
                .retry(5)
                .block(Duration.ofSeconds(20));

        Objects.requireNonNull(result);

        List<Track> tracks = switch (result) {

            // Трек завантажено з прямого посилання //
            case TrackLoaded trackLoaded -> List.of(trackLoaded.getTrack());

            // Декілька треків знайдено за цим запитом //
            case PlaylistLoaded playlistLoaded -> playlistLoaded.getTracks();

            // Нічого не знайдено //
            case NoMatches ignored -> List.of();

            // Сталась помилка при спробі завантажити треки //
            case LoadFailed loadFailed -> throw new TrackLoadException(loadFailed.getException().toString());

            // Знайдено треки на якійсь платформі //
            case SearchResult searchResult -> searchResult.getTracks();

            default -> throw new IllegalStateException("Unknown LavalinkLoadResult `" + result + "`");

        };

        return tracks.stream()
                .map(MusicTrack::new)
                .collect(Collectors.toList());

    }

    //
    // LIFECYCLE
    //

    /**
     * Запускає дискорд бота. Запускає усі необхідні tasks та підключає до голосового каналу.
     * Після виконання цього методу, бот може зупинитись якщо плейліст буде пустим.
     */
    public synchronized void connect(@NotNull AudioChannel channel) {

        if (task != null) {
            throw new IllegalStateException("GuildPlayer already active");
        }

        // bot.getBot().getDirectAudioController().connect(channel);
        botGuildMember.getGuild().getAudioManager().openAudioConnection(channel);

        // Чекаємо поки Discord підключить нас, перед тим як прововжувати ініціалізацію.
        CommonUtils.waitUntil(() -> {
            updateCurrentChannel();
            return this.currentChannel != null;
        }, 5000);

        task = musicModule.getSbds().getScheduler().schedule(musicModule, bot.getName() + "-" + guild.getId() + "-MusicPlayer", this::task, 1000, 1000);

    }

    /**
     * Повністю зупинити музичний плеєр та відключити бота від поточного каналу.
     */
    public synchronized void disconnect() {

        if (task == null) {
            throw new IllegalArgumentException("GuildPlayer is not currently active");
        }

        task.tryCancel();
        task = null;

//        bot.getBot().getDirectAudioController().disconnect(guild);
        botGuildMember.getGuild().getAudioManager().closeAudioConnection();
        this.currentChannel = null;

        this.paused = false;
        this.loop = LoopMode.DISABLED;
        this.idleDisconnect = true;

        this.waitingForTracks = true;

        this.playing = false;
        this.playingIndex = 0;
        this.playlist.clear();

        this.adminLock = false;

    }

    private void task() {

        updateCurrentChannel();

        if (currentChannel == null) {
            musicModule.getSbds().getScheduler().schedule(musicModule, this::disconnect, 0, 0); // Зупиняємо player через scheduler, щоб оминути нескінченне блокування
            return;
        }

        List<Member> members = getMembers();
        if ((members.size() == 1 || members.stream().allMatch(m -> m.getUser().isBot())) && idleDisconnect) {
            musicModule.getSbds().getScheduler().schedule(musicModule, this::disconnect, 0, 0);
            return;
        }

        if (!isPlaying()) {

            // Не дає цьому плеєру від'єднатись, поки ми не завантажимо у нього треки.
            if (waitingForTracks) {

                if (!playlist.isEmpty()) {
                    updateTrack();
                    waitingForTracks = false;
                }

                return;

            }

            if (isLastTrack()) {

                if (loop == LoopMode.PLAYLIST || !idleDisconnect) {
                    this.playingIndex = -1;
                }

                else if (loop == LoopMode.DISABLED) {
                    musicModule.getSbds().getScheduler().schedule(musicModule, this::disconnect, 0, 0);
                    return;
                }

            }

            if (loop != LoopMode.TRACK) {
                playingIndex++;
            }

            updateTrack();

        }

    }

    private List<Member> getMembers() {
        return Objects.requireNonNull(getBot().getManager().getModule().getSbds().getBot().getChannelById(AudioChannel.class, currentChannel.getId())).getMembers();
    }

    public void onTrackEnd(@NotNull TrackEndEvent event) {

        if (!event.getEndReason().getMayStartNext()) {
            return;
        }

        playing = false;

    }

    private void updateCurrentChannel() {

        var state = botGuildMember.getVoiceState();
        if (state == null) {
            this.currentChannel = null;
            return;
        }

        this.currentChannel = botGuildMember.getVoiceState().getChannel();

    }

    private void updateTrack() {

        MusicTrack musicTrack = getCurrentPlaying();
        Track track = musicTrack != null ? musicTrack.getTrack() : null;

        lavalinkPlayer.setTrack(track).block(Duration.ofSeconds(5000));
        this.playing = true;

    }

    //
    // GUILD PLAYER
    //

    // STATE //

    public boolean isActive() {
        return task != null;
    }

    public boolean isPlaying() {
        return playing;
    }

    public boolean isLastTrack() {
        return playingIndex + 1 >= playlist.size();
    }

    private void checkActive() {

        if (!isActive()) {
            throw new IllegalStateException("GuildPlayer is not currently active");
        }

    }

    // TRACKS //

    public void addTracks(@NotNull MusicTrack... tracks) {
        addTracks(List.of(tracks));
    }

    public void addTracks(@NotNull Collection<MusicTrack> tracks) {

        Objects.requireNonNull(tracks, "tracks == null");
        checkActive();

        this.playlist.addAll(tracks);

    }

    public void changePlayingIndex(int steps) {

        checkActive();

        int nextPlayingIndex = this.playingIndex + steps;
        if (nextPlayingIndex >= playlist.size() || nextPlayingIndex < 0) {
            throw new IllegalArgumentException("Invalid playing index `" + nextPlayingIndex + "`");
        }

        this.playingIndex += steps;
        updateTrack();

    }

    public void setPlayingIndex(int index) {

        checkActive();

        if (index < 0) {
            throw new IllegalArgumentException("index is negative");
        }

        if (index >= playlist.size()) {
            throw new IllegalArgumentException("index >= playlist.size()");
        }

        this.playingIndex = index;
        updateTrack();

    }

    public @Nullable MusicTrack getCurrentPlaying() {

        if (playlist.isEmpty() || playingIndex >= playlist.size()) {
            return null;
        }

        return playlist.get(playingIndex);

    }

    public int getPlayingIndex() {
        return playingIndex;
    }

    public @NotNull List<MusicTrack> getPlaylist() {
        return new ArrayList<>(playlist);
    }

    public int getPlaylistSize() {
        return playlist.size();
    }

    // pause //

    public void setPaused(boolean v) {
        checkActive();
        lavalinkPlayer.setPaused(v).subscribe();
        paused = v;
    }

    public boolean isPaused() {
        return paused;
    }

    // loop //

    public void setLoopMode(@NotNull LoopMode loop) {
        checkActive();
        this.loop = loop;
    }

    public @NotNull LoopMode getLoopMode() {
        return loop;
    }

    // idle disconnect //

    public void setIdleDisconnect(boolean v) {
        checkActive();
        this.idleDisconnect = v;
    }

    public boolean isIdleDisconnect() {
        checkActive();
        return idleDisconnect;
    }

    // admin lock //

    public void setAdminLock(boolean v) {
        checkActive();
        this.adminLock = v;
    }

    public boolean hasAdminLock() {
        checkActive();
        return adminLock;
    }

}
