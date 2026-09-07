package net.survivalboom.sbds.modules.logging.api.events.members;

import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.survivalboom.sbds.api.events.EventCancellableBase;
import net.survivalboom.sbds.api.modules.ModuleMain;
import org.jetbrains.annotations.NotNull;

public class UserLeaveEvent extends EventCancellableBase {

    private final GuildMemberRemoveEvent jdaEvent;

    public UserLeaveEvent(@NotNull ModuleMain module, @NotNull GuildMemberRemoveEvent jdaEvent) {
        super(module);
        this.jdaEvent = jdaEvent;
    }

    public @NotNull GuildMemberRemoveEvent getJdaEvent() {
        return jdaEvent;
    }
}