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

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import eu.locklogin.plugin.velocity.permissibles.PluginPermission;
import eu.locklogin.plugin.velocity.util.files.Config;
import eu.locklogin.plugin.velocity.util.files.data.lock.LockedAccount;
import eu.locklogin.plugin.velocity.util.player.SessionCheck;
import eu.locklogin.plugin.velocity.util.player.User;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.api.common.timer.AdvancedPluginTimer;
import eu.locklogin.api.account.AccountID;
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.module.plugin.api.event.user.UserAuthenticateEvent;
import eu.locklogin.api.module.plugin.api.event.user.UserJoinEvent;
import eu.locklogin.api.module.plugin.api.event.user.UserPostJoinEvent;
import eu.locklogin.api.module.plugin.api.event.user.UserPreJoinEvent;
import eu.locklogin.api.module.plugin.client.ModulePlayer;
import eu.locklogin.api.module.plugin.javamodule.JavaModuleManager;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.api.common.security.BruteForce;
import eu.locklogin.api.common.security.client.AccountData;
import eu.locklogin.api.common.security.client.IpData;
import eu.locklogin.api.common.security.client.Name;
import eu.locklogin.api.common.security.client.Proxy;
import eu.locklogin.api.common.session.SessionDataContainer;
import eu.locklogin.api.common.session.SessionKeeper;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.common.utils.InstantParser;
import eu.locklogin.api.common.utils.other.UUIDGen;
import eu.locklogin.api.common.utils.plugin.ServerDataStorager;
import eu.locklogin.plugin.velocity.plugin.sender.DataSender;
import eu.locklogin.plugin.velocity.util.files.Message;
import eu.locklogin.plugin.velocity.util.files.client.OfflineClient;
import eu.locklogin.plugin.velocity.util.files.data.lock.LockedData;
import ml.karmaconfigs.api.common.utils.enums.Level;
import net.kyori.adventure.text.Component;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static eu.locklogin.plugin.velocity.LockLogin.*;
import static eu.locklogin.plugin.velocity.plugin.sender.DataSender.*;

public final class JoinListener {

    private final static Map<InetAddress, String> verified = new HashMap<>();

    private static final String IPV4_REGEX =
            "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
    private static final Pattern IPv4_PATTERN = Pattern.compile(IPV4_REGEX);

    @Subscribe(order = PostOrder.FIRST)
    public final void onServerPing(ProxyPingEvent e) {
        verified.put(e.getConnection().getRemoteAddress().getAddress(), "");
    }

    @SuppressWarnings("all")
    @Subscribe(order = PostOrder.FIRST)
    public final void onPreLogin(PreLoginEvent e) {
        Message messages = new Message();
        InetAddress ip = e.getConnection().getRemoteAddress().getAddress();

        String conn_name = e.getUsername();
        UUID tar_uuid = UUIDGen.getUUID(conn_name);

        String address = "null";
        try {
            address = ip.getHostAddress();
        } catch (Throwable ignored) {
        }
        if (!e.getResult().isAllowed()) {
            try {
                if (validateIP(ip)) {
                    PluginConfiguration config = CurrentPlatform.getConfiguration();

                    UUID gen_uuid = UUIDGen.getUUID(conn_name);

                    if (config.registerOptions().maxAccounts() > 0) {
                        AccountData data = new AccountData(ip, AccountID.fromUUID(tar_uuid));

                        if (data.allow(config.registerOptions().maxAccounts())) {
                            data.save();

                            int amount = data.getAlts().size();
                            if (amount > 2) {
                                for (Player online : server.getAllPlayers()) {
                                    User user = new User(online);

                                    if (user.hasPermission(PluginPermission.altInfo())) {
                                        user.send(messages.prefix() + messages.altFound(conn_name, amount - 1));
                                    }
                                }

                                if (!messages.altFound(conn_name, amount).replaceAll("\\s", "").isEmpty())
                                    Console.send(messages.prefix() + messages.altFound(conn_name, amount - 1));
                            }
                        } else {
                            e.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text().content(StringUtils.toColor(messages.maxRegisters())).build()));
                            return;
                        }
                    }

                    if (config.bruteForceOptions().getMaxTries() > 0) {
                        BruteForce protection = new BruteForce(ip);
                        if (protection.isBlocked()) {
                            e.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text().content(StringUtils.toColor(messages.ipBlocked(protection.getBlockLeft()))).build()));
                            return;
                        }
                    }

                    if (config.antiBot()) {
                        if (verified.containsKey(ip)) {
                            String name = verified.getOrDefault(ip, "");

                            if (!name.replaceAll("\\s", "").isEmpty() && !name.equals(conn_name)) {
                                //The anti bot is like a whitelist, only players in a certain list can join, the difference with LockLogin is that players are
                                //assigned to an IP, so the anti bot security is reinforced
                                e.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text().content(StringUtils.toColor(messages.antiBot())).build()));
                                return;
                            } else {
                                if (name.replaceAll("\\s", "").isEmpty())
                                    verified.put(ip, conn_name);
                            }
                        } else {
                            //The anti bot is like a whitelist, only players in a certain list can join, the difference with LockLogin is that players are
                            //assigned to an IP, so the anti bot security is reinforced
                            e.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text().content(StringUtils.toColor(messages.antiBot())).build()));
                            return;
                        }
                    }

                    if (!gen_uuid.equals(tar_uuid)) {
                        e.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text().content(StringUtils.toColor(messages.uuidFetchError())).build()));
                        return;
                    }

                    Name name = new Name(conn_name);
                    name.check();

                    if (name.notValid()) {
                        e.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text().content(StringUtils.toColor(messages.illegalName(name.getInvalidChars()))).build()));
                        return;
                    }

                    OfflineClient offline = new OfflineClient(conn_name);
                    AccountManager manager = offline.getAccount();
                    if (manager != null) {
                        LockedAccount account = new LockedAccount(manager.getUUID());
                        LockedData data = account.getData();

                        if (data.isLocked()) {
                            String administrator = data.getAdministrator();
                            Instant date = data.getLockDate();
                            InstantParser parser = new InstantParser(date);
                            String dateString = parser.getDay() + " " + parser.getMonth() + " " + parser.getYear();

                            e.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text().content(StringUtils.toColor(messages.forcedAccountRemoval(administrator + " [ " + dateString + " ]"))).build()));
                            logger.scheduleLog(Level.WARNING, "Client {0} tried to join, but his account was blocked by {1} on {2}", conn_name, administrator, dateString);
                            return;
                        }
                    }

                    UserPreJoinEvent event = new UserPreJoinEvent(e.getConnection().getRemoteAddress().getAddress(), null, e.getUsername(), e);
                    JavaModuleManager.callEvent(event);
                } else {
                    e.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text().content(StringUtils.toColor(messages.ipProxyError())).build()));
                    logger.scheduleLog(Level.GRAVE, "Player {0}[{2}] tried to join with an invalid IP address ( {1} ), his connection got rejected with ip is proxy message", conn_name, address, tar_uuid);
                }
            } catch (Throwable ex) {
                logger.scheduleLog(Level.GRAVE, ex);
                e.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text().content(StringUtils.toColor(messages.ipProxyError())).build()));
                logger.scheduleLog(Level.GRAVE, "Player {0}[{2}] tried to join with an invalid IP address ( {1} ), his connection got rejected with ip is proxy message", conn_name, address, tar_uuid);
            }
        }
    }

    @Subscribe(order = PostOrder.FIRST)
    public final void onLogin(LoginEvent e) {
        if (e.getResult().isAllowed()) {
            Player player = e.getPlayer();
            Message messages = new Message();
            PluginConfiguration config = CurrentPlatform.getConfiguration();

            IpData data = new IpData(player.getRemoteAddress().getAddress());
            int amount = data.getClonesAmount();

            if (amount + 1 == config.accountsPerIP()) {
                e.setResult(ResultedEvent.ComponentResult.denied(Component.text().content(StringUtils.toColor(messages.maxIP())).build()));
                return;
            }
            data.addClone();

            UserJoinEvent event = new UserJoinEvent(e.getPlayer().getRemoteAddress().getAddress(), e.getPlayer().getUniqueId(), e.getPlayer().getUsername(), e);
            JavaModuleManager.callEvent(event);
        }
    }

    @Subscribe(order = PostOrder.FIRST)
    public final void onPostLogin(PostLoginEvent e) {
        server.getScheduler().buildTask(plugin, () -> {
            PluginConfiguration config = CurrentPlatform.getConfiguration();

            Player player = e.getPlayer();
            InetSocketAddress ip = player.getRemoteAddress();
            User user = new User(player);

            Optional<ServerConnection> tmp_server = player.getCurrentServer();
            if (tmp_server.isPresent()) {
                ServerConnection connection = tmp_server.get();

                RegisteredServer info = connection.getServer();
                eu.locklogin.plugin.velocity.util.files.Proxy proxy = new eu.locklogin.plugin.velocity.util.files.Proxy();

                if (ServerDataStorager.needsRegister(info.getServerInfo().getName()) || ServerDataStorager.needsProxyKnowledge(info.getServerInfo().getName())) {
                    if (ServerDataStorager.needsRegister(info.getServerInfo().getName()))
                        DataSender.send(info, DataSender.getBuilder(DataType.KEY, ACCESS_CHANNEL, player).addTextData(proxy.proxyKey()).addTextData(info.getServerInfo().getName()).addBoolData(proxy.multiBungee()).build());

                    if (ServerDataStorager.needsProxyKnowledge(info.getServerInfo().getName()))
                        DataSender.send(info, DataSender.getBuilder(DataType.REGISTER, ACCESS_CHANNEL, player).addTextData(proxy.proxyKey()).addTextData(info.getServerInfo().getName()).build());
                }
            }

            DataSender.send(player, DataSender.getBuilder(DataType.MESSAGES, PLUGIN_CHANNEL, player).addTextData(Message.manager.getMessages()).build());
            DataSender.send(player, DataSender.getBuilder(DataType.CONFIG, PLUGIN_CHANNEL, player).addTextData(Config.manager.getConfiguration()).build());
            DataSender.send(player, DataSender.getBuilder(DataType.LOGGED, PLUGIN_CHANNEL, player).addIntData(SessionDataContainer.getLogged()).build());
            DataSender.send(player, DataSender.getBuilder(DataType.REGISTERED, PLUGIN_CHANNEL, player).addIntData(SessionDataContainer.getRegistered()).build());

            MessageData validation = getBuilder(DataType.VALIDATION, DataSender.CHANNEL_PLAYER, player).build();
            DataSender.send(player, validation);

            Message messages = new Message();

            Proxy proxy = new Proxy(ip);
            if (proxy.isProxy()) {
                user.kick(messages.ipProxyError());
                return;
            }

            user.applySessionEffects();

            if (config.clearChat()) {
                for (int i = 0; i < 150; i++)
                    server.getScheduler().buildTask(plugin, () -> player.sendMessage(Component.text().content("").build()));
            }

            ClientSession session = user.getSession();
            session.validate();

            if (!config.captchaOptions().isEnabled())
                session.setCaptchaLogged(true);

            AdvancedPluginTimer tmp_timer = null;
            if (!session.isCaptchaLogged()) {
                tmp_timer = new AdvancedPluginTimer(1, true);
                tmp_timer.addAction(() -> player.sendActionBar(Component.text().content(StringUtils.toColor(messages.captcha(session.getCaptcha()))).build())).start();
            }

            MessageData join = DataSender.getBuilder(DataType.JOIN, CHANNEL_PLAYER, player)
                    .addBoolData(session.isLogged())
                    .addBoolData(session.is2FALogged())
                    .addBoolData(session.isPinLogged())
                    .addBoolData(user.isRegistered()).build();
            DataSender.send(player, join);

            AdvancedPluginTimer timer = tmp_timer;
            SessionCheck check = new SessionCheck(player, target -> {
                player.sendActionBar(Component.text().content("").build());
                if (timer != null)
                    timer.setCancelled();
            }, target -> {
                player.sendActionBar(Component.text().content("").build());
                if (timer != null)
                    timer.setCancelled();
            });

            server.getScheduler().buildTask(plugin, check).schedule();
            forceSessionLogin(player);

            DataSender.send(player, DataSender.getBuilder(DataType.CAPTCHA, CHANNEL_PLAYER, player).build());

            e.getPlayer().getCurrentServer().ifPresent(server -> {
                if (eu.locklogin.plugin.velocity.util.files.Proxy.isAuth(server.getServerInfo())) {
                    user.checkServer();
                }
            });

            UserPostJoinEvent event = new UserPostJoinEvent(fromPlayer(e.getPlayer()), e);
            JavaModuleManager.callEvent(event);
        }).delay((long) 1.5, TimeUnit.SECONDS).schedule();
    }

    @Subscribe(order = PostOrder.FIRST)
    public final void onSwitch(ServerConnectedEvent e) {
        server.getScheduler().buildTask(plugin, () -> {
            Player player = e.getPlayer();
            User user = new User(player);

            Optional<ServerConnection> tmp_server = player.getCurrentServer();
            if (tmp_server.isPresent()) {
                ServerConnection connection = tmp_server.get();

                RegisteredServer info = connection.getServer();
                eu.locklogin.plugin.velocity.util.files.Proxy proxy = new eu.locklogin.plugin.velocity.util.files.Proxy();

                if (ServerDataStorager.needsRegister(info.getServerInfo().getName()) || ServerDataStorager.needsProxyKnowledge(info.getServerInfo().getName())) {
                    if (ServerDataStorager.needsRegister(info.getServerInfo().getName()))
                        DataSender.send(info, DataSender.getBuilder(DataType.KEY, ACCESS_CHANNEL, player).addTextData(proxy.proxyKey()).addTextData(info.getServerInfo().getName()).addBoolData(proxy.multiBungee()).build());

                    if (ServerDataStorager.needsProxyKnowledge(info.getServerInfo().getName()))
                        DataSender.send(info, DataSender.getBuilder(DataType.REGISTER, ACCESS_CHANNEL, player).addTextData(proxy.proxyKey()).addTextData(info.getServerInfo().getName()).build());
                }
            }

            DataSender.send(player, DataSender.getBuilder(DataType.MESSAGES, PLUGIN_CHANNEL, player).addTextData(Message.manager.getMessages()).build());
            DataSender.send(player, DataSender.getBuilder(DataType.CONFIG, PLUGIN_CHANNEL, player).addTextData(Message.manager.getMessages()).build());
            DataSender.send(player, DataSender.getBuilder(DataType.LOGGED, PLUGIN_CHANNEL, player).addIntData(SessionDataContainer.getLogged()).build());
            DataSender.send(player, DataSender.getBuilder(DataType.REGISTERED, PLUGIN_CHANNEL, player).addIntData(SessionDataContainer.getRegistered()).build());

            MessageData validation = getBuilder(DataType.VALIDATION, DataSender.CHANNEL_PLAYER, player).build();
            DataSender.send(player, validation);

            ClientSession session = user.getSession();
            session.validate();

            MessageData join = DataSender.getBuilder(DataType.JOIN, CHANNEL_PLAYER, player)
                    .addBoolData(session.isLogged())
                    .addBoolData(session.is2FALogged())
                    .addBoolData(session.isPinLogged())
                    .addBoolData(user.isRegistered()).build();
            DataSender.send(player, join);

            DataSender.send(player, DataSender.getBuilder(DataType.CAPTCHA, DataSender.CHANNEL_PLAYER, player).build());

            user.checkServer();
        }).delay((long) 1.5, TimeUnit.SECONDS).schedule();
    }

    /**
     * Check if the ip is valid
     *
     * @param ip the ip address
     * @return if the ip is valid
     */
    private boolean validateIP(final InetAddress ip) {
        if (StringUtils.isNullOrEmpty(ip.getHostAddress())) {
            return false;
        }

        Matcher matcher = IPv4_PATTERN.matcher(ip.getHostAddress());
        return matcher.matches();
    }

    /**
     * Get if the player has a session and
     * validate it
     *
     * @param player the player
     */
    protected void forceSessionLogin(final Player player) {
        ModulePlayer modulePlayer = fromPlayer(player);

        SessionKeeper keeper = new SessionKeeper(modulePlayer);
        if (keeper.hasSession()) {
            User user = new User(player);
            ClientSession session = user.getSession();

            session.setCaptchaLogged(true);
            session.setLogged(true);
            session.setPinLogged(true);
            session.set2FALogged(true);

            MessageData login = DataSender.getBuilder(DataType.SESSION, CHANNEL_PLAYER, player).build();
            MessageData pin = DataSender.getBuilder(DataType.PIN, CHANNEL_PLAYER, player).addTextData("close").build();
            MessageData gauth = DataSender.getBuilder(DataType.GAUTH, CHANNEL_PLAYER, player).build();

            DataSender.send(player, login);
            DataSender.send(player, pin);
            DataSender.send(player, gauth);

            keeper.destroy();

            user.checkServer();

            UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PASSWORD, UserAuthenticateEvent.Result.SUCCESS, fromPlayer(player), "", null);
            JavaModuleManager.callEvent(event);
        }
    }
}
