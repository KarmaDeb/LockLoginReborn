package eu.locklogin.plugin.bukkit.listener.data;

import ml.karmaconfigs.api.common.utils.enums.Level;
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
            RegisteredServiceProvider<Permission> provider = plugin.getServer().getServicesManager().getRegistration(Permission.class);
            if (provider != null) {
                Permission permission = provider.getProvider();
                if (permission.isEnabled()) {
                    permissions.put(player.getUniqueId(), new TransientPermissionData(permission, player));
                }
            }
        } catch (Throwable ignored) {}
    }

    public static void apply(final Player player) {
        TransientPermissionData data = permissions.getOrDefault(player.getUniqueId(), null);

        if (data != null) {
            permissions.remove(player.getUniqueId());

            RegisteredServiceProvider<Permission> provider = plugin.getServer().getServicesManager().getRegistration(Permission.class);
            assert provider != null;
            Permission permission = provider.getProvider();

            for (String group : data.getGroups()) {
                permission.playerAddGroup(player, group);
            }
            permission.playerAddGroup(null, player, data.getPrimaryGroup()); //As I've read in spigot, this is how you defined the primary group

            for (String node : data.getPermissions()) {
                if (node.equals("*")) {
                    plugin.console().send("Player {0} had the permission '*'. LockLogin will prevent that player from having it as it may cause lot of security issues. We highly recommend you to define a good permission policy in order to keep your server safe", Level.GRAVE, player.getName());
                    permission.playerRemove(player, "*");
                } else {
                    permission.playerAdd(player, node);
                }
            }

            player.setOp(false);
            if (data.isOp()) {
                plugin.console().send("Player {0} was OP. For security reasons, LockLogin will remove op on that player as it may cause lot of security issues. If that's your account, we highly recommend you to define a good permission policy in order to keep your server safe", Level.GRAVE, player.getName());
            }
        }
    }
}
