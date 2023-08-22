package eu.locklogin.plugin.bungee.com;

import eu.locklogin.api.common.communication.DataSender;
import eu.locklogin.api.common.communication.queue.DataQueue;
import eu.locklogin.plugin.bungee.com.queue.MessageQueue;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("UnstableApiUsage")
public class ProxyDataSender extends DataSender {

    public final Map<String, String> server_maps = new ConcurrentHashMap<>();

    /*public ProxyDataSender(final SocketClient socket) {
        SimpleScheduler scheduler = new SourceScheduler(plugin, 200, SchedulerUnit.MILLISECOND, true).multiThreading(true);
        final Set<Integer> pending_confirmation = Collections.newSetFromMap(new ConcurrentHashMap<>());
        final Set<Integer> pre_confirmed = Collections.newSetFromMap(new ConcurrentHashMap<>());

        Gson gson = new GsonBuilder().setLenient().create();

        scheduler.restartAction(() -> {
            for (String server : QueueHandler.proxy_server_queues.keySet()) {
                DataQueue queue = QueueHandler.proxy_server_queues.get(server);
                Packet next_data = queue.next();

                if (next_data == null) {
                    queue.cancel();
                } else {
                    if (ServerDataStorage.needsProxyKnowledge(server)) {
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
                                            pending_confirmation.add(id);
                                            queue.consume();

                                            SimpleScheduler waiter = new SourceScheduler(plugin, 10, SchedulerUnit.SECOND, false);
                                            waiter.endAction(() -> {
                                                if (pending_confirmation.contains(id)) {
                                                    plugin.console().send("Packet with ID: {0} didn't got a confirmation, re-sending", Level.INFO);
                                                    queue.insert(next_data);

                                                    pending_confirmation.remove(id);
                                                }
                                            });
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
                                    if (!pending_confirmation.contains(id)) {
                                        pre_confirmed.add(id);
                                    } else {
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
    }*/

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
