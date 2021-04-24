package ml.karmaconfigs.locklogin.plugin.bukkit.plugin.bungee;

import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.api.common.utils.StringUtils;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.*;

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
     * @param pin the player pin input
     */
    public static void sendPinInput(final Player player, final String pin) {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);
        try {
            out.writeUTF("pin");
            out.writeUTF(player.getUniqueId().toString());
            out.writeUTF(pin);
        } catch (Throwable ex) {
            logger.scheduleLog(Level.GRAVE, ex);
            logger.scheduleLog(Level.INFO, "Error while sending player pin GUI input to bungee");
        }

        player.sendPluginMessage(plugin, "ll_account", b.toByteArray());
    }
}
