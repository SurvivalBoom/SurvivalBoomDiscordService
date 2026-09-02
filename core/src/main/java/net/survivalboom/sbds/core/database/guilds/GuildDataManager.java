package net.survivalboom.sbds.core.database.guilds;

import net.survivalboom.sbds.api.ISBDS;
import net.survivalboom.sbds.api.database.IRepository;
import net.survivalboom.sbds.api.database.guilds.IGuildData;
import net.survivalboom.sbds.api.database.guilds.IGuildDataManager;
import net.survivalboom.sbds.api.utils.map.SoftRefHashMap;
import net.survivalboom.sbds.api.utils.valid.Manager;
import net.survivalboom.sbds.core.SBDS;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;

public class GuildDataManager extends Manager implements IGuildDataManager {

    private final SBDS sbds;

    private IRepository<GuildDataRecord> repository;

    private final Map<Long, IGuildData> cache = new SoftRefHashMap<>();


    public GuildDataManager(@NotNull SBDS sbds) {
        this.sbds = sbds;
    }

    @Override
    public @NotNull ISBDS getSbds() {
        return sbds;
    }

    //
    // MANAGER
    //

    @Override
    protected void init0() {
        this.repository = sbds.getDatabase().createRepository0(null, GuildDataRecord.class);
    }

    @Override
    protected void shutdown0() {
        this.repository = null;
        cache.clear();
    }

    //
    // GUILD DATA
    //

    // CREATE //

    @Override
    public @NotNull CompletableFuture<IGuildData> create(long id) {
        return get(id).thenCompose(guild -> {

            if (guild != null) {
                throw new IllegalStateException("Guild data with id `" + id + "` already exists");
            }

            return repository.queueSessionReturnRequest(session -> {

                GuildDataRecord record = new GuildDataRecord(id);
                session.persist(record);

                GuildData guildData = new GuildData(record, this);
                cache.put(id, guildData);

                return guildData;

            });

        });
    }

    // DELETE //

    @Override
    public @NotNull CompletableFuture<Void> delete(long id) {
        return get(id).thenCompose(guild -> {

            if (guild == null) {
                throw new IllegalArgumentException("Guild data with id `" + id + "` does not exist");
            }

            return repository.queueSessionRequest(session -> {

                GuildData guildData = (GuildData) guild;
                GuildDataRecord record = guildData.getRecord();

                guildData.setValid(false);

                session.remove(record);

                cache.remove(id);

            });

        });
    }

    // GET //

    @Override
    public @NotNull CompletableFuture<@Nullable IGuildData> get(long id) {

        checkValid();

        if (cache.containsKey(id)) {
            return CompletableFuture.completedFuture(cache.get(id));
        }

        return repository.queueSessionReturnRequest(session -> {

            GuildDataRecord record = session.get(GuildDataRecord.class, id);
            if (record == null) {
                cache.put(id, null);
                return null;
            }

            GuildData guildData = new GuildData(record, this);
            cache.put(id, guildData);

            return guildData;

        });

    }

    // OBTAIN //


    @Override
    public @NotNull CompletableFuture<@NotNull IGuildData> obtain(long id) {
        return get(id).thenCompose(guild -> {

            if (guild != null) {
                return CompletableFuture.completedFuture(guild);
            }

            return create(id);

        });
    }

    // SAVE //


    @Override
    public void save(@NotNull IGuildData guild) {

        checkValid();

        GuildData guildData = (GuildData) guild;
        if (!cache.containsKey(guildData.getGuild().getIdLong()) || !guildData.isValid()) {
            throw new IllegalArgumentException("GuildData object `" + guildData + "` is no linger valid");
        }

        repository.saveRecord(guildData.getRecord());

    }

}
