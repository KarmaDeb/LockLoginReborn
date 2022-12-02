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
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientSession;
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
import eu.locklogin.api.module.plugin.javamodule.server.TargetServer;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.api.util.platform.ModuleServer;
import eu.locklogin.plugin.bungee.plugin.sender.DataSender;
import eu.locklogin.plugin.bungee.util.files.cache.TargetServerStorage;
import eu.locklogin.plugin.bungee.util.player.User;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.UUID;

import static eu.locklogin.plugin.bungee.LockLogin.*;
import static eu.locklogin.plugin.bungee.plugin.sender.DataSender.CHANNEL_PLAYER;

@SuppressWarnings("UnstableApiUsage")
public final class MessageListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    @SuppressWarnings("unchecked")
    public void onMessageReceive(PluginMessageEvent e) {
        if (!e.isCancelled()) {
            try {
                if (e.getTag().equalsIgnoreCase("ll:access") || e.getTag().equalsIgnoreCase("ll:plugin") || e.getTag().equalsIgnoreCase("ll:account")) {
                    PluginMessages messages = CurrentPlatform.getMessages();
                    ProxyConfiguration proxy = CurrentPlatform.getProxyConfiguration();
                    Server server = (Server) e.getSender();

                    ByteArrayDataInput input = ByteStreams.newDataInput(e.getData());

                    String token = input.readUTF();
                    String name = input.readUTF();

                    boolean canRead = true;
                    if (!e.getTag().equalsIgnoreCase("ll:access")) {
                        canRead = /*TokenGen.matches(token, name, proxy.proxyKey())*/ DataSender.validate(token);
                    }

                    if (canRead) {
                        DataType sub = DataType.valueOf(input.readUTF().toUpperCase());
                        String id = input.readUTF();
                        switch (e.getTag().toLowerCase()) {
                            case "ll:account":
                                UUID uuid = UUID.fromString(id);
                                ProxiedPlayer player = plugin.getProxy().getPlayer(uuid);

                                if (player != null) {
                                    switch (sub) {
                                        case PIN:
                                            String pin = input.readUTF();
                                            if (player.isConnected()) {
                                                User user = new User(player);
                                                ClientSession session = user.getSession();
                                                AccountManager manager = user.getManager();

                                                if (session.isValid()) {
                                                    if (manager.hasPin() && CryptoFactory.getBuilder().withPassword(pin).withToken(manager.getPin()).build().validate(Validation.ALL) && !pin.equalsIgnoreCase("error")) {
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

                                                        DataSender.send(player, DataSender.getBuilder(DataType.PIN, DataSender.CHANNEL_PLAYER, player).addTextData("close").build());
                                                    } else {
                                                        if (pin.equalsIgnoreCase("error") || !manager.hasPin()) {
                                                            DataSender.send(player, DataSender.getBuilder(DataType.PIN, DataSender.CHANNEL_PLAYER, player).addTextData("close").build());

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
                                                        } else {
                                                            if (!pin.equalsIgnoreCase("error") && manager.hasPin()) {
                                                                UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PIN,
                                                                        UserAuthenticateEvent.Result.ERROR,
                                                                        user.getModule(),
                                                                        "", null);
                                                                ModulePlugin.callEvent(event);

                                                                if (!event.getAuthMessage().isEmpty()) {
                                                                    user.send(messages.prefix() + event.getAuthMessage());
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            break;
                                        case JOIN:
                                            //In BungeeCord, this means that player has been validated
                                            User user = new User(player);
                                            UserPostValidationEvent event = new UserPostValidationEvent(user.getModule(), name, null);
                                            ModulePlugin.callEvent(event);
                                            break;
                                        default:
                                            break;
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
                                        String address = input.readUTF();
                                        int port = input.readInt();

                                        /*
                                        Manager.connect(address, port).whenComplete((rsl, error) -> {
                                            if (error == null) {
                                                if (rsl) {
                                                    console.send("Connected to remote messaging server for modules!", Level.INFO);

                                                    Manager.getClient().rename(CurrentPlatform.getConfiguration().serverName());
                                                } else {
                                                    console.send("Failed to connect to remote messaging server for modules", Level.GRAVE);
                                                }
                                            } else {
                                                logger.scheduleLog(Level.GRAVE, error);
                                                logger.scheduleLog(Level.INFO, "Failed to connect client to remote messaging server");
                                            }
                                        });*/
                                        break;
                                }
                                break;
                            case "ll:access":
                                switch (sub) {
                                    case KEY:
                                        if (!id.equalsIgnoreCase("invalid")) {
                                            TargetServerStorage storage = new TargetServerStorage(name);
                                            //storage.save(TokenGen.find(name));

                                            InetAddress address = getIp(server.getSocketAddress());
                                            InetSocketAddress socket = getSocketIp(server.getSocketAddress());

                                            if (address != null && socket != null) {
                                                TargetServer target_server = new TargetServer(name, /*TokenGen.find(name)*/ UUID.randomUUID(), address, socket.getPort(), true);
                                                TargetServer stored = CurrentPlatform.getServer().getServer(name);

                                                Field f =  ModuleServer.class.getDeclaredField("servers");
                                                f.setAccessible(true);
                                                Set<TargetServer> stored_set = (Set<TargetServer>) f.get(ModuleServer.class);
                                                if (stored != null) {
                                                    //Remove from stored servers
                                                    stored_set.remove(stored);
                                                }
                                                stored_set.add(target_server);

                                                if (ServerDataStorage.needsRegister(name)) {
                                                    console.send("Registered proxy key into server {0}", Level.INFO, name);
                                                    ServerDataStorage.setKeyRegistered(name);
                                                }
                                            }
                                        } else {
                                            console.send("Failed to set proxy key in {0}", Level.GRAVE, name);
                                            e.setCancelled(true);
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
                                                e.setCancelled(true);
                                            }
                                        } else {
                                            console.send("Failed to register this proxy in {0}", Level.GRAVE, name);
                                            e.setCancelled(true);
                                        }
                                        break;
                                    case REMOVE:
                                        if (!id.equalsIgnoreCase("invalid")) {
                                            //Only listen if the proxy id is this one
                                            if (proxy.getProxyID().toString().equalsIgnoreCase(id)) {
                                                if (!ServerDataStorage.needsProxyKnowledge(name)) {
                                                    console.send("Removed ths proxy from server {0}", Level.INFO, name);
                                                    ServerDataStorage.removeProxyRegistered(name);
                                                }

                                                TargetServer stored = CurrentPlatform.getServer().getServer(name);

                                                Field f =  ModuleServer.class.getDeclaredField("servers");
                                                f.setAccessible(true);
                                                Set<TargetServer> stored_set = (Set<TargetServer>) f.get(ModuleServer.class);
                                                if (stored != null) {
                                                    //Remove from stored servers
                                                    stored_set.remove(stored);
                                                }
                                            } else {
                                                ServerDataStorage.removeKeyRegistered(name);
                                                e.setCancelled(true);
                                            }
                                        } else {
                                            console.send("Failed to remove this proxy from server {0}", Level.GRAVE, name);
                                            e.setCancelled(true);
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