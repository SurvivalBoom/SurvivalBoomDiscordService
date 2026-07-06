package net.survivalboom.sbds.modules.voice.voice;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.survivalboom.sbds.api.events.EventHandler;
import net.survivalboom.sbds.api.events.EventListener;
import net.survivalboom.sbds.api.interaction.component.ComponentInteractionInfo;
import net.survivalboom.sbds.api.modules.ModuleMain;
import net.survivalboom.sbds.api.scheduler.ISchedulerTask;
import net.survivalboom.sbds.api.utils.CommonUtils;
import net.survivalboom.sbds.api.utils.ConcurrentHashSet;
import net.survivalboom.sbds.api.utils.valid.Manager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class VoiceManager extends Manager implements EventListener {

    private static final Logger log = LoggerFactory.getLogger(VoiceManager.class.getSimpleName());

    private final ModuleMain module;

    private final ControlPanelListener controlPanelListener = new ControlPanelListener(this);


    private final Map<VoiceChannel, PrivateVoice> voiceMap = new ConcurrentHashMap<>();

    protected final Set<Member> ignoredMembers = new ConcurrentHashSet<>();


    private ISchedulerTask task;


    public VoiceManager(@NotNull ModuleMain module) {
        this.module = module;
    }

    public @NotNull ModuleMain getModule() {
        return module;
    }

    //
    // MANAGER
    //

    @Override
    protected void init0() {
        task = module.schedule(this::task, 10, 3000);
    }

    @Override
    protected void shutdown0() {

        task.tryCancel();
        task = null;

        getVoices().forEach(voice -> deleteVoice(voice).join());
        voiceMap.clear();

    }

    private void task() {

        for (PrivateVoice voice : voiceMap.values()) {

            try {
                voice.tick();
            }

            catch (Throwable t) {
                log.error("Failed to tick channel `{}`.", voice, t);
            }

        }

    }

    @EventHandler
    public void onVoiceStateUpdate(@NotNull GuildVoiceUpdateEvent event) {

        Member member = event.getMember();
        if (member.getUser().isBot()) {
            return;
        }

        if (!(event.getNewValue() instanceof VoiceChannel channel)) {
            return;
        }

        if (ignoredMembers.contains(member)) {
            return;
        }

        isVoiceCreator(channel).thenAccept(is -> {

            if (!is) {
                return;
            }

            module.schedule(() -> createVoice(member, channel).join(), 0, 0); // TODO: Це просто жахливо! Потрібно негайно переробити асинхронність у всьому боті!

        });

    }

    @EventHandler
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {

        if (!(event.getChannel() instanceof VoiceChannel channel)) {
            return;
        }

        if (event.getAuthor().isBot()) {
            return;
        }

        PrivateVoice voice = getVoice(channel);
        if (voice == null) {
            return;
        }

        if (!voice.getMembers().contains(event.getMember())) {
            return;
        }

        if (!event.getMessage().getContentRaw().equals("+")) {
            return;
        }

        channel.getHistory().retrievePast(100).queue(messages -> {

            List<Message> toDelete = messages.stream()
                    .filter(m -> !m.isPinned())
                    .filter(m -> m.getTimeCreated().isAfter(OffsetDateTime.now().minusDays(14)))
                    .toList();

            if (!toDelete.isEmpty()) {
                channel.deleteMessages(toDelete).queue(v -> voice.updateControlPanel(true));
            }

        });

    }

    @EventHandler
    public void onControlPanelDropdown(@NotNull ComponentInteractionInfo<StringSelectInteractionEvent> info) {
        controlPanelListener.onControlPanelDropdown(info);
    }

    //
    // OPERATIONS
    //

    // CREATE //

    public @NotNull CompletableFuture<PrivateVoice> createVoice(@NotNull Member member, @NotNull VoiceChannel creator) {
        checkValid();
        return createChannel(creator, member.getEffectiveName())
                .thenCompose(channel -> {

                    PrivateVoice privateVoice = new PrivateVoice(this, channel);
                    privateVoice.setOwner(member);

                    privateVoice.updateControlPanel(true);

                    log.info("Created voice channel &b{} &rin &b{}&r.", channel.getGuild().getName(), channel.getName());

                    return member.getGuild().moveVoiceMember(member, channel).submit()
                            .thenApply(v -> {
                                voiceMap.put(channel, privateVoice);
                                return privateVoice;
                            });

                });
    }

    private @NotNull CompletableFuture<VoiceChannel> createChannel(VoiceChannel creator, String name) {

        Category category = creator.getParentCategory();
        if (category == null) {
            return creator.getGuild().createVoiceChannel(name).submit();
        }

        return category.createVoiceChannel(name).submit();

    }

    // DELETE //

    public @NotNull CompletableFuture<Void> deleteVoice(@NotNull PrivateVoice voice) {

        checkValid();

        if (!voiceMap.containsKey(voice.getChannel())) {
            throw new IllegalArgumentException("Voice is not registered");
        }

        return getFallbackVoice(voice.getGuild()).thenCompose(fallback -> {

            voiceMap.remove(voice.getChannel());
            log.info("Deleted voice channel &b{} &rin &b{}&r.", voice.getChannel().getGuild().getName(), voice.getChannel().getName());

            if (fallback != null) {

                List<Member> members = voice.getMembers();
                var futures = members.stream()
                        .map(member -> member.getGuild().moveVoiceMember(member, fallback).submit())
                        .toList();

                return CommonUtils.sequenceAsync(futures)
                        .thenCompose(v -> voice.getChannel().delete().submit());

            }

            return voice.getChannel().delete().submit();

        });

    }

    //
    // SETTINGS
    //

    // VOICE CREATOR //

    public @NotNull CompletableFuture<Boolean> isVoiceCreator(@NotNull VoiceChannel channel) {
        checkValid();
        Guild guild = channel.getGuild();
        return getVoiceCreator(guild).thenApply(chnl -> Objects.equals(chnl, channel));
    }

    public @NotNull CompletableFuture<@Nullable VoiceChannel> getVoiceCreator(@NotNull Guild guild) {
        return module.getGuildConfig()
                .obtainConfig(guild)
                .get("creator", VoiceChannel.class)
                .thenApply(opt -> opt.orElse(null));
    }

    // FALLBACK VOICE //

    public @NotNull CompletableFuture<@Nullable VoiceChannel> getFallbackVoice(@NotNull Guild guild) {
        checkValid();
        return module.getGuildConfig()
                .obtainConfig(guild)
                .get("fallback", VoiceChannel.class)
                .thenApply(v -> v.orElse(null));
    }

    //
    // GETTERS
    //

    public @Nullable PrivateVoice getVoice(@NotNull VoiceChannel channel) {
        checkValid();
        return voiceMap.get(channel);
    }

    public @NotNull List<PrivateVoice> getVoices() {
        checkValid();
        return new ArrayList<>(voiceMap.values());
    }


}
