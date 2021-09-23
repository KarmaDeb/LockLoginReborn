package eu.locklogin.plugin.bukkit.listener;

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
import eu.locklogin.api.common.security.client.ClientData;
import eu.locklogin.api.common.security.client.Name;
import eu.locklogin.api.common.security.client.ProxyCheck;
import eu.locklogin.api.common.session.SessionCheck;
import eu.locklogin.api.common.session.SessionKeeper;
import eu.locklogin.api.common.utils.InstantParser;
import eu.locklogin.api.common.utils.other.UUIDGen;
import eu.locklogin.api.common.utils.plugin.FloodGateUtil;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.module.plugin.api.event.plugin.PluginIpValidationEvent;
import eu.locklogin.api.module.plugin.api.event.user.GenericJoinEvent;
import eu.locklogin.api.module.plugin.api.event.user.UserJoinEvent;
import eu.locklogin.api.module.plugin.api.event.user.UserPostJoinEvent;
import eu.locklogin.api.module.plugin.api.event.user.UserPreJoinEvent;
import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.util.files.client.OfflineClient;
import eu.locklogin.plugin.bukkit.util.files.data.LastLocation;
import eu.locklogin.plugin.bukkit.util.files.data.Spawn;
import eu.locklogin.plugin.bukkit.util.files.data.lock.LockedAccount;
import eu.locklogin.plugin.bukkit.util.files.data.lock.LockedData;
import eu.locklogin.plugin.bukkit.util.player.ClientVisor;
import eu.locklogin.plugin.bukkit.util.player.User;
import me.clip.placeholderapi.PlaceholderAPI;
import ml.karmaconfigs.api.bukkit.reflections.BarMessage;
import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.timer.SourceSecondsTimer;
import ml.karmaconfigs.api.common.timer.scheduler.LateScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.worker.AsyncLateScheduler;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.api.common.utils.enums.Level;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.server.ServerListPingEvent;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static eu.locklogin.plugin.bukkit.LockLogin.*;
import static eu.locklogin.plugin.bukkit.plugin.PluginPermission.altAlert;

public final class JoinListener implements Listener {

    private final static PluginConfiguration config = CurrentPlatform.getConfiguration();
    private final static PluginMessages messages = CurrentPlatform.getMessages();

    private static final String IPV4_REGEX =
            "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
    private static final Pattern IPv4_PATTERN = Pattern.compile(IPV4_REGEX);

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerPing(ServerListPingEvent e) {
        if (!config.isBungeeCord()) {
            ClientData client = new ClientData(e.getAddress());
            if (!client.isVerified()) {
                client.setVerified(true);

                Event ipEvent = new PluginIpValidationEvent(e.getAddress(), PluginIpValidationEvent.ValidationProcess.SERVER_PING,
                        PluginIpValidationEvent.ValidationResult.SUCCESS,
                        "Plugin added the IP to the IP validation queue", e);
                ModulePlugin.callEvent(ipEvent);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent e) {
        InetAddress ip = e.getAddress();

        PluginIpValidationEvent.ValidationResult validationResult = validateIP(ip);
        if (config.isBungeeCord()) {
            validationResult = PluginIpValidationEvent.ValidationResult.SUCCESS.withReason("Plugin is BungeeCord mode, the IP validation process is done in BungeeCord/Velocity");
        }

        PluginIpValidationEvent ipEvent = new PluginIpValidationEvent(ip, PluginIpValidationEvent.ValidationProcess.VALID_IP,
                validationResult,
                validationResult.getReason(),
                e);
        ModulePlugin.callEvent(ipEvent);

        String conn_name = e.getName();
        UUID gen_uuid = UUIDGen.getUUID(e.getName());
        UUID tar_uuid = e.getUniqueId();

        if (!ipEvent.isHandled()) {
            switch (ipEvent.getResult()) {
                case SUCCESS:
                    if (e.getLoginResult().equals(AsyncPlayerPreLoginEvent.Result.ALLOWED)) {
                        if (!config.isBungeeCord()) {
                            //If the anti bot is disabled, verify the IP to avoid
                            //errors
                            if (!config.antiBot()) {
                                ClientData client = new ClientData(ip);
                                client.setVerified(true);
                            }

                            if (config.registerOptions().maxAccounts() > 0) {
                                AccountData data = new AccountData(ip, AccountID.fromUUID(tar_uuid));

                                if (data.allow(config.registerOptions().maxAccounts())) {
                                    data.save();

                                    int amount = data.getAlts().size();
                                    if (amount > 2) {
                                        for (Player online : plugin.getServer().getOnlinePlayers()) {
                                            if (online.hasPermission(altAlert())) {
                                                User user = new User(online);
                                                user.send(messages.prefix() + messages.altFound(conn_name, amount - 1));
                                            }
                                        }

                                        if (!messages.altFound(conn_name, amount).replaceAll("\\s", "").isEmpty())
                                            console.send(messages.prefix() + messages.altFound(conn_name, amount - 1));
                                    }
                                } else {
                                    e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_FULL, StringUtils.toColor(messages.maxRegisters()));
                                    return;
                                }
                            }

                            if (config.bruteForceOptions().getMaxTries() > 0) {
                                BruteForce protection = new BruteForce(ip);
                                if (protection.isBlocked()) {
                                    e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, StringUtils.toColor(messages.ipBlocked(protection.getBlockLeft())));
                                    return;
                                }
                            }

                            ClientData client = new ClientData(ip);
                            if (config.antiBot()) {
                                if (client.isVerified()) {
                                    if (client.canAssign(config.accountsPerIP(), conn_name, tar_uuid)) {
                                        logger.scheduleLog(Level.INFO, "Assigned IP address {0} to client {1}", ip.getHostAddress(), conn_name);
                                    } else {
                                        e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, StringUtils.toColor(messages.maxIP()));
                                        return;
                                    }
                                } else {
                                    e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, StringUtils.toColor(messages.antiBot()));
                                    return;
                                }
                            } else {
                                if (client.canAssign(config.accountsPerIP(), conn_name, tar_uuid)) {
                                    logger.scheduleLog(Level.INFO, "Assigned IP address {0} to client {1}", ip.getHostAddress(), conn_name);
                                } else {
                                    e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, StringUtils.toColor(messages.maxIP()));
                                    return;
                                }
                            }

                            if (config.uuidValidator()) {
                                if (!gen_uuid.equals(tar_uuid)) {
                                    FloodGateUtil util = new FloodGateUtil(tar_uuid);
                                    if (!util.isFloodClient()) {
                                        e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, StringUtils.toColor(messages.uuidFetchError()));
                                        return;
                                    }
                                }
                            }

                            if (config.checkNames()) {
                                Name name = new Name(conn_name);
                                name.check();

                                if (name.notValid()) {
                                    e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, StringUtils.toColor(messages.illegalName(name.getInvalidChars())));
                                    return;
                                }
                            }

                            Player player = plugin.getServer().getPlayer(tar_uuid);
                            if (player != null) {
                                if (config.allowSameIP()) {
                                    InetSocketAddress playerIP = player.getAddress();
                                    if (playerIP != null) {
                                        if (!playerIP.getAddress().equals(ip)) {
                                            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, StringUtils.toColor(messages.alreadyPlaying()));
                                            return;
                                        }
                                    }
                                }
                            }

                            OfflineClient offline = new OfflineClient(e.getName());
                            AccountManager manager = offline.getAccount();
                            if (manager != null) {
                                if (config.enforceNameCheck()) {
                                    if (!manager.getName().equals(e.getName())) {
                                        e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, StringUtils.toColor(messages.similarName(manager.getName())));
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

                                    e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, StringUtils.toColor(messages.forcedAccountRemoval(administrator + " [ " + dateString + " ]")));
                                    logger.scheduleLog(Level.WARNING, "Client {0} tried to join, but his account was blocked by {1} on {2}", conn_name, administrator, dateString);
                                    return;
                                }
                            }

                            if (!ipEvent.getResult().equals(validationResult)) {
                                logger.scheduleLog(Level.WARNING, "Module {0} changed the plugin IP validation result from {1} to {2} with reason {3}",
                                        ipEvent.getHandleOwner().name(), validationResult.name(), ipEvent.getResult().name(), ipEvent.getResult().getReason());
                            }

                            e.allow();
                        } else {
                            SimpleScheduler timer = new SourceSecondsTimer(plugin, 5, false);
                            timer.endAction(() -> {
                                Player online = plugin.getServer().getPlayer(tar_uuid);
                                if (online != null && online.isOnline()) {
                                    User user = new User(online);
                                    if (!user.getSession().isValid())
                                        user.kick(messages.bungeeProxy());
                                }
                            }).start();
                        }

                        if (!config.isBungeeCord()) {
                            Event event = new UserPreJoinEvent(ip, tar_uuid, conn_name, e);
                            ModulePlugin.callEvent(event);

                            if (event.isHandled()) {
                                e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, StringUtils.toColor(event.getHandleReason()));
                                return;
                            }
                        }
                    }
                    break;
                case INVALID:
                case ERROR:
                default:
                    if (!ipEvent.getResult().equals(validationResult)) {
                        logger.scheduleLog(Level.WARNING, "Module {0} changed the plugin IP validation result from {1} to {2} with reason {3}",
                                ipEvent.getHandleOwner().name(), validationResult.name(), ipEvent.getResult().name(), ipEvent.getResult().getReason());
                    }

                    logger.scheduleLog(Level.INFO, "Denied player {0} to join with reason: {1}", e.getName(), ipEvent.getResult().getReason());
                    e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                            StringUtils.toColor(
                                    StringUtils.formatString(messages.ipProxyError() + "\n\n{0}",
                                            ipEvent.getResult().getReason())));
                    break;
            }
        } else {
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, StringUtils.toColor(ipEvent.getHandleReason()));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(PlayerLoginEvent e) {
        if (e.getResult().equals(PlayerLoginEvent.Result.ALLOWED)) {
            Player player = e.getPlayer();

            LateScheduler<Event> result = new AsyncLateScheduler<>();
            tryAsync(() -> {
                User user = new User(player);
                if (!user.isLockLoginUser())
                    user.applyLockLoginUser();

                if (!config.isBungeeCord()) {
                    ClientSession session = user.getSession();
                    session.validate();
                    session.setPinLogged(false);
                    session.set2FALogged(false);
                    session.setLogged(false);

                    //Automatically mark players as captcha verified if captcha is disabled
                    if (!config.captchaOptions().isEnabled())
                        session.setCaptchaLogged(true);

                    //Check if the player has a session keeper active, if yes, restore his
                    //login status
                    forceSessionLogin(player);

                    OfflinePlayer offline = plugin.getServer().getOfflinePlayer(player.getUniqueId());

                    Event event = new UserJoinEvent(e.getAddress(), player.getUniqueId(), offline.getName(), e);
                    result.complete(event);
                }
            });
            result.whenComplete((event) -> {
                if (!config.isBungeeCord()) {
                    ModulePlugin.callEvent(event);

                    if (event.isHandled()) {
                        e.disallow(PlayerLoginEvent.Result.KICK_OTHER, StringUtils.toColor(event.getHandleReason()));
                    }
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPostLogin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        InetSocketAddress ip = player.getAddress();
        User user = new User(player);

        PluginIpValidationEvent.ValidationResult validationResult = PluginIpValidationEvent.ValidationResult.SUCCESS.withReason("Plugin configuration tells to ignore proxy IPs");
        if (!config.isBungeeCord()) {
            ProxyCheck proxy = new ProxyCheck(ip);
            if (proxy.isProxy()) {
                validationResult = PluginIpValidationEvent.ValidationResult.INVALID.withReason("IP has been detected as proxy");
            }
        }

        PluginIpValidationEvent ipEvent = new PluginIpValidationEvent(ip.getAddress(), PluginIpValidationEvent.ValidationProcess.PROXY_IP,
                validationResult,
                validationResult.getReason(), e);
        ModulePlugin.callEvent(ipEvent);

        if (!ipEvent.isHandled()) {
            switch (ipEvent.getResult()) {
                case SUCCESS:
                    LateScheduler<Event> result = new AsyncLateScheduler<>();

                    tryAsync(() -> {
                        ClientSession session = user.getSession();

                        if (!config.isBungeeCord()) {
                            user.savePotionEffects();
                            user.applySessionEffects();

                            if (config.clearChat()) {
                                for (int i = 0; i < 150; i++)
                                    player.sendMessage("");
                            }

                            String barMessage = messages.captcha(session.getCaptcha());
                            try {
                                if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null)
                                    barMessage = PlaceholderAPI.setPlaceholders(player, barMessage);
                            } catch (Throwable ignored) {
                            }

                            BarMessage bar = new BarMessage(player, barMessage);
                            if (!session.isCaptchaLogged())
                                bar.send(true);

                            SessionCheck<Player> check = user.getChecker().whenComplete(() -> {
                                user.restorePotionEffects();

                                bar.setMessage("");
                                bar.stop();
                            });

                            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, check);

                            if (player.getLocation().getBlock().getType().name().contains("PORTAL"))
                                user.setTempSpectator(true);

                            if (config.hideNonLogged()) {
                                ClientVisor visor = new ClientVisor(player);
                                if (!session.isLogged()) {
                                    visor.hide();
                                }
                            }

                            if (config.enableSpawn()) {
                                trySync(() -> player.teleport(player.getWorld().getSpawnLocation()));

                                Spawn spawn = new Spawn(player.getWorld());
                                spawn.load().whenComplete(() -> spawn.teleport(player));
                            }
                        }

                        if (session.isCaptchaLogged() && session.isLogged() && session.isTempLogged()) {
                            if (config.takeBack()) {
                                LastLocation location = new LastLocation(player);
                                location.teleport();
                            }
                        }

                        Event event = new UserPostJoinEvent(user.getModule(), e);
                        result.complete(event);
                    });
                    result.whenComplete((event) -> {
                        if (!config.isBungeeCord()) {
                            ModulePlugin.callEvent(event);

                            if (event.isHandled()) {
                                user.kick(event.getHandleReason());
                            }
                        }
                    });

                    if (!ipEvent.getResult().equals(validationResult)) {
                        logger.scheduleLog(Level.WARNING, "Module {0} changed the plugin IP validation result from {1} to {2} with reason {3}",
                                ipEvent.getHandleOwner().name(), validationResult.name(), ipEvent.getResult().name(), ipEvent.getResult().getReason());
                    }
                    break;
                case ERROR:
                case INVALID:
                default:
                    if (!ipEvent.getResult().equals(validationResult)) {
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

    /**
     * Get if the player has a session and
     * validate it
     *
     * @param player the player
     */
    private void forceSessionLogin(final Player player) {
        User user = new User(player);

        tryAsync(() -> {
            SessionKeeper keeper = new SessionKeeper(user.getModule());
            if (keeper.hasSession()) {
                ClientSession session = user.getSession();

                session.setCaptchaLogged(true);
                session.setLogged(true);
                session.setPinLogged(true);
                session.set2FALogged(true);

                keeper.destroy();

                if (config.hideNonLogged()) {
                    ClientVisor visor = new ClientVisor(player);
                    visor.show();
                }
            }
        });
    }
}
