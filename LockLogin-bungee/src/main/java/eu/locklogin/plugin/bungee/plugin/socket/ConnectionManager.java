package eu.locklogin.plugin.bungee.plugin.socket;

import com.google.gson.*;
import eu.locklogin.api.common.utils.Channel;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.common.web.services.socket.SocketClient;
import eu.locklogin.plugin.bungee.com.ProxyDataSender;
import io.socket.client.Socket;
import ml.karmaconfigs.api.common.timer.scheduler.LateScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.worker.AsyncLateScheduler;
import ml.karmaconfigs.api.common.utils.enums.Level;
import net.md_5.bungee.api.config.ServerInfo;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static eu.locklogin.plugin.bungee.LockLogin.*;

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
     * @return when the socket has connected
     */
    public LateScheduler<Integer> connect(final int tries) {
        AtomicInteger tmp_tries = new AtomicInteger(0);

        LateScheduler<Integer> action = new AsyncLateScheduler<>();
        /*Socket socket = client.client();
        socket.on("welcome", (args) -> {
            socket.emit("init");
            action.complete(tmp_tries.get());
            socket.off("kick");
        });
        socket.once("kick", (message) -> {
            console.send("LockLogin web services denied our connection ({0})", Level.GRAVE, message); //Kick is the only message that is directly sent as string
            action.complete(-2);
        });
        socket.connect();

        plugin.async().queue("connect_web_services", () -> {
            long start = System.currentTimeMillis();

            while (!socket.connected()) {
                long now = System.currentTimeMillis();
                if (TimeUnit.MILLISECONDS.toSeconds(now - start) >= 30) {
                    if (tmp_tries.getAndIncrement() < tries) {
                        socket.disconnect(); //Avoid multiple connections from a single instance
                        logger.scheduleLog(Level.INFO, "Failed to connect to LockLogin web services, retrying");
                        socket.connect();
                        start = now;
                    } else {
                        logger.scheduleLog(Level.INFO, "Failed to connect to LockLogin web services after {0} tries, giving up...", tmp_tries.get());
                        action.complete(-1);
                    }
                }
            }
        });*/
        action.complete(-2);

        return action;
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

        socket.on(channel.getName(), (data) -> {
            try {
                JsonElement response = gson.fromJson(String.valueOf(data[0]), JsonElement.class);
                if (response.isJsonObject()) {
                    JsonObject object = response.getAsJsonObject();
                    if (object.has("data_type") && object.has("from")) {
                        JsonElement data_type = object.remove("data_type");
                        JsonElement from = object.remove("from");

                        if (data_type != null && data_type.isJsonPrimitive() && from != null && from.isJsonPrimitive()) {
                            JsonPrimitive primitive_type = data_type.getAsJsonPrimitive();
                            JsonPrimitive primitive_from = from.getAsJsonPrimitive();

                            if (primitive_type.isString() && primitive_from.isString()) {
                                String str_type = primitive_type.getAsString();
                                String str_from = primitive_from.getAsString();

                                if (type.name().equalsIgnoreCase(str_type)) {
                                    ServerInfo info = pds.resolve(str_from);
                                    if (info != null) {
                                        event.accept(info, object);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}
        });
    }

    /**
     * Add a listener for when a server connects
     *
     * @param consumer the server connect listener
     */
    public void onServerConnected(final BiConsumer<String, String> consumer) {
        Socket socket = client.client();
        Gson gson = new GsonBuilder().create();

        socket.on("connection", (data) -> {
            try {
                JsonElement response = gson.fromJson(String.valueOf(data[0]), JsonElement.class);
                if (response.isJsonObject()) {
                    JsonObject object = response.getAsJsonObject();
                    if (object.has("name") && object.has("aka")) {
                        JsonElement name = object.get("name");
                        JsonElement aka = object.get("aka");

                        if (name.isJsonPrimitive() && aka.isJsonPrimitive()) {
                            JsonPrimitive primitive_name = name.getAsJsonPrimitive();
                            JsonPrimitive primitive_aka = aka.getAsJsonPrimitive();

                            if (primitive_name.isString() && primitive_aka.isString()) {
                                consumer.accept(primitive_name.getAsString(), primitive_aka.getAsString());
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}
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

        socket.on("proxy", (data) -> {
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
        });
    }
}
