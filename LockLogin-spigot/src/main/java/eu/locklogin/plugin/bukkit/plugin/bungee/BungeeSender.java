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
import eu.locklogin.api.common.security.TokenGen;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.plugin.bukkit.LockLogin;
import eu.locklogin.plugin.bukkit.plugin.bungee.data.BungeeDataStorager;
import ml.karmaconfigs.api.common.utils.enums.Level;
import org.bukkit.entity.Player;

import static eu.locklogin.plugin.bukkit.LockLogin.plugin;

@SuppressWarnings("UnstableApiUsage")
public final class BungeeSender {

    /**
     * Send to the proxy the feedback
     * of access channel
     *
     * @param recipient the recipient used to receive and send message
     * @param id        the proxy id
     * @param sub       the proxy sub channel
     */
    public static void sendProxyStatus(final Player recipient, final String id, final String sub) {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        try {
            BungeeDataStorager storager = new BungeeDataStorager();

            String token = TokenGen.request("LOCAL_TOKEN", plugin.getServer().getName());
            if (token == null) {
                TokenGen.generate(plugin.getServer().getName());
                token = TokenGen.request("LOCAL_TOKEN", plugin.getServer().getName());
                assert token != null;
            }

            output.writeUTF(token);
            output.writeUTF(storager.getServerName());
            output.writeUTF(sub);
            output.writeUTF(id);
            if (sub.equalsIgnoreCase("register")) {
                output.writeUTF(TokenGen.expiration("LOCAL_TOKEN").toString());
            }

            recipient.sendPluginMessage(LockLogin.plugin, "ll:access", output.toByteArray());
        } catch (Throwable ex) {
            LockLogin.logger.scheduleLog(Level.GRAVE, ex);
            LockLogin.logger.scheduleLog(Level.INFO, "Error while sending proxy status to bungee");
        }
    }

    /**
     * Send the player pin input to BungeeCord
     *
     * @param player the player
     * @param pin    the player pin input
     */
    public static void sendPinInput(final Player player, final String pin) {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        try {
            BungeeDataStorager storager = new BungeeDataStorager();

            String token = TokenGen.request("LOCAL_TOKEN", plugin.getServer().getName());
            if (token == null) {
                TokenGen.generate(plugin.getServer().getName());
                token = TokenGen.request("LOCAL_TOKEN", plugin.getServer().getName());
                assert token != null;
            }

            output.writeUTF(token);
            output.writeUTF(storager.getServerName());
            output.writeUTF("pin");
            output.writeUTF(player.getUniqueId().toString());
            output.writeUTF(pin);

            player.sendPluginMessage(LockLogin.plugin, "ll:account", output.toByteArray());
        } catch (Throwable ex) {
            LockLogin.logger.scheduleLog(Level.GRAVE, ex);
            LockLogin.logger.scheduleLog(Level.INFO, "Error while sending player pin GUI input to bungee");
        }
    }

    /**
     * Send a module player object to all bungeecord instance
     *
     * @param recipient the recipient used to receive and send messages
     * @param player    the module player serialized string
     * @param id        the server id who originally sent the module player object
     */
    public static void sendPlayerInstance(final Player recipient, final String player, final String id) {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        try {
            BungeeDataStorager storager = new BungeeDataStorager();

            String token = TokenGen.request("LOCAL_TOKEN", plugin.getServer().getName());
            if (token == null) {
                TokenGen.generate(plugin.getServer().getName());
                token = TokenGen.request("LOCAL_TOKEN", plugin.getServer().getName());
                assert token != null;
            }

            output.writeUTF(token);
            output.writeUTF(storager.getServerName());
            output.writeUTF(DataType.PLAYER.name().toLowerCase());
            output.writeUTF(id);
            output.writeUTF(player);

            recipient.sendPluginMessage(LockLogin.plugin, "ll:plugin", output.toByteArray());
        } catch (Throwable ex) {
            LockLogin.logger.scheduleLog(Level.GRAVE, ex);
            LockLogin.logger.scheduleLog(Level.INFO, "Error while sending player instance to bungee");
        }
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

            String token = TokenGen.request("LOCAL_TOKEN", plugin.getServer().getName());
            if (token == null) {
                TokenGen.generate(plugin.getServer().getName());
                token = TokenGen.request("LOCAL_TOKEN", plugin.getServer().getName());
                assert token != null;
            }

            output.writeUTF(token);
            output.writeUTF(storager.getServerName());
            output.writeUTF(DataType.MODULE.name().toLowerCase());
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
