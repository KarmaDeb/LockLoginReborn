package eu.locklogin.plugin.bukkit.listener.data;

import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.util.platform.CurrentPlatform;
import lombok.Getter;
import ml.karmaconfigs.api.common.utils.enums.Level;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.group.GroupManager;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import net.luckperms.api.platform.PlayerAdapter;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

import static eu.locklogin.plugin.bukkit.LockLogin.plugin;

/**
 * Transient permission
 */
@Getter
public final class TransientPermissionData {

    /**
     * -- GETTER --
     *  Get if the transient permission is operator
     *
     */
    private final boolean op;
    private final Set<String> permissions = new LinkedHashSet<>();
    private final Set<Node> nodes = new LinkedHashSet<>();
    private final Set<String> groups = new LinkedHashSet<>();
    private final String group;

    /**
     * Initialize the transient permission
     *
     * @param permission the permission instance
     * @param player the player
     */
    public TransientPermissionData(final LuckPerms permission, final Player player) {
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        op = player.isOp();
        if (op && config.permissionConfig().block_operator()) {
            plugin.console().send("Detected OP player, this can result in security issues for player accounts", Level.WARNING);
            player.setOp(false); //We revoke operator from player ONLY if block operator option is enabled
        }

        GroupManager groupManager = permission.getGroupManager();

        UserManager userManager = permission.getUserManager();
        PlayerAdapter<Player> adapter = permission.getPlayerAdapter(Player.class);
        User user = adapter.getUser(player);

        group = user.getPrimaryGroup(); //Thanks LuckPerms <3

        Group primaryGroup = groupManager.getGroup(group);
        Collection<Group> userGroups = user.getInheritedGroups(user.getQueryOptions());
        for (Group userGroup : userGroups) {
            if (!userGroup.equals(primaryGroup)) {
                groups.add(userGroup.getName());
            }
        }

        for (Node node : user.getNodes()) {
            for (Group group : userGroups) {
                if (!group.getNodes().contains(node)) {
                    nodes.add(node); //Add the permission only if the player doesn't have it because of a group
                }
            }
        }

        if (config.permissionConfig().remove_permissions()) {
            userManager.modifyUser(user.getUniqueId(), (modifiable) -> modifiable.data().clear((node) -> {
                nodes.add(node);
                return true;
            }));
        }
    }

    /**
     * Initialize the transient permission
     *
     * @param permission the permission instance
     * @param player the player
     */
    public TransientPermissionData(final Permission permission, final Player player) {
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        op = player.isOp();
        if (op && config.permissionConfig().block_operator()) {
            plugin.console().send("Detected OP player, this can result in security issues for player accounts", Level.WARNING);
            player.setOp(false); //We revoke operator from player ONLY if block operator option is enabled
        }

        group = permission.getPrimaryGroup(player);
        groups.addAll(Arrays.asList(permission.getPlayerGroups(player)));
        player.getEffectivePermissions().forEach((bukkitPerm) -> {
            String node = bukkitPerm.getPermission();

            for (String group : groups) {
                if (!permission.groupHas((World) null, group, node) && !permission.groupHas(player.getWorld(), group, node)) {
                    permissions.add(node); //Add the permission only if the player doesn't have it because of a group
                }
            }
        });

        if (config.permissionConfig().remove_permissions()) {
            groups.forEach((g) -> permission.playerRemoveGroup(player, g));
            permissions.forEach((p) -> permission.playerRemove(player, p));
        }
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
     * Get the permissions
     *
     * @return the permissions
     */
    public Node[] getNodes() {
        return nodes.toArray(new Node[0]);
    }

    /**
     * Get the primary group
     *
     * @return the primary group
     */
    public String getPrimaryGroup() {
        return group;
    }
}
