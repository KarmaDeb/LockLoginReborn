package eu.locklogin.plugin.velocity.listener;

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

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import eu.locklogin.api.file.ProxyConfiguration;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.api.common.Console;
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.encryption.CryptoUtil;
import eu.locklogin.api.module.plugin.api.channel.ModuleMessageService;
import eu.locklogin.api.module.plugin.api.event.user.UserAuthenticateEvent;
import eu.locklogin.api.module.plugin.client.ModulePlayer;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.common.utils.plugin.ServerDataStorager;
import eu.locklogin.plugin.velocity.plugin.sender.DataSender;
import eu.locklogin.plugin.velocity.util.files.Message;
import eu.locklogin.plugin.velocity.util.files.client.PlayerFile;
import eu.locklogin.plugin.velocity.util.player.User;
import ml.karmaconfigs.api.common.utils.enums.Level;

import java.util.Optional;
import java.util.UUID;

import static eu.locklogin.plugin.velocity.LockLogin.*;

@SuppressWarnings("UnstableApiUsage")
public final class MessageListener {

    @Subscribe(order = PostOrder.FIRST)
    public final void onMessageReceive(PluginMessageEvent e) {
        if (e.getResult().isAllowed()) {
            Message messages = new Message();
            ProxyConfiguration proxy = CurrentPlatform.getProxyConfiguration();

            ByteArrayDataInput input = ByteStreams.newDataInput(e.getData());

            DataType sub = DataType.valueOf(input.readUTF().toUpperCase());
            String id = input.readUTF();

            switch (e.getIdentifier().getId().toLowerCase()) {
                case "ll:account":
                    if (sub == DataType.PIN) {

                        UUID uuid = UUID.fromString(id);
                        String pin = input.readUTF();

                        Optional<Player> tmp_player = server.getPlayer(uuid);
                        if (tmp_player.isPresent() && tmp_player.get().isActive()) {
                            Player player = tmp_player.get();
                            User user = new User(player);
                            ClientSession session = user.getSession();
                            AccountManager manager = user.getManager();

                            if (session.isValid()) {
                                if (!manager.hasPin() || CryptoUtil.getBuilder().withPassword(pin).withToken(manager.getPin()).build().validate() || pin.equalsIgnoreCase("error")) {
                                    DataSender.send(player, DataSender.getBuilder(DataType.PIN, DataSender.CHANNEL_PLAYER, player).addTextData("close").build());

                                    UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PIN,
                                            (manager.has2FA() ? UserAuthenticateEvent.Result.SUCCESS_TEMP : UserAuthenticateEvent.Result.SUCCESS), fromPlayer(player),
                                            (manager.has2FA() ? messages.gAuthInstructions() : messages.logged()), null);
                                    ModulePlugin.callEvent(event);

                                    user.send(messages.prefix() + event.getAuthMessage());
                                    session.setPinLogged(true);

                                    user.checkServer(0);
                                } else {
                                    DataSender.send(player, DataSender.getBuilder(DataType.PIN, DataSender.CHANNEL_PLAYER, player).addTextData("open").build());

                                    UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PIN, UserAuthenticateEvent.Result.FAILED, fromPlayer(player), "", null);
                                    ModulePlugin.callEvent(event);

                                    user.send(messages.prefix() + event.getAuthMessage());
                                }
                            }
                        }
                    }
                    break;
                case "ll:plugin":
                    switch (sub) {
                        case PLAYER:
                            if (!id.equalsIgnoreCase(proxy.getProxyID().toString())) {
                                ModulePlayer modulePlayer = StringUtils.loadUnsafe(input.readUTF());
                                if (modulePlayer != null) {
                                    AccountManager manager = modulePlayer.getAccount();

                                    if (manager != null) {
                                        AccountManager newManager = new PlayerFile(manager.getUUID());

                                        if (!newManager.exists())
                                            newManager.create();

                                        newManager.setName(manager.getName());
                                        newManager.setPassword(manager.getPassword());
                                        newManager.setPin(manager.getPin());
                                        newManager.set2FA(manager.has2FA());
                                        newManager.setGAuth(manager.getGAuth());
                                    }
                                }
                            }
                            break;
                        case MODULE:
                            int byteLength = input.readInt();
                            byte[] bytes = new byte[byteLength];

                            int i = 0;
                            while (i < byteLength) {
                                bytes[i] = input.readByte();
                                i++;
                            }

                            ModuleMessageService.listenMessage(id, bytes);
                            break;
                    }
                case "ll:access":
                    String name = input.readUTF();

                    switch (sub) {
                        case KEY:
                            if (!id.equalsIgnoreCase("invalid")) {
                                //As the key should be global, when a proxy registers a key, all the proxies should know that
                                Console.send(source, "Registered proxy key into server {0}", Level.INFO, name);
                                ServerDataStorager.setKeyRegistered(name);
                            } else {
                                Console.send(source, "Failed to set proxy key in {0}", Level.GRAVE, name);
                                e.setResult(PluginMessageEvent.ForwardResult.handled());
                            }
                            break;
                        case REGISTER:
                            if (!id.equalsIgnoreCase("invalid")) {
                                //Only listen if the proxy id is this one
                                if (proxy.getProxyID().toString().equalsIgnoreCase(id)) {
                                    Console.send(source, "Registered this proxy into server {0}", Level.INFO, name);
                                    ServerDataStorager.setProxyRegistered(name);
                                    DataSender.updateDataPool(name);
                                } else {
                                    e.setResult(PluginMessageEvent.ForwardResult.handled());
                                }
                            } else {
                                Console.send(source, "Failed to register this proxy in {0}", Level.GRAVE, name);
                                e.setResult(PluginMessageEvent.ForwardResult.handled());
                            }
                            break;
                        case REMOVE:
                            if (!id.equalsIgnoreCase("invalid")) {
                                //Only listen if the proxy id is this one
                                if (proxy.getProxyID().toString().equalsIgnoreCase(id)) {
                                    Console.send(source, "Removed ths proxy from server {0}", Level.INFO, name);
                                    ServerDataStorager.removeProxyRegistered(name);
                                } else {
                                    ServerDataStorager.removeKeyRegistered(name);
                                    e.setResult(PluginMessageEvent.ForwardResult.handled());
                                }
                            } else {
                                Console.send(source, "Failed to remove this proxy from server {0}", Level.GRAVE, name);
                                e.setResult(PluginMessageEvent.ForwardResult.handled());
                            }
                            break;
                    }
                    break;
            }
        }
    }
}
