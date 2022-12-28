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
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.common.security.TokenGen;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.common.utils.other.PlayerAccount;
import eu.locklogin.api.common.utils.plugin.ServerDataStorage;
import eu.locklogin.api.encryption.CryptoFactory;
import eu.locklogin.api.encryption.Validation;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.file.ProxyConfiguration;
import eu.locklogin.api.module.plugin.api.event.user.UserAuthenticateEvent;
import eu.locklogin.api.module.plugin.api.event.user.UserPostValidationEvent;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.velocity.plugin.sender.DataSender;
import eu.locklogin.plugin.velocity.util.player.User;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.string.StringUtils;

import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static eu.locklogin.plugin.velocity.LockLogin.*;
import static eu.locklogin.plugin.velocity.plugin.sender.DataSender.CHANNEL_PLAYER;

@SuppressWarnings("UnstableApiUsage")
public final class MessageListener {

    @Subscribe(order = PostOrder.FIRST)
    public void onMessageReceive(PluginMessageEvent e) {
        if (e.getResult().isAllowed()) {
            try {
                String identifier = e.getIdentifier().getId();
                if (identifier.equalsIgnoreCase("ll:access") || identifier.equalsIgnoreCase("ll:plugin") || identifier.equalsIgnoreCase("ll:account")) {

                    PluginMessages messages = CurrentPlatform.getMessages();
                    ProxyConfiguration proxy = CurrentPlatform.getProxyConfiguration();

                    ByteArrayDataInput input = ByteStreams.newDataInput(e.getData());

                    String token = input.readUTF();
                    String name = input.readUTF();

                    boolean canRead = true;
                    /*if (!identifier.equalsIgnoreCase("ll:access")) {
                        canRead = TokenGen.matches(token, name, proxy.proxyKey());
                    }*/

                    if (canRead) {
                        DataType sub = DataType.valueOf(input.readUTF().toUpperCase());
                        String id = input.readUTF();
                        switch (e.getIdentifier().getId().toLowerCase()) {
                            case "ll:account":
                                UUID uuid = UUID.fromString(id);
                                Optional<Player> tmp_player = plugin.getServer().getPlayer(uuid);

                                switch (sub) {
                                    case PIN:
                                        String pin = input.readUTF();
                                        if (tmp_player.isPresent() && tmp_player.get().isActive()) {
                                            Player player = tmp_player.get();
                                            User user = new User(player);
                                            ClientSession session = user.getSession();
                                            AccountManager manager = user.getManager();

                                            if (session.isValid()) {
                                                if (manager.hasPin() && CryptoFactory.getBuilder().withPassword(pin).withToken(manager.getPin()).build().validate(Validation.ALL) && !pin.equalsIgnoreCase("error")) {
                                                    DataSender.send(player, DataSender.getBuilder(DataType.PIN, CHANNEL_PLAYER, player).addTextData("close").build());

                                                    UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PIN,
                                                            (manager.has2FA() ? UserAuthenticateEvent.Result.SUCCESS_TEMP : UserAuthenticateEvent.Result.SUCCESS),
                                                            user.getModule(),
                                                            (manager.has2FA() ? messages.gAuthInstructions() : messages.logged()), null);
                                                    ModulePlugin.callEvent(event);

                                                    user.send(messages.prefix() + event.getAuthMessage());
                                                    session.setPinLogged(true);
                                                    if (manager.has2FA()) {
                                                        session.set2FALogged(false);
                                                    } else {
                                                        session.set2FALogged(true);

                                                        DataSender.MessageData gauth = DataSender.getBuilder(DataType.GAUTH, CHANNEL_PLAYER, player).build();
                                                        DataSender.send(player, gauth);

                                                        user.checkServer(0);
                                                    }
                                                } else {
                                                    if (pin.equalsIgnoreCase("error") || !manager.hasPin()) {
                                                        DataSender.send(player, DataSender.getBuilder(DataType.PIN, CHANNEL_PLAYER, player).addTextData("close").build());

                                                        UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PIN,
                                                                UserAuthenticateEvent.Result.ERROR,
                                                                user.getModule(),
                                                                (manager.has2FA() ? messages.gAuthInstructions() : messages.logged()), null);
                                                        ModulePlugin.callEvent(event);

                                                        user.send(messages.prefix() + event.getAuthMessage());
                                                        session.setPinLogged(true);
                                                        if (manager.has2FA()) {
                                                            session.set2FALogged(false);
                                                        } else {
                                                            session.set2FALogged(true);

                                                            DataSender.MessageData gauth = DataSender.getBuilder(DataType.GAUTH, CHANNEL_PLAYER, player).build();
                                                            DataSender.send(player, gauth);

                                                            user.checkServer(0);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    case JOIN:
                                        tmp_player.ifPresent((player) -> {
                                            User user = new User(player);
                                            UserPostValidationEvent event = new UserPostValidationEvent(user.getModule(), name, null);
                                            ModulePlugin.callEvent(event);
                                        });
                                        break;
                                    default:
                                        break;
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
                                                    AccountManager newManager = new PlayerAccount(manager.getUUID());

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
                                    case LISTENER:
                                        int byteLength = input.readInt();
                                        byte[] bytes = new byte[byteLength];

                                        int i = 0;
                                        while (i < byteLength) {
                                            bytes[i] = input.readByte();
                                            i++;
                                        }

                                        //ModuleMessageService.listenMessage(id, bytes);
                                        break;
                                }
                            case "ll:access":
                                switch (sub) {
                                    case KEY:
                                        if (!id.equalsIgnoreCase("invalid")) {
                                            if (ServerDataStorage.needsProxyKnowledge(name)) {
                                                console.send("Registered proxy key into server {0}", Level.INFO, name);
                                                ServerDataStorage.setProxyRegistered(name);
                                            }
                                        } else {
                                            console.send("Failed to set proxy key in {0}", Level.GRAVE, name);
                                            e.setResult(PluginMessageEvent.ForwardResult.handled());
                                        }
                                        break;
                                    case REGISTER:
                                        if (!id.equalsIgnoreCase("invalid")) {
                                            //Only listen if the proxy id is this one
                                            if (proxy.getProxyID().toString().equalsIgnoreCase(id)) {
                                                if (ServerDataStorage.needsProxyKnowledge(name)) {
                                                    ServerDataStorage.setProxyRegistered(name);
                                                    DataSender.updateDataPool(name);

                                                    //TokenGen.assign(new String(Base64.getUrlEncoder().encode(token.getBytes())), name, proxy.proxyKey(), Instant.parse(input.readUTF()));
                                                }
                                            } else {
                                                e.setResult(PluginMessageEvent.ForwardResult.handled());
                                            }
                                        } else {
                                            console.send("Failed to register this proxy in {0}", Level.GRAVE, name);
                                            e.setResult(PluginMessageEvent.ForwardResult.handled());
                                        }
                                        break;
                                    case REMOVE:
                                        if (!id.equalsIgnoreCase("invalid")) {
                                            //Only listen if the proxy id is this one
                                            if (proxy.getProxyID().toString().equalsIgnoreCase(id)) {
                                                console.send("Removed ths proxy from server {0}", Level.INFO, name);
                                                ServerDataStorage.removeProxyRegistered(name);
                                            } else {
                                                ServerDataStorage.removeProxyRegistered(name);
                                                e.setResult(PluginMessageEvent.ForwardResult.handled());
                                            }
                                        } else {
                                            console.send("Failed to remove this proxy from server {0}", Level.GRAVE, name);
                                            e.setResult(PluginMessageEvent.ForwardResult.handled());
                                        }
                                        break;
                                }
                                break;
                        }
                    }
                }
            } catch (Throwable ex) {
                logger.scheduleLog(Level.GRAVE, ex);
                logger.scheduleLog(Level.INFO, "Failed to read message from server");
            }
        }
    }
}
