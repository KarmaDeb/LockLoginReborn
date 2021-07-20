package eu.locklogin.plugin.bungee.util;

import eu.locklogin.api.common.utils.plugin.ServerDataStorage;
import eu.locklogin.api.file.ProxyConfiguration;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bungee.LockLogin;
import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.api.common.timer.SourceSecondsTimer;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import ml.karmaconfigs.api.common.utils.enums.Level;

import java.util.HashMap;
import java.util.Map;

import static eu.locklogin.plugin.bungee.LockLogin.plugin;

public final class ServerLifeChecker {

    private final ProxyConfiguration proxy = CurrentPlatform.getProxyConfiguration();

    private static boolean restart = false;

    private final static Map<String, Integer> fail_checks = new HashMap<>();

    /**
     * Start checking for the servers
     */
    public final void startCheck() {
        SimpleScheduler timer = new SourceSecondsTimer(plugin, proxy.proxyLifeCheck(), true).multiThreading(true);
        timer.secondChangeAction((second) -> {
            if (restart) {
                restart = false;
                timer.cancel();
                startCheck();
            }
        }).restartAction(() -> LockLogin.plugin.getProxy().getServers().values().forEach(server -> server.ping((result, error) -> {
            if (error != null) {
                //If the server has players it means the plugin can't just ping it, but it's working
                if (server.getPlayers().isEmpty()) {
                    if (fail_checks.getOrDefault(server.getName(), 0) >= 1) {
                        boolean changes = false;

                        if (!ServerDataStorage.needsRegister(server.getName())) {
                            ServerDataStorage.removeKeyRegistered(server.getName());
                            changes = true;
                        }
                        if (!ServerDataStorage.needsProxyKnowledge(server.getName())) {
                            ServerDataStorage.removeProxyRegistered(server.getName());
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
        })));

        timer.start();
    }

    public static void restart() {
        restart = true;
        fail_checks.clear();
    }
}
