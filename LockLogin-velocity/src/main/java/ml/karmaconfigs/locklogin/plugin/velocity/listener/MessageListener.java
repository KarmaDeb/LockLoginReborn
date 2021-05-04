package ml.karmaconfigs.locklogin.plugin.velocity.listener;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.encryption.CryptoUtil;
import ml.karmaconfigs.locklogin.api.encryption.HashType;
import ml.karmaconfigs.locklogin.api.modules.javamodule.JavaModuleManager;
import ml.karmaconfigs.locklogin.api.modules.event.user.UserAuthenticateEvent;
import ml.karmaconfigs.locklogin.plugin.common.session.SessionDataContainer;
import ml.karmaconfigs.locklogin.plugin.velocity.plugin.sender.DataSender;
import ml.karmaconfigs.locklogin.plugin.velocity.plugin.sender.DataType;
import ml.karmaconfigs.locklogin.plugin.velocity.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.velocity.util.player.User;

import java.util.Optional;
import java.util.UUID;

import static ml.karmaconfigs.locklogin.plugin.velocity.LockLogin.*;

@SuppressWarnings("UnstableApiUsage")
public final class MessageListener {

    private static String token = "";

    @Subscribe(order = PostOrder.FIRST)
    public final void onMessageReceive(PluginMessageEvent e) {
        if (e.getResult().isAllowed()) {
            Message messages = new Message();
            ByteArrayDataInput input = ByteStreams.newDataInput(e.getData());

            //LockLogin bungeecord is only supposed to listen account channel
            if (e.getIdentifier().getId().equalsIgnoreCase("ll:account")) {
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

                        Optional<Player> tmp_player = server.getPlayer(uuid);
                        if (tmp_player.isPresent() && tmp_player.get().isActive()) {
                            Player player = tmp_player.get();
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
                        e.setResult(PluginMessageEvent.ForwardResult.handled());
                    }
                }
            }
        }
    }
}
