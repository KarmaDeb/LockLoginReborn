package ml.karmaconfigs.locklogin.plugin.bungee.util;

import ml.karmaconfigs.api.bungee.Console;
import ml.karmaconfigs.api.bungee.timer.AdvancedPluginTimer;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.locklogin.plugin.bungee.util.files.Proxy;
import ml.karmaconfigs.locklogin.plugin.common.utils.plugin.ServerDataStorager;

import java.util.HashMap;
import java.util.Map;

import static ml.karmaconfigs.locklogin.plugin.bungee.LockLogin.plugin;

public final class ServerLifeChecker {

    private final Proxy proxy = new Proxy();

    private static boolean restart = false;

    private final static Map<String, Integer> fail_checks = new HashMap<>();

    /**
     * Start checking for the servers
     */
    public final void startCheck() {
        AdvancedPluginTimer timer = new AdvancedPluginTimer(plugin, proxy.proxyLifeCheck(), true).setAsync(true);
        timer.addAction(() -> {
            if (restart) {
                restart = false;
                timer.setCancelled();
                startCheck();
            }
        }).addActionOnEnd(() -> plugin.getProxy().getServers().values().forEach(server -> {
            server.ping((result, error) -> {
                if (error != null) {
                    //If the server has players it means the plugin can't just ping it, but it's working
                    if (server.getPlayers().isEmpty()) {
                        if (fail_checks.getOrDefault(server.getName(), 0) >= 1) {
                            boolean changes = false;

                            if (!ServerDataStorager.needsRegister(server.getName())) {
                                ServerDataStorager.removeKeyRegistered(server.getName());
                                changes = true;
                            }
                            if (!ServerDataStorager.needsProxyKnowledge(server.getName())) {
                                ServerDataStorager.removeProxyRegistered(server.getName());
                                changes = true;
                            }

                            if (changes) {
                                Console.send(plugin, "Removed this proxy and key from {0} due too many failed connection requests", Level.GRAVE, server.getName());
                            }
                            fail_checks.put(server.getName(), 0);
                        }

                        fail_checks.put(server.getName(), fail_checks.getOrDefault(server.getName(), 0) + 1);
                    }
                } else {
                    fail_checks.remove(server.getName());
                }
            });
        })).start();
    }

    public static void restart() {
        restart = true;
        fail_checks.clear();
    }
}
