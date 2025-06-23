package net.survivalboom.sbds.api.commands.argument.misc;

import net.survivalboom.sbds.api.commands.argument.internal.AbstractSelectArgument;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class StringSelectArgument extends AbstractSelectArgument<String> {

    public StringSelectArgument(@NotNull List<String> choices) {
        super(choices);
    }

    public StringSelectArgument(@NotNull String... choices) {
        super(choices);
    }

}
