package eu.locklogin.plugin.velocity.util;

import com.velocitypowered.api.proxy.server.ServerInfo;
import eu.locklogin.api.common.utils.plugin.ServerDataStorage;
import eu.locklogin.api.file.ProxyConfiguration;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.velocity.LockLogin;
import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.api.common.timer.SourceSecondsTimer;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import ml.karmaconfigs.api.common.utils.enums.Level;

import java.util.HashMap;
import java.util.Map;

import static eu.locklogin.plugin.velocity.LockLogin.main;
import static eu.locklogin.plugin.velocity.LockLogin.source;

public final class ServerLifeChecker {

    private final ProxyConfiguration proxy = CurrentPlatform.getProxyConfiguration();

    private static boolean restart = false;

    private final static Map<String, Integer> fail_checks = new HashMap<>();

    /**
     * Start checking for the servers
     */
    public final void startCheck() {
        SimpleScheduler timer = new SourceSecondsTimer(main, proxy.proxyLifeCheck(), true).multiThreading(true);
        timer.secondChangeAction((second) -> {
            if (restart) {
                restart = false;
                timer.cancel();
                startCheck();
            }
        }).restartAction(() -> LockLogin.server.getAllServers().forEach(server -> {
            ServerInfo info = server.getServerInfo();
            server.ping().whenComplete((result, error) -> {
                if (error != null) {
                    //If the server has players it means the plugin can't just ping it, but it's working
                    if (server.getPlayersConnected().isEmpty()) {
                        if (fail_checks.getOrDefault(info.getName(), 0) >= 1) {
                            boolean changes = false;

                            if (!ServerDataStorage.needsRegister(info.getName())) {
                                ServerDataStorage.removeKeyRegistered(info.getName());
                                changes = true;
                            }
                            if (!ServerDataStorage.needsProxyKnowledge(info.getName())) {
                                ServerDataStorage.removeProxyRegistered(info.getName());
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
