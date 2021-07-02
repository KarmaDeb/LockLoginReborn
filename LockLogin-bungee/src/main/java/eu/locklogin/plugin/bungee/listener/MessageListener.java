package eu.locklogin.plugin.bungee.listener;

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
import eu.locklogin.api.file.ProxyConfiguration;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bungee.plugin.sender.DataSender;
import eu.locklogin.plugin.bungee.util.files.Message;
import eu.locklogin.plugin.bungee.util.files.client.PlayerFile;
import eu.locklogin.plugin.bungee.util.player.User;
import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.StringUtils;
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.encryption.CryptoUtil;
import eu.locklogin.api.module.plugin.api.channel.ModuleMessageService;
import eu.locklogin.api.module.plugin.api.event.user.UserAuthenticateEvent;
import eu.locklogin.api.module.plugin.client.ModulePlayer;
import eu.locklogin.api.module.plugin.javamodule.JavaModuleManager;
import eu.locklogin.plugin.bungee.util.files.Proxy;
import eu.locklogin.api.common.session.SessionDataContainer;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.common.utils.plugin.ServerDataStorager;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.UUID;

import static eu.locklogin.plugin.bungee.LockLogin.fromPlayer;
import static eu.locklogin.plugin.bungee.LockLogin.plugin;

@SuppressWarnings("UnstableApiUsage")
public final class MessageListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public final void onMessageReceive(PluginMessageEvent e) {
        if (!e.isCancelled()) {
            try {
                Message messages = new Message();
                ProxyConfiguration proxy = CurrentPlatform.getProxyConfiguration();

                ByteArrayDataInput input = ByteStreams.newDataInput(e.getData());

                DataType sub = DataType.valueOf(input.readUTF().toUpperCase());
                String id = input.readUTF();
                switch (e.getTag().toLowerCase()) {
                    case "ll:account":
                        if (sub == DataType.PIN) {
                            UUID uuid = UUID.fromString(id);
                            String pin = input.readUTF();

                            ProxiedPlayer player = plugin.getProxy().getPlayer(uuid);
                            if (player != null && player.isConnected()) {
                                User user = new User(player);
                                ClientSession session = user.getSession();
                                AccountManager manager = user.getManager();

                                if (session.isValid()) {
                                    if (manager.getPin().replaceAll("\\s", "").isEmpty() || CryptoUtil.getBuilder().withPassword(pin).withToken(manager.getPin()).build().validate() || pin.equalsIgnoreCase("error")) {
                                        DataSender.send(player, DataSender.getBuilder(DataType.PIN, DataSender.CHANNEL_PLAYER, player).addTextData("close").build());

                                        UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PIN,
                                                (manager.has2FA() ? UserAuthenticateEvent.Result.SUCCESS_TEMP : UserAuthenticateEvent.Result.SUCCESS), fromPlayer(player),
                                                (manager.has2FA() ? messages.gAuthInstructions() : messages.logged()), null);
                                        JavaModuleManager.callEvent(event);

                                        user.send(messages.prefix() + event.getAuthMessage());
                                        session.setPinLogged(true);

                                        user.checkServer(0);
                                    } else {
                                        DataSender.send(player, DataSender.getBuilder(DataType.PIN, DataSender.CHANNEL_PLAYER, player).addTextData("open").build());

                                        UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PIN, UserAuthenticateEvent.Result.FAILED, fromPlayer(player), "", null);
                                        JavaModuleManager.callEvent(event);

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
                        break;
                    case "ll:access":
                        String name = input.readUTF();
                        switch (sub) {
                            case KEY:
                                if (!id.equalsIgnoreCase("invalid")) {
                                    //As the key should be global, when a proxy registers a key, all the proxies should know that

                                    if (ServerDataStorager.needsRegister(name)) {
                                        Console.send(plugin, "Registered proxy key into server {0}", Level.INFO, name);
                                        ServerDataStorager.setKeyRegistered(name);
                                    }
                                } else {
                                    Console.send(plugin, "Failed to set proxy key in {0}", Level.GRAVE, name);
                                    e.setCancelled(true);
                                }
                                break;
                            case REGISTER:
                                if (!id.equalsIgnoreCase("invalid")) {
                                    //Only listen if the proxy id is this one
                                    if (proxy.getProxyID().toString().equalsIgnoreCase(id)) {
                                        if (ServerDataStorager.needsProxyKnowledge(name)) {
                                            Console.send(plugin, "Registered this proxy into server {0}", Level.INFO, name);
                                            ServerDataStorager.setProxyRegistered(name);
                                            DataSender.updateDataPool(name);
                                        }
                                    } else {
                                        e.setCancelled(true);
                                    }
                                } else {
                                    Console.send(plugin, "Failed to register this proxy in {0}", Level.GRAVE, name);
                                    e.setCancelled(true);
                                }
                                break;
                            case REMOVE:
                                if (!id.equalsIgnoreCase("invalid")) {
                                    //Only listen if the proxy id is this one
                                    if (proxy.getProxyID().toString().equalsIgnoreCase(id)) {
                                        if (!ServerDataStorager.needsProxyKnowledge(name)) {
                                            Console.send(plugin, "Removed ths proxy from server {0}", Level.INFO, name);
                                            ServerDataStorager.removeProxyRegistered(name);
                                        }
                                    } else {
                                        ServerDataStorager.removeKeyRegistered(name);
                                        e.setCancelled(true);
                                    }
                                } else {
                                    Console.send(plugin, "Failed to remove this proxy from server {0}", Level.GRAVE, name);
                                    e.setCancelled(true);
                                }
                                break;
                        }
                        break;
                }
            } catch (Throwable ignored) {}
        }
    }
}
