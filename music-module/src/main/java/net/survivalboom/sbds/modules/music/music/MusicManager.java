package net.survivalboom.sbds.modules.music.music;

import dev.arbjerg.lavalink.client.NodeOptions;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.survivalboom.sbds.api.database.members.IMemberData;
import net.survivalboom.sbds.api.database.members.IMemberDataManager;
import net.survivalboom.sbds.api.utils.*;
import net.survivalboom.sbds.api.utils.valid.Manager;
import net.survivalboom.sbds.modules.music.MusicModule;
import net.survivalboom.sbds.modules.music.utils.IntegratedLavalinkManager;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class MusicManager extends Manager {


    private final MusicModule module;

    private final IntegratedLavalinkManager integratedLavalinkManager;

    private final Logger logger;

    private final File botsFolder;


    private final IMemberDataManager memberDataManager;

    private final NamespacedKey key;


    private final Set<MusicBot> musicBots = new HashSet<>();

    private final Set<NodeOptions> nodeInfos = new HashSet<>();


    public MusicManager(@NotNull MusicModule module, @NotNull IntegratedLavalinkManager integratedLavalinkManager) {

        this.module = module;
        this.integratedLavalinkManager = integratedLavalinkManager;

        this.logger = module.getLogger();
        this.botsFolder = new File(module.getModule().getDataFolder(), "bots");

        this.memberDataManager = module.getSbds().getMemberDataManager();
        this.key = NamespacedKey.fromModule(module, "data");

    }

    @Override
    protected void init0() {

        logger.info("Loading bot manager...");

        loadNodes();
        loadBots();

    }

    private void loadNodes() {

        if (integratedLavalinkManager.isEnabled()) {
            nodeInfos.add(integratedLavalinkManager.createNodes());
            return;
        }

        for (ConfigurationNode node : module.getConfig().node("nodes").childrenList()) {

            String name = node.node("name").getString();
            String url = node.node("url").getString();
            String password = node.node("password").getString();

            if (name == null || url == null || password == null) {
                logger.warn("Invalid node {}. Skipping...", node.key());
                continue;
            }

            NodeOptions.Builder builder = new NodeOptions.Builder();
            builder.setName(name);
            builder.setServerUri(url);
            builder.setPassword(password);

            nodeInfos.add(builder.build());

        }

    }

    private void loadBots() {

        //noinspection ResultOfMethodCallIgnored
        botsFolder.mkdirs();

        for (File file : Objects.requireNonNull(botsFolder.listFiles())) {

            String fileName = file.getName();
            if (!fileName.endsWith(".token")) {
                continue;
            }

            String name = fileName.replace(".token", "");

            MusicBot bot;
            try {

                String token = loadToken(file);
                bot = new MusicBot(this, nodeInfos, name, token);

                bot.init();

            }

            catch (Throwable t) {
                logger.error("Failed to load music bot `{}`.", name, t);
                continue;
            }

            musicBots.add(bot);

        }

        if (!musicBots.isEmpty()) {
            logger.info("Loaded {} music bots.", musicBots.size());
        }

        else {
            logger.warn("No music bots loaded.");
        }

    }

    private String loadToken(File file) throws IOException {

        byte[] bytes;
        try (FileInputStream stream = new FileInputStream(file)) {
            bytes = stream.readAllBytes();
        }

        return new String(bytes);

    }

    @Override
    protected void shutdown0() {

        nodeInfos.clear();

        for (MusicBot bot : getMusicBots()) {

            try {

                bot.shutdown();

            }

            catch (Throwable t) {
                logger.error("Failed to shutdown music bot `{}` properly. This may cause memory leaks!", bot.getName(), t);
                continue;
            }

            musicBots.remove(bot);

        }

    }


    public @NotNull MusicModule getModule() {
        return module;
    }


    public @NotNull List<MusicBot> getMusicBots() {
        return new ArrayList<>(musicBots);
    }

    public @NotNull List<MusicBot> findFreeBots(@NotNull AudioChannelUnion channel) {

        Objects.requireNonNull(channel, "channel == null");
        checkValid();

        List<MusicBot> botsInGuild = findBotsInGuild(channel.getGuild());
        if (botsInGuild.isEmpty()) return botsInGuild;

        Guild guild = channel.getGuild();

        return botsInGuild.stream().filter(bot -> {
            GuildPlayer player = bot.getPlayer(guild);
            return player == null || !player.isActive();
        }).toList();

    }

    public @Nullable GuildPlayer findCurrentPlayer(@NotNull AudioChannelUnion channel) {

        Objects.requireNonNull(channel, "channel == null");
        checkValid();

        Guild guild = channel.getGuild();
        return musicBots
                .stream()
                .map(bot -> bot.getPlayer(guild))
                .filter(Objects::nonNull)
                .filter(GuildPlayer::isActive)
                .filter(p -> channel.getIdLong() == Objects.requireNonNull(p.getConnectedChannel()).getIdLong())
                .findFirst()
                .orElse(null);
    }

    public @NotNull List<MusicBot> findBotsInGuild(@NotNull Guild guild) {
        checkValid();
        //        Collections.shuffle(bots); <--- не прикольно і не зручно;
        return musicBots.stream()
                .filter(bot -> bot.getBot().getGuilds().contains(guild))
                .collect(Collectors.toList());
    }

    //
    // MUSIC BAN
    //

    @Blocking
    public boolean isMusicBanned(@NotNull Member member) {

        IMemberData memberData = memberDataManager.get(member).join();
        if (memberData == null) {
            return false;
        }

        ConfigurationNode node = memberData.container().getNode(key).orElse(null);
        if (node == null) {
            return false;
        }

        return node.node("banned").getBoolean(false);

    }

    @Blocking
    public void setMusicBanned(@NotNull Member member, boolean state) {

        IMemberData memberData = memberDataManager.obtain(member).join();
        ConfigurationNode node = memberData.container()
                .obtainNode(key)
                .node("banned");

        try {
            node.set(state);
        }

        catch (SerializationException e) {
            throw new RuntimeException(e);
        }

        memberData.save();

    }

}
