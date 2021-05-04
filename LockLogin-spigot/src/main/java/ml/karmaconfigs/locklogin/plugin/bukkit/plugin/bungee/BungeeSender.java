package ml.karmaconfigs.locklogin.plugin.bukkit.plugin.bungee;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.api.common.utils.StringUtils;
import org.bukkit.entity.Player;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.logger;
import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.plugin;

@SuppressWarnings("UnstableApiUsage")
public final class BungeeSender {

    private static String key = "";

    static {
        if (key.replaceAll("\\s", "").isEmpty())
            key = StringUtils.randomString(18, StringUtils.StringGen.NUMBERS_AND_LETTERS, StringUtils.StringType.RANDOM_SIZE);
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
            output.writeUTF("pin");
            output.writeUTF(key);
            output.writeUTF(player.getUniqueId().toString());
            output.writeUTF(pin);
        } catch (Throwable ex) {
            logger.scheduleLog(Level.GRAVE, ex);
            logger.scheduleLog(Level.INFO, "Error while sending player pin GUI input to bungee");
        }

        player.sendPluginMessage(plugin, "ll:account", output.toByteArray());
    }
}
