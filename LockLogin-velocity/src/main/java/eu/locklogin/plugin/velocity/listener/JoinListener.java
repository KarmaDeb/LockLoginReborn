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
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import eu.locklogin.api.account.AccountID;
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.common.security.BruteForce;
import eu.locklogin.api.common.security.TokenGen;
import eu.locklogin.api.common.security.client.AccountData;
import eu.locklogin.api.common.security.client.ClientData;
import eu.locklogin.api.common.security.client.Name;
import eu.locklogin.api.common.security.client.ProxyCheck;
import eu.locklogin.api.common.session.SessionCheck;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.common.utils.InstantParser;
import eu.locklogin.api.common.utils.other.UUIDGen;
import eu.locklogin.api.common.utils.plugin.FloodGateUtil;
import eu.locklogin.api.common.utils.plugin.ServerDataStorage;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.file.ProxyConfiguration;
import eu.locklogin.api.module.plugin.api.event.user.UserJoinEvent;
import eu.locklogin.api.module.plugin.api.event.user.UserPostJoinEvent;
import eu.locklogin.api.module.plugin.api.event.user.UserPreJoinEvent;
import eu.locklogin.api.module.plugin.api.event.user.VelocityGameProfileEvent;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.velocity.permissibles.PluginPermission;
import eu.locklogin.plugin.velocity.plugin.sender.DataSender;
import eu.locklogin.plugin.velocity.util.files.Config;
import eu.locklogin.plugin.velocity.util.files.Proxy;
import eu.locklogin.plugin.velocity.util.files.client.OfflineClient;
import eu.locklogin.plugin.velocity.util.files.data.lock.LockedAccount;
import eu.locklogin.plugin.velocity.util.files.data.lock.LockedData;
import eu.locklogin.plugin.velocity.util.player.PlayerPool;
import eu.locklogin.plugin.velocity.util.player.User;
import ml.karmaconfigs.api.common.timer.SourceSecondsTimer;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.api.common.utils.enums.Level;
import net.kyori.adventure.text.Component;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static eu.locklogin.plugin.velocity.LockLogin.*;
import static eu.locklogin.plugin.velocity.plugin.sender.DataSender.*;

public final class JoinListener {

    private static final String IPV4_REGEX =
            "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
    private static final Pattern IPv4_PATTERN = Pattern.compile(IPV4_REGEX);

    private final static Map<String, UUID> ids = new ConcurrentHashMap<>();

    /**
     * Initialize the join listener
     */
    public JoinListener() {
        PlayerPool.whenValid((player) -> {
            PluginConfiguration config = CurrentPlatform.getConfiguration();

            InetSocketAddress ip = player.getRemoteAddress();
            User user = new User(player);

            Optional<ServerConnection> tmp_server = player.getCurrentServer();
            if (tmp_server.isPresent()) {
                ServerConnection connection = tmp_server.get();

                RegisteredServer server = connection.getServer();
                ProxyConfiguration proxy = CurrentPlatform.getProxyConfiguration();

                if (ServerDataStorage.needsRegister(server.getServerInfo().getName()) || ServerDataStorage.needsProxyKnowledge(server.getServerInfo().getName())) {
                    if (ServerDataStorage.needsRegister(server.getServerInfo().getName()))
                        DataSender.send(server, DataSender.getBuilder(DataType.KEY, ACCESS_CHANNEL, player).addTextData(proxy.proxyKey()).addTextData(server.getServerInfo().getName()).addBoolData(proxy.multiBungee()).build());

                    if (ServerDataStorage.needsProxyKnowledge(server.getServerInfo().getName())) {
                        DataSender.send(server, DataSender.getBuilder(DataType.REGISTER, ACCESS_CHANNEL, player)
                                .addTextData(proxy.proxyKey()).addTextData(server.getServerInfo().getName())
                                .addTextData(TokenGen.expiration("LOCAL_TOKEN").toString())
                                .build());
                    }
                }
                DataSender.send(player, DataSender.getBuilder(DataType.MESSAGES, PLUGIN_CHANNEL, player).addTextData(proxy.proxyKey()).addTextData(server.getServerInfo().getName()).addTextData(CurrentPlatform.getMessages().toString()).build());
                DataSender.send(player, DataSender.getBuilder(DataType.CONFIG, PLUGIN_CHANNEL, player).addTextData(proxy.proxyKey()).addTextData(server.getServerInfo().getName()).addTextData(Config.manager.getConfiguration()).build());
            }

            CurrentPlatform.requestDataContainerUpdate();

            MessageData validation = getBuilder(DataType.VALIDATION, DataSender.CHANNEL_PLAYER, player).build();
            DataSender.send(player, validation);

            PluginMessages messages = CurrentPlatform.getMessages();

            ProxyCheck proxy = new ProxyCheck(ip);
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

            SimpleScheduler tmp_timer = null;
            if (!session.isCaptchaLogged()) {
                tmp_timer = new SourceSecondsTimer(main, 1, true);
                tmp_timer.secondChangeAction((second) -> player.sendActionBar(Component.text().content(StringUtils.toColor(messages.captcha(session.getCaptcha()))).build())).start();
            }

            MessageData join = DataSender.getBuilder(DataType.JOIN, CHANNEL_PLAYER, player)
                    .addBoolData(session.isLogged())
                    .addBoolData(session.is2FALogged())
                    .addBoolData(session.isPinLogged())
                    .addBoolData(user.isRegistered()).build();
            DataSender.send(player, join);

            SimpleScheduler timer = tmp_timer;
            SessionCheck<Player> check = user.getChecker().whenComplete(() -> {
                user.restorePotionEffects();
                player.sendActionBar(Component.text().content("").build());

                if (timer != null)
                    timer.cancel();
            });
            server.getScheduler().buildTask(plugin, check).schedule();

            DataSender.send(player, DataSender.getBuilder(DataType.CAPTCHA, CHANNEL_PLAYER, player).build());

            player.getCurrentServer().ifPresent(server -> {
                if (Proxy.isAuth(server.getServerInfo())) {
                    user.checkServer(0);
                }
            });

            UserPostJoinEvent event = new UserPostJoinEvent(fromPlayer(player), null);
            ModulePlugin.callEvent(event);

            if (event.isHandled()) {
                user.kick(event.getHandleReason());
            }
        });
    }

    @Subscribe(order = PostOrder.FIRST)
    public final void onServerPing(ProxyPingEvent e) {
        ClientData client = new ClientData(e.getConnection().getRemoteAddress().getAddress());
        if (!client.isVerified())
            client.setVerified(true);
    }

    @Subscribe(order = PostOrder.LAST)
    public final void onGameProfileRequest(GameProfileRequestEvent e) {
        VelocityGameProfileEvent event = new VelocityGameProfileEvent(e);
        ModulePlugin.callEvent(event);

        ids.put(e.getGameProfile().getName(), e.getGameProfile().getId());
        ids.put(e.getOriginalProfile().getName(), e.getGameProfile().getId());
    }

    @Subscribe(order = PostOrder.FIRST)
    public final void onPreLogin(PreLoginEvent e) {
        PluginMessages messages = CurrentPlatform.getMessages();
        InetAddress ip = e.getConnection().getRemoteAddress().getAddress();

        String conn_name = e.getUsername();
        UUID tar_uuid = ids.getOrDefault(conn_name, UUIDGen.getUUID(conn_name));

        ids.remove(conn_name);

        String address = "null";
        try {
            address = ip.getHostAddress();
        } catch (Throwable ignored) {}
        if (e.getResult().isAllowed()) {
            try {
                if (validateIP(ip)) {
                    PluginConfiguration config = CurrentPlatform.getConfiguration();

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
                                    console.send(messages.prefix() + messages.altFound(conn_name, amount - 1));
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
                        ClientData client = new ClientData(ip);
                        if (client.isVerified()) {
                            String name = e.getUsername();

                            if (client.canAssign(config.accountsPerIP(), name, tar_uuid)) {
                                logger.scheduleLog(Level.INFO, "Assigned IP address {0} to client {1}", ip.getHostAddress(), name);
                            } else {
                                e.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text().content(StringUtils.toColor(messages.antiBot())).build()));
                                return;
                            }
                        } else {
                            e.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text().content(StringUtils.toColor(messages.antiBot())).build()));
                            return;
                        }
                    }

                    if (config.checkNames()) {
                        Name name = new Name(conn_name);
                        name.check();

                        if (name.notValid()) {
                            e.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text().content(StringUtils.toColor(messages.illegalName(name.getInvalidChars()))).build()));
                            return;
                        }
                    }

                    OfflineClient offline = new OfflineClient(conn_name);
                    AccountManager manager = offline.getAccount();
                    if (manager != null) {
                        if (config.enforceNameCheck()) {
                            if (!manager.getName().equals(conn_name)) {
                                e.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text().content(StringUtils.toColor(messages.similarName(manager.getName()))).build()));
                                return;
                            }
                        }

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

                    UserPreJoinEvent event = new UserPreJoinEvent(e.getConnection().getRemoteAddress().getAddress(), tar_uuid, e.getUsername(), e);
                    ModulePlugin.callEvent(event);

                    if (event.isHandled()) {
                        e.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text().content(StringUtils.toColor(event.getHandleReason())).build()));
                        return;
                    }

                    PlayerPool.addPlayer(tar_uuid);
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
            PluginConfiguration config = CurrentPlatform.getConfiguration();
            PluginMessages messages = CurrentPlatform.getMessages();

            if (config.uuidValidator()) {
                UUID tar_uuid = e.getPlayer().getGameProfile().getId();
                UUID gen_uuid = UUIDGen.getUUID(e.getPlayer().getGameProfile().getName());

                if (!gen_uuid.equals(tar_uuid)) {
                    FloodGateUtil util = new FloodGateUtil(tar_uuid);
                    if (!util.isFloodClient()) {
                        e.setResult(ResultedEvent.ComponentResult.denied(Component.text().content(StringUtils.toColor(messages.uuidFetchError())).build()));
                        return;
                    }
                }
            }

            UserJoinEvent event = new UserJoinEvent(e.getPlayer().getRemoteAddress().getAddress(), e.getPlayer().getUniqueId(), e.getPlayer().getUsername(), e);
            ModulePlugin.callEvent(event);

            if (event.isHandled()) {
                e.setResult(ResultedEvent.ComponentResult.denied(Component.text().content(StringUtils.toColor(event.getHandleReason())).build()));
                PlayerPool.delPlayer(e.getPlayer().getUniqueId());
            }
        }
    }

    @Subscribe(order = PostOrder.FIRST)
    public final void onSwitch(ServerConnectedEvent e) {
        server.getScheduler().buildTask(plugin, () -> {
            Player player = e.getPlayer();
            User user = new User(player);

            RegisteredServer server = e.getServer();
            ProxyConfiguration proxy = CurrentPlatform.getProxyConfiguration();

            if (ServerDataStorage.needsRegister(server.getServerInfo().getName()) || ServerDataStorage.needsProxyKnowledge(server.getServerInfo().getName())) {
                if (ServerDataStorage.needsRegister(server.getServerInfo().getName()))
                    DataSender.send(server, DataSender.getBuilder(DataType.KEY, ACCESS_CHANNEL, player).addTextData(proxy.proxyKey()).addTextData(server.getServerInfo().getName()).addBoolData(proxy.multiBungee()).build());

                if (ServerDataStorage.needsProxyKnowledge(server.getServerInfo().getName())) {
                    DataSender.send(server, DataSender.getBuilder(DataType.REGISTER, ACCESS_CHANNEL, player)
                            .addTextData(proxy.proxyKey()).addTextData(server.getServerInfo().getName())
                            .addTextData(TokenGen.expiration("LOCAL_TOKEN").toString())
                            .build());
                }
            }
            DataSender.send(player, DataSender.getBuilder(DataType.MESSAGES, PLUGIN_CHANNEL, player).addTextData(proxy.proxyKey()).addTextData(server.getServerInfo().getName()).addTextData(CurrentPlatform.getMessages().toString()).build());
            DataSender.send(player, DataSender.getBuilder(DataType.CONFIG, PLUGIN_CHANNEL, player).addTextData(proxy.proxyKey()).addTextData(server.getServerInfo().getName()).addTextData(Config.manager.getConfiguration()).build());

            CurrentPlatform.requestDataContainerUpdate();

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

            if (!user.hasPermission(PluginPermission.limbo()))
                user.checkServer(0);
        }).delay((long) 1.5, TimeUnit.SECONDS).schedule();
    }

    /**
     * Check if the ip is valid
     *
     * @param ip the ip address
     * @return if the ip is valid
     */
    private boolean validateIP(final InetAddress ip) {
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        if (config.ipHealthCheck()) {
            if (StringUtils.isNullOrEmpty(ip.getHostAddress())) {
                return false;
            }

            Matcher matcher = IPv4_PATTERN.matcher(ip.getHostAddress());
            return matcher.matches();
        }

        return true;
    }
}
