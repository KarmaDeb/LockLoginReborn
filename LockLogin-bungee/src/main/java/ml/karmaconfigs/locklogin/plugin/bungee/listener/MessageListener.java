package ml.karmaconfigs.locklogin.plugin.bungee.listener;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.encryption.CryptoUtil;
import ml.karmaconfigs.locklogin.api.encryption.HashType;
import ml.karmaconfigs.locklogin.api.modules.javamodule.JavaModuleManager;
import ml.karmaconfigs.locklogin.api.modules.event.user.UserAuthenticateEvent;
import ml.karmaconfigs.locklogin.plugin.bungee.plugin.sender.DataSender;
import ml.karmaconfigs.locklogin.plugin.bungee.plugin.sender.DataType;
import ml.karmaconfigs.locklogin.plugin.bungee.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.bungee.util.player.User;
import ml.karmaconfigs.locklogin.plugin.common.session.SessionDataContainer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.UUID;

import static ml.karmaconfigs.locklogin.plugin.bungee.LockLogin.*;

@SuppressWarnings("UnstableApiUsage")
public final class MessageListener implements Listener {

    private static String token = "";

    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onMessageReceive(PluginMessageEvent e) {
        if (!e.isCancelled()) {
            Message messages = new Message();

            ByteArrayDataInput input = ByteStreams.newDataInput(e.getData());

            //LockLogin bungeecord is only supposed to listen account channel
            if (e.getTag().equalsIgnoreCase("ll:account")) {
                String sub = input.readUTF();
                String key = input.readUTF();

                if (token.replaceAll("\\s", "").isEmpty()) {
                    token = CryptoUtil.getBuilder().withPassword(key).build().hash(HashType.SHA512, true);
                }

                //LockLogin bungeecord is also only supposed to listen pin sub channel
                if (sub.equalsIgnoreCase("pin")) {
                    if (CryptoUtil.getBuilder().withPassword(key).withToken(token).build().validate()) {
                        String id = input.readUTF();
                        UUID uuid = UUID.fromString(id);
                        String pin = input.readUTF();

                        ProxiedPlayer player = plugin.getProxy().getPlayer(uuid);
                        if (player != null && player.isConnected()) {
                            User user = new User(player);
                            ClientSession session = user.getSession();
                            AccountManager manager = user.getManager();

                            if (session.isValid()) {
                                if (manager.getPin().replaceAll("\\s", "").isEmpty()) {
                                    DataSender.send(player, DataSender.getBuilder(DataType.PIN, DataSender.CHANNEL_PLAYER).addTextData("close").build());

                                    UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PIN,
                                            (manager.has2FA() ? UserAuthenticateEvent.Result.SUCCESS_TEMP : UserAuthenticateEvent.Result.SUCCESS), fromPlayer(player),
                                            (manager.has2FA() ? messages.gAuthInstructions() : messages.logged()), null);
                                    JavaModuleManager.callEvent(event);

                                    user.send(messages.prefix() + event.getAuthMessage());
                                    session.setPinLogged(true);

                                    SessionDataContainer.setLogged(SessionDataContainer.getLogged() + 1);
                                } else {
                                    if (CryptoUtil.getBuilder().withPassword(pin).withToken(manager.getPin()).build().validate() || pin.equalsIgnoreCase("error")) {
                                        DataSender.send(player, DataSender.getBuilder(DataType.PIN, DataSender.CHANNEL_PLAYER).addTextData("close").build());

                                        UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PIN,
                                                (manager.has2FA() ? UserAuthenticateEvent.Result.SUCCESS_TEMP : UserAuthenticateEvent.Result.SUCCESS), fromPlayer(player),
                                                (manager.has2FA() ? messages.gAuthInstructions() : messages.logged()), null);
                                        JavaModuleManager.callEvent(event);

                                        user.send(messages.prefix() + event.getAuthMessage());
                                        session.setPinLogged(true);

                                        if (!manager.has2FA())
                                            SessionDataContainer.setLogged(SessionDataContainer.getLogged() + 1);
                                    } else {
                                        DataSender.send(player, DataSender.getBuilder(DataType.PIN, DataSender.CHANNEL_PLAYER).addTextData("open").build());

                                        UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PIN, UserAuthenticateEvent.Result.FAILED, fromPlayer(player), "", null);
                                        JavaModuleManager.callEvent(event);

                                        user.send(messages.prefix() + event.getAuthMessage());
                                    }
                                }
                            }
                        }
                    } else {
                        logger.scheduleLog(Level.GRAVE, "Someone tried to access the plugin message channel with an invalid key ( {0} )", key);
                        e.setCancelled(true);
                    }
                }
            }
        }
    }
}
