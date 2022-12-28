package eu.locklogin.plugin.bungee.com;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import eu.locklogin.api.common.communication.DataSender;
import eu.locklogin.api.common.communication.queue.DataQueue;
import eu.locklogin.api.common.web.services.socket.SocketClient;
import eu.locklogin.plugin.bungee.com.queue.MessageQueue;
import io.socket.client.Ack;
import io.socket.client.Socket;
import ml.karmaconfigs.api.common.timer.SchedulerUnit;
import ml.karmaconfigs.api.common.timer.SourceScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import ml.karmaconfigs.api.common.utils.enums.Level;
import net.md_5.bungee.api.config.ServerInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static eu.locklogin.plugin.bungee.LockLogin.console;
import static eu.locklogin.plugin.bungee.LockLogin.plugin;

@SuppressWarnings("UnstableApiUsage")
public class ProxyDataSender extends DataSender {

    private final Map<String, String> server_maps = new ConcurrentHashMap<>();

    public ProxyDataSender(final SocketClient socket) {
        SimpleScheduler scheduler = new SourceScheduler(plugin, 1, SchedulerUnit.SECOND, true).multiThreading(true);
        scheduler.restartAction(() -> {
            Gson gson = new GsonBuilder().setLenient().create();

            Map<String, Long> wait = new ConcurrentHashMap<>();
            for (String server : QueueHandler.server_queues.keySet()) {
                if (server_maps.containsKey(server)) {
                    DataQueue queue = QueueHandler.server_queues.computeIfAbsent(server, (nm) -> new MessageQueue());
                    byte[] next_data = queue.next();

                    if (next_data != null) { //Being read or no data to send... Who knows?
                        ByteArrayDataInput in = ByteStreams.newDataInput(next_data);
                        String line = in.readUTF();

                        JsonObject json = gson.fromJson(line, JsonObject.class);
                        String channel = json.remove("channel").getAsString();
                        json.addProperty("for", server_maps.get(server));

                        ByteArrayDataOutput out = ByteStreams.newDataOutput();
                        out.writeUTF(gson.toJson(json));

                        Socket connection = socket.client();
                        wait.put(server, System.currentTimeMillis());
                        connection.emit(channel, json, (Ack) (r) -> {
                            JsonObject response = gson.fromJson(String.valueOf(r[0]), JsonObject.class);
                            if (response.get("success").getAsBoolean()) {
                                queue.consume();
                            } else {
                                queue.cancel();
                            }
                        });
                    } else {
                        if (wait.containsKey(server)) {
                            long msWaited = System.currentTimeMillis() - wait.get(server);
                            if (msWaited >= TimeUnit.SECONDS.toMillis(70)) {
                                queue.cancel();
                                wait.remove(server);
                                console.send("Failed to send message to server {0} after 1 minute. Trying again", Level.INFO, server);
                            }
                        }
                    }
                }
            }
        });

        scheduler.start();
    }

    /**
     * Get the data queue
     *
     * @param name the queue name
     * @return the data queue
     */
    @Override
    public DataQueue queue(final String name) {
        return QueueHandler.server_queues.computeIfAbsent(name, (nm) -> new MessageQueue());
    }

    /**
     * Map a server
     *
     * @param server the server
     * @param secret the server secret
     */
    public void addMap(final ServerInfo server, final String secret) {
        server_maps.put(server.getName(), secret);
    }

    /**
     * Check if the server has been mapped
     *
     * @param server the server to check
     * @return if the server has been mapped
     */
    public boolean hasMap(final ServerInfo server) {
        return server_maps.containsKey(server.getName());
    }

    /**
     * Resolve a secret to fetch its server
     *
     * @param secret the server secret
     * @return the server if known
     */
    public ServerInfo resolve(final String secret) {
        for (String name : server_maps.keySet()) {
            ServerInfo info = plugin.getProxy().getServerInfo(name);
            if (info != null) {
                String value = server_maps.getOrDefault(name, "");
                if (value.equals(secret)) {
                    return info;
                }
            }
        }

        return null;
    }
}
