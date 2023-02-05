package eu.locklogin.plugin.bungee.com;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.locklogin.api.common.communication.DataSender;
import eu.locklogin.api.common.communication.Packet;
import eu.locklogin.api.common.communication.queue.DataQueue;
import eu.locklogin.api.common.utils.plugin.ServerDataStorage;
import eu.locklogin.api.common.web.services.socket.SocketClient;
import eu.locklogin.plugin.bungee.com.queue.MessageQueue;
import io.socket.client.Ack;
import io.socket.client.Socket;
import ml.karmaconfigs.api.common.timer.SchedulerUnit;
import ml.karmaconfigs.api.common.timer.SourceScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import ml.karmaconfigs.api.common.utils.enums.Level;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static eu.locklogin.plugin.bungee.LockLogin.*;

@SuppressWarnings("UnstableApiUsage")
public class ProxyDataSender extends DataSender {

    public final Map<String, String> server_maps = new ConcurrentHashMap<>();

    public ProxyDataSender(final SocketClient socket) {
        SimpleScheduler scheduler = new SourceScheduler(plugin, 1, SchedulerUnit.SECOND, true).multiThreading(true);
        final Map<Integer, String> pending_confirmation = new ConcurrentHashMap<>();
        final Set<Integer> pre_confirmed = Collections.newSetFromMap(new ConcurrentHashMap<>());

        Gson gson = new GsonBuilder().setLenient().create();

        scheduler.restartAction(() -> {
            for (String server : QueueHandler.proxy_server_queues.keySet()) {
                DataQueue queue = QueueHandler.proxy_server_queues.get(server);
                Packet next_data = queue.next();

                if (next_data == null) {
                    queue.cancel();
                } else {
                    if (ServerDataStorage.needsProxyKnowledge(server) || pending_confirmation.containsValue(server)) {
                        queue.cancel();
                    } else {
                        ByteArrayDataInput in = ByteStreams.newDataInput(next_data.packetData());
                        String line = in.readUTF();

                        JsonElement element = gson.fromJson(line, JsonElement.class);
                        if (!element.isJsonObject()) {
                            queue.consume();
                        } else {
                            assert element instanceof JsonObject;

                            JsonObject obj = (JsonObject) element;
                            obj.addProperty("target", server_maps.get(server));

                            Socket connection = socket.client();
                            connection.emit("out", obj, (Ack) (response) -> {
                                try {
                                    JsonObject object = gson.fromJson(response[0].toString(), JsonObject.class);
                                    if (object.has("message")) {
                                        int id = object.get("message").getAsInt();

                                        if (!pre_confirmed.contains(id)) { //There may be the case that the confirmation arrives before the ACK
                                            pending_confirmation.put(id, server);
                                        } else {
                                            queue.consume();
                                            pre_confirmed.remove(id);
                                        }
                                    } else {
                                        queue.cancel();
                                    }
                                } catch (Throwable ex) {
                                    logger.scheduleLog(Level.GRAVE, ex);
                                    queue.cancel();
                                }
                            });
                            connection.on("confirmation", (data) -> {
                                try {
                                    JsonObject response = gson.fromJson(data[0].toString(), JsonObject.class);

                                    int id = response.get("message").getAsInt();
                                    if (!pending_confirmation.containsKey(id)) {
                                        pre_confirmed.add(id);
                                    } else {
                                        queue.consume();
                                        pending_confirmation.remove(id);
                                        pre_confirmed.remove(id); //Just in case
                                    }
                                } catch (Throwable ex) {
                                    logger.scheduleLog(Level.GRAVE, ex);
                                }
                            });
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
        DataQueue stored = QueueHandler.proxy_server_queues.getOrDefault(name, null);
        if (stored == null) {
            stored = new MessageQueue();
            QueueHandler.proxy_server_queues.put(name, stored);
        }

        return stored;
    }
}
