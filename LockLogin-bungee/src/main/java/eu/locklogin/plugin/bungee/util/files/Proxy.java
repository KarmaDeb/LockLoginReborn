package eu.locklogin.plugin.bungee.util.files;

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

import ml.karmaconfigs.api.common.karmafile.karmayaml.FileCopy;
import ml.karmaconfigs.api.common.karmafile.karmayaml.KarmaYamlManager;
import ml.karmaconfigs.api.common.karmafile.karmayaml.YamlReloader;
import ml.karmaconfigs.api.common.utils.StringUtils;
import eu.locklogin.api.file.ProxyConfiguration;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.io.File;
import java.util.*;

import static eu.locklogin.plugin.bungee.LockLogin.plugin;

public final class Proxy extends ProxyConfiguration {

    private final static File cfg_file = new File(plugin.getDataFolder(), "proxy.yml");
    private static KarmaYamlManager cfg;

    /**
     * Initialize configuration
     */
    public Proxy() {
        if (cfg == null) {
            if (!cfg_file.exists()) {
                FileCopy copy = new FileCopy(plugin, "cfg/proxy.yml");
                try {
                    copy.copy(cfg_file);
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }

            cfg = new KarmaYamlManager(cfg_file);
        }
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

            cfg.set("ProxyKey", key);
            cfg.save(cfg_file, plugin, "cfg/proxy.yml");
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
            cfg.save(cfg_file, plugin, "cfg/proxy.yml");
        } else {
            try {
                uuid = UUID.fromString(cfg.getString("ID", ""));
            } catch (Throwable ex) {
                cfg.set("ID", uuid.toString());
                cfg.save(cfg_file, plugin, "cfg/proxy.yml");
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
    public final <T> List<T> lobbyServers(final Class<T> instance) {
        List<String> lobbies = cfg.getStringList("Servers.Lobby");
        List<T> servers = Collections.synchronizedList(new ArrayList<>());
        if (lobbies.contains("*")) {
            Collection<ServerInfo> infos = plugin.getProxy().getServers().values();
            for (ServerInfo info : infos) {
                if (isAssignable(info.getClass(), instance)) {
                    servers.add(instance.cast(info));
                }
            }
        } else {
            if (!lobbies.isEmpty() && arrayValid(lobbies)) {
                Collection<ServerInfo> infos = plugin.getProxy().getServers().values();
                for (ServerInfo info : infos) {
                    if (lobbies.contains(info.getName())) {
                        if (isAssignable(info.getClass(), instance)) {
                            servers.add(instance.cast(info));
                        }
                    }
                }
            }
        }

        return servers;
    }

    /**
     * Get all the auth servers
     *
     * @param <T> the server type
     * @param instance the server class instance
     * @return all the available auth servers
     */
    @Override
    public final <T> List<T> authServer(final Class<T> instance) {
        List<String> auths = cfg.getStringList("Servers.Auth");
        List<T> servers = Collections.synchronizedList(new ArrayList<>());
        if (auths.contains("*")) {
            Collection<ServerInfo> infos = plugin.getProxy().getServers().values();
            for (ServerInfo info : infos) {
                if (isAssignable(info.getClass(), instance)) {
                    servers.add(instance.cast(info));
                }
            }
        } else {
            if (!auths.isEmpty() && arrayValid(auths)) {
                Collection<ServerInfo> infos = plugin.getProxy().getServers().values();
                for (ServerInfo info : infos) {
                    if (auths.contains(info.getName())) {
                        if (isAssignable(info.getClass(), instance)) {
                            servers.add(instance.cast(info));
                        }
                    }
                }
            }
        }

        return servers;
    }

    /**
     * Check if the player is in an auth server
     *
     * @param player the player
     * @return if the player is in an auth server
     */
    public static boolean inAuth(final ProxiedPlayer player) {
        return isAuth(player.getServer().getInfo());
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
     * @param check the item to check
     * @param type the instance
     * @return if the item is an instance of the instance
     */
    private static boolean isAssignable(final Class<?> check, final Class<?> type) {
        return type.isAssignableFrom(check);
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
                YamlReloader reloader = cfg.getReloader();
                if (reloader != null) {
                    reloader.reload();
                    return true;
                }

                return false;
            } catch (Throwable ex) {
                return false;
            }
        }
    }
}
