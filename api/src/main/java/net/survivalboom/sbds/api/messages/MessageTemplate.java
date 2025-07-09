package net.survivalboom.sbds.api.messages;

import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.survivalboom.sbds.api.utils.Placeholders;
import net.survivalboom.sbds.api.utils.TypeMap;
import org.bspfsystems.yamlconfiguration.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

public class MessageTemplate {

    private final String content;

    private final List<EmbedTemplate> embeds;

    private final List<Component> components;


    private MessageTemplate(
            @Nullable String content,
            @NotNull List<EmbedTemplate> embeds,
            @NotNull List<Component> components
    ) {

        this.content = content;
        this.components = components;
        this.embeds = embeds;

    }


    public @NotNull String buildString(@Nullable Placeholders placeholders) {

        if (!embeds.isEmpty() || !components.isEmpty() || content == null) {
            return toString();
        }

        return Placeholders.parse(content, placeholders);

    }

    public @NotNull MessageCreateData build(@Nullable Function<Component, String> componentIdCreator, @NotNull Function<String, String> parser) {

        Objects.requireNonNull(parser, "parser == null");

        if (!components.isEmpty()) {
            Objects.requireNonNull(componentIdCreator, "componentIdCreator == null while components.size() > 0");
        }

        MessageCreateBuilder builder = new MessageCreateBuilder();
        if (content != null) builder.setContent(parser.apply(content));

        // embeds //
        embeds.forEach(t -> builder.addEmbeds(t.build(parser)));

        // components //
        appendActionRows(builder, componentIdCreator, parser);

        return builder.build();

    }

    private void appendActionRows(@NotNull MessageCreateBuilder builder, @Nullable Function<Component, String> componentIdCreator, @NotNull Function<String, String> parser) {

        Objects.requireNonNull(componentIdCreator, "componentIdCreator == null");

        appendActionRow(builder, 1, componentIdCreator, parser);
        appendActionRow(builder, 2, componentIdCreator, parser);
        appendActionRow(builder, 3, componentIdCreator, parser);
        appendActionRow(builder, 4, componentIdCreator, parser);
        appendActionRow(builder, 5, componentIdCreator, parser);

    }

    private void appendActionRow(@NotNull MessageCreateBuilder builder, int row, @NotNull Function<Component, String> componentIdCreator, @NotNull Function<String, String> parser) {

        List<ItemComponent> list = components.stream()
                        .filter(c -> c.row() == row)
                        .sorted(Comparator.comparing(Component::priority))
                        .map(c -> c.build(componentIdCreator, parser))
                        .toList();

        if (list.isEmpty()) return;

        builder.addActionRow(list);

    }

    @Override
    public String toString() {
        return "MessageTemplate{content=" + content + ", embeds=" + embeds.size() + ", components=" + components.size() + "}";
    }

    //
    // BUILDER
    //

    public static class Builder {

        @Nullable
        private String content;

        private final List<EmbedTemplate> embeds;

        private final List<Component> components;


        private Builder(@Nullable String content, @NotNull List<EmbedTemplate> embeds, @NotNull List<Component> components) {
            this.content = content;
            this.embeds = embeds;
            this.components = components;
        }


        // CONTENT //
        public @NotNull Builder setContent(@Nullable String content) {
            this.content = content;
            return this;
        }

        // COMPONENTS //

        public @NotNull Builder addComponent(@NotNull Component component) {
            components.add(component);
            return this;
        }

        public @NotNull Builder addComponents(@NotNull Collection<Component> components) {
            this.components.addAll(components);
            return this;
        }

        public @NotNull Builder addComponents(@NotNull Component... components) {
            this.components.addAll(List.of(components));
            return this;
        }

        // EMBEDS //

        public @NotNull Builder addEmbed(@NotNull EmbedTemplate embed) {
            this.embeds.add(embed);
            return this;
        }

        public @NotNull Builder addEmbeds(@NotNull Collection<EmbedTemplate> embeds) {
            this.embeds.addAll(embeds);
            return this;
        }

        public @NotNull Builder addEmbeds(@NotNull EmbedTemplate... embeds) {
            this.embeds.addAll(List.of(embeds));
            return this;
        }

        // BUILD//

        public @NotNull MessageTemplate build() {
            return new MessageTemplate(content, embeds, components);
        }

        public @NotNull Builder copy() {

            // Перевірка на дебіла.
            if (content == null && embeds.isEmpty()) {
                throw new IllegalStateException("Empty message. content == null && embeds.size() == 0");
            }

            return new Builder(content, embeds, components);

        }

    }


    //
    // STATIC
    //

    public static @NotNull Builder builder() {
        return new Builder(null, new ArrayList<>(), new ArrayList<>());
    }

    public static @NotNull MessageTemplate fromSection(@NotNull ConfigurationSection section) throws InvalidEmbedException, InvalidComponentException {

        String content = section.getString("$content");
        List<EmbedTemplate> embeds = createEmbeds(section);

        List<Component> components = Component.createComponents(TypeMap.ofMapList(section.getMapList("$components")));

        return new MessageTemplate(content, embeds, components);

    }

    public static @NotNull MessageTemplate fromContent(@NotNull String content) {
        return new MessageTemplate(content, new ArrayList<>(), new ArrayList<>());
    }

    private static @NotNull List<EmbedTemplate> createEmbeds(@NotNull ConfigurationSection section) throws InvalidEmbedException {

        Objects.requireNonNull(section, "section == null");

        List<EmbedTemplate> out = new ArrayList<>();

        if (!section.contains("$embed") && !section.contains("$embeds")) return new ArrayList<>();

        if (!section.contains("$embeds")) {

            ConfigurationSection embedSection = section.getConfigurationSection("$embed");
            Objects.requireNonNull(embedSection);

            EmbedTemplate embed = EmbedTemplate.fromSection(embedSection);

            return new ArrayList<>(List.of(embed));

        }


        List<Map<?, ?>> map = section.getMapList("$embeds");
        for (Map<?, ?> m : map) {
            out.add(EmbedTemplate.fromSection(m));
        }

        return out;

    }

    // TODO: Зробити перевірку на правильність кількості компонентів на повідомленні.
    private static void checkComponentValidation(@NotNull List<Component> componentList) throws InvalidComponentException {

        if (componentList.stream().anyMatch(c -> c.type() == net.dv8tion.jda.api.interactions.components.Component.Type.BUTTON) && componentList.stream().anyMatch(c -> c.type() != net.dv8tion.jda.api.interactions.components.Component.Type.BUTTON)) {
            throw new InvalidComponentException("");
        }

    }

}
