package eu.locklogin.plugin.bungee.com;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.locklogin.api.common.communication.DataSender;
import eu.locklogin.api.common.communication.queue.DataQueue;
import eu.locklogin.api.common.utils.Channel;
import eu.locklogin.api.common.web.services.socket.SocketClient;
import eu.locklogin.plugin.bungee.com.queue.MessageQueue;
import io.socket.client.Socket;
import ml.karmaconfigs.api.common.timer.SchedulerUnit;
import ml.karmaconfigs.api.common.timer.SourceScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import net.md_5.bungee.api.config.ServerInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static eu.locklogin.plugin.bungee.LockLogin.plugin;

@SuppressWarnings("UnstableApiUsage")
public class ProxyDataSender extends DataSender {

    public final Map<String, String> server_maps = new ConcurrentHashMap<>();

    public ProxyDataSender(final SocketClient socket) {
        SimpleScheduler scheduler = new SourceScheduler(plugin, 1, SchedulerUnit.SECOND, true).multiThreading(true);
        scheduler.restartAction(() -> {
            Gson gson = new GsonBuilder().setLenient().create();

            for (String server : QueueHandler.server_queues.keySet()) {
                DataQueue queue = QueueHandler.server_queues.computeIfAbsent(server, (nm) -> new MessageQueue());
                byte[] next_data = queue.next();

                if (next_data == null) {
                    queue.cancel();
                    continue;
                }

                ByteArrayDataInput in = ByteStreams.newDataInput(next_data);
                String line = in.readUTF();

                JsonElement element = gson.fromJson(line, JsonElement.class);
                if (!element.isJsonObject()) {
                    queue.consume();
                    continue;
                }
                assert element instanceof JsonObject;

                JsonObject obj = (JsonObject) element;
                JsonElement ch = obj.remove("channel");
                if (!ch.isJsonPrimitive() || !ch.getAsJsonPrimitive().isString()) {
                    queue.consume();
                    continue;
                }
                Channel channel;

                String ch_name = ch.getAsString();
                try {
                    channel = Channel.valueOf(ch_name);
                } catch (IllegalArgumentException e) {
                    queue.consume();
                    continue;
                }

                if (queue.locked() && !channel.equals(Channel.ACCESS)) {
                    queue.shift();
                    continue;
                }

                obj.addProperty("for", server_maps.get(server));
                Socket connection = socket.client();
                connection.emit(channel.getName(), obj);
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
