package net.survivalboom.sbds.modules.logging.module.kostily;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteCreateEvent;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteDeleteEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.survivalboom.sbds.api.events.EventHandler;
import net.survivalboom.sbds.api.events.EventListener;
import net.survivalboom.sbds.modules.logging.module.LoggingModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

// Тут за допомогою костилів, говна та пердуляції ми отримуємо інформацію про те хто запросив людину на сервер.
// НЕ ТРЕБА ДИВИТИСЬ СЮДИ В КОД ЯКЩО НЕ ХОЧЕТЕ ТРАВМУВАТИ СВОЮ ПСИХІКУ!!
// Жопа, срака і пиздець:
public class InviteTracker implements EventListener {
    private final LoggingModule module;

    private final Map<Long, Map<String, InviteData>> cache = new ConcurrentHashMap<>();

    private final Map<Long, ReentrantLock> guildLocks = new ConcurrentHashMap<>();

    private record InviteData(int uses, long inviterId) {}

    public InviteTracker(@NotNull LoggingModule module) {
        this.module = module;
    }

    public void init() {
        for (Guild guild : module.getSbds().getBot().getGuilds()) {
            updateGuildCache(guild);
        }
    }

    public void shutdown() {
        cache.clear();
        guildLocks.clear();
    }

    private ReentrantLock getLock(long guildId) {
        return guildLocks.computeIfAbsent(guildId, k -> new ReentrantLock());
    }

    public @NotNull CompletableFuture<@Nullable Invite> findInviter(@NotNull GuildMemberJoinEvent event) {
        CompletableFuture<Invite> future = new CompletableFuture<>();
        long guildId = event.getGuild().getIdLong();

        module.schedule("invite_track_" + event.getUser().getIdLong(), () -> {
            ReentrantLock lock = getLock(guildId);

            lock.lock();
            try {
                if (!event.getGuild().getSelfMember().hasPermission(Permission.MANAGE_SERVER)) {
                    future.complete(null);
                    return;
                }

                List<Invite> newInvites = event.getGuild().retrieveInvites().complete();
                Map<String, InviteData> oldInvites = cache.getOrDefault(guildId, new ConcurrentHashMap<>());

                Invite usedInvite = null;

                for (Invite newInvite : newInvites) {
                    InviteData oldData = oldInvites.get(newInvite.getCode());
                    if (oldData != null && newInvite.getUses() > oldData.uses()) {
                        usedInvite = newInvite;
                        break;
                    }
                }

                Map<String, InviteData> updatedInvites = new ConcurrentHashMap<>();
                for (Invite inv : newInvites) {
                    if (inv.getInviter() != null) {
                        updatedInvites.put(inv.getCode(), new InviteData(inv.getUses(), inv.getInviter().getIdLong()));
                    }
                }
                cache.put(guildId, updatedInvites);

                future.complete(usedInvite);
            } catch (Exception ex) {
                future.complete(null);
                throw new RuntimeException("Failed to track invite for user " + event.getUser().getIdLong(), ex);
            } finally {
                lock.unlock();
            }
        }, 0, 0);

        return future;
    }

    private void updateGuildCache(Guild guild) {
        long guildId = guild.getIdLong();

        module.schedule("invite_cache_" + guildId, () -> {
            ReentrantLock lock = getLock(guildId);
            lock.lock();
            try {
                if (!guild.getSelfMember().hasPermission(Permission.MANAGE_SERVER)) return;

                List<Invite> invites = guild.retrieveInvites().complete();
                Map<String, InviteData> guildInvites = new ConcurrentHashMap<>();

                for (Invite invite : invites) {
                    if (invite.getInviter() != null) {
                        guildInvites.put(invite.getCode(), new InviteData(invite.getUses(), invite.getInviter().getIdLong()));
                    }
                }
                cache.put(guildId, guildInvites);
            } catch (Exception ex) {
                module.getLogger().warn("Failed to update invite cache for guild {}", guild.getName(), ex);
            } finally {
                lock.unlock();
            }
        }, 0, 0);
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onGuildReady(GuildReadyEvent event) { updateGuildCache(event.getGuild()); }

    @SuppressWarnings("unused")
    @EventHandler
    public void onGuildJoin(GuildJoinEvent event) { updateGuildCache(event.getGuild()); }

    @SuppressWarnings("unused")
    @EventHandler
    public void onInviteCreate(GuildInviteCreateEvent event) {
        Map<String, InviteData> guildInvites = cache.get(event.getGuild().getIdLong());
        if (guildInvites != null && event.getInvite().getInviter() != null) {
            guildInvites.put(event.getInvite().getCode(), new InviteData(event.getInvite().getUses(), event.getInvite().getInviter().getIdLong()));
        }
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onInviteDelete(GuildInviteDeleteEvent event) {
        Map<String, InviteData> guildInvites = cache.get(event.getGuild().getIdLong());
        if (guildInvites != null) guildInvites.remove(event.getCode());
    }
}