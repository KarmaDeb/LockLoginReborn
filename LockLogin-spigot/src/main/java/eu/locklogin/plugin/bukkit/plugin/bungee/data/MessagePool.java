package eu.locklogin.plugin.bukkit.plugin.bungee.data;

import com.google.common.io.ByteArrayDataInput;
import ml.karmaconfigs.api.common.timer.SchedulerUnit;
import ml.karmaconfigs.api.common.timer.SourceScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import ml.karmaconfigs.api.common.utils.TriConsumer;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static eu.locklogin.plugin.bukkit.LockLogin.plugin;

public final class MessagePool {

    private final static Map<UUID, ByteArrayDataInput> players = new ConcurrentHashMap<>();
    private final static Map<UUID, String> channels = new ConcurrentHashMap<>();
    private final static SimpleScheduler checkScheduler = new SourceScheduler(plugin, 1, SchedulerUnit.SECOND, true).multiThreading(true);
    private static TriConsumer<String, Player, ByteArrayDataInput> whenValidPlayer = null;

    /**
     * Add a player to the player pool
     *
     * @param channel the message channel
     * @param id      the player uuid
     * @param data    the message data
     */
    public static void addPlayer(final String channel, final UUID id, final ByteArrayDataInput data) {
        Player player = plugin.getServer().getPlayer(id);

        if (player == null) {
            players.put(id, data);
            channels.put(id, channel);
        } else {
            if (whenValidPlayer != null) {
                whenValidPlayer.accept(channel, player, data);
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
        channels.remove(id);
    }

    /**
     * Do an action when the player is valid
     *
     * @param valid when the player is valid
     */
    public static void whenValid(final TriConsumer<String, Player, ByteArrayDataInput> valid) {
        whenValidPlayer = valid;

        if (!checkScheduler.isRunning())
            checkScheduler.start();
    }

    /**
     * Start the check task
     */
    public static void startCheckTask() {
        checkScheduler.restartAction(() -> {
            for (UUID id : players.keySet()) {
                Player player = plugin.getServer().getPlayer(id);

                if (player != null && whenValidPlayer != null) {
                    whenValidPlayer.accept(channels.remove(id), player, players.remove(id));
                }
            }
        });

        checkScheduler.start();
    }
}
