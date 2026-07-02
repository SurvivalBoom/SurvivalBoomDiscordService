package net.survivalboom.sbds.core.permissions;

import net.survivalboom.sbds.api.database.guilds.IGuildData;
import net.survivalboom.sbds.api.permissions.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Collection;
import java.util.List;

public class GuildPermissionsGroup extends AbstractPermissionHolder implements IGuildPermissionsGroup {

    private final PermissionManager manager;

    private final String name;

    private final IGuildData guild;


    public GuildPermissionsGroup(
            @NotNull String name,
            @Nullable Collection<Permission> permissions,
            int weight,
            @NotNull IGuildData guild,
            @NotNull PermissionManager manager
    ) {
        super(manager);

        this.name = name;
        this.guild = guild;
        this.manager = manager;

        this.weight = weight;

        if (permissions != null) {
            permissions.forEach(p -> permissionMap.put(p.permission(), p));
        }

    }

    @Override
    public @NotNull IPermissionManager getManager() {
        return manager;
    }

    @Override
    protected void save() {

        ConfigurationNode node = guild.container().obtainNode(IPermissionManager.PERMISSION_CONTAINER_KEY).node(name);

        List<Permission> permissions = getPermissionList();
        try {
            node.node("permissions").setList(Permission.class, permissions);
            node.node("weight").set(weight);
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }

        guild.save();

    }

    @Override
    public @NotNull IGuildData getGuild() {
        return guild;
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

}
