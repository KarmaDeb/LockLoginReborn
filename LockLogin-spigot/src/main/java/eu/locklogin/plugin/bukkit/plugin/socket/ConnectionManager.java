package eu.locklogin.plugin.bukkit.plugin.socket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import eu.locklogin.api.common.utils.Channel;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.common.web.services.socket.SocketClient;
import eu.locklogin.api.util.platform.CurrentPlatform;
import io.socket.client.Socket;
import ml.karmaconfigs.api.common.timer.scheduler.LateScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.worker.AsyncLateScheduler;
import ml.karmaconfigs.api.common.utils.enums.Level;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static eu.locklogin.plugin.bukkit.LockLogin.*;

@SuppressWarnings("all")
public final class ConnectionManager {

    private final SocketClient client;

    public ConnectionManager(final SocketClient cl) {
        client = cl;
    }

    /**
     * Connect to the socket
     *
     * @param tries the amount of tries to perform before giving up
     * @return when the socket has connected
     */
    public LateScheduler<Integer> connect(final int tries) {
        AtomicInteger tmp_tries = new AtomicInteger(0);

        Gson gson = new GsonBuilder().create();

        LateScheduler<Integer> action = new AsyncLateScheduler<>();
        Socket socket = client.client();
        socket.on("welcome", (args) -> {
            socket.io().reconnection(true);
            socket.io().reconnectionAttempts(60);
            socket.io().reconnectionDelay(10000);

            socket.on("message", (data) -> {
                if (data.length >= 1) {
                    try {
                        JsonObject response = gson.fromJson(String.valueOf(data[0]), JsonObject.class);

                        String msg = response.get("message").getAsString();
                        int level = response.get("level").getAsInt();

                        switch (level) {
                            case 0:
                                plugin.console().send(" (LockLogin WS) {0}", Level.OK, msg);
                                break;
                            case 1:
                                plugin.console().send(" (LockLogin WS) {0}", Level.INFO, msg);
                                break;
                            case 2:
                                plugin.console().send(" (LockLogin WS) {0}", Level.WARNING, msg);
                                break;
                            case 3:
                                plugin.console().send(" (LockLogin WS) {0}", Level.GRAVE, msg);
                                break;
                            default:
                                plugin.console().send("Message from server: {0}", msg);
                                break;
                        }
                    } catch (Throwable ex) {
                        plugin.console().send("Message from server: {0}", Level.INFO, data[0]);
                    }
                }
            });

            socket.emit("auth", CurrentPlatform.getServerHash());

            try {
                action.complete(tmp_tries.get());

                socket.on("disconnect", (reason) -> {
                    plugin.console().send("Disconnected from web services. Trying to reconnect", Level.WARNING);

                    long start = System.currentTimeMillis();
                    plugin.async().queue("check_task", () -> {
                        boolean connected = true;

                        while (!socket.connected()) {
                            long end = System.currentTimeMillis();

                            long diff = end - start;
                            if (diff > 30000) {
                                plugin.console().send("Failed to reconnect. Giving up", Level.GRAVE);
                                connected = false;
                                break;
                            }
                        }

                        if (connected) {
                            plugin.console().send("Successfully reconnected. Proxy servers may need to refresh this server memory", Level.INFO);
                        }
                    });
                });
            } catch (Throwable ex) {
                logger.scheduleLog(Level.GRAVE, ex);
            }

            socket.off("kick");
        });
        socket.once("decline", (message) -> {
            console.send("LockLogin web services denied our connection ({0})", Level.GRAVE, message); //Kick is the only message that is directly sent as string
            action.complete(-2);
            socket.disconnect();
        });
        socket.connect();

        plugin.async().queue("connect_web_services", () -> {
            long start = System.currentTimeMillis();

            while (!socket.connected()) {
                long now = System.currentTimeMillis();
                if (TimeUnit.MILLISECONDS.toSeconds(now - start) >= 10) { //10 seconds timeout
                    if (tmp_tries.getAndIncrement() < tries) {
                        socket.disconnect(); //Avoid multiple connections from a single instance
                        logger.scheduleLog(Level.INFO, "Failed to connect to LockLogin web services, retrying");
                        socket.connect();
                        start = now;
                    } else {
                        logger.scheduleLog(Level.INFO, "Failed to connect to LockLogin web services after {0} tries, giving up...", tmp_tries.get());
                        action.complete(-1);
                        break;
                    }
                }
            }
        });
        //action.complete(-2);

        return action;
    }

    /**
     * Add a listener for the connection
     *
     * @param channel the channel to listen on
     * @param type the data type
     * @param event the event consumer
     * @param reply the reply
     */
    public void addListener(final Channel channel, final DataType type, final Consumer<JsonObject> event) {
        Socket socket = client.client();
        Gson gson = new GsonBuilder().create();

        socket.on("in", (data) -> {
            try {
                JsonObject response = gson.fromJson(String.valueOf(data[0]), JsonObject.class);

                String ch_name = response.get("channel").getAsString();
                String data_type = response.get("type").getAsString();

                int message_id = response.get("id").getAsInt(); //Unused for now...
                logger.scheduleLog(Level.INFO, "Received web service message at {0} with id {1}", ch_name, message_id);
                if (channel.name().equalsIgnoreCase(ch_name)) {
                    JsonObject message_data = response.get("data").getAsJsonObject();
                    if (type.name().equalsIgnoreCase(data_type)) {
                        event.andThen((reply) -> {
                            if (reply != null) {

                            }
                        }).accept(message_data);
                    }
                }
            } catch (Throwable ex) {
                logger.scheduleLog(Level.GRAVE, ex);
            }
        });
    }

    public void onProxyInitialization(final Consumer<String> consumer) {
        Socket socket = client.client();
        Gson gson = new GsonBuilder().create();

        socket.on("proxy_init", (data) -> {
            try {
                JsonObject response = gson.fromJson(String.valueOf(data[0]), JsonObject.class);

                String nm = response.get("server").getAsString();
                consumer.accept(nm);
            } catch (Throwable ex) {
                logger.scheduleLog(Level.GRAVE, ex);
            }
        });
    }
}
