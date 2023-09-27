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
import eu.locklogin.api.common.utils.Channel;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.module.plugin.api.event.user.UserPostValidationEvent;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.security.LockLoginRuntime;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.LockLogin;
import eu.locklogin.plugin.bukkit.plugin.bungee.data.BungeeDataStorager;
import eu.locklogin.plugin.bukkit.util.player.User;
import ml.karmaconfigs.api.common.utils.enums.Level;
import org.bukkit.entity.Player;

import static eu.locklogin.plugin.bukkit.LockLogin.plugin;

@SuppressWarnings("UnstableApiUsage")
public final class BungeeSender {

    static {
        LockLoginRuntime.checkSecurity(true);
    }

    /**
     * Send to the proxy the feedback
     * of access channel
     *
     * @param sub       the proxy sub channel
     */
    public static void sendProxyStatus(final String sub) {
        JsonObject json = new JsonObject();
        Gson gson = new GsonBuilder().create();

        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        BungeeDataStorager storager = new BungeeDataStorager();

        PluginConfiguration config = CurrentPlatform.getConfiguration();

        json.addProperty("key", config.comKey());
        json.addProperty("server", storager.getServerName());
        json.addProperty("data_type", sub);
        output.writeUTF(gson.toJson(json));

        plugin.getServer().sendPluginMessage(plugin, "ll:access", output.toByteArray());
    }

    /**
     * Asks bungeecord to validate the player
     *
     * @param player the player
     */
    public static void askPlayerValidation(final Player player) {
        JsonObject json = new JsonObject();
        Gson gson = new GsonBuilder().create();

        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        BungeeDataStorager storager = new BungeeDataStorager();

        PluginConfiguration config = CurrentPlatform.getConfiguration();

        json.addProperty("key", config.comKey());
        json.addProperty("channel", Channel.ACCOUNT.getName());
        json.addProperty("server", storager.getServerName());
        json.addProperty("data_type", DataType.VALIDATION.name());
        json.addProperty("player", player.getUniqueId().toString());

        output.writeUTF(gson.toJson(json));

        plugin.getServer().sendPluginMessage(plugin, "ll:account", output.toByteArray());
    }

    /**
     * Send the player validation to BungeeCord
     *
     * @param player the player that has been validated
     */
    public static void validatePlayer(final Player player) {
        JsonObject json = new JsonObject();
        Gson gson = new GsonBuilder().create();

        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        BungeeDataStorager storager = new BungeeDataStorager();

        PluginConfiguration config = CurrentPlatform.getConfiguration();

        json.addProperty("key", config.comKey());
        json.addProperty("channel", Channel.ACCOUNT.getName());
        json.addProperty("server", storager.getServerName());
        json.addProperty("data_type", DataType.JOIN.name());
        json.addProperty("player", player.getUniqueId().toString());

        output.writeUTF(gson.toJson(json));

        plugin.getServer().sendPluginMessage(plugin, "ll:account", output.toByteArray());

        User user = new User(player);
        UserPostValidationEvent event = new UserPostValidationEvent(user.getModule(), storager.getServerName(), null);
        ModulePlugin.callEvent(event);
    }

    /**
     * Send the player pin input to BungeeCord
     *
     * @param player the player
     * @param pin    the player pin input
     */
    public static void sendPinInput(final Player player, final String pin) {
        JsonObject json = new JsonObject();
        Gson gson = new GsonBuilder().create();

        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        BungeeDataStorager storager = new BungeeDataStorager();

        PluginConfiguration config = CurrentPlatform.getConfiguration();

        json.addProperty("key", config.comKey());
        json.addProperty("server", storager.getServerName());
        json.addProperty("channel", Channel.ACCOUNT.getName());
        json.addProperty("data_type", DataType.PIN.name());
        json.addProperty("player", player.getUniqueId().toString());
        json.addProperty("pin_code", pin);
        json.addProperty("pin_input", pin);
        output.writeUTF(gson.toJson(json));

        plugin.getServer().sendPluginMessage(plugin, "ll:account", output.toByteArray());
    }

    /**
     * Send a module player object to all bungeecord instance
     *
     * @param player    the module player serialized string
     */
    public static void sendPlayerInstance(final String player) {
        JsonObject json = new JsonObject();
        Gson gson = new GsonBuilder().create();

        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        BungeeDataStorager storager = new BungeeDataStorager();

        json.addProperty("channel", Channel.PLUGIN.getName());
        json.addProperty("server", storager.getServerName());
        json.addProperty("data_type", DataType.PLAYER.name());
        json.addProperty("player_info", player);
        output.writeUTF(gson.toJson(json));

        plugin.getServer().sendPluginMessage(plugin, "ll:plugin", output.toByteArray());
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
            PluginConfiguration config = CurrentPlatform.getConfiguration();

            output.writeUTF(config.comKey());
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
