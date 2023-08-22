package eu.locklogin.plugin.bukkit.listener.data;

import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.utils.enums.Level;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeEqualityPredicate;
import net.luckperms.api.platform.PlayerAdapter;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static eu.locklogin.plugin.bukkit.LockLogin.plugin;

public final class TransientMap {

    private final static Map<UUID, TransientPermissionData> permissions = new ConcurrentHashMap<>();

    public static void add(final Player player) {
        try {
            if (plugin.getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
                RegisteredServiceProvider<LuckPerms> provider = plugin.getServer().getServicesManager().getRegistration(LuckPerms.class);
                if (provider != null) {
                    LuckPerms permission = provider.getProvider();
                    permissions.put(player.getUniqueId(), new TransientPermissionData(permission, player));
                }
            } else {
                RegisteredServiceProvider<Permission> provider = plugin.getServer().getServicesManager().getRegistration(Permission.class);
                if (provider != null) {
                    Permission permission = provider.getProvider();
                    if (permission.isEnabled()) {
                        permissions.put(player.getUniqueId(), new TransientPermissionData(permission, player));
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    public static void apply(final Player player) {
        PluginConfiguration configuration = CurrentPlatform.getConfiguration();
        TransientPermissionData data = permissions.getOrDefault(player.getUniqueId(), null);

        if (data != null) {
            permissions.remove(player.getUniqueId());

            if (plugin.getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
                applyLuckPerms(player, data);
            } else if (plugin.getServer().getPluginManager().isPluginEnabled("Vault")) {
                applyVault(player, data);
            }

            if (configuration.permissionConfig().block_operator() && data.isOp()) {
                if (configuration.permissionConfig().block_operator()) {
                    plugin.console().send("Player {0} was OP. For security reasons, LockLogin will remove op on that player as it may cause lot of security issues. If that's your account, we highly recommend you to define a good permission policy in order to keep your server safe", Level.GRAVE, player.getName());
                    return;
                }

                player.setOp(true);
            }
        }
    }

    private static void applyLuckPerms(final Player player, final TransientPermissionData data) {
        PluginConfiguration config = CurrentPlatform.getConfiguration();
        RegisteredServiceProvider<LuckPerms> provider = plugin.getServer().getServicesManager().getRegistration(LuckPerms.class);
        assert provider != null;
        LuckPerms permission = provider.getProvider();

        UserManager manager = permission.getUserManager();
        PlayerAdapter<Player> adapter = permission.getPlayerAdapter(Player.class);
        User user = adapter.getUser(player);

        if (config.permissionConfig().remove_permissions()) {
            user.setPrimaryGroup(data.getPrimaryGroup());
        }
        manager.modifyUser(player.getUniqueId(), (modifiable) -> {
            for (Node node : data.getNodes()) {
                if (node.getKey().equals("*") && !config.permissionConfig().allow_wildcard()) {
                    plugin.console().send("Player {0} had the permission '*'. LockLogin will prevent that player from having it as it may cause lot of security issues. We highly recommend you to define a good permission policy in order to keep your server safe", Level.GRAVE, player.getName());
                    continue;
                }

                if (!modifiable.data().contains(node, NodeEqualityPredicate.ONLY_KEY).asBoolean() && config.permissionConfig().remove_permissions()) {
                    DataMutateResult result = modifiable.data().add(node);
                    if (!result.wasSuccessful()) {
                        plugin.console().send("Failed to grant node {0} to player {1}", Level.WARNING, node, player.getName());
                    }
                }
            }
        });
    }

    private static void applyVault(final Player player, final TransientPermissionData data) {
        PluginConfiguration config = CurrentPlatform.getConfiguration();
        RegisteredServiceProvider<Permission> provider = plugin.getServer().getServicesManager().getRegistration(Permission.class);
        assert provider != null;
        Permission permission = provider.getProvider();

        if (config.permissionConfig().remove_permissions()) {
            permission.playerAddGroup(null, player, data.getPrimaryGroup()); //As I've read in spigot, this is how you defined the primary group
            for (String group : data.getGroups()) {
                permission.playerAddGroup(player, group);
            }
        }

        for (String node : data.getPermissions()) {
            if (node.equals("*") && !config.permissionConfig().allow_wildcard()) {
                plugin.console().send("Player {0} had the permission '*'. LockLogin will prevent that player from having it as it may cause lot of security issues. We highly recommend you to define a good permission policy in order to keep your server safe", Level.GRAVE, player.getName());
                permission.playerRemove(player, "*");
            } else {
                if (config.permissionConfig().remove_permissions()) {
                    permission.playerAdd(null, player, node);
                }
            }
        }
    }
}
