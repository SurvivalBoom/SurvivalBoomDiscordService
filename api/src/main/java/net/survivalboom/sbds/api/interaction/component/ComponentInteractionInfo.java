package net.survivalboom.sbds.api.interaction.component;

import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.survivalboom.sbds.api.ISBDS;
import net.survivalboom.sbds.api.interaction.InteractionExecutionInfo;
import net.survivalboom.sbds.api.interaction.InteractionHolder;
import org.jetbrains.annotations.NotNull;

public class ComponentInteractionInfo<event extends GenericComponentInteractionCreateEvent> extends InteractionExecutionInfo<event> implements InteractionHolder {

    private final IComponentInteractionManager.IRegisteredComponent component;

    public ComponentInteractionInfo(
            @NotNull event event,
            @NotNull IComponentInteractionManager.IRegisteredComponent component,
            @NotNull ISBDS sbds
    ) {
        super(event, component.isEphemeral(), sbds);
        this.component = component;
    }

    @Override
    public void invalidateInputs() {
        invalidateInputs0(event.getMessage());
    }

    public @NotNull IComponentInteractionManager.IRegisteredComponent component() {
        return component;
    }

}
