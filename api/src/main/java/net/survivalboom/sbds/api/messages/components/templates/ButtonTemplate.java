package net.survivalboom.sbds.api.messages.components.templates;

import net.dv8tion.jda.api.components.Component;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.survivalboom.sbds.api.messages.components.MessageInteractableComponentTemplate;
import net.survivalboom.sbds.api.messages.components.ComponentLinker;
import net.survivalboom.sbds.api.messages.parsers.StringParser;
import net.survivalboom.sbds.api.utils.CommonUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.Objects;

public class ButtonTemplate implements MessageInteractableComponentTemplate<Button> {

    private final @Nullable String name;
    private final @Nullable String url;

    private final @Nullable String label;
    private final @Nullable Emoji emoji;
    private final @NotNull ButtonStyle style;

    private final int row;
    private final int index;

    private final boolean isStatic;


    public ButtonTemplate(
            @NotNull String name,
            @Nullable String label,
            @Nullable Emoji emoji,
            @NotNull ButtonStyle style,
            int row,
            int index,
            boolean isStatic
    ) {

        Objects.requireNonNull(name, "name == null");

        this.name = name;
        this.url = null;

        this.label = label;
        this.emoji = emoji;
        this.style = style;
        this.row = row;
        this.index = index;
        this.isStatic = isStatic;

        if (label == null && emoji == null) {
            throw new IllegalArgumentException("Emoji and label == null");
        }

    }

    public ButtonTemplate(
            @NotNull String url,
            @Nullable String label,
            @Nullable Emoji emoji,
            @NotNull ButtonStyle style,
            int row,
            int index
    ) {

        Objects.requireNonNull(url, "url == null");

        this.name = null;
        this.url = url;

        this.label = label;
        this.emoji = emoji;
        this.style = style;
        this.row = row;
        this.index = index;

        this.isStatic = true;

    }

    @Override
    public int getRow() {
        return row;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public @Nullable String getName() {
        return name;
    }

    @Override
    public boolean isStatic() {
        return isStatic;
    }

    public @Nullable String getUrl() {
        return url;
    }

    // COMPONENT //

    @Override
    public @NotNull Class<Button> getComponentClass() {
        return Button.class;
    }

    @Override
    public @NotNull Component.Type getType() {
        return Component.Type.BUTTON;
    }

    @Override
    public @NotNull Button build(@Nullable StringParser parser, @Nullable ComponentLinker linker) {
        return Button.of(
                style,
                url == null ? ComponentLinker.stLink(linker, this) : url,
                StringParser.stParseNullable(parser, label),
                emoji
        );
    }

    public @NotNull Builder copy() {
        return new Builder(this);
    }

    @Override
    public String toString() {

        if (url != null) {
            return String.format(
                    "ButtonTemplate{url=%s, label=%s, emoji=%s, style=%s, index=%s, row=%s, static=%s}",
                    url,
                    label,
                    emoji,
                    style,
                    row,
                    index,
                    isStatic
            );
        }

        return String.format(
                "ButtonTemplate{name=%s, label=%s, emoji=%s, style=%s, index=%s, row=%s, static=%s}",
                name,
                label,
                emoji,
                style,
                row,
                index,
                isStatic
        );

    }

    //
    // BUILDER
    //

    public static @NotNull Builder fromSection(@NotNull ConfigurationNode section) {

        String name = section.node("name").getString();
        String url = section.node("url").getString();

        String label = section.node("label").getString();

        String styleRaw = section.node("style").getString();
        ButtonStyle style = styleRaw != null ? CommonUtils.getEnumValue(ButtonStyle.class, styleRaw) : ButtonStyle.SECONDARY;
        if (style == null) {
            throw new IllegalArgumentException("Unknown ButtonStyle `" + styleRaw + "`");
        }

        String emojiRaw = section.node("emoji").getString();
        Emoji emoji = emojiRaw != null ? Emoji.fromUnicode(emojiRaw) : null;

        int row = section.node("row").getInt();
        int index = section.node("index").getInt();

        boolean isStatic = section.node("static").getBoolean();

        return builder()
                .setName(name)
                .setUrl(url)
                .setLabel(label)
                .setStyle(style)
                .setEmoji(emoji)
                .setRow(row)
                .setIndex(index)
                .setStatic(isStatic);

    }

    public static @NotNull Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String name;

        private String url;


        private String label;

        private Emoji emoji;

        private ButtonStyle style;

        private int index;

        private int row;

        private boolean isStatic;


        private Builder() {}

        private Builder(@NotNull Builder builder) {

            this.name = builder.name;
            this.url = builder.url;

            this.label = builder.label;
            this.emoji = builder.emoji;
            this.style = builder.style;

            this.index = builder.index;
            this.row = builder.row;

            this.isStatic = builder.isStatic;

        }

        private Builder(@NotNull ButtonTemplate template) {

            this.name = template.name;
            this.url = template.url;

            this.label = template.label;
            this.emoji = template.emoji;
            this.style = template.style;

            this.index = template.index;
            this.row = template.row;

            this.isStatic = template.isStatic;

        }

        // NAME //
        
        public @NotNull Builder setName(@Nullable String name) {
            this.name = name;
            return this;
        }
        
        public String getName() {
            return name;
        }

        // URL //

        public @NotNull Builder setUrl(@Nullable String url) {
            this.url = url;
            return this;
        }

        public String getUrl() {
            return url;
        }
        
        // LABEL //

        public @NotNull Builder setLabel(@Nullable String label) {
            this.label = label;
            return this;
        }
        
        public String getLabel() {
            return label;
        }
        
        // EMOJI //

        public @NotNull Builder setEmoji(@Nullable Emoji emoji) {
            this.emoji = emoji;
            return this;
        }
        
        public Emoji getEmoji() {
            return emoji;
        }
        
        // STYLE //

        public @NotNull Builder setStyle(@NotNull ButtonStyle style) {
            this.style = style;
            return this;
        }
        
        public ButtonStyle getStyle() {
            return style;
        }
        
        // ROW //

        public @NotNull Builder setRow(int row) {

            if (row < 0 || row > 5) {
                throw new IllegalArgumentException("Row must be between 0 and 5, got " + row);
            }

            this.row = row;

            return this;

        }
        
        public int getRow() {
            return row;
        }
        
        // INDEX //

        public @NotNull Builder setIndex(int index) {
            this.index = index;
            return this;
        }
        
        public int getIndex() {
            return index;
        }
        
        // STATIC //

        public @NotNull Builder setStatic(boolean isStatic) {
            this.isStatic = isStatic;
            return this;
        }
        
        public boolean isStatic() {
            return isStatic;
        }

        // BUILD //
        
        public @NotNull ButtonTemplate build() {

            if (url != null) {
                return new ButtonTemplate(url, label, emoji, style, row, index);
            }

            return new ButtonTemplate(name, label, emoji, style, row, index, isStatic);

        }

        public @NotNull Builder copy() {
            return new Builder(this);
        }

    }

}
