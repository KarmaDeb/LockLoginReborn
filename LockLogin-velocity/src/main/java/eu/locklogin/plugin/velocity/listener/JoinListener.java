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
import eu.locklogin.api.common.security.client.Name;
import eu.locklogin.api.common.security.client.ProxyCheck;
import eu.locklogin.api.common.session.SessionCheck;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.common.utils.InstantParser;
import eu.locklogin.api.common.utils.other.LockedAccount;
import eu.locklogin.api.common.utils.plugin.FloodGateUtil;
import eu.locklogin.api.common.utils.plugin.ServerDataStorage;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.file.ProxyConfiguration;
import eu.locklogin.api.module.plugin.api.event.plugin.PluginIpValidationEvent;
import eu.locklogin.api.module.plugin.api.event.user.UserJoinEvent;
import eu.locklogin.api.module.plugin.api.event.user.UserPostJoinEvent;
import eu.locklogin.api.module.plugin.api.event.user.UserPreJoinEvent;
import eu.locklogin.api.module.plugin.api.event.user.VelocityGameProfileEvent;
import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.client.permission.plugin.PluginPermissions;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.velocity.plugin.Manager;
import eu.locklogin.plugin.velocity.plugin.sender.DataSender;
import eu.locklogin.plugin.velocity.util.files.Config;
import eu.locklogin.plugin.velocity.util.files.Proxy;
import eu.locklogin.plugin.velocity.util.files.client.OfflineClient;
import eu.locklogin.plugin.velocity.util.player.PlayerPool;
import eu.locklogin.plugin.velocity.util.player.User;
import ml.karmaconfigs.api.common.timer.SchedulerUnit;
import ml.karmaconfigs.api.common.timer.SourceScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
import ml.karmaconfigs.api.common.utils.uuid.UUIDType;
import ml.karmaconfigs.api.common.utils.uuid.UUIDUtil;
import net.kyori.adventure.text.Component;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static eu.locklogin.plugin.velocity.LockLogin.*;
import static eu.locklogin.plugin.velocity.plugin.sender.DataSender.*;

public final class JoinListener {

    private final static Set<UUID> switch_pool = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final static PluginConfiguration config = CurrentPlatform.getConfiguration();
    private final static ProxyConfiguration proxy = CurrentPlatform.getProxyConfiguration();
    private final static PluginMessages messages = CurrentPlatform.getMessages();

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
            User user = new User(player);

            if (!switch_pool.contains(player.getUniqueId())) {
                InetSocketAddress ip = player.getRemoteAddress();
                ModulePlayer sender = new ModulePlayer(
                        player.getGameProfile().getName(),
                        player.getUniqueId(),
                        user.getSession(),
                        user.getManager(),
                        (player.getRemoteAddress() == null ? null : player.getRemoteAddress().getAddress())
                );

                //If velocity player objects changes module player also changes
                CurrentPlatform.connectPlayer(sender, player);

                PluginIpValidationEvent.ValidationResult validationResult = PluginIpValidationEvent.ValidationResult.SUCCESS.withReason("Plugin configuration tells to ignore proxy IPs");
                ProxyCheck proxyCheck = new ProxyCheck(ip);
                if (proxyCheck.isProxy()) {
                    validationResult = PluginIpValidationEvent.ValidationResult.INVALID.withReason("IP has been detected as proxy");
                }

                PluginIpValidationEvent ipEvent = new PluginIpValidationEvent(ip.getAddress(), PluginIpValidationEvent.ValidationProcess.PROXY_IP,
                        validationResult,
                        validationResult.getReason(), null);
                ModulePlugin.callEvent(ipEvent);

                if (ipEvent.getResult() != validationResult && ipEvent.getHandleOwner() == null) {
                    try {
                        Field f = ipEvent.getClass().getDeclaredField("validationResult");
                        f.setAccessible(true);

                        f.set(ipEvent, validationResult); //Deny changes from unknown sources
                    } catch (Throwable ignored) {
                    }
                }

                if (!ipEvent.isHandled()) {
                    switch (ipEvent.getResult()) {
                        case SUCCESS:
                            Optional<ServerConnection> tmp_server = player.getCurrentServer();
                            if (tmp_server.isPresent()) {
                                ServerConnection connection = tmp_server.get();

                                RegisteredServer server = connection.getServer();

                                if (ServerDataStorage.needsRegister(server.getServerInfo().getName()) || ServerDataStorage.needsProxyKnowledge(server.getServerInfo().getName())) {
                                    if (ServerDataStorage.needsRegister(server.getServerInfo().getName()))
                                        DataSender.send(server, DataSender.getBuilder(DataType.KEY, ACCESS_CHANNEL, player).addTextData(proxy.proxyKey()).addTextData(server.getServerInfo().getName()).addBoolData(proxy.multiBungee()).build());

                                    if (ServerDataStorage.needsProxyKnowledge(server.getServerInfo().getName())) {
                                        DataSender.send(server, DataSender.getBuilder(DataType.REGISTER, ACCESS_CHANNEL, player)
                                                .addTextData(proxy.proxyKey()).addTextData(server.getServerInfo().getName())
                                                .addTextData(TokenGen.expiration("local_token").toString())
                                                .build());
                                    }
                                }
                                DataSender.send(player, DataSender.getBuilder(DataType.MESSAGES, PLUGIN_CHANNEL, player).addTextData(proxy.proxyKey()).addTextData(server.getServerInfo().getName()).addTextData(CurrentPlatform.getMessages().toString()).build());
                                DataSender.send(player, DataSender.getBuilder(DataType.CONFIG, PLUGIN_CHANNEL, player).addTextData(proxy.proxyKey()).addTextData(server.getServerInfo().getName()).addTextData(Config.manager.getConfiguration()).build());
                            }

                            CurrentPlatform.requestDataContainerUpdate();

                            MessageData validation = getBuilder(DataType.VALIDATION, DataSender.CHANNEL_PLAYER, player).build();
                            DataSender.send(player, validation);

                            user.applySessionEffects();

                            if (config.clearChat()) {
                                for (int i = 0; i < 150; i++)
                                    plugin.getServer().getScheduler().buildTask(plugin.getContainer(), () -> player.sendMessage(Component.text().content("").build()));
                            }

                            ClientSession session = user.getSession();
                            AccountManager manager = user.getManager();
                            session.validate();

                            if (!config.captchaOptions().isEnabled())
                                session.setCaptchaLogged(true);

                            SimpleScheduler tmp_timer = null;
                            if (!session.isCaptchaLogged()) {
                                tmp_timer = new SourceScheduler(plugin, 1, SchedulerUnit.SECOND, true);
                                tmp_timer.changeAction((second) -> player.sendActionBar(Component.text().content(StringUtils.toColor(messages.captcha(session.getCaptcha()))).build())).start();
                            }

                            MessageData join = DataSender.getBuilder(DataType.JOIN, CHANNEL_PLAYER, player)
                                    .addBoolData(session.isLogged())
                                    .addBoolData(session.is2FALogged())
                                    .addBoolData(session.isPinLogged())
                                    .addBoolData(manager.isRegistered()).build();
                            DataSender.send(player, join);

                            SimpleScheduler timer = tmp_timer;
                            SessionCheck<Player> check = user.getChecker().whenComplete(() -> {
                                user.restorePotionEffects();
                                player.sendActionBar(Component.text().content("").build());

                                if (timer != null)
                                    timer.cancel();
                            });
                            plugin.getServer().getScheduler().buildTask(plugin.getContainer(), check).schedule();

                            DataSender.send(player, DataSender.getBuilder(DataType.CAPTCHA, CHANNEL_PLAYER, player).build());

                            player.getCurrentServer().ifPresent(server -> {
                                if (Proxy.isAuth(server.getServerInfo())) {
                                    user.checkServer(0);
                                }
                            });

                            Event event = new UserPostJoinEvent(user.getModule(), null);
                            ModulePlugin.callEvent(event);

                            if (event.isHandled()) {
                                user.kick(event.getHandleReason());
                            }

                            if (!ipEvent.getResult().equals(validationResult) && ipEvent.getHandleOwner() != null) {
                                logger.scheduleLog(Level.WARNING, "Module {0} changed the plugin IP validation result from {1} to {2} with reason {3}",
                                        ipEvent.getHandleOwner().name(), validationResult.name(), ipEvent.getResult().name(), ipEvent.getResult().getReason());
                            }
                            break;
                        case INVALID:
                        case ERROR:
                        default:
                            if (!ipEvent.getResult().equals(validationResult) && ipEvent.getHandleOwner() != null) {
                                logger.scheduleLog(Level.WARNING, "Module {0} changed the plugin IP validation result from {1} to {2} with reason {3}",
                                        ipEvent.getHandleOwner().name(), validationResult.name(), ipEvent.getResult().name(), ipEvent.getResult().getReason());
                            }

                            logger.scheduleLog(Level.INFO, "Denied player {0} to join with reason: {1}", StringUtils.stripColor(player.getUsername()), ipEvent.getResult().getReason());
                            user.kick(StringUtils.toColor(
                                    StringUtils.formatString(messages.ipProxyError() + "\n\n{0}",
                                            ipEvent.getResult().getReason())));
                            break;
                    }
                } else {
                    user.kick(StringUtils.toColor(ipEvent.getHandleReason()));
                }
            } else {
                MessageData validation = getBuilder(DataType.VALIDATION, DataSender.CHANNEL_PLAYER, player).build();
                DataSender.send(player, validation);

                ClientSession session = user.getSession();
                AccountManager manager = user.getManager();
                session.validate();

                MessageData join = DataSender.getBuilder(DataType.JOIN, CHANNEL_PLAYER, player)
                        .addBoolData(session.isLogged())
                        .addBoolData(session.is2FALogged())
                        .addBoolData(session.isPinLogged())
                        .addBoolData(manager.isRegistered()).build();
                DataSender.send(player, join);

                DataSender.send(player, DataSender.getBuilder(DataType.CAPTCHA, DataSender.CHANNEL_PLAYER, player).build());

                user.checkServer(0);
            }
        });
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onServerPing(ProxyPingEvent e) {
        if (Manager.isInitialized()) {
            InetAddress ip = e.getConnection().getRemoteAddress().getAddress();

            Event ipEvent = new PluginIpValidationEvent(ip, PluginIpValidationEvent.ValidationProcess.SERVER_PING,
                    PluginIpValidationEvent.ValidationResult.SUCCESS,
                    "Plugin added the IP to the IP validation queue", e);

            ModulePlugin.callEvent(ipEvent);
        } else {
            if (config.showMOTD()) {
                e.setPing(e.getPing().asBuilder().description(Component.text().content(StringUtils.toColor("&dLockLogin &8&l| &aStarting server")).build()).build());
            }
        }
    }

    @Subscribe(order = PostOrder.LAST)
    public void onGameProfileRequest(GameProfileRequestEvent e) {
        Event event = new VelocityGameProfileEvent(e);
        ModulePlugin.callEvent(event);

        ids.put(e.getGameProfile().getName(), e.getGameProfile().getId());
        ids.put(e.getOriginalProfile().getName(), e.getGameProfile().getId());
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPreLogin(PreLoginEvent e) {
        if (Manager.isInitialized()) {
            InetAddress ip = e.getConnection().getRemoteAddress().getAddress();

            PluginIpValidationEvent.ValidationResult validationResult = validateIP(ip);

            PluginIpValidationEvent ipEvent = new PluginIpValidationEvent(ip, PluginIpValidationEvent.ValidationProcess.VALID_IP,
                    validationResult,
                    validationResult.getReason(),
                    e);
            ModulePlugin.callEvent(ipEvent);

            if (ipEvent.getResult() != validationResult && ipEvent.getHandleOwner() == null) {
                try {
                    Field f = ipEvent.getClass().getDeclaredField("validationResult");
                    f.setAccessible(true);

                    f.set(ipEvent, validationResult); //Deny changes from unknown sources
                } catch (Throwable ignored) {}
            }

            String conn_name = e.getUsername();
            UUID gen_uuid = UUIDUtil.fetch(conn_name, UUIDType.OFFLINE);
            if (CurrentPlatform.isOnline() || e.getResult().isOnlineModeAllowed()) {
                gen_uuid = UUIDUtil.fetch(conn_name, UUIDType.ONLINE);
            }
            UUID tar_uuid = ids.getOrDefault(conn_name, gen_uuid);

            if (!ipEvent.isHandled()) {
                switch (ipEvent.getResult()) {
                    case SUCCESS:
                        if (e.getResult().isAllowed()) {
                            if (config.registerOptions().maxAccounts() > 0) {
                                AccountData data = new AccountData(ip, AccountID.fromUUID(tar_uuid));

                                if (data.allow(config.registerOptions().maxAccounts())) {
                                    data.save();

                                    int amount = data.getAlts().size();
                                    if (amount > 2) {
                                        for (Player online : plugin.getServer().getAllPlayers()) {
                                            User user = new User(online);

                                            if (user.hasPermission(PluginPermissions.info_alt_alert())) {
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

                            boolean check = CurrentPlatform.isOnline() || !e.getResult().isOnlineModeAllowed();
                            if (check) {
                                if (config.uuidValidator()) {
                                    if (!gen_uuid.equals(tar_uuid)) {
                                        FloodGateUtil util = new FloodGateUtil(tar_uuid);
                                        if (!util.isFloodClient()) {
                                            e.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text().content(StringUtils.toColor(messages.uuidFetchError())).build()));
                                            return;
                                        }
                                    }
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

                                if (account.isLocked()) {
                                    String administrator = account.getIssuer();
                                    Instant date = account.getLockDate();
                                    InstantParser parser = new InstantParser(date);
                                    String dateString = parser.getDay() + " " + parser.getMonth() + " " + parser.getYear();

                                    e.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text().content(StringUtils.toColor(messages.forcedAccountRemoval(administrator + " [ " + dateString + " ]"))).build()));
                                    logger.scheduleLog(Level.WARNING, "Client {0} tried to join, but his account was blocked by {1} on {2}", conn_name, administrator, dateString);
                                    return;
                                }
                            }

                            Event event = new UserPreJoinEvent(e.getConnection().getRemoteAddress().getAddress(), tar_uuid, conn_name, e);
                            ModulePlugin.callEvent(event);

                            if (event.isHandled()) {
                                e.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text().content(StringUtils.toColor(event.getHandleReason())).build()));
                                return;
                            }

                            PlayerPool.addPlayer(tar_uuid);
                        }
                        break;
                    case INVALID:
                    case ERROR:
                    default:
                        if (!ipEvent.getResult().equals(validationResult) && ipEvent.getHandleOwner() != null) {
                            logger.scheduleLog(Level.WARNING, "Module {0} changed the plugin IP validation result from {1} to {2} with reason {3}",
                                    ipEvent.getHandleOwner().name(), validationResult.name(), ipEvent.getResult().name(), ipEvent.getResult().getReason());
                        }

                        logger.scheduleLog(Level.INFO, "Denied player {0} to join with reason: {1}", conn_name, ipEvent.getResult().getReason());
                        e.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text().content(StringUtils.toColor(
                                StringUtils.formatString(messages.ipProxyError() + "\n\n{0}",
                                        ipEvent.getResult().getReason()))).build())
                        );

                        break;
                }
            } else {
                e.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text().content(StringUtils.toColor(ipEvent.getHandleReason())).build()));
            }

            ids.remove(conn_name);
        } else {
            e.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text().content(StringUtils.toColor("&cThe server is starting up!")).build()));
        }
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onLogin(LoginEvent e) {
        if (e.getResult().isAllowed()) {
            Event event = new UserJoinEvent(e.getPlayer().getRemoteAddress().getAddress(), e.getPlayer().getUniqueId(), e.getPlayer().getUsername(), e);
            ModulePlugin.callEvent(event);

            if (event.isHandled()) {
                e.setResult(ResultedEvent.ComponentResult.denied(Component.text().content(StringUtils.toColor(event.getHandleReason())).build()));
                PlayerPool.delPlayer(e.getPlayer().getUniqueId());
            }
        }
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onSwitch(ServerConnectedEvent e) {
        plugin.getServer().getScheduler().buildTask(plugin.getContainer(), () -> {
            Player player = e.getPlayer();

            RegisteredServer server = e.getServer();
            ProxyConfiguration proxy = CurrentPlatform.getProxyConfiguration();

            if (ServerDataStorage.needsRegister(server.getServerInfo().getName()) || ServerDataStorage.needsProxyKnowledge(server.getServerInfo().getName())) {
                if (ServerDataStorage.needsRegister(server.getServerInfo().getName()))
                    DataSender.send(server, DataSender.getBuilder(DataType.KEY, ACCESS_CHANNEL, player).addTextData(proxy.proxyKey()).addTextData(server.getServerInfo().getName()).addBoolData(proxy.multiBungee()).build());

                if (ServerDataStorage.needsProxyKnowledge(server.getServerInfo().getName())) {
                    DataSender.send(server, DataSender.getBuilder(DataType.REGISTER, ACCESS_CHANNEL, player)
                            .addTextData(proxy.proxyKey()).addTextData(server.getServerInfo().getName())
                            .addTextData(TokenGen.expiration("local_token").toString())
                            .build());
                }
            }
            DataSender.send(player, DataSender.getBuilder(DataType.MESSAGES, PLUGIN_CHANNEL, player).addTextData(proxy.proxyKey()).addTextData(server.getServerInfo().getName()).addTextData(CurrentPlatform.getMessages().toString()).build());
            DataSender.send(player, DataSender.getBuilder(DataType.CONFIG, PLUGIN_CHANNEL, player).addTextData(proxy.proxyKey()).addTextData(server.getServerInfo().getName()).addTextData(Config.manager.getConfiguration()).build());

            CurrentPlatform.requestDataContainerUpdate();

            if (CurrentPlatform.getServer().isOnline(player.getUniqueId())) {
                switch_pool.add(player.getUniqueId());
            }

            PlayerPool.addPlayer(player.getUniqueId());
        }).delay((long) 1.5, TimeUnit.SECONDS).schedule();
    }

    /**
     * Check if the ip is valid
     *
     * @param ip the ip address
     * @return if the ip is valid
     */
    private PluginIpValidationEvent.ValidationResult validateIP(final InetAddress ip) {
        try {
            if (config.ipHealthCheck()) {
                if (StringUtils.isNullOrEmpty(ip.getHostAddress())) {
                    return PluginIpValidationEvent.ValidationResult.INVALID.withReason("The IP host address is null or empty");
                }

                Matcher matcher = IPv4_PATTERN.matcher(ip.getHostAddress());
                return (matcher.matches() ?
                        PluginIpValidationEvent.ValidationResult.SUCCESS.withReason("Plugin determined IP is valid") :
                        PluginIpValidationEvent.ValidationResult.INVALID.withReason("Plugin determined IP is not valid for regex"));
            }

            return PluginIpValidationEvent.ValidationResult.SUCCESS.withReason("Plugin configuration tells to ignore invalid IPs");
        } catch (Throwable ex) {
            return PluginIpValidationEvent.ValidationResult.ERROR.withReason("Failed to check IP: " + ex.fillInStackTrace());
        }
    }
}
