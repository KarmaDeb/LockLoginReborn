package eu.locklogin.plugin.velocity.util.player;

import com.velocitypowered.api.proxy.Player;
import ml.karmaconfigs.api.common.timer.SourceSecondsTimer;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static eu.locklogin.plugin.velocity.LockLogin.*;

public class PlayerPool {

    private static Consumer<Player> whenValidPlayer = null;

    private final static Set<UUID> players = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final static SimpleScheduler checkScheduler = new SourceSecondsTimer(source, 1, true).multiThreading(true);

    /**
     * Add a player to the player pool
     *
     * @param id the player uuid
     */
    public static void addPlayer(final UUID id) {
        Optional<Player> player = server.getPlayer(id);

        if (!player.isPresent() || !player.get().getCurrentServer().isPresent() || player.get().getCurrentServer().get().getServer() == null) {
            players.add(id);
        } else {
            if (whenValidPlayer != null) {
                whenValidPlayer.accept(player.get());
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
    public static void whenValid(final Consumer<Player> valid) {
        whenValidPlayer = valid;

        checkScheduler.start();
    }

    /**
     * Start the check task
     */
    public static void startCheckTask() {
        checkScheduler.restartAction(() -> {
            for (UUID id : players) {
                Optional<Player> optPlayer = server.getPlayer(id);

                optPlayer.ifPresent(player -> player.getCurrentServer().ifPresent(connection -> {
                    if (connection.getServer() != null) {
                        if (whenValidPlayer != null) {
                            players.remove(id);
                            whenValidPlayer.accept(player);
                        }
                    }
                }));
            }
        });

        checkScheduler.start();
    }
}
