package eu.locklogin.plugin.bukkit.listener.data;

import ml.karmaconfigs.api.common.utils.enums.Level;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static eu.locklogin.plugin.bukkit.LockLogin.plugin;

/**
 * Transient permission
 */
public final class TransientPermissionData {

    private final boolean op;
    private final Set<String> permissions = new LinkedHashSet<>();
    private final Set<String> groups = new LinkedHashSet<>();
    private final String group;

    /**
     * Initialize the transient permission
     *
     * @param permission the permission instance
     * @param player the player
     */
    public TransientPermissionData(final Permission permission, final Player player) {
        op = player.isOp();
        if (op) {
            plugin.console().send("Detected OP player, this can result in security issues for player accounts", Level.WARNING);
        }

        group = permission.getPrimaryGroup(player);
        groups.addAll(Arrays.asList(permission.getPlayerGroups(player)));
        player.getEffectivePermissions().forEach((bukkitPerm) -> {
            String node = bukkitPerm.getPermission();
            boolean contains = false;
            for (String g : groups) {
                if (permission.groupHas((World) null, g, node) || permission.groupHas(player.getWorld(), g, node)) {
                    contains = true;
                    break;
                }
            }

            permission.playerRemove(player, node);
            if (!contains)
                permissions.add(node);
        });
        groups.forEach((g) -> permission.playerRemoveGroup(player, g));
    }

    /**
     * Get the groups
     *
     * @return the groups
     */
    public String[] getGroups() {
        return groups.toArray(new String[0]);
    }

    /**
     * Get the permissions
     *
     * @return the permissions
     */
    public String[] getPermissions() {
        return permissions.toArray(new String[0]);
    }

    /**
     * Get the primary group
     *
     * @return the primary group
     */
    public String getPrimaryGroup() {
        return group;
    }

    /**
     * Get if the transient permission is operator
     *
     * @return if op allowed
     */
    public boolean isOp() {
        return op;
    }
}
