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
import eu.locklogin.plugin.bukkit.LockLogin;
import ml.karmaconfigs.api.common.utils.enums.Level;
import eu.locklogin.api.common.utils.DataType;
import org.bukkit.entity.Player;

import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public final class BungeeSender {

    private static int proxy_tries = 0;
    private static int inst_tries = 0;

    /**
     * Send the player pin input to BungeeCord
     *
     * @param player the player
     * @param pin    the player pin input
     */
    public static void sendPinInput(final Player player, final String pin) {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        try {
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
     * Send to the proxy the feedback
     * of access channel
     *
     * @param uuid the player id used to send the message
     * @param name this server name
     * @param id the proxy id
     * @param sub the proxy sub channel
     */
    public static void sendProxyStatus(final String uuid, final String id, final String name, final String sub) {
        if (uuid != null) {
            Player recipient = LockLogin.plugin.getServer().getPlayer(UUID.fromString(uuid));

            if (recipient != null) {
                proxy_tries = 0;
                ByteArrayDataOutput output = ByteStreams.newDataOutput();
                try {
                    output.writeUTF(sub);
                    output.writeUTF(id);
                    output.writeUTF(name);

                    recipient.sendPluginMessage(LockLogin.plugin, "ll:access", output.toByteArray());
                } catch (Throwable ex) {
                    LockLogin.logger.scheduleLog(Level.GRAVE, ex);
                    LockLogin.logger.scheduleLog(Level.INFO, "Error while sending proxy status to bungee");
                }
            } else {
                if (proxy_tries != 3) {
                    LockLogin.plugin.getServer().getScheduler().runTaskLater(LockLogin.plugin, () -> sendProxyStatus(uuid, id, name, sub), 20 * 3);
                    proxy_tries++;
                }
            }
        }
    }

    /**
     * Send a module player object to all bungeecord instance
     *
     * @param uuid the player uuid used to send the message
     * @param player the module player serialized string
     * @param id the server id who originally sent the module player object
     */
    public static void sendPlayerInstance(final String uuid, final String player, final String id) {
        if (uuid != null) {
            Player recipient = LockLogin.plugin.getServer().getPlayer(UUID.fromString(uuid));

            if (recipient != null) {
                inst_tries = 0;

                ByteArrayDataOutput output = ByteStreams.newDataOutput();
                try {
                    output.writeUTF(DataType.PLAYER.name().toLowerCase());
                    output.writeUTF(id);
                    output.writeUTF(player);

                    recipient.sendPluginMessage(LockLogin.plugin, "ll:plugin", output.toByteArray());
                } catch (Throwable ex) {
                    LockLogin.logger.scheduleLog(Level.GRAVE, ex);
                    LockLogin.logger.scheduleLog(Level.INFO, "Error while sending player instance to bungee");
                }
            } else {
                if (inst_tries != 3) {
                    LockLogin.plugin.getServer().getScheduler().runTaskLater(LockLogin.plugin, () -> sendPlayerInstance(uuid, id, LockLogin.name), 20 * 3);
                    inst_tries++;
                }
            }
        }
    }


    /**
     * Send a plugin message to the server
     *
     * @param channel the channel name
     * @param data the data to send
     */
    public static void sendModule(final String channel, final byte[] data) {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        try {
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
