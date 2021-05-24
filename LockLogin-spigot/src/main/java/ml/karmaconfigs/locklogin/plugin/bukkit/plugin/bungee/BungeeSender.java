package ml.karmaconfigs.locklogin.plugin.bukkit.plugin.bungee;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.locklogin.plugin.common.utils.DataType;
import ml.karmaconfigs.locklogin.plugin.common.utils.plugin.ServerDataStorager;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.logger;
import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.plugin;

@SuppressWarnings("UnstableApiUsage")
public final class BungeeSender {

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

            player.sendPluginMessage(plugin, "ll:account", output.toByteArray());
        } catch (Throwable ex) {
            logger.scheduleLog(Level.GRAVE, ex);
            logger.scheduleLog(Level.INFO, "Error while sending player pin GUI input to bungee");
        }
    }

    /**
     * Send to the proxy the feedback
     * of access channel
     *
     * @param recipient the player used to send the message
     * @param name this server name
     * @param id the proxy id
     * @param sub the proxy sub channel
     */
    public static void sendProxyStatus(final Player recipient, final String id, final String name, final String sub) {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        try {
            output.writeUTF(sub);
            output.writeUTF(id);
            output.writeUTF(name);

            recipient.sendPluginMessage(plugin, "ll:access", output.toByteArray());
        } catch (Throwable ex) {
            logger.scheduleLog(Level.GRAVE, ex);
            logger.scheduleLog(Level.INFO, "Error while sending proxy status to bungee");
        }
    }

    /**
     * Send a module player object to all bungeecord instance
     *
     * @param recipient the player used to send the message
     * @param player the module player serialized string
     * @param id the server id who originally sent the module player object
     */
    public static void sendPlayerInstance(final Player recipient, final String player, final String id) {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        try {
            output.writeUTF(DataType.PLAYER.name().toLowerCase());
            output.writeUTF(id);
            output.writeUTF(player);

            recipient.sendPluginMessage(plugin, "ll:plugin", output.toByteArray());
        } catch (Throwable ex) {
            logger.scheduleLog(Level.GRAVE, ex);
            logger.scheduleLog(Level.INFO, "Error while sending player instance to bungee");
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

            for (Player player : plugin.getServer().getOnlinePlayers()) {
                player.sendPluginMessage(plugin, "ll:plugin", output.toByteArray());
                return;
            }
        } catch (Throwable ex) {
            logger.scheduleLog(Level.GRAVE, ex);
            logger.scheduleLog(Level.INFO, "Error while sending module message to bungee");
        }
    }
}
