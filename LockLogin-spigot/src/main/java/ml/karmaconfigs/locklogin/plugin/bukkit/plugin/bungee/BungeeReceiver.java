package ml.karmaconfigs.locklogin.plugin.bukkit.plugin.bungee;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.configuration.Config;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.data.LastLocation;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.data.Spawn;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.player.User;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.*;

public final class BungeeReceiver implements PluginMessageListener {

    /**
     * Listens for incoming plugin messages
     *
     * @param channel the channel
     * @param player the player used to send the message
     * @param bytes the message bytes
     */
    @SuppressWarnings("all")
    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] bytes) {
        ByteArrayDataInput input = ByteStreams.newDataInput(bytes);

        String sub = input.readUTF();
        String key = input.readUTF();

        BungeeDataStorager storager = new BungeeDataStorager(key);

        if (storager.validate()) {
            User user = new User(player);
            ClientSession session = user.getSession();
            Config config = new Config();

            switch (channel.toLowerCase()) {
                case "ll_account":
                    switch (sub.toLowerCase()) {
                        case "join":
                            if (!user.isLockLoginUser()) {
                                user.applyLockLoginUser();
                            }
                            session.validate();

                            if (config.enableSpawn()) {
                                Spawn spawn = new Spawn(player.getWorld());
                                spawn.teleport(player);
                            }

                            break;
                        case "captchalog":
                            if (!session.isCaptchaLogged()) {
                                session.setCaptchaLogged(true);
                            }
                            break;
                        case "login":
                            if (!session.isLogged()) {
                                session.setLogged(true);
                                storager.addLogin();
                            }
                            
                            if (config.takeBack()) {
                                LastLocation location = new LastLocation(player);
                                location.teleport();
                            }

                            break;
                        case "templog":
                            if (!session.isTempLogged()) {
                                session.setTempLogged(true);
                            }

                            break;
                        case "unlogin":
                            if (session.isLogged()) {
                                session.setLogged(false);
                                session.setTempLogged(false);
                                storager.restLogin();
                            }

                            break;
                        case "effects":
                            boolean status = input.readBoolean();
                            boolean nausea = input.readBoolean();

                            if (status) {
                                user.restorePotionEffects();
                            } else {
                                user.savePotionEffects();
                                user.applySessionEffects(nausea);
                            }
                            break;
                        case "invalidate":
                            session.invalidate();
                            break;
                        default:
                            logger.scheduleLog(Level.GRAVE, "Unknown account sub channel message: " + sub);
                            break;
                    }
                    break;
                case "ll_plugin":
                    switch (sub.toLowerCase()) {
                        case "messages":
                            String messageString = input.readUTF();
                            Message.manager.loadBungee(messageString);
                            break;
                        case "logged_ammount":
                            storager.setLoggedAccounts(input.readInt());
                            break;
                        case "register_amount":
                            storager.setRegisteredAccounts(input.readInt());
                            break;
                        default:
                            logger.scheduleLog(Level.GRAVE, "Unknown plugin sub channel message: " + sub);
                            break;
                    }
                    break;
            }
        } else {
            logger.scheduleLog(Level.GRAVE, "Someone tried to access the plugin message channel with an invalid key");
        }
    }
}
