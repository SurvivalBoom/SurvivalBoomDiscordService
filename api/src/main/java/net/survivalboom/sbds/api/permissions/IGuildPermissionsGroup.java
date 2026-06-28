package net.survivalboom.sbds.api.permissions;

import net.dv8tion.jda.api.entities.Guild;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface IGuildPermissionsGroup extends IPermissionsHolder {

    @NotNull IPermissionManager getManager();

    long getId();

    @NotNull Guild getGuild();

}
