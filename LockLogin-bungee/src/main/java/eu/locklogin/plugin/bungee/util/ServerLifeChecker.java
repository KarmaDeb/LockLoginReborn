package eu.locklogin.plugin.bungee.util;

import eu.locklogin.api.file.ProxyConfiguration;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bungee.LockLogin;
import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.api.common.timer.AdvancedSimpleTimer;
import ml.karmaconfigs.api.common.utils.enums.Level;
import eu.locklogin.api.common.utils.plugin.ServerDataStorager;

import java.util.HashMap;
import java.util.Map;

import static eu.locklogin.plugin.bungee.LockLogin.*;

public final class ServerLifeChecker {

    private final ProxyConfiguration proxy = CurrentPlatform.getProxyConfiguration();

    private static boolean restart = false;

    private final static Map<String, Integer> fail_checks = new HashMap<>();

    /**
     * Start checking for the servers
     */
    public final void startCheck() {
        AdvancedSimpleTimer timer = new AdvancedSimpleTimer(plugin, proxy.proxyLifeCheck(), true).setAsync(true);
        timer.addAction(() -> {
            if (restart) {
                restart = false;
                timer.setCancelled();
                startCheck();
            }
        }).addActionOnEnd(() -> LockLogin.plugin.getProxy().getServers().values().forEach(server -> server.ping((result, error) -> {
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
                            Console.send(LockLogin.plugin, "Removed this proxy and key from {0} due too many failed connection requests", Level.GRAVE, server.getName());
                        }
                        fail_checks.put(server.getName(), 0);
                    }

                    fail_checks.put(server.getName(), fail_checks.getOrDefault(server.getName(), 0) + 1);
                }
            } else {
                fail_checks.remove(server.getName());
            }
        }))).start();
    }

    public static void restart() {
        restart = true;
        fail_checks.clear();
    }
}
