package eu.locklogin.plugin.velocity.util;

import com.velocitypowered.api.proxy.server.ServerInfo;
import eu.locklogin.api.file.ProxyConfiguration;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.velocity.LockLogin;
import eu.locklogin.api.common.utils.plugin.ServerDataStorager;
import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.api.common.timer.AdvancedSimpleTimer;
import ml.karmaconfigs.api.common.utils.enums.Level;

import java.util.HashMap;
import java.util.Map;

import static eu.locklogin.plugin.velocity.LockLogin.*;

public final class ServerLifeChecker {

    private final ProxyConfiguration proxy = CurrentPlatform.getProxyConfiguration();

    private static boolean restart = false;

    private final static Map<String, Integer> fail_checks = new HashMap<>();

    /**
     * Start checking for the servers
     */
    public final void startCheck() {
        AdvancedSimpleTimer timer = new AdvancedSimpleTimer(main, proxy.proxyLifeCheck(), true).setAsync(true);
        timer.addAction(() -> {
            if (restart) {
                restart = false;
                timer.setCancelled();
                startCheck();
            }
        }).addActionOnEnd(() -> LockLogin.server.getAllServers().forEach(server -> {
            ServerInfo info = server.getServerInfo();
            server.ping().whenComplete((result, error) -> {
                if (error != null) {
                    //If the server has players it means the plugin can't just ping it, but it's working
                    if (server.getPlayersConnected().isEmpty()) {
                        if (fail_checks.getOrDefault(info.getName(), 0) >= 1) {
                            boolean changes = false;

                            if (!ServerDataStorager.needsRegister(info.getName())) {
                                ServerDataStorager.removeKeyRegistered(info.getName());
                                changes = true;
                            }
                            if (!ServerDataStorager.needsProxyKnowledge(info.getName())) {
                                ServerDataStorager.removeProxyRegistered(info.getName());
                                changes = true;
                            }

                            if (changes) {
                                Console.send(source, "Removed this proxy and key from {0} due too many failed connection requests", Level.GRAVE, info.getName());
                            }
                            fail_checks.put(info.getName(), 0);
                        }

                        fail_checks.put(info.getName(), fail_checks.getOrDefault(info.getName(), 0) + 1);
                    }
                } else {
                    fail_checks.remove(info.getName());
                }
            }).exceptionally(throwable -> {
                fail_checks.put(info.getName(), fail_checks.getOrDefault(info.getName(), 0) + 1);
                return null;
            });
        })).start();
    }

    public static void restart() {
        restart = true;
        fail_checks.clear();
    }
}
