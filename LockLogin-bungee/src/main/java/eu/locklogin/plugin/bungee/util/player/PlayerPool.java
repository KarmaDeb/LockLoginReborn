package eu.locklogin.plugin.bungee.util.player;

import ml.karmaconfigs.api.common.timer.SchedulerUnit;
import ml.karmaconfigs.api.common.timer.SourceScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import ml.karmaconfigs.api.common.utils.enums.Level;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static eu.locklogin.plugin.bungee.LockLogin.console;
import static eu.locklogin.plugin.bungee.LockLogin.plugin;

public class PlayerPool {

    private final Set<UUID> players = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final SimpleScheduler checkScheduler = new SourceScheduler(plugin, 1, SchedulerUnit.SECOND, true).multiThreading(true);
    private Consumer<ProxiedPlayer> whenValidPlayer = null;

    private final String poolName;

    /**
     * Initialize the player pool
     *
     * @param name the pool name
     */
    public PlayerPool(final String name) {
        poolName = name;
    }

    /**
     * Add a player to the player pool
     *
     * @param id the player uuid
     */
    public void addPlayer(final UUID id) {
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
    public void delPlayer(final UUID id) {
        players.remove(id);
    }

    /**
     * Do an action when the player is valid
     *
     * @param valid when the player is valid
     */
    public void whenValid(final Consumer<ProxiedPlayer> valid) {
        whenValidPlayer = valid;
    }

    /**
     * Start the check task
     */
    public void startCheckTask() {
        if (!checkScheduler.isRunning()) {
            console.send("Starting pool {0}", Level.INFO, poolName);

            checkScheduler.restartAction(() -> {
                for (UUID id : players) {
                    ProxiedPlayer player = plugin.getProxy().getPlayer(id);

                    if (player != null && whenValidPlayer != null && player.getServer() != null && player.getServer().getInfo() != null) {
                        players.remove(id);
                        try {
                            whenValidPlayer.accept(player);
                        } catch (Throwable ex) {
                            console.send("An exception occurred at pool {0}. More information at logs", Level.GRAVE, poolName);
                        }
                    }
                }
            });

            checkScheduler.start();
        }
    }
}
