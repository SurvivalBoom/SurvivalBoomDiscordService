package net.survivalboom.sbds.core.permissions;

import net.dv8tion.jda.api.entities.Guild;
import net.survivalboom.sbds.api.permissions.*;
import net.survivalboom.sbds.api.utils.valid.Valid;
import net.survivalboom.sbds.core.permissions.records.GuildPermissionsGroupRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GuildPermissionsGroup extends AbstractPermissionHolder implements IGuildPermissionsGroup {

    private final PermissionManager manager;

    private final Guild guild;

    private final GuildPermissionsGroupRecord record;


    public GuildPermissionsGroup(@NotNull GuildPermissionsGroupRecord record, @NotNull PermissionManager manager) {
        super(manager);

        this.record = record;
        this.manager = manager;

        this.guild = manager.getSbds().getBot().getGuildById(record.getGuildId());

    }

    @Override
    public @NotNull IPermissionManager getManager() {
        return manager;
    }

    @Override
    public long getId() {
        return record.getId();
    }

    @Override
    public @NotNull Guild getGuild() {
        return guild;
    }

    @Override
    public @NotNull String getName() {
        return record.getGroupName();
    }

    public @NotNull GuildPermissionsGroupRecord getRecord() {
        return record;
    }

}
