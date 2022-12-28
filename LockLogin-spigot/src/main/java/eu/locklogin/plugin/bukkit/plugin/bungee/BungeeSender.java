package eu.locklogin.plugin.bukkit.plugin.bungee;

/*
 * GNU LESSER GENERAL PUBLIC LICENSE
 * Version 2.1, February 1999
 * <p>
 * Copyright (C) 1991, 1999 Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * Everyone is permitted to copy and distribute verbatim copies
 * of this license document, but changing it is not allowed.
 * <p>
 * [This is the first released version of the Lesser GPL.  It also counts
 * as the successor of the GNU Library Public License, version 2, hence
 * the version number 2.1.]
 */

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.common.web.services.socket.SocketClient;
import eu.locklogin.api.module.plugin.api.event.user.UserPostValidationEvent;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.plugin.bukkit.LockLogin;
import eu.locklogin.plugin.bukkit.plugin.bungee.data.BungeeDataStorager;
import eu.locklogin.plugin.bukkit.util.player.User;
import io.socket.client.Ack;
import io.socket.client.Socket;
import ml.karmaconfigs.api.common.utils.enums.Level;
import org.bukkit.entity.Player;

import static eu.locklogin.plugin.bukkit.LockLogin.plugin;

@SuppressWarnings("UnstableApiUsage")
public final class BungeeSender {

    /**
     * Send to the proxy the feedback
     * of access channel
     *
     * @param sub       the proxy sub channel
     */
    public static void sendProxyStatus(final String sub) {
        System.out.println("Sending status");
        JsonObject json = new JsonObject();
        Gson gson = new GsonBuilder().create();

        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        BungeeDataStorager storager = new BungeeDataStorager();

        json.addProperty("key", BungeeReceiver.proxy_com);
        json.addProperty("server", storager.getServerName());
        json.addProperty("data_type", sub);
        if (BungeeReceiver.usesSocket) {
            json.addProperty("for", ""); //Sockets does not use access channel, for now...
        }
        output.writeUTF(gson.toJson(json));

        if (BungeeReceiver.usesSocket) {
            /*SocketClient socket = BungeeReceiver.client;
            Socket client = socket.client();

            client.emit("ll:access", json, (Ack) (response) -> {
                JsonObject r = gson.fromJson(String.valueOf(response[0]), JsonObject.class);
                if (!r.get("success").getAsBoolean()) {
                    sendProxyStatus(recipient, id, sub);
                }
            });*/
        } else {
            plugin.getServer().sendPluginMessage(plugin, "ll:access", output.toByteArray());

            /*try {
                 output.writeUTF(BungeeReceiver.proxy_com);
                 output.writeUTF(storager.getServerName());
                 output.writeUTF(sub);
                 output.writeUTF(id);
                if (sub.equalsIgnoreCase("register")) {
                    output.writeUTF(TokenGen.expiration("local_token").toString());
                }

                recipient.sendPluginMessage(LockLogin.plugin, "ll:access", output.toByteArray());
            } catch (Throwable ex) {
                LockLogin.logger.scheduleLog(Level.GRAVE, ex);
                LockLogin.logger.scheduleLog(Level.INFO, "Error while sending proxy status to bungee");
            }*/
        }
    }

    /**
     * Send the player validation to BungeeCord
     *
     * @param player the player that has been validated
     */
    public static void validatePlayer(final Player player, final String f) {
        JsonObject json = new JsonObject();
        Gson gson = new GsonBuilder().create();

        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        BungeeDataStorager storager = new BungeeDataStorager();

        json.addProperty("key", BungeeReceiver.proxy_com);
        json.addProperty("server", storager.getServerName());
        json.addProperty("data_type", DataType.JOIN.name());
        json.addProperty("player", player.getUniqueId().toString());
        if (BungeeReceiver.usesSocket) {
            json.addProperty("for", f);
        }
        output.writeUTF(gson.toJson(json));

        if (BungeeReceiver.usesSocket) {
            SocketClient socket = BungeeReceiver.client;
            Socket client = socket.client();

            client.emit("ll:account", json, (Ack) (response) -> {
                JsonObject r = gson.fromJson(String.valueOf(response[0]), JsonObject.class);
                if (!r.get("success").getAsBoolean()) {
                    validatePlayer(player, f);
                } else {
                    User user = new User(player);
                    UserPostValidationEvent event = new UserPostValidationEvent(user.getModule(), storager.getServerName(), null);
                    ModulePlugin.callEvent(event);
                }
            });
        } else {
            plugin.getServer().sendPluginMessage(plugin, "ll:account", output.toByteArray());

            User user = new User(player);
            UserPostValidationEvent event = new UserPostValidationEvent(user.getModule(), storager.getServerName(), null);
            ModulePlugin.callEvent(event);
        }

        /*try {
            output.writeUTF(BungeeReceiver.proxy_com);
            output.writeUTF(storager.getServerName());
            output.writeUTF("join");
            output.writeUTF(player.getUniqueId().toString());

            player.sendPluginMessage(LockLogin.plugin, "ll:account", output.toByteArray());

            User user = new User(player);
            UserPostValidationEvent event = new UserPostValidationEvent(user.getModule(), storager.getServerName(), null);
            ModulePlugin.callEvent(event);
        } catch (Throwable ex) {
            LockLogin.logger.scheduleLog(Level.GRAVE, ex);
            LockLogin.logger.scheduleLog(Level.INFO, "Error while sending player pin GUI input to bungee");
        }*/
    }

    /**
     * Send the player pin input to BungeeCord
     *
     * @param player the player
     * @param pin    the player pin input
     */
    public static void sendPinInput(final Player player, final String pin, final String f) {
        JsonObject json = new JsonObject();
        Gson gson = new GsonBuilder().create();

        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        BungeeDataStorager storager = new BungeeDataStorager();

        json.addProperty("key", BungeeReceiver.proxy_com);
        json.addProperty("server", storager.getServerName());
        json.addProperty("data_type", DataType.PIN.name());
        json.addProperty("player", player.getUniqueId().toString());
        json.addProperty("pin_code", pin);
        if (BungeeReceiver.usesSocket) {
            json.addProperty("for", f);
        }
        output.writeUTF(gson.toJson(json));

        if (BungeeReceiver.usesSocket) {
            SocketClient socket = BungeeReceiver.client;
            Socket client = socket.client();

            client.emit("ll:account", json, (Ack) (response) -> {
                JsonObject r = gson.fromJson(String.valueOf(response[0]), JsonObject.class);
                if (!r.get("success").getAsBoolean()) {
                    sendPinInput(player, pin, f);
                }
            });
        } else {
            plugin.getServer().sendPluginMessage(plugin, "ll:account", output.toByteArray());
        }

        /*try {
            output.writeUTF(BungeeReceiver.proxy_com);
            output.writeUTF(storager.getServerName());
            output.writeUTF("pin");
            output.writeUTF(player.getUniqueId().toString());
            output.writeUTF(pin);

            player.sendPluginMessage(LockLogin.plugin, "ll:account", output.toByteArray());
        } catch (Throwable ex) {
            LockLogin.logger.scheduleLog(Level.GRAVE, ex);
            LockLogin.logger.scheduleLog(Level.INFO, "Error while sending player pin GUI input to bungee");
        }*/
    }

    /**
     * Send a module player object to all bungeecord instance
     *
     * @param player    the module player serialized string
     * @param id        the server id who originally sent the module player object
     */
    public static void sendPlayerInstance(final String player, final String id) {
        JsonObject json = new JsonObject();
        Gson gson = new GsonBuilder().create();

        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        BungeeDataStorager storager = new BungeeDataStorager();

        json.addProperty("key", BungeeReceiver.proxy_com);
        json.addProperty("server", storager.getServerName());
        json.addProperty("data_type", DataType.PLAYER.name());
        json.addProperty("player_info", player);
        if (BungeeReceiver.usesSocket) {
            json.addProperty("for", id);
        }
        output.writeUTF(gson.toJson(json));

        if (BungeeReceiver.usesSocket) {
            SocketClient socket = BungeeReceiver.client;
            Socket client = socket.client();

            client.emit("ll:plugin", json, (Ack) (response) -> {
                JsonObject r = gson.fromJson(String.valueOf(response[0]), JsonObject.class);
                if (!r.get("success").getAsBoolean()) {
                    sendPlayerInstance(player, id);
                }
            });
        } else {
            plugin.getServer().sendPluginMessage(plugin, "ll:plugin", output.toByteArray());
        }
        /*try {
            output.writeUTF(BungeeReceiver.proxy_com);
            output.writeUTF(storager.getServerName());
            output.writeUTF(DataType.PLAYER.name().toLowerCase());
            output.writeUTF(id);
            output.writeUTF(player);

            recipient.sendPluginMessage(LockLogin.plugin, "ll:plugin", output.toByteArray());
        } catch (Throwable ex) {
            LockLogin.logger.scheduleLog(Level.GRAVE, ex);
            LockLogin.logger.scheduleLog(Level.INFO, "Error while sending player instance to bungee");
        }*/
    }


    /**
     * Send a plugin message to the server
     *
     * @param channel the channel name
     * @param data    the data to send
     */
    public static void sendModule(final String channel, final byte[] data) {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        try {
            BungeeDataStorager storager = new BungeeDataStorager();

           /*String token = TokenGen.request("local_token", plugin.getServer().getName());
            if (token == null) {
                TokenGen.generate(plugin.getServer().getName());
                token = TokenGen.request("local_token", plugin.getServer().getName());
                assert token != null;
            }*/

            output.writeUTF(BungeeReceiver.proxy_com);
            output.writeUTF(storager.getServerName());
            output.writeUTF(DataType.LISTENER.name().toLowerCase());
            output.writeUTF(channel);
            output.writeInt(data.length);
            output.write(data);

            for (Player player : LockLogin.plugin.getServer().getOnlinePlayers()) {
                player.sendPluginMessage(LockLogin.plugin, "ll:plugin", output.toByteArray());
                return;
            }
        } catch (Throwable ex) {
            LockLogin.logger.scheduleLog(Level.GRAVE, ex);
            LockLogin.logger.scheduleLog(Level.INFO, "Error while sending module message to bungee");
        }
    }
}
