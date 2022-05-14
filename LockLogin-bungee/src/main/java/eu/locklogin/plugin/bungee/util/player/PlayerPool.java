package eu.locklogin.plugin.bungee.util.player;

import ml.karmaconfigs.api.common.timer.SchedulerUnit;
import ml.karmaconfigs.api.common.timer.SourceScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static eu.locklogin.plugin.bungee.LockLogin.plugin;

public class PlayerPool {

    private final static Set<UUID> players = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final static SimpleScheduler checkScheduler = new SourceScheduler(plugin, 1, SchedulerUnit.SECOND, true).multiThreading(true);
    private static Consumer<ProxiedPlayer> whenValidPlayer = null;

    /**
     * Add a player to the player pool
     *
     * @param id the player uuid
     */
    public static void addPlayer(final UUID id) {
        ProxiedPlayer player = plugin.getProxy().getPlayer(id);

        if (player == null || player.getServer() == null || player.getServer().getInfo() == null) {
            players.add(id);
        } else {
            if (whenValidPlayer != null) {
                whenValidPlayer.accept(player);
            }
        }
    }

    /**
     * Remove a player from the player pool
     *
     * @param id the player uuid
     */
    public static void delPlayer(final UUID id) {
        players.remove(id);
    }

    /**
     * Do an action when the player is valid
     *
     * @param valid when the player is valid
     */
    public static void whenValid(final Consumer<ProxiedPlayer> valid) {
        whenValidPlayer = valid;

        if (!checkScheduler.isRunning())
            checkScheduler.start();
    }

    /**
     * Start the check task
     */
    public static void startCheckTask() {
        checkScheduler.restartAction(() -> {
            for (UUID id : players) {
                ProxiedPlayer player = plugin.getProxy().getPlayer(id);

                if (player != null && whenValidPlayer != null && player.getServer() != null && player.getServer().getInfo() != null) {
                    players.remove(id);
                    whenValidPlayer.accept(player);
                }
            }
        });

        checkScheduler.start();
    }
}
