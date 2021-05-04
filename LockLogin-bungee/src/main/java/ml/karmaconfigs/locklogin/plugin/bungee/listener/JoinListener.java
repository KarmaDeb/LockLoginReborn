package ml.karmaconfigs.locklogin.plugin.bungee.listener;

import ml.karmaconfigs.api.bungee.Console;
import ml.karmaconfigs.api.bungee.timer.AdvancedPluginTimer;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.account.AccountID;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.files.PluginConfiguration;
import ml.karmaconfigs.locklogin.api.modules.javamodule.JavaModuleManager;
import ml.karmaconfigs.locklogin.api.modules.event.user.UserJoinEvent;
import ml.karmaconfigs.locklogin.api.modules.event.user.UserPostJoinEvent;
import ml.karmaconfigs.locklogin.api.modules.event.user.UserPreJoinEvent;
import ml.karmaconfigs.locklogin.api.utils.platform.CurrentPlatform;
import ml.karmaconfigs.locklogin.plugin.bungee.plugin.sender.DataSender;
import ml.karmaconfigs.locklogin.plugin.bungee.plugin.sender.DataType;
import ml.karmaconfigs.locklogin.plugin.bungee.util.files.client.OfflineClient;
import ml.karmaconfigs.locklogin.plugin.bungee.util.files.data.lock.LockedAccount;
import ml.karmaconfigs.locklogin.plugin.bungee.util.files.data.lock.LockedData;
import ml.karmaconfigs.locklogin.plugin.bungee.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.bungee.util.player.SessionCheck;
import ml.karmaconfigs.locklogin.plugin.bungee.util.player.User;
import ml.karmaconfigs.locklogin.plugin.common.security.BruteForce;
import ml.karmaconfigs.locklogin.plugin.common.security.client.AccountData;
import ml.karmaconfigs.locklogin.plugin.common.security.client.IpData;
import ml.karmaconfigs.locklogin.plugin.common.security.client.Name;
import ml.karmaconfigs.locklogin.plugin.common.security.client.Proxy;
import ml.karmaconfigs.locklogin.plugin.common.session.SessionDataContainer;
import ml.karmaconfigs.locklogin.plugin.common.utils.InstantParser;
import ml.karmaconfigs.locklogin.plugin.common.utils.UUIDGen;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ml.karmaconfigs.locklogin.plugin.bungee.LockLogin.*;
import static ml.karmaconfigs.locklogin.plugin.bungee.permissibles.PluginPermission.altInfo;
import static ml.karmaconfigs.locklogin.plugin.bungee.plugin.sender.DataSender.*;

public final class JoinListener implements Listener {

    private final static Map<InetAddress, String> verified = new HashMap<>();

    private static final String IPV4_REGEX =
            "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
    private static final Pattern IPv4_PATTERN = Pattern.compile(IPV4_REGEX);

    @EventHandler(priority = EventPriority.LOWEST)
    public final void onPreLogin_APICall(PreLoginEvent e) {
        UserPreJoinEvent event = new UserPreJoinEvent(getIp(e.getConnection().getSocketAddress()), e.getConnection().getUniqueId(), e.getConnection().getName(), e);
        JavaModuleManager.callEvent(event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public final void onLogin_APICall(LoginEvent e) {
        UserJoinEvent event = new UserJoinEvent(getIp(e.getConnection().getSocketAddress()), e.getConnection().getUniqueId(), e.getConnection().getName(), e);
        JavaModuleManager.callEvent(event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public final void onPostLogin_APICall(PostLoginEvent e) {
        UserPostJoinEvent event = new UserPostJoinEvent(fromPlayer(e.getPlayer()), e);
        JavaModuleManager.callEvent(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onServerPing(ProxyPingEvent e) {
        verified.put(getIp(e.getConnection().getSocketAddress()), "");
    }

    @SuppressWarnings("all")
    @EventHandler(priority = EventPriority.LOWEST)
    public final void onPreLogin(PreLoginEvent e) {
        Message messages = new Message();
        InetAddress ip = getIp(e.getConnection().getSocketAddress());

        String conn_name = e.getConnection().getName();
        UUID tar_uuid = UUIDGen.getUUID(conn_name);

        String address = "null";
        try {
            address = ip.getHostAddress();
        } catch (Throwable ignored) {
        }
        if (!e.isCancelled()) {
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
                                for (ProxiedPlayer online : plugin.getProxy().getPlayers()) {
                                    User user = new User(online);

                                    if (user.hasPermission(altInfo())) {
                                        user.send(messages.prefix() + messages.altFound(conn_name, amount - 1));
                                    }
                                }

                                if (!messages.altFound(conn_name, amount).replaceAll("\\s", "").isEmpty())
                                    Console.send(messages.prefix() + messages.altFound(conn_name, amount - 1));
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

                    if (config.antiBot()) {
                        if (verified.containsKey(ip)) {
                            String name = verified.getOrDefault(ip, "");

                            if (!name.replaceAll("\\s", "").isEmpty() && !name.equals(conn_name)) {
                                //The anti bot is like a whitelist, only players in a certain list can join, the difference with LockLogin is that players are
                                //assigned to an IP, so the anti bot security is reinforced
                                e.setCancelled(true);
                                e.setCancelReason(TextComponent.fromLegacyText(StringUtils.toColor(messages.antiBot())));
                                return;
                            } else {
                                if (name.replaceAll("\\s", "").isEmpty())
                                    verified.put(ip, conn_name);
                            }
                        } else {
                            //The anti bot is like a whitelist, only players in a certain list can join, the difference with LockLogin is that players are
                            //assigned to an IP, so the anti bot security is reinforced
                            e.setCancelled(true);
                            e.setCancelReason(TextComponent.fromLegacyText(StringUtils.toColor(messages.antiBot())));
                            return;
                        }
                    }

                    if (!gen_uuid.equals(tar_uuid)) {
                        e.setCancelled(true);
                        e.setCancelReason(TextComponent.fromLegacyText(StringUtils.toColor(messages.uuidFetchError())));
                        return;
                    }

                    Name name = new Name(conn_name);
                    name.check();

                    if (name.notValid()) {
                        e.setCancelled(true);
                        e.setCancelReason(TextComponent.fromLegacyText(StringUtils.toColor(messages.illegalName(name.getInvalidChars()))));
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

                            e.setCancelled(true);
                            e.setCancelReason(TextComponent.fromLegacyText(StringUtils.toColor(messages.forcedAccountRemoval(administrator + " [ " + dateString + " ]"))));
                            logger.scheduleLog(Level.WARNING, "Client {0} tried to join, but his account was blocked by {1} on {2}", conn_name, administrator, dateString);
                            return;
                        }
                    }
                } else {
                    e.setCancelled(true);
                    e.setCancelReason(TextComponent.fromLegacyText(StringUtils.toColor(messages.ipProxyError())));
                    logger.scheduleLog(Level.GRAVE, "Player {0}[{2}] tried to join with an invalid IP address ( {1} ), his connection got rejected with ip is proxy message", conn_name, address, e.getConnection().getUniqueId());
                }
            } catch (Throwable ex) {
                logger.scheduleLog(Level.GRAVE, ex);
                e.setCancelled(true);
                e.setCancelReason(TextComponent.fromLegacyText(StringUtils.toColor(messages.ipProxyError())));
                logger.scheduleLog(Level.GRAVE, "Player {0}[{2}] tried to join with an invalid IP address ( {1} ), his connection got rejected with ip is proxy message", conn_name, address, e.getConnection().getUniqueId());
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public final void onLogin(LoginEvent e) {
        if (!e.isCancelled()) {
            Message messages = new Message();
            PluginConfiguration config = CurrentPlatform.getConfiguration();

            IpData data = new IpData(getIp(e.getConnection().getSocketAddress()));
            int amount = data.getClonesAmount();

            if (amount + 1 == config.accountsPerIP()) {
                e.setCancelled(true);
                e.setCancelReason(TextComponent.fromLegacyText(StringUtils.toColor(messages.maxIP())));
                return;
            }
            data.addClone();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public final void onPostLogin(PostLoginEvent e) {
        plugin.getProxy().getScheduler().schedule(plugin, () -> {
            PluginConfiguration config = CurrentPlatform.getConfiguration();

            ProxiedPlayer player = e.getPlayer();
            InetSocketAddress ip = getSocketIp(player.getSocketAddress());
            User user = new User(player);

            DataSender.send(player, DataSender.getBuilder(DataType.MESSAGES, PLUGIN_CHANNEL).addTextData(Message.manager.getMessages()).build());
            DataSender.send(player, DataSender.getBuilder(DataType.CONFIG, PLUGIN_CHANNEL).addTextData(Message.manager.getMessages()).build());
            DataSender.send(player, DataSender.getBuilder(DataType.LOGGED, PLUGIN_CHANNEL).addIntData(SessionDataContainer.getLogged()).build());
            DataSender.send(player, DataSender.getBuilder(DataType.REGISTERED, PLUGIN_CHANNEL).addIntData(SessionDataContainer.getRegistered()).build());

            MessageData validation = getBuilder(DataType.VALIDATION, DataSender.CHANNEL_PLAYER).build();
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
                    plugin.getProxy().getScheduler().runAsync(plugin, () -> player.sendMessage(TextComponent.fromLegacyText("")));
            }

            ClientSession session = user.getSession();
            session.validate();

            if (!config.captchaOptions().isEnabled())
                session.setCaptchaLogged(true);

            AdvancedPluginTimer tmp_timer = null;
            if (!session.isCaptchaLogged()) {
                tmp_timer = new AdvancedPluginTimer(plugin, 1, true);
                tmp_timer.addAction(() -> {
                    player.sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(StringUtils.toColor(messages.captcha(session.getCaptcha()))));
                }).start();
            }

            MessageData join = DataSender.getBuilder(DataType.JOIN, CHANNEL_PLAYER)
                    .addBoolData(session.isLogged())
                    .addBoolData(session.is2FALogged())
                    .addBoolData(session.isPinLogged())
                    .addBoolData(user.isRegistered()).build();
            DataSender.send(player, join);

            AdvancedPluginTimer timer = tmp_timer;
            SessionCheck check = new SessionCheck(player, target -> {
                player.sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(""));
                if (timer != null)
                    timer.setCancelled();
            }, target -> {
                player.sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(""));
                if (timer != null)
                    timer.setCancelled();
            });

            plugin.getProxy().getScheduler().runAsync(plugin, check);

            DataSender.send(player, DataSender.getBuilder(DataType.LOGGED, PLUGIN_CHANNEL).addIntData(SessionDataContainer.getLogged()).build());
            DataSender.send(player, DataSender.getBuilder(DataType.REGISTERED, PLUGIN_CHANNEL).addIntData(SessionDataContainer.getRegistered()).build());

            user.checkServer();
        }, 2, TimeUnit.SECONDS);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onSwitch(ServerSwitchEvent e) {
        plugin.getProxy().getScheduler().schedule(plugin, () -> {
            ProxiedPlayer player = e.getPlayer();
            User user = new User(player);

            DataSender.send(player, DataSender.getBuilder(DataType.MESSAGES, PLUGIN_CHANNEL).addTextData(Message.manager.getMessages()).build());
            DataSender.send(player, DataSender.getBuilder(DataType.CONFIG, PLUGIN_CHANNEL).addTextData(Message.manager.getMessages()).build());
            DataSender.send(player, DataSender.getBuilder(DataType.LOGGED, PLUGIN_CHANNEL).addIntData(SessionDataContainer.getLogged()).build());
            DataSender.send(player, DataSender.getBuilder(DataType.REGISTERED, PLUGIN_CHANNEL).addIntData(SessionDataContainer.getRegistered()).build());

            MessageData validation = getBuilder(DataType.VALIDATION, DataSender.CHANNEL_PLAYER).build();
            DataSender.send(player, validation);

            ClientSession session = user.getSession();
            session.validate();

            MessageData join = DataSender.getBuilder(DataType.JOIN, CHANNEL_PLAYER)
                    .addBoolData(session.isLogged())
                    .addBoolData(session.is2FALogged())
                    .addBoolData(session.isPinLogged())
                    .addBoolData(user.isRegistered()).build();
            DataSender.send(player, join);

            DataSender.send(player, DataSender.getBuilder(DataType.LOGGED, PLUGIN_CHANNEL).addIntData(SessionDataContainer.getLogged()).build());
            DataSender.send(player, DataSender.getBuilder(DataType.REGISTERED, PLUGIN_CHANNEL).addIntData(SessionDataContainer.getRegistered()).build());

            user.checkServer();
        }, 2, TimeUnit.SECONDS);
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
}
