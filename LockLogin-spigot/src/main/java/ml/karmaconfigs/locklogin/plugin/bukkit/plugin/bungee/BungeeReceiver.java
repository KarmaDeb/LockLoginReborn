package ml.karmaconfigs.locklogin.plugin.bukkit.plugin.bungee;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.files.PluginConfiguration;
import ml.karmaconfigs.locklogin.api.utils.platform.CurrentPlatform;
import ml.karmaconfigs.locklogin.plugin.bukkit.plugin.bungee.data.AccountParser;
import ml.karmaconfigs.locklogin.plugin.bukkit.plugin.bungee.data.BungeeDataStorager;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.Config;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.data.LastLocation;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.data.Spawn;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.inventory.AltAccountsInventory;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.inventory.PinInventory;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.inventory.PlayersInfoInventory;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.player.ClientVisor;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.player.User;
import ml.karmaconfigs.locklogin.plugin.common.security.client.IpData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.logger;

@SuppressWarnings("UnstableApiUsage")
public final class BungeeReceiver implements PluginMessageListener {

    /**
     * Listens for incoming plugin messages
     *
     * @param channel the channel
     * @param player  the player used to send the message
     * @param bytes   the message bytes
     */
    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] bytes) {
        ByteArrayDataInput input = ByteStreams.newDataInput(bytes);
        try {
            String sub = input.readUTF();
            String key = input.readUTF();

            BungeeDataStorager storager = new BungeeDataStorager(key);

            if (storager.validate()) {
                User user = new User(player);
                ClientSession session = user.getSession();
                PluginConfiguration config = CurrentPlatform.getConfiguration();

                switch (channel.toLowerCase()) {
                    case "ll:account":
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

                                session.setLogged(input.readBoolean());
                                session.set2FALogged(input.readBoolean());
                                session.setPinLogged(input.readBoolean());
                                user.setRegistered(input.readBoolean());
                                break;
                            case "quit":
                                InetSocketAddress ip = player.getAddress();
                                if (user.isLockLoginUser()) {
                                    if (ip != null) {
                                        IpData data = new IpData(ip.getAddress());
                                        data.delClone();
                                    }

                                    //Last location will be always saved since if the server
                                    //owner wants to enable it, it would be good to see
                                    //the player last location has been stored to avoid
                                    //location problems
                                    LastLocation last_loc = new LastLocation(player);
                                    last_loc.save();

                                    session.invalidate();
                                    session.setLogged(false);
                                    session.setPinLogged(false);
                                    session.set2FALogged(false);

                                    user.removeLockLoginUser();
                                }

                                user.setTempSpectator(false);
                                break;
                            case "validate":
                                session.validate();
                                break;
                            case "captchalog":
                                if (!session.isCaptchaLogged()) {
                                    session.setCaptchaLogged(true);
                                }
                                break;
                            case "login":
                                if (!session.isLogged()) {
                                    session.setLogged(true);
                                }

                                if (config.takeBack()) {
                                    LastLocation location = new LastLocation(player);
                                    location.teleport();
                                }

                                break;
                            case "pin":
                                String pin_sub = input.readUTF();

                                switch (pin_sub.toLowerCase()) {
                                    case "open":
                                        if (!session.isPinLogged()) {
                                            PinInventory inventory = new PinInventory(player);
                                            inventory.open();
                                        }
                                        break;
                                    case "close":
                                        if (player.getOpenInventory().getTopInventory().getHolder() instanceof PinInventory) {
                                            PinInventory inventory = new PinInventory(player);
                                            inventory.close();
                                        }
                                        session.setPinLogged(true);
                                        break;
                                    default:
                                        logger.scheduleLog(Level.GRAVE, "Unknown pin sub channel: " + pin_sub);
                                        break;
                                }
                                break;
                            case "2fa":
                                session.set2FALogged(true);

                                ClientVisor visor = new ClientVisor(player);
                                visor.authenticate();
                                break;
                            case "unlogin":
                                if (session.isLogged()) {
                                    session.setLogged(false);
                                    session.set2FALogged(false);
                                    session.setPinLogged(false);
                                }

                                break;
                            case "effects":
                                boolean status = input.readBoolean();

                                if (status) {
                                    user.savePotionEffects();
                                    user.applySessionEffects();
                                } else {
                                    user.restorePotionEffects();
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
                    case "ll:plugin":
                        switch (sub.toLowerCase()) {
                            case "messages":
                                String messageString = input.readUTF();
                                Message.manager.loadBungee(messageString);
                                break;
                            case "configuration":
                                String configString = input.readUTF();
                                Config.manager.loadBungee(configString);
                            case "logged_amount":
                                storager.setLoggedAccounts(input.readInt());
                                break;
                            case "register_amount":
                                storager.setRegisteredAccounts(input.readInt());
                                break;
                            case "info":
                                String infoMessage = input.readUTF();
                                infoMessage = StringUtils.replaceLast(infoMessage.replaceFirst("AccountParser\\(", ""), "\\)", "");

                                String[] infoData = infoMessage.split(";");
                                Set<AccountManager> infoAccounts = new HashSet<>();
                                for (String str : infoData) {
                                    AccountParser parser = new AccountParser(str);
                                    AccountManager manager = parser.parse();
                                    if (manager != null) {
                                        infoAccounts.add(parser.parse());
                                    }
                                }

                                storager.updateAccounts(infoAccounts.toArray(new AccountManager[0]));

                                new PlayersInfoInventory(infoAccounts, player);
                                break;
                            case "lookup":
                                String lookupMessage = input.readUTF();
                                lookupMessage = StringUtils.replaceLast(lookupMessage.replaceFirst("AccountParser\\(", ""), ")", "");

                                String[] lookupData = lookupMessage.split(";");
                                Set<AccountManager> lookupAccounts = new HashSet<>();
                                for (String str : lookupData) {
                                    AccountParser parser = new AccountParser(str);
                                    AccountManager manager = parser.parse();
                                    if (manager != null) {
                                        lookupAccounts.add(parser.parse());
                                    }
                                }

                                storager.updateAccounts(lookupAccounts.toArray(new AccountManager[0]));

                                new AltAccountsInventory(lookupAccounts, player);
                                break;
                            default:
                                logger.scheduleLog(Level.GRAVE, "Unknown plugin sub channel message: " + sub);
                                break;
                        }
                        break;
                }
            } else {
                logger.scheduleLog(Level.GRAVE, "Someone tried to access the plugin message channel with an invalid key ( {0} )", key);
            }
        } catch (Throwable ex) {
            logger.scheduleLog(Level.GRAVE, ex);
            logger.scheduleLog(Level.INFO, "Failed to read bungeecord message");
        }
    }
}
