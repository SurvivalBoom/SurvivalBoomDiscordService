package net.survivalboom.sbds.modules.voice.storage;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.survivalboom.sbds.api.database.IDatabase;
import net.survivalboom.sbds.api.database.guilds.IGuildData;
import net.survivalboom.sbds.api.database.guilds.IGuildRepositoryHandler;
import net.survivalboom.sbds.api.modules.ModuleMain;
import net.survivalboom.sbds.api.utils.Manager;
import net.survivalboom.sbds.api.utils.NamespacedKey;
import net.survivalboom.sbds.api.utils.TypeMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

public class VoiceCreatorChannels extends Manager {

    private final NamespacedKey key;

    private final Map<VoiceChannel, Boolean> channels = new WeakHashMap<>();

    private final IDatabase database;


    private IGuildRepositoryHandler repository;


    public VoiceCreatorChannels(@NotNull ModuleMain module) {
        this.database = module.getModule().getSbds().getDatabase();
        this.key = NamespacedKey.fromModule(module, "voices");
    }


    @Override
    protected void init0() {
        this.repository = database.getRepositoryHandler("sbds:guilds", IGuildRepositoryHandler.class);
    }

    @Override
    protected void shutdown0() {

    }

    public boolean isVoiceCreator(@NotNull VoiceChannel channel) {
        return channels.computeIfAbsent(channel, key -> channel.equals(getVoiceCreator(channel.getGuild())));
    }

    public @Nullable VoiceChannel getVoiceCreator(@NotNull Guild guild) {

        VoiceChannel channel = channels.keySet().stream().filter(v -> v.getGuild().equals(guild)).findAny().orElse(null);
        if (channel != null) {
            return channel;
        }

        IGuildData guildData = repository.createGuildData(guild);
        String channelId = guildData.container().getOrCreate(key).getCastOrNull("creator", String.class);

        if (channelId == null) {
            return null;
        }

        channel = guild.getChannelById(VoiceChannel.class, channelId);
        if (channel != null) channels.put(channel, true);

        return channel;

    }

    public void setVoiceCreator(@NotNull VoiceChannel channel) {

        IGuildData guildData = repository.createGuildData(channel.getGuild());
        TypeMap typeMap = guildData.container().getOrCreate(key);

        typeMap.put("creator", channel.getId());
        guildData.save();

        channels.put(channel, true);

    }

    public void removeVoiceCreator(@NotNull Guild guild) {

        IGuildData guildData = repository.createGuildData(guild);
        TypeMap typeMap = guildData.container().getOrCreate(key);

        typeMap.remove("creator");
        guildData.save();

        channels.keySet().stream().filter(c -> c.getGuild().equals(guild)).toList().forEach(channels::remove);

    }


}
