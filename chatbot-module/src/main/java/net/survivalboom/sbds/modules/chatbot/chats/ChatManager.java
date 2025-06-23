package net.survivalboom.sbds.modules.chatbot.chats;

import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.survivalboom.sbds.api.modules.ModuleMain;
import net.survivalboom.sbds.api.scheduler.IScheduler;
import net.survivalboom.sbds.api.scheduler.ISchedulerTask;
import net.survivalboom.sbds.api.utils.Manager;
import net.survivalboom.sbds.modules.chatbot.ai.OpenAiManager;
import net.survivalboom.sbds.modules.chatbot.storage.AllowedChannels;
import net.survivalboom.sbds.modules.chatbot.storage.BannedUsers;
import net.survivalboom.sbds.modules.chatbot.storage.GuildModels;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ChatManager extends Manager {


    private static final Logger log = LoggerFactory.getLogger(ChatManager.class);


    private final ModuleMain module;

    private final OpenAiManager openAiManager;

    private final IScheduler scheduler;


    private final AllowedChannels allowedChannels;

    private final BannedUsers bannedUsers;

    private final GuildModels guildModels;


    private final List<String> allowedGuilds = new ArrayList<>();



    private final Set<ChannelChat> chats = new HashSet<>();

    private final List<TextChannel> toStop = new ArrayList<>();


    private final File botTokenFile;

    private JDA bot;

    private boolean enabled = false;


    private String prompt;

    private List<String> characterNames;


    public ChatManager(@NotNull ModuleMain module, @NotNull OpenAiManager openAiManager) {

        this.module = module;
        this.openAiManager = openAiManager;
        this.scheduler = module.getSbds().getScheduler();

        this.botTokenFile = new File(module.getDataFolder(), "bot-token");

        this.allowedChannels = new AllowedChannels(module);
        this.bannedUsers = new BannedUsers(module);
        this.guildModels = new GuildModels(module);

    }


    @Override
    protected void init0() {

        allowedGuilds.addAll(module.getConfig().getStringList("allowed-guilds"));

        enabled = botTokenFile.exists();
        if (!enabled) {

            try {
                botTokenFile.createNewFile();
            }

            catch (IOException e) {
                throw new RuntimeException(e);
            }

            log.error("Bot token not provided. Please provide a new bot token in `{}`", botTokenFile.getAbsolutePath());
            return;
        }

        String token;
        try {
            token = readToken();
        }

        catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            bot = JDABuilder.createLight(token).build();
            bot.awaitReady();
        }

        catch (Throwable t) {
            log.error("Failed to log in.", t);
            enabled = false;
            return;
        }

        prompt = module.getConfig().getString("prompt", "");
        characterNames = module.getConfig().getStringList("character-names");

        allowedChannels.init();
        bannedUsers.init();
        guildModels.init();

    }

    @Override
    protected void shutdown0() {

        if (bot == null) return;
        bot.shutdown();
        bot = null;

        allowedGuilds.clear();

        allowedChannels.shutdown();
        bannedUsers.shutdown();
        guildModels.shutdown();

        chats.clear();

    }

    private @NotNull String readToken() throws IOException {

        try (FileInputStream stream = new FileInputStream(botTokenFile)) {
            return new String(stream.readAllBytes());
        }

    }



    public @Nullable ChannelChat getChat(@NotNull TextChannel channel) {
        return chats.stream().filter(ch -> ch.getChannel().equals(channel)).findAny().orElse(null);
    }

    public @NotNull ChannelChat createChat(@NotNull TextChannel channel) {

        if (chats.stream().anyMatch(c -> c.getChannel().equals(channel))) {
            throw new IllegalStateException("Chat for this channel already exist");
        }

        ChannelChat chat = new ChannelChat(this, channel, bot);

        chats.add(chat);

        return chat;

    }

    public void removeUnusedChats() {

        for (ChannelChat chat : chats) {

            Message message = chat.getLastMessage();
            if (message == null) continue;

            long time = message.getTimeCreated().toEpochSecond();
            if (System.currentTimeMillis() - time * 1000 < 600000) continue;

            chats.remove(chat);

        }

    }

    public @NotNull CompletableFuture<Void> react(@NotNull TextChannel channel, @NotNull List<Message> messages) {

        return CompletableFuture.runAsync(() -> {

            ChannelChat chat = getChat(channel);
            if (chat == null) {
                chat = createChat(channel);
            }

            chat.putMessages(messages);

            List<ChatCompletionMessageParam> history = chat.generateMessages(prompt);
            TextChannel botChannel = Objects.requireNonNull(bot.getChannelById(TextChannel.class, channel.getId()));

            ISchedulerTask task = scheduler.schedule(module, () -> botChannel.sendTyping().queue(), 1, 10000);

            ChatCompletion completion;

            ChatModel model = guildModels.getModel(channel.getGuild());
            log.warn(model.toString());

            try {
                completion = openAiManager.chatCompletion(history, model);
            }

            catch (Throwable t) {
                log.error("Response generation error.", t);
                botChannel.sendMessage("`" + t + "`").queue();
                task.cancel();
                chats.remove(chat);
                return;
            }

            String string = completion.choices().getFirst().message().content().orElseThrow();
            boolean needToStop = string.contains("{STOP}");

            if (needToStop) {
                string = string.replace("{STOP}", "");
            }

            if (!string.isBlank()) {
                Message message = botChannel.sendMessage(string).complete();
                chat.putMessage(message);
            }

            task.cancel();

            if (needToStop) {
                log.info("Received stop message from bot.");
                chats.remove(chat);
                toStop.add(channel);
            }

        });

    }


    public @NotNull ModuleMain getModule() {
        return module;
    }

    public @NotNull AllowedChannels allowedChannels() {
        return allowedChannels;
    }

    public @NotNull BannedUsers bannedUsers() {
        return bannedUsers;
    }

    public @NotNull GuildModels guildModels() {
        return guildModels;
    }

    public @NotNull List<String> getCharacterNames() {
        return characterNames;
    }

    public @NotNull JDA getBot() {
        return bot;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public @NotNull List<TextChannel> toStop() {
        return toStop;
    }

    public boolean isGuildAllowed(@NotNull Guild guild) {
        if (allowedGuilds.isEmpty()) return true;
        return allowedGuilds.contains(guild.getId());
    }

}
