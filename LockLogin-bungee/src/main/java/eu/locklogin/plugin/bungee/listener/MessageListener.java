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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.common.utils.Channel;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.common.utils.other.PlayerAccount;
import eu.locklogin.api.common.utils.plugin.ServerDataStorage;
import eu.locklogin.api.encryption.CryptoFactory;
import eu.locklogin.api.encryption.Validation;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.module.plugin.api.event.user.UserAuthenticateEvent;
import eu.locklogin.api.module.plugin.api.event.user.UserPostValidationEvent;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import eu.locklogin.api.module.plugin.javamodule.server.TargetServer;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.api.util.platform.ModuleServer;
import eu.locklogin.plugin.bungee.BungeeSender;
import eu.locklogin.plugin.bungee.com.BungeeDataSender;
import eu.locklogin.plugin.bungee.com.message.DataMessage;
import eu.locklogin.plugin.bungee.util.player.User;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.utils.enums.Level;
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

@SuppressWarnings("UnstableApiUsage")
public final class MessageListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    @SuppressWarnings("unchecked")
    public void onMessageReceive(PluginMessageEvent e) {
        if (!e.isCancelled()) {
            try {
                if (e.getTag().equalsIgnoreCase("ll:access") || e.getTag().equalsIgnoreCase("ll:plugin") || e.getTag().equalsIgnoreCase("ll:account")) {
                    PluginMessages messages = CurrentPlatform.getMessages();
                    Server server = (Server) e.getSender();

                    ByteArrayDataInput input = ByteStreams.newDataInput(e.getData());
                    Gson gson = new GsonBuilder().setLenient().create();
                    JsonObject json = gson.fromJson(input.readUTF(), JsonObject.class);

                    String token = json.get("key").getAsString();
                    String name = json.get("server").getAsString();

                    boolean canRead = true;
                    if (!e.getTag().equalsIgnoreCase("ll:access")) {
                        canRead = /*TokenGen.matches(token, name, proxy.proxyKey())*/ BungeeDataSender.validate(token);
                    }

                    if (canRead) {
                        DataType sub = DataType.valueOf(json.get("data_type").getAsString().toUpperCase());
                        switch (e.getTag().toLowerCase()) {
                            case "ll:account":
                                UUID uuid = UUID.fromString(json.get("player").getAsString());
                                ProxiedPlayer player = plugin.getProxy().getPlayer(uuid);

                                if (player != null) {
                                    switch (sub) {
                                        case PIN:
                                            String pin = json.get("pin_code").getAsString();
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

                                                            BungeeSender.sender.queue(server.getInfo().getName())
                                                                            .insert(DataMessage.newInstance(DataType.GAUTH, Channel.ACCOUNT)
                                                                                    .addProperty("player", player.getUniqueId()).getInstance().build());

                                                            user.checkServer(0);
                                                        }

                                                        BungeeSender.sender.queue(server.getInfo().getName())
                                                                .insert(DataMessage.newInstance(DataType.PIN, Channel.ACCOUNT)
                                                                        .addProperty("player", player.getUniqueId())
                                                                        .addProperty("pin", false).getInstance().build());
                                                    } else {
                                                        if (pin.equalsIgnoreCase("error") || !manager.hasPin()) {
                                                            BungeeSender.sender.queue(server.getInfo().getName())
                                                                    .insert(DataMessage.newInstance(DataType.PIN, Channel.ACCOUNT)
                                                                            .addProperty("player", player.getUniqueId())
                                                                            .addProperty("pin", false).getInstance().build());

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

                                                                BungeeSender.sender.queue(server.getInfo().getName())
                                                                        .insert(DataMessage.newInstance(DataType.GAUTH, Channel.ACCOUNT)
                                                                                .addProperty("player", player.getUniqueId()).getInstance().build());

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
                                if (sub.equals(DataType.PLAYER)) {
                                    ModulePlayer modulePlayer = StringUtils.loadUnsafe(json.get("player_info").getAsString());
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
                            case "ll:access":
                                if (sub.equals(DataType.REGISTER)) {
                                    //TargetServerStorage storage = new TargetServerStorage(name);
                                    //storage.save(TokenGen.find(name));

                                    InetAddress address = getIp(server.getSocketAddress());
                                    InetSocketAddress socket = getSocketIp(server.getSocketAddress());

                                    if (address != null && socket != null) {
                                        TargetServer target_server = new TargetServer(name, /*TokenGen.find(name)*/ UUID.randomUUID(), address, socket.getPort(), true);
                                        TargetServer stored = CurrentPlatform.getServer().getServer(name);

                                        Field f = ModuleServer.class.getDeclaredField("servers");
                                        f.setAccessible(true);
                                        Set<TargetServer> stored_set = (Set<TargetServer>) f.get(ModuleServer.class);
                                        if (stored != null) {
                                            //Remove from stored servers
                                            stored_set.remove(stored);
                                        }
                                        stored_set.add(target_server);

                                        if (ServerDataStorage.needsProxyKnowledge(name)) {
                                            BungeeSender.sender.queue(name).unlock();
                                            BungeeSender.registered_servers++;

                                            console.send("Registered proxy key into server {0}", Level.INFO, name);
                                            ServerDataStorage.setProxyRegistered(name);
                                        }
                                    }
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
