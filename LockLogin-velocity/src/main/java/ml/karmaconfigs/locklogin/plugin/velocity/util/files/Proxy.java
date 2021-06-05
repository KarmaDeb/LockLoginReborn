package ml.karmaconfigs.locklogin.plugin.velocity.util.files;

/*
 * GNU LESSER GENERAL PUBLIC LICENSE
 * Version 2.1, February 1999
 * <p>
 * Copyright (C) 1991, 1999 Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * Everyone is permitted to copy and distribute verbatim copies
 * of this license document, but changing it is not allowed.
 * <p>
 * [This is the first released version of the Lesser GPL.  It also counts
 * as the successor of the GNU Library Public License, version 2, hence
 * the version number 2.1.]
 */

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import ml.karmaconfigs.api.bungee.Configuration;
import ml.karmaconfigs.api.bungee.YamlConfiguration;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.api.velocity.Util;
import ml.karmaconfigs.api.velocity.karmayaml.FileCopy;
import ml.karmaconfigs.api.velocity.karmayaml.YamlManager;
import ml.karmaconfigs.api.velocity.karmayaml.YamlReloader;
import ml.karmaconfigs.locklogin.api.files.ProxyConfiguration;
import ml.karmaconfigs.locklogin.plugin.velocity.LockLogin;

import java.io.File;
import java.util.*;

import static ml.karmaconfigs.locklogin.plugin.velocity.LockLogin.plugin;
import static ml.karmaconfigs.locklogin.plugin.velocity.LockLogin.server;

public final class Proxy extends ProxyConfiguration {

    private final static Util util = new Util(plugin);

    private final static File cfg_file = new File(util.getDataFolder(), "proxy.yml");
    private static Configuration cfg;

    /**
     * Initialize configuration
     */
    public Proxy() {
        if (!cfg_file.exists()) {
            FileCopy copy = new FileCopy(plugin, "cfg/proxy.yml");
            try {
                copy.copy(cfg_file);
                if (!Config.manager.reload())
                    cfg = new YamlManager(plugin, "proxy").getBungeeManager();
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }

        if (cfg == null)
            cfg = new YamlManager(plugin, "proxy").getBungeeManager();
    }

    /**
     * Get if the proxy has multiple bungeecord
     * instances
     *
     * @return if the server has multiple bungeecord instances
     */
    @Override
    public final boolean multiBungee() {
        return cfg.getBoolean("Options.MultiBungee", false);
    }

    /**
     * Get if the player should be sent to the
     * servers in lobby servers or auth servers
     *
     * @return if the servers are enabled
     */
    @Override
    public final boolean sendToServers() {
        return cfg.getBoolean("Options.SendToServers", true);
    }

    /**
     * Get the proxy key used to register this
     * proxy instance
     *
     * @return the proxy key
     */
    @Override
    public final String proxyKey() {
        String key = cfg.getString("ProxyKey", "");
        if (StringUtils.isNullOrEmpty(key)) {
            key = StringUtils.randomString(32, StringUtils.StringGen.NUMBERS_AND_LETTERS, StringUtils.StringType.RANDOM_SIZE);

            cfg.set("ProxyKey", key);
            try {
                YamlConfiguration.getProvider(YamlConfiguration.class).save(cfg, cfg_file);
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
            manager.reload();
        }

        return key;
    }

    /**
     * Get the servers check interval
     *
     * @return the servers check interval time
     */
    @Override
    public int proxyLifeCheck() {
        return cfg.getInt("ServerLifeCheck", 5);
    }

    /**
     * Get the proxy ID
     *
     * @return the proxy server ID
     */
    @Override
    public final UUID getProxyID() {
        UUID uuid = UUID.randomUUID();

        if (cfg.getString("ID", "").replaceAll("\\s", "").isEmpty()) {
            cfg.set("ID", uuid.toString());
            try {
                YamlConfiguration.getProvider(YamlConfiguration.class).save(cfg, cfg_file);
            } catch (Throwable ignored) {}
        } else {
            try {
                uuid = UUID.fromString(cfg.getString("ID", ""));
            } catch (Throwable ex) {
                cfg.set("ID", uuid.toString());
                try {
                    YamlConfiguration.getProvider(YamlConfiguration.class).save(cfg, cfg_file);
                } catch (Throwable ignored) {}
            }
        }

        return uuid;
    }

    /**
     * Get all the lobby servers
     *
     * @return all the available lobby servers
     */
    @Override
    public final <T> Iterator<T> lobbyServers(final Class<T> instance) {
        List<String> lobbies = cfg.getStringList("Servers.Lobby");
        Set<T> servers = new LinkedHashSet<>();
        if (lobbies.contains("*")) {
            Collection<RegisteredServer> registered = server.getAllServers();
            for (RegisteredServer server : registered) {
                if (isAssignable(instance, server)) {
                    servers.add(instance.cast(server));
                }
            }
        } else {
            if (!lobbies.isEmpty() && arrayValid(lobbies)) {
                for (String name : lobbies) {
                    if (!name.replaceAll("\\s", "").isEmpty()) {
                        Optional<RegisteredServer> server = LockLogin.server.getServer(name);
                        server.ifPresent(val -> {
                            if (isAssignable(instance, server))
                                servers.add(instance.cast(val));
                        });
                    }
                }
            }
        }

        return servers.iterator();
    }

    /**
     * Get all the auth servers
     *
     * @return all the available auth servers
     */
    @Override
    public final <T> Iterator<T> authServer(final Class<T> instance) {
        List<String> auths = cfg.getStringList("Servers.Auth");
        Set<T> servers = new LinkedHashSet<>();
        if (auths.contains("*")) {
            Collection<RegisteredServer> registered = server.getAllServers();
            for (RegisteredServer server : registered) {
                if (isAssignable(instance, server)) {
                    servers.add(instance.cast(server));
                }
            }
        } else {
            if (!auths.isEmpty() && arrayValid(auths)) {
                for (String name : auths) {
                    if (!name.replaceAll("\\s", "").isEmpty()) {
                        Optional<RegisteredServer> server = LockLogin.server.getServer(name);
                        server.ifPresent(val -> {
                            if (isAssignable(instance, server))
                                servers.add(instance.cast(val));
                        });
                    }
                }
            }
        }

        return servers.iterator();
    }

    /**
     * Check if the player is in an auth server
     *
     * @param player the player
     * @return if the player is in an auth server
     */
    public static boolean inAuth(final Player player) {
        List<String> auths = cfg.getStringList("Servers.Auth");
        for (String str : auths) {
            Optional<ServerConnection> tmp_server = player.getCurrentServer();

            if (tmp_server.isPresent()) {
                ServerConnection server = tmp_server.get();
                if (server.getServer().getServerInfo().getName().equalsIgnoreCase(str))
                    return true;
            }
        }

        return false;
    }

    /**
     * Get if the lobby servers are valid
     *
     * @return if the lobby servers are valid
     */
    public static boolean lobbiesValid() {
        List<String> lobbies = cfg.getStringList("Servers.Lobby");
        return arrayValid(lobbies);
    }

    /**
     * Get if the auth servers are valid
     *
     * @return if the auth servers are valid
     */
    public static boolean authsValid() {
        List<String> auths = cfg.getStringList("Servers.Auth");
        return arrayValid(auths);
    }

    /**
     * Get if the specified server is an auth server
     *
     * @param server the server to check
     * @return if the server is an auth server
     */
    public static boolean isAuth(final ServerInfo server) {
        if (server != null) {
            List<String> auths = cfg.getStringList("Servers.Auth");
            if (arrayValid(auths)) {
                return auths.contains(server.getName());
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * Get if the specified array is valid or not
     *
     * @param array the array
     * @return if the array is valid
     */
    private static boolean arrayValid(final List<String> array) {
        if (!array.isEmpty()) {
            for (String val : array) {
                if (!StringUtils.isNullOrEmpty(val))
                    return true;
            }
        }

        return false;
    }

    /**
     * Get if the instance is assignable from
     * the specified item
     *
     * @param instance the instance
     * @param item the item to check
     * @return if the item is an instance of the instance
     */
    private static boolean isAssignable(final Object instance, final Object item) {
        if (instance instanceof Class) {
            Class<?> clazz = (Class<?>) instance;
            if (item instanceof Class) {
                Class<?> itemClass = (Class<?>) item;
                return clazz.isAssignableFrom(itemClass);
            } else {
                return clazz.isAssignableFrom(item.getClass());
            }
        } else {
            if (item instanceof Class) {
                Class<?> itemClass = (Class<?>) item;
                return instance.getClass().isAssignableFrom(itemClass);
            } else {
                return instance.getClass().isAssignableFrom(item.getClass());
            }
        }
    }

    /**
     * Get the proxy configuration manager
     */
    public interface manager {

        /**
         * Reload the configuration
         *
         * @return if the configuration could be reloaded
         */
        static boolean reload() {
            try {
                YamlReloader reloader = new YamlReloader(plugin, cfg_file, "cfg/proxy.yml");
                Configuration result = reloader.reloadAndCopy();
                if (result != null) {
                    cfg = result;
                    return true;
                }

                return false;
            } catch (Throwable ex) {
                return false;
            }
        }
    }

}
