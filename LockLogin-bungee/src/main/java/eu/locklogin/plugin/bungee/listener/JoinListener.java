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

import eu.locklogin.api.account.AccountID;
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.common.security.BruteForce;
import eu.locklogin.api.common.security.client.AccountData;
import eu.locklogin.api.common.security.client.Name;
import eu.locklogin.api.common.security.client.ProxyCheck;
import eu.locklogin.api.common.session.SessionCheck;
import eu.locklogin.api.common.utils.Channel;
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
import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.client.permission.plugin.PluginPermissions;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bungee.BungeeSender;
import eu.locklogin.plugin.bungee.com.message.DataMessage;
import eu.locklogin.plugin.bungee.plugin.Manager;
import eu.locklogin.plugin.bungee.util.files.Proxy;
import eu.locklogin.plugin.bungee.util.files.client.OfflineClient;
import eu.locklogin.plugin.bungee.util.player.PlayerPool;
import eu.locklogin.plugin.bungee.util.player.User;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.timer.SchedulerUnit;
import ml.karmaconfigs.api.common.timer.SourceScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.uuid.UUIDType;
import ml.karmaconfigs.api.common.utils.uuid.UUIDUtil;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static eu.locklogin.plugin.bungee.LockLogin.*;

public final class JoinListener implements Listener {

    private final PluginConfiguration config = CurrentPlatform.getConfiguration();
    private final ProxyConfiguration proxy = CurrentPlatform.getProxyConfiguration();
    private final PluginMessages messages = CurrentPlatform.getMessages();

    private final String IPV4_REGEX =
            "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
    private final Pattern IPv4_PATTERN = Pattern.compile(IPV4_REGEX);

    public final PlayerPool pool;
    public final PlayerPool switch_pool;

    private final Map<UUID, String> old_servers = new ConcurrentHashMap<>();

    /**
     * Initialize the join listener
     */
    public JoinListener() {
        pool = new PlayerPool("playerJoinQueue");
        switch_pool = new PlayerPool("playerSwitchQueue");

        pool.whenValid((player) -> {
            User user = new User(player);

            InetSocketAddress ip = getSocketIp(player.getSocketAddress());

            if (ip != null) {
                ModulePlayer sender = new ModulePlayer(
                        player.getName(),
                        player.getUniqueId(),
                        user.getSession(),
                        user.getManager(),
                        (player.getSocketAddress() == null ? null : ((InetSocketAddress) player.getSocketAddress()).getAddress())
                );
                //If bungee player objects changes module player also changes
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
                            Server server = player.getServer();
                            if (server != null && server.getInfo() != null) {
                                ServerInfo info = server.getInfo();
                                ProxyConfiguration proxy = CurrentPlatform.getProxyConfiguration();

                                if (ServerDataStorage.needsProxyKnowledge(info.getName())) {
                                    if (BungeeSender.useSocket) {
                                        Manager.sendSecondaryTopFunction
                                                .apply(DataMessage.newInstance(DataType.REGISTER, Channel.ACCESS, player)
                                                                .addProperty("key", proxy.proxyKey())
                                                                .addProperty("server", info.getName())
                                                                .addProperty("socket", BungeeSender.useSocket).getInstance(),
                                                        info);
                                    } else {
                                        Manager.sendTopFunction
                                                .apply(DataMessage.newInstance(DataType.REGISTER, Channel.ACCESS, player)
                                                                .addProperty("key", proxy.proxyKey())
                                                                .addProperty("server", info.getName())
                                                                .addProperty("socket", BungeeSender.useSocket).getInstance(),
                                                        info);
                                    }
                                }
                            }

                            CurrentPlatform.requestDataContainerUpdate();

                            ServerInfo info = BungeeSender.serverFromPlayer(player);

                            Manager.sendFunction.apply(DataMessage.newInstance(DataType.VALIDATION, Channel.ACCOUNT, player)
                                    .getInstance(),
                                    info);

                            user.applySessionEffects();

                            if (config.clearChat()) {
                                for (int i = 0; i < 150; i++)
                                    plugin.getProxy().getScheduler().runAsync(plugin, () -> player.sendMessage(TextComponent.fromLegacyText("")));
                            }

                            ClientSession session = user.getSession();
                            AccountManager manager = user.getManager();
                            session.validate();

                            if (!config.captchaOptions().isEnabled())
                                session.setCaptchaLogged(true);

                            SimpleScheduler tmp_timer = null;
                            if (!session.isCaptchaLogged()) {
                                tmp_timer = new SourceScheduler(plugin, 1, SchedulerUnit.SECOND, true);
                                final String captcha_message = StringUtils.toColor(messages.captcha(session.getCaptcha())); //If we do this, then the captcha code won't get updated on each second

                                tmp_timer.changeAction((second) -> player.sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(captcha_message))).start();
                            }

                            if (FloodGateUtil.hasFloodgate()) {
                                FloodGateUtil floodGate = new FloodGateUtil(player.getUniqueId());
                                if (floodGate.isBedrockClient() && config.bedrockLogin()) {
                                    AccountManager account = user.getManager();
                                    if (account.isRegistered()) {
                                        session.setCaptchaLogged(true);
                                        session.setLogged(true);
                                        session.set2FALogged(true);
                                        session.setPinLogged(true);

                                        plugin.console().send("Detected bedrock player {0}. He has been authenticated without requesting login", Level.INFO, player.getName());
                                    }
                                }
                            }

                            Manager.sendFunction.apply(DataMessage.newInstance(DataType.JOIN, Channel.ACCOUNT, player)
                                    .addProperty("pass_login", session.isLogged())
                                    .addProperty("2fa_login", session.is2FALogged())
                                    .addProperty("pin_login", session.isPinLogged())
                                    .addProperty("registered", manager.isRegistered())
                                    .getInstance(),
                                    info);

                            SimpleScheduler timer = tmp_timer;
                            SessionCheck<ProxiedPlayer> check = user.getChecker().whenComplete(() -> {
                                user.restorePotionEffects();
                                player.sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(""));

                                if (timer != null)
                                    timer.cancel();
                            });

                            plugin.getProxy().getScheduler().runAsync(plugin, check);

                            Manager.sendFunction.apply(DataMessage.newInstance(DataType.CAPTCHA, Channel.ACCOUNT, player)
                                            .getInstance(),
                                    info);

                            if (!Proxy.isAuth(player.getServer().getInfo())) {
                                user.checkServer(0);
                            }

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

                            logger.scheduleLog(Level.INFO, "Denied player {0} to join with reason: {1}", StringUtils.stripColor(player.getDisplayName()), ipEvent.getResult().getReason());
                            user.kick(StringUtils.toColor(
                                    StringUtils.formatString(messages.ipProxyError() + "\n\n{0}",
                                            ipEvent.getResult().getReason())));
                            break;
                    }
                } else {
                    user.kick(StringUtils.toColor(ipEvent.getHandleReason()));
                }
            } else {
                user.kick(StringUtils.toColor(messages.ipProxyError()));
            }
        });
        switch_pool.whenValid((player) -> {
            Server connected = player.getServer();
            if (connected != null && old_servers.containsKey(player.getUniqueId())) {
                ServerInfo info = connected.getInfo();
                String old = old_servers.get(player.getUniqueId());

                if (!info.getName().equals(old)) {
                    if (ServerDataStorage.needsProxyKnowledge(info.getName())) {
                        if (BungeeSender.useSocket) {
                            Manager.sendSecondaryTopFunction
                                    .apply(DataMessage.newInstance(DataType.REGISTER, Channel.ACCESS, player)
                                                    .addProperty("key", proxy.proxyKey())
                                                    .addProperty("server", info.getName())
                                                    .addProperty("socket", BungeeSender.useSocket).getInstance(),
                                            info);
                        } else {
                            Manager.sendTopFunction
                                    .apply(DataMessage.newInstance(DataType.REGISTER, Channel.ACCESS, player)
                                                    .addProperty("key", proxy.proxyKey())
                                                    .addProperty("server", info.getName())
                                                    .addProperty("socket", BungeeSender.useSocket).getInstance(),
                                            info);
                        }
                    }

                    User user = new User(player);

                    Manager.sendFunction.apply(DataMessage.newInstance(DataType.VALIDATION, Channel.ACCOUNT, player)
                            .getInstance(), info);

                    CurrentPlatform.requestDataContainerUpdate();

                    ClientSession session = user.getSession();
                    AccountManager manager = user.getManager();
                    session.validate();

                    if (FloodGateUtil.hasFloodgate()) {
                        FloodGateUtil floodGate = new FloodGateUtil(player.getUniqueId());
                        if (floodGate.isBedrockClient() && config.bedrockLogin()) {
                            AccountManager account = user.getManager();
                            if (account.isRegistered() && config.bedrockLogin()) {
                                session.setCaptchaLogged(true);
                                session.setLogged(true);
                                session.set2FALogged(true);
                                session.setPinLogged(true);

                                plugin.console().send("Detected bedrock player {0}. He has been authenticated without requesting login", Level.INFO, player.getName());
                            }
                        }
                    }

                    Manager.sendFunction.apply(DataMessage.newInstance(DataType.JOIN, Channel.ACCOUNT, player)
                            .addProperty("pass_login", session.isLogged())
                            .addProperty("2fa_login", session.is2FALogged())
                            .addProperty("pin_login", session.isPinLogged())
                            .addProperty("registered", manager.isRegistered())
                            .getInstance(), info);

                    Manager.sendFunction.apply(DataMessage.newInstance(DataType.CAPTCHA, Channel.ACCOUNT, player)
                            .getInstance(), info);

                    user.checkServer(0);
                    return;
                }
            }

            switch_pool.addPlayer(player.getUniqueId());
        });

        pool.startCheckTask();
        switch_pool.startCheckTask();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onServerPing(ProxyPingEvent e) {
        if (Manager.isInitialized()) {
            InetAddress ip = getIp(e.getConnection().getSocketAddress());

            Event ipEvent = new PluginIpValidationEvent(ip, PluginIpValidationEvent.ValidationProcess.SERVER_PING,
                    PluginIpValidationEvent.ValidationResult.SUCCESS,
                    "Plugin added the IP to the IP validation queue", e);
            ModulePlugin.callEvent(ipEvent);
        } else {
            if (config.showMOTD()) {
                ServerPing p = e.getResponse();
                p.setDescriptionComponent(new TextComponent(StringUtils.toColor("&dLockLogin &8&l| &aStarting server")));

                e.setResponse(p);
            }
        }
    }

    @SuppressWarnings("all")
    @EventHandler(priority = EventPriority.LOWEST)
    public final void onPreLogin(PreLoginEvent e) {
        if (Manager.isInitialized()) {
            InetAddress ip = getIp(e.getConnection().getSocketAddress());

            PluginIpValidationEvent.ValidationResult validationResult = validateIP(ip);

            PluginIpValidationEvent ipEvent = new PluginIpValidationEvent(ip, PluginIpValidationEvent.ValidationProcess.VALID_IP,
                    validationResult,
                    validationResult.getReason(),
                    e);
            ModulePlugin.callEvent(ipEvent);

            String conn_name = e.getConnection().getName();
            UUID gen_uuid = UUIDUtil.fetch(conn_name, UUIDType.OFFLINE);
            if (CurrentPlatform.isOnline() || e.getConnection().isOnlineMode()) {
                gen_uuid = UUIDUtil.fetch(conn_name, UUIDType.ONLINE);
            }

            if (!ipEvent.isHandled()) {
                QuitListener.tmp_clients.remove(gen_uuid);

                switch (ipEvent.getResult()) {
                    case SUCCESS:
                        if (!e.isCancelled()) {
                            if (config.registerOptions().maxAccounts() > 0) {
                                AccountData data = new AccountData(ip, AccountID.fromUUID(gen_uuid));

                                if (data.allow(config.registerOptions().maxAccounts())) {
                                    data.save();

                                    int amount = data.getAlts().size();
                                    if (amount > 2) {
                                        for (ProxiedPlayer online : plugin.getProxy().getPlayers()) {
                                            User user = new User(online);

                                            if (user.hasPermission(PluginPermissions.info_alt_alert())) {
                                                user.send(messages.prefix() + messages.altFound(conn_name, amount - 1));
                                            }
                                        }

                                        if (!messages.altFound(conn_name, amount).replaceAll("\\s", "").isEmpty())
                                            console.send(messages.prefix() + messages.altFound(conn_name, amount - 1));
                                    }
                                } else {
                                    e.setCancelled(true);
                                    e.setCancelReason(TextComponent.fromLegacyText(StringUtils.toColor(messages.maxRegisters())));
                                    return;
                                }
                            }

                            if (config.bruteForceOptions().getMaxTries() > 0) {
                                BruteForce protection = new BruteForce(ip);
                                if (protection.isBlocked()) {
                                    e.setCancelled(true);
                                    e.setCancelReason(TextComponent.fromLegacyText(StringUtils.toColor(messages.ipBlocked(protection.getBlockLeft()))));
                                    return;
                                }
                            }

                            if (config.checkNames()) {
                                Name name = new Name(conn_name);
                                name.check();

                                if (name.notValid()) {
                                    boolean r = false;

                                    if (FloodGateUtil.hasFloodgate()) {
                                        FloodGateUtil util = new FloodGateUtil(gen_uuid);
                                        if (util.isBedrockClient()) {
                                            r = true;
                                            plugin.console().send("Connected player {0} from bedrock", Level.WARNING, conn_name);
                                        }
                                    }

                                    if (!r) {
                                        e.setCancelled(true);
                                        e.setCancelReason(TextComponent.fromLegacyText(StringUtils.toColor(messages.illegalName(name.getInvalidChars()))));
                                        return;
                                    }
                                }
                            }

                            OfflineClient offline = new OfflineClient(conn_name);
                            AccountManager manager = offline.getAccount();

                            if (manager != null) {
                                if (config.enforceNameCheck()) {
                                    if (!manager.getName().equals(conn_name)) {
                                        e.setCancelled(true);
                                        e.setCancelReason(TextComponent.fromLegacyText(StringUtils.toColor(messages.similarName(manager.getName()))));
                                        return;
                                    }
                                }

                                LockedAccount account = new LockedAccount(manager.getUUID());

                                if (account.isLocked()) {
                                    String administrator = account.getIssuer();
                                    Instant date = account.getLockDate();
                                    InstantParser parser = new InstantParser(date);
                                    String dateString = parser.getDay() + " " + parser.getMonth() + " " + parser.getYear();

                                    e.setCancelled(true);
                                    e.setCancelReason(TextComponent.fromLegacyText(StringUtils.toColor(messages.forcedAccountRemoval(administrator + " [ " + dateString + " ]"))));
                                    logger.scheduleLog(Level.WARNING, "Client {0} tried to join, but his account was blocked by {1} on {2}", conn_name, administrator, dateString);
                                    return;
                                }
                            }

                            Event event = new UserPreJoinEvent(ip, gen_uuid, conn_name, e);
                            ModulePlugin.callEvent(event);

                            if (event.isHandled()) {
                                e.setCancelled(true);
                                e.setCancelReason(TextComponent.fromLegacyText(StringUtils.toColor(event.getHandleReason())));
                                return;
                            }

                            e.setCancelled(false);
                        }
                        break;
                    case INVALID:
                    case ERROR:
                    default:
                        if (!ipEvent.getResult().equals(validationResult)) {
                            logger.scheduleLog(Level.WARNING, "Module {0} changed the plugin IP validation result from {1} to {2} with reason {3}",
                                    ipEvent.getHandleOwner().name(), validationResult.name(), ipEvent.getResult().name(), ipEvent.getResult().getReason());
                        }

                        logger.scheduleLog(Level.INFO, "Denied player {0} to join with reason: {1}", conn_name, ipEvent.getResult().getReason());
                        e.setCancelled(true);
                        e.setCancelReason(TextComponent.fromLegacyText(StringUtils.toColor(
                                StringUtils.formatString(messages.ipProxyError() + "\n\n{0}",
                                        ipEvent.getResult().getReason()))));
                        break;
                }
            } else {
                e.setCancelled(true);
                e.setCancelReason(TextComponent.fromLegacyText(StringUtils.toColor(ipEvent.getHandleReason())));
            }
        } else {
            e.setCancelled(true);
            e.setCancelReason(TextComponent.fromLegacyText(StringUtils.toColor("&cThe server is starting up!")));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(LoginEvent e) {
        if (!e.isCancelled()) {
            PendingConnection connection = e.getConnection();

            boolean check = CurrentPlatform.isOnline() || !connection.isOnlineMode();
            UUID tar_uuid = connection.getUniqueId();
            if (check) {
                UUID gen_uuid = UUIDUtil.fetch(connection.getName(), UUIDType.OFFLINE);
                if (CurrentPlatform.isOnline() && e.getConnection().isOnlineMode()) {
                    gen_uuid = UUIDUtil.fetch(connection.getName(), UUIDType.ONLINE);
                }

                if (config.uuidValidator()) {
                    if (!gen_uuid.equals(tar_uuid)) {
                        boolean r = false;
                        if (FloodGateUtil.hasFloodgate()) {
                            FloodGateUtil util = new FloodGateUtil(tar_uuid);
                            if (util.isBedrockClient()) {
                                r = true;
                            }
                        }

                        if (!r) {
                            e.setCancelled(true);
                            e.setCancelReason(TextComponent.fromLegacyText(StringUtils.toColor(messages.uuidFetchError())));
                            return;
                        }
                    }
                }
            }

            Event event = new UserJoinEvent(getIp(e.getConnection().getSocketAddress()), e.getConnection().getUniqueId(), e.getConnection().getName(), e);
            ModulePlugin.callEvent(event);

            if (event.isHandled()) {
                e.setCancelled(true);
                e.setCancelReason(TextComponent.fromLegacyText(StringUtils.toColor(event.getHandleReason())));
                pool.delPlayer(e.getConnection().getUniqueId());
            } else {
                if (tar_uuid != connection.getUniqueId()) {
                    //Making sure we update UUID for premium plugins which change UUIDs
                    tar_uuid = connection.getUniqueId();
                }

                pool.addPlayer(tar_uuid);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSwitch(ServerSwitchEvent e) {
        ProxiedPlayer player = e.getPlayer();

        if (CurrentPlatform.getServer().isOnline(player.getUniqueId())) {
            old_servers.put(player.getUniqueId(), e.getFrom().getName());
            switch_pool.addPlayer(player.getUniqueId());
        }
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
