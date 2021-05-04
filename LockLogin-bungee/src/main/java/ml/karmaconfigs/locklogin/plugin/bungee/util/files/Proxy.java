package ml.karmaconfigs.locklogin.plugin.bungee.util.files;

import ml.karmaconfigs.api.bungee.karmayaml.FileCopy;
import ml.karmaconfigs.api.bungee.karmayaml.YamlManager;
import ml.karmaconfigs.api.bungee.karmayaml.YamlReloader;
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
    public boolean multiBungee() {
        return cfg.getBoolean("Options.MultiBungee", false);
    }

    /**
     * Get if the player should be sent to the
     * servers in lobby servers or auth servers
     *
     * @return if the servers are enabled
     */
    @Override
    public boolean sendToServers() {
        return cfg.getBoolean("Options.SendToServers", true);
    }

    /**
     * Get the proxy ID
     *
     * @return the proxy server ID
     */
    @Override
    public UUID getProxyID() {
        UUID uuid = UUID.randomUUID();

        if (cfg.getString("ID", "").replaceAll("\\s", "").isEmpty()) {
            cfg.set("ID", uuid);
            try {
                YamlConfiguration.getProvider(YamlConfiguration.class).save(cfg, cfg_file);
            } catch (Throwable ignored) {}
        } else {
            try {
                uuid = UUID.fromString(cfg.getString("ID", ""));
            } catch (Throwable ex) {
                cfg.set("ID", uuid);
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
    public Iterator<Object> lobbyServers() {
        List<String> lobbies = cfg.getStringList("Servers.Lobby");
        Set<Object> servers = new LinkedHashSet<>();

        for (String name : lobbies) {
            ServerInfo server = plugin.getProxy().getServerInfo(name);
            if (server != null) {
                AtomicBoolean online = new AtomicBoolean(false);
                server.ping((result, error) -> {
                    if (error != null)
                        online.set(true);
                });

                if (online.get()) {
                    servers.add(server);
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
    public Iterator<Object> authServer() {
        List<String> auths = cfg.getStringList("Servers.Auth");
        Set<Object> servers = new LinkedHashSet<>();

        for (String name : auths) {
            ServerInfo server = plugin.getProxy().getServerInfo(name);
            if (server != null) {
                AtomicBoolean online = new AtomicBoolean(false);
                server.ping((result, error) -> {
                    if (error != null)
                        online.set(true);
                });

                if (online.get()) {
                    servers.add(server);
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
        for (String str : auths) {
            if (player.getServer().getInfo().getName().equalsIgnoreCase(str))
                return true;
        }

        return false;
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
