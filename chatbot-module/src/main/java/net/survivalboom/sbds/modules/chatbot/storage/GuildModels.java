package net.survivalboom.sbds.modules.chatbot.storage;

import com.openai.models.ChatModel;
import net.dv8tion.jda.api.entities.Guild;
import net.survivalboom.sbds.api.database.IDatabase;
import net.survivalboom.sbds.api.database.guilds.IGuildData;
import net.survivalboom.sbds.api.database.guilds.IGuildRepositoryHandler;
import net.survivalboom.sbds.api.modules.ModuleMain;
import net.survivalboom.sbds.api.utils.Manager;
import net.survivalboom.sbds.api.utils.NamespacedKey;
import net.survivalboom.sbds.api.utils.TypeMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GuildModels extends Manager {

    private static final Logger log = LoggerFactory.getLogger(GuildModels.class);
    private final Map<Guild, ChatModel> guildModelMap = new HashMap<>();


    private final ModuleMain module;

    private final NamespacedKey key;

    private final IDatabase database;


    private ChatModel defaultModel;

    private IGuildRepositoryHandler repository;


    public GuildModels(@NotNull ModuleMain module) {
        this.database = module.getDatabase();
        this.module = module;
        this.key = NamespacedKey.fromModule(module, "model");
    }

    @Override
    protected void init0() {

        repository = database.getRepositoryHandler("sbds:guilds", IGuildRepositoryHandler.class);

        String defaultModelRaw = module.getConfig().getString("default-model");
        if (defaultModelRaw == null) {
            log.warn("Default model is null. Using default model `GPT_4_1_MINI`.");
            defaultModel = ChatModel.GPT_4_1_MINI;
            return;
        }

        defaultModel = getModelFromFuckingKotlin(defaultModelRaw);
        if (defaultModel == null) {
            log.error("Invalid default model `{}`. Using fallback model `GPT_4_1_MINI`.", defaultModelRaw);
            defaultModel = ChatModel.GPT_4_1_MINI;
        }


    }

    @Override
    protected void shutdown0() {
        repository = null;
        defaultModel = null;
    }

    public @NotNull ChatModel getModel(@NotNull Guild guild) {

        checkValid();

        Objects.requireNonNull(guild, "guild == null");

        return guildModelMap.computeIfAbsent(guild, key -> {

            TypeMap map = repository.createGuildData(key).container().getOrCreate(this.key);
            String modelRaw = map.getCastOrNull("model", String.class);
            if (modelRaw == null) {
                return defaultModel;
            }

            ChatModel model = getModelFromFuckingKotlin(modelRaw);
            if (model == null) {
                return defaultModel;
            }

            return model;

        });

    }

    public void setModel(@NotNull Guild guild, @NotNull ChatModel model) {

        checkValid();

        Objects.requireNonNull(guild, "guild == null");
        Objects.requireNonNull(model, "model == null");

        guildModelMap.put(guild, model);

        IGuildData guildData = repository.createGuildData(guild);
        guildData.container().getOrCreate(key).put("model", model.value().toString());

        guildData.save();

    }

    public static @Nullable ChatModel getModelFromFuckingKotlin(String raw) {

        return switch (raw) {

            case "GPT_4_1" -> ChatModel.GPT_4_1;

            case "GPT_4_1_MINI" -> ChatModel.GPT_4_1_MINI;

            case "GPT_4_1_NANO" -> ChatModel.GPT_4_1_NANO;

            default -> null;

        };

    }

}
