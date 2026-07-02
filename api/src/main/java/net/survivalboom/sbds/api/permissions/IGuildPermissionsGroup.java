package net.survivalboom.sbds.api.permissions;

import net.survivalboom.sbds.api.database.guilds.IGuildData;
import org.jetbrains.annotations.NotNull;

public interface IGuildPermissionsGroup extends IPermissionsHolder {

    @NotNull IPermissionManager getManager();

    @NotNull IGuildData getGuild();

}
