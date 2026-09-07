package net.survivalboom.sbds.api.interaction.modal;

import net.dv8tion.jda.api.components.ModalTopLevelComponent;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;
import net.survivalboom.sbds.api.messages.components.ModalComponentTemplate;
import net.survivalboom.sbds.api.messages.components.templates.EntitySelectTemplate;
import net.survivalboom.sbds.api.messages.components.templates.LabelTemplate;
import net.survivalboom.sbds.api.messages.components.templates.StringSelectTemplate;
import net.survivalboom.sbds.api.messages.components.templates.TextInputTemplate;
import net.survivalboom.sbds.api.messages.parsers.StringParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class ModalTemplate {

    private final String title;

    private final List<ModalComponentTemplate<?>> components = new ArrayList<>();


    public ModalTemplate(
            @NotNull String title,
            @NotNull Collection<ModalComponentTemplate<?>> components
    ) {

        Objects.requireNonNull(title, "title == null");
        Objects.requireNonNull(components, "components == null");

        this.title = title;
        this.components.addAll(components);

    }

    //
    // BUILD
    //

    public @NotNull Modal build(@NotNull String id, @Nullable StringParser parser) {

        Objects.requireNonNull(id, "id == null");

        String title = StringParser.stParse(parser, this.title);
        Modal.Builder builder = Modal.create(id, title);

        for (ModalComponentTemplate<?> template : components) {

            ModalTopLevelComponent component = template.build(parser, null);
            builder.addComponents(component);

        }

        return builder.build();

    }

    public @NotNull Builder copy() {
        return new Builder(this);
    }

    //
    // BUILDER
    //

    public static @NotNull Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String title;

        private final List<ModalComponentTemplate<?>> components = new ArrayList<>();


        private Builder() {}

        private Builder(@NotNull Builder builder) {
            this.title = builder.title;
            this.components.addAll(builder.components);
        }

        private Builder(@NotNull ModalTemplate template) {
            this.title = template.title;
            this.components.addAll(template.components);
        }

        // TITLE //

        public @NotNull Builder setTitle(@NotNull String title) {
            Objects.requireNonNull(title, "title == null");
            this.title = title;
            return this;
        }

        public String getTitle() {
            return title;
        }

        // COMPONENTS //

        public @NotNull Builder setComponents(@Nullable Collection<ModalComponentTemplate<?>> components) {

            this.components.clear();

            if (components != null) {
                this.components.addAll(components);
            }

            return this;

        }

        public @NotNull Builder addComponents(@NotNull Collection<ModalComponentTemplate<?>> components) {
            this.components.addAll(components);
            return this;
        }

        public @NotNull Builder addComponents(@NotNull ModalComponentTemplate<?>... components) {
            this.components.addAll(List.of(components));
            return this;
        }

        public @NotNull Builder addComponent(@NotNull ModalComponentTemplate<?> component) {
            this.components.add(component);
            return this;
        }

        // INPUT //

        public @NotNull Builder addInput(
                @NotNull String name,
                @NotNull String title,
                @Nullable String description,
                @Nullable String placeholder,
                @Nullable String value,
                @NotNull TextInputStyle style,
                int min,
                int max,
                boolean required
        ) {
            this.components.add(new LabelTemplate(title, description, components.size(), new TextInputTemplate(name, min, max, required, placeholder, value, style)));
            return this;
        }

        // SELECT MENU //

        public @NotNull Builder addSelectMenu(
                @NotNull String title,
                @Nullable String description,
                @NotNull StringSelectTemplate dropdown
        ) {
            this.components.add(new LabelTemplate(title, description, components.size(), dropdown));
            return this;
        }

        public @NotNull Builder addSelectMenu(
                @NotNull String title,
                @Nullable String description,
                @NotNull EntitySelectTemplate dropdown
        ) {
            this.components.add(new LabelTemplate(title, description, components.size(), dropdown));
            return this;
        }

        //
        // BUILD
        //

        public @NotNull ModalTemplate build() {
            return new ModalTemplate(title, components);
        }

        public @NotNull Builder copy() {
            return new Builder(this);
        }

    }

}
