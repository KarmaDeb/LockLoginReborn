package eu.locklogin.plugin.bukkit.plugin.bungee.que;

import com.google.gson.JsonObject;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import static eu.locklogin.plugin.bukkit.LockLogin.plugin;

public class BungeeProcessQueue {

    private final static Map<UUID, BungeeProcessQueue> queues = new ConcurrentHashMap<>();

    static {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (UUID id : queues.keySet()) {
                Player player = plugin.getServer().getPlayer(id);
                if (player != null && player.isOnline()) {
                    BungeeProcessQueue queue = queues.get(id);
                    if (queue.validPlayer != null) queue.validPlayer.accept(player);
                }
            }
        }, 0, 10);
    }

    private final BlockingQueue<QueueData> messageQueue = new LinkedBlockingQueue<>();
    private Consumer<Player> validPlayer;
    private BungeeProcessQueue(final UUID id) {
        queues.put(id, this);
    }

    public void append(final JsonObject message) {
        messageQueue.add(new QueueData(message));
    }

    public QueueData nextMessage() {
        return messageQueue.poll();
    }

    public void onValid(final Consumer<Player> consumer) {
        if (validPlayer != null) return;
        validPlayer = consumer;
    }

    public static BungeeProcessQueue ofId(final UUID id) {
        return queues.computeIfAbsent(id, (queue) -> new BungeeProcessQueue(id));
    }
}
