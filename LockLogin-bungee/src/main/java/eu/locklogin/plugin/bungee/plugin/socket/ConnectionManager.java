package eu.locklogin.plugin.bungee.plugin.socket;

import com.google.gson.*;
import eu.locklogin.api.common.utils.Channel;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.common.web.services.socket.SocketClient;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bungee.BungeeSender;
import eu.locklogin.plugin.bungee.Main;
import eu.locklogin.plugin.bungee.com.BungeeDataSender;
import eu.locklogin.plugin.bungee.com.ProxyDataSender;
import io.socket.client.Ack;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.timer.SchedulerUnit;
import ml.karmaconfigs.api.common.timer.SourceScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.LateScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.worker.AsyncLateScheduler;
import ml.karmaconfigs.api.common.triple.consumer.TriConsumer;
import ml.karmaconfigs.api.common.utils.enums.Level;
import net.md_5.bungee.api.config.ServerInfo;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static eu.locklogin.plugin.bungee.LockLogin.*;

@SuppressWarnings("all")
public final class ConnectionManager {

    private final SocketClient client;
    private final ProxyDataSender pds;

    public ConnectionManager(final SocketClient cl, final ProxyDataSender ds) {
        client = cl;
        pds = ds;
    }

    /**
     * Connect to the socket
     *
     * @param tries the amount of tries to perform before giving up
     * @param onceConnect the action to perform instantly if there were servers
     *                    connected
     * @param s the data sender
     * @param bs the bungee data sender
     * @return when the socket has connected
     */
    public LateScheduler<Integer> connect(final int tries, final TriConsumer<String, String, String> onceConnect, final BungeeSender s, final BungeeDataSender bs) {
        AtomicInteger tmp_tries = new AtomicInteger(0);

        Gson gson = new GsonBuilder().setLenient().create();

        LateScheduler<Integer> action = new AsyncLateScheduler<>();
        Socket socket = client.client();
        socket.on("welcome", (args) -> {
            socket.io().reconnection(true);
            socket.io().reconnectionAttempts(60);
            socket.io().reconnectionDelay(10000);

            socket.emit("auth", CurrentPlatform.getServerHash());

            try {
                KarmaMain license_data = new KarmaMain(Objects.requireNonNull(Main.class.getResourceAsStream("/license.dat")));
                if (!license_data.exists())
                    license_data.exportDefaults();

                String key = license_data.get("key").getAsString();

                JsonObject message = new JsonObject();
                message.addProperty("license", license_data.get("license").getAsString());
                message.addProperty("keyCode", key);
                message.addProperty("name", CurrentPlatform.getConfiguration().serverName());
                message.addProperty("proxy", true);

                socket.on("server_alive", (data) -> {
                    try {
                        JsonObject response = gson.fromJson(String.valueOf(data[0]), JsonObject.class);

                        String sv_name = response.get("server_name").getAsString();
                        String sv_id = response.get("server_id").getAsString();
                        String sv_hash = response.get("server_hash").getAsString();

                        onceConnect.accept(sv_name, sv_id, sv_hash);
                    } catch (Throwable ex) {
                        logger.scheduleLog(Level.INFO, ex);
                    }
                });

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

                socket.emit("init", message, (Ack) (dt) -> {
                    JsonObject response = gson.fromJson(String.valueOf(dt[0]), JsonObject.class);
                    boolean success = response.get("success").getAsBoolean();
                    String response_message = response.get("message").getAsString();

                    if (success) {
                        plugin.console().send("Message from server: {0}", Level.INFO, response_message);
                        action.complete(tmp_tries.get());

                        socket.once("disconnect", (Emitter.Listener) (reason) -> {
                            plugin.console().send("Disconnected from web services. Trying to reconnect", Level.WARNING);

                            long start = System.currentTimeMillis();
                            plugin.async().queue("check_task", () -> {
                                boolean connected = true;

                                while (!socket.connected()) {
                                    long end = System.currentTimeMillis();

                                    long diff = end - start;
                                    if (diff > 30000) {
                                        plugin.console().send("Failed to reconnect. Giving up", Level.GRAVE);
                                        BungeeSender.useSocket = false;
                                        s.sender = s.secondarySender;
                                        connected = false;
                                        break;
                                    }
                                }

                                if (connected) {
                                    plugin.console().send("Successfully reconnected", Level.INFO);
                                    invokeReconnect(tries, socket, onceConnect, s);
                                }
                            });
                        });
                    } else {
                        console.send("LockLogin web services denied our connection ({0})", Level.GRAVE, response_message);
                        action.complete(-2);
                        socket.disconnect();
                    }
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
                        socket.disconnect();
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
     * Setup the server as it was recently connected
     *
     * @param tries the connection tries
     * @param socket the socket
     * @param onceConnect actions to perform if other servers are already connected
     * @param s the sender
     */
    private void invokeReconnect(final int tries, final Socket socket, final TriConsumer<String, String, String> onceConnect, final BungeeSender s) {
        try {
            Gson gson = new GsonBuilder().create();
            KarmaMain main = new KarmaMain(Objects.requireNonNull(Main.class.getResourceAsStream("/license.dat")));

            String key = main.get("key").getAsString();

            JsonObject message = new JsonObject();
            message.addProperty("license", main.get("license").getAsString());
            message.addProperty("keyCode", key);
            message.addProperty("name", CurrentPlatform.getConfiguration().serverName());
            message.addProperty("proxy", true);

            socket.emit("auth", CurrentPlatform.getServerHash());

            socket.off("server_alive");
            socket.on("server_alive", (data) -> {
                try {
                    JsonObject response = gson.fromJson(String.valueOf(data[0]), JsonObject.class);

                    String sv_name = response.get("server_name").getAsString();
                    String sv_id = response.get("server_id").getAsString();
                    String sv_hash = response.get("server_hash").getAsString();

                    onceConnect.accept(sv_name, sv_id, sv_hash);
                } catch (Throwable ex) {
                    logger.scheduleLog(Level.INFO, ex);
                }
            });

            AtomicInteger tmp_tries = new AtomicInteger(0);
            socket.emit("init", message, (Ack) (data) -> {
                JsonObject response = gson.fromJson(String.valueOf(data[0]), JsonObject.class);
                boolean success = response.get("success").getAsBoolean();
                String response_message = response.get("message").getAsString();

                if (success) {
                    plugin.console().send("Successfully registered the proxy into web services", Level.INFO);

                    socket.off("disconnect");
                    socket.once("disconnect", (Emitter.Listener) (reason) -> {
                        plugin.console().send("Disconnected from web services. Trying to reconnect", Level.WARNING);

                        long start = System.currentTimeMillis();
                        plugin.async().queue("check_task", () -> {
                            boolean connected = true;

                            while (!socket.connected()) {
                                long end = System.currentTimeMillis();

                                long diff = end - start;
                                if (diff > 30000) {
                                    plugin.console().send("Failed to reconnect. Retrying in 1 minute", Level.GRAVE);
                                    BungeeSender.useSocket = false;
                                    s.sender = s.secondarySender;
                                    connected = false;

                                    SimpleScheduler scheduler = new SourceScheduler(plugin, 1, SchedulerUnit.MINUTE, false);
                                    scheduler.endAction(() -> {
                                        plugin.console().send("Trying to reconnect to web services", Level.INFO);
                                        socket.connect();

                                        plugin.async().queue("connect_web_services", () -> {
                                            long st = System.currentTimeMillis();

                                            boolean co = true;
                                            while (!socket.connected()) {
                                                long now = System.currentTimeMillis();
                                                if (TimeUnit.MILLISECONDS.toSeconds(now - st) >= 10) { //10 seconds timeout
                                                    if (tmp_tries.getAndIncrement() < tries) {
                                                        socket.disconnect(); //Avoid multiple connections from a single instance
                                                        logger.scheduleLog(Level.INFO, "Failed to connect to LockLogin web services, retrying");
                                                        socket.connect();
                                                        st = now;
                                                    } else {
                                                        socket.disconnect();
                                                        logger.scheduleLog(Level.INFO, "Failed to connect to LockLogin web services after {0} tries, giving up...", tmp_tries.get());
                                                        co = false;
                                                        break;
                                                    }
                                                }
                                            }

                                            if (co) {
                                                plugin.console().send("Succesfully reconnected to web services", Level.INFO);
                                                scheduler.cancel();

                                                invokeReconnect(tries, socket, onceConnect, s);
                                            }
                                        });
                                    });
                                    scheduler.start();

                                    break;
                                }
                            }

                            if (connected) {
                                plugin.console().send("Successfully reconnected", Level.INFO);
                                invokeReconnect(tries, socket, onceConnect, s);
                            }
                        });
                    });
                } else {
                    console.send("LockLogin web services denied our connection ({0})", Level.GRAVE, response_message);
                    socket.disconnect();
                }
            });
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Add a listener for the connection
     *
     * @param channel the channel to listen on
     * @param type the data type
     */
    public void addListener(final Channel channel, final DataType type, final BiConsumer<ServerInfo, JsonObject> event) {
        Socket socket = client.client();
        Gson gson = new GsonBuilder().create();

        socket.on("in", (data) -> {
            try {
                JsonObject response = gson.fromJson(String.valueOf(data[0]), JsonObject.class);

                String ch_name = response.get("channel").getAsString();
                String source = response.get("source").getAsString();
                String data_type = response.get("type").getAsString();
                ServerInfo info = plugin.getProxy().getServerInfo(source);

                if (info != null) {
                    int message_id = response.get("id").getAsInt(); //Unused for now...
                    logger.scheduleLog(Level.INFO, "Received web service message at {0} with id {1}", ch_name, message_id);
                    if (channel.name().equalsIgnoreCase(ch_name)) {
                        JsonObject message_data = response.get("data").getAsJsonObject();
                        if (type.name().equalsIgnoreCase(data_type)) {
                            event.accept(info, message_data);
                        }
                    }
                }
            } catch (Throwable ex) {
                logger.scheduleLog(Level.GRAVE, ex);
            }
        });
    }

    /**
     * Add a listener for when a server connects
     *
     * @param consumer the server connect listener
     */
    public void onServerConnected(final TriConsumer<String, String, String> consumer) {
        Socket socket = client.client();
        Gson gson = new GsonBuilder().create();

        socket.on("server_join", (data) -> {
            try {
                JsonObject response = gson.fromJson(String.valueOf(data[0]), JsonObject.class);

                String sv_name = response.get("server_name").getAsString();
                String sv_id = response.get("server_id").getAsString();
                String sv_hash = response.get("server_hash").getAsString();

                consumer.accept(sv_name, sv_id, sv_hash);
            } catch (Throwable ex) {
                ex.printStackTrace();
                logger.scheduleLog(Level.INFO, ex);
            }
        });
    }

    /**
     * Add a listener for when a server disconnects
     *
     * @param consumer the server disconnect listener
     */
    public void onServerDisconnected(final BiConsumer<String, String> consumer) {
        Socket socket = client.client();
        Gson gson = new GsonBuilder().create();

        socket.on("server_leave", (data) -> {
            try {
                JsonObject response = gson.fromJson(String.valueOf(data[0]), JsonObject.class);
                String sv_name = response.get("server").getAsString();
                String reason = response.get("cause").getAsString();

                consumer.accept(sv_name, reason);
            } catch (Throwable ex) {
                logger.scheduleLog(Level.INFO, ex);
            }
        });
    }

    /**
     * Add a listener for when a proxy connects
     *
     * @param consumer the proxy connect listener
     */
    public void onProxyConnected(final Consumer<String> consumer) {
        Socket socket = client.client();
        Gson gson = new GsonBuilder().create();

        /*socket.on("proxy", (data) -> {
            try {
                JsonElement response = gson.fromJson(String.valueOf(data[0]), JsonElement.class);
                if (response.isJsonObject()) {
                    JsonObject object = response.getAsJsonObject();
                    if (object.has("address")) {
                        JsonElement address = object.get("address");

                        if (address.isJsonPrimitive()) {
                            JsonPrimitive primitive_address = address.getAsJsonPrimitive();

                            if (primitive_address.isString()) {
                                consumer.accept(primitive_address.getAsString());
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}
        });*/
    }
}
