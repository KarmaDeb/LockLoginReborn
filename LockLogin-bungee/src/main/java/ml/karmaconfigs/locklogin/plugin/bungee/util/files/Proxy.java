package ml.karmaconfigs.locklogin.plugin.bungee.util.files;

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

import ml.karmaconfigs.api.bungee.karmayaml.FileCopy;
import ml.karmaconfigs.api.bungee.karmayaml.YamlManager;
import ml.karmaconfigs.api.bungee.karmayaml.YamlReloader;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.files.ProxyConfiguration;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static ml.karmaconfigs.locklogin.plugin.bungee.LockLogin.plugin;

public final class Proxy extends ProxyConfiguration {

    private final static File cfg_file = new File(plugin.getDataFolder(), "proxy.yml");
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
    public String proxyKey() {
        String key = cfg.getString("ProxyKey", "");
        if (StringUtils.isNullOrEmpty(key)) {
            key = StringUtils.randomString(32, StringUtils.StringGen.NUMBERS_AND_LETTERS, StringUtils.StringType.RANDOM_SIZE);
        }

        return key;
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
     * @param <T> the server type
     * @param instance the server class instance
     * @return all the available lobby servers
     */
    @Override
    public final <T> Iterator<T> lobbyServers(final Class<T> instance) {
        List<String> lobbies = cfg.getStringList("Servers.Lobby");
        Set<T> servers = new LinkedHashSet<>();
        if (lobbies.contains("*")) {
            Collection<ServerInfo> infos = plugin.getProxy().getServers().values();
            for (ServerInfo info : infos) {
                if (isAssignable(instance, info)) {
                    servers.add(instance.cast(info));
                }
            }
        } else {
            if (!lobbies.isEmpty() && arrayValid(lobbies)) {
                for (String name : lobbies) {
                    if (!name.replaceAll("\\s", "").isEmpty()) {
                        ServerInfo server = plugin.getProxy().getServerInfo(name);
                        if (server != null) {
                            AtomicBoolean online = new AtomicBoolean(false);
                            server.ping((result, error) -> {
                                if (error != null)
                                    online.set(true);
                            });

                            if (online.get()) {
                                if (isAssignable(instance, server)) {
                                    servers.add(instance.cast(server));
                                }
                            }
                        }
                    }
                }
            }
        }

        return servers.iterator();
    }

    /**
     * Get all the auth servers
     *
     * @param <T> the server type
     * @param instance the server class instance
     * @return all the available auth servers
     */
    @Override
    public final <T> Iterator<T> authServer(final Class<T> instance) {
        List<String> auths = cfg.getStringList("Servers.Auth");
        Set<T> servers = new LinkedHashSet<>();
        if (auths.contains("*")) {
            Collection<ServerInfo> infos = plugin.getProxy().getServers().values();
            for (ServerInfo info : infos) {
                if (isAssignable(instance, info)) {
                    servers.add(instance.cast(info));
                }
            }
        } else {
            servers = new LinkedHashSet<>();

            if (!auths.isEmpty() && arrayValid(auths)) {
                for (String name : auths) {
                    if (!name.replaceAll("\\s", "").isEmpty()) {
                        ServerInfo server = plugin.getProxy().getServerInfo(name);
                        if (server != null) {
                            AtomicBoolean online = new AtomicBoolean(false);
                            server.ping((result, error) -> {
                                if (error != null)
                                    online.set(true);
                            });

                            if (online.get()) {
                                if (isAssignable(instance, server)) {
                                    servers.add(instance.cast(server));
                                }
                            }
                        }
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
    public static boolean inAuth(final ProxiedPlayer player) {
        List<String> auths = cfg.getStringList("Servers.Auth");
        if (arrayValid(auths)) {
            for (String str : auths) {
                if (player.getServer().getInfo().getName().equalsIgnoreCase(str))
                    return true;
            }

            return false;
        }

        return true;
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
