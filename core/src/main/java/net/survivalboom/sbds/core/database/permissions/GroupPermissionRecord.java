package net.survivalboom.sbds.core.database.permissions;

import jakarta.persistence.*;
import net.survivalboom.sbds.api.database.DataRecord;
import net.survivalboom.sbds.api.permissions.Permission;
import org.jetbrains.annotations.NotNull;

@Entity
@Table(name = "sbds_gp")
public class GroupPermissionRecord extends DataRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private long guildId;

    @Column(name = "group_name", nullable = false)
    private String groupName;

    @Column(nullable = false)
    private String permission;

    @Column(nullable = false)
    private boolean value;

    // Getter
    public String getGroupName() {
        return groupName;
    }

    // Setter
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public @NotNull Permission toPermission() {
        return new Permission(permission, value);
    }
}
