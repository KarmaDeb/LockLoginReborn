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
import eu.locklogin.api.common.security.client.Name;
import eu.locklogin.api.common.security.client.ProxyCheck;
import eu.locklogin.api.common.session.SessionCheck;
import eu.locklogin.api.common.utils.InstantParser;
import eu.locklogin.api.common.utils.other.LockedAccount;
import eu.locklogin.api.common.utils.plugin.FloodGateUtil;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.module.plugin.api.event.plugin.PluginIpValidationEvent;
import eu.locklogin.api.module.plugin.api.event.user.UserJoinEvent;
import eu.locklogin.api.module.plugin.api.event.user.UserPostJoinEvent;
import eu.locklogin.api.module.plugin.api.event.user.UserPreJoinEvent;
import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.client.permission.plugin.PluginPermissions;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.TaskTarget;
import eu.locklogin.plugin.bukkit.plugin.Manager;
import eu.locklogin.plugin.bukkit.util.files.client.OfflineClient;
import eu.locklogin.plugin.bukkit.util.files.data.LastLocation;
import eu.locklogin.plugin.bukkit.util.files.data.Spawn;
import eu.locklogin.plugin.bukkit.util.player.ClientVisor;
import eu.locklogin.plugin.bukkit.util.player.User;
import me.clip.placeholderapi.PlaceholderAPI;
import ml.karmaconfigs.api.bukkit.reflection.BarMessage;
import ml.karmaconfigs.api.common.timer.SchedulerUnit;
import ml.karmaconfigs.api.common.timer.SourceScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
import ml.karmaconfigs.api.common.utils.uuid.UUIDType;
import ml.karmaconfigs.api.common.utils.uuid.UUIDUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.server.ServerListPingEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static eu.locklogin.plugin.bukkit.LockLogin.*;

public final class JoinListener implements Listener {

    private final static PluginConfiguration config = CurrentPlatform.getConfiguration();
    private final static PluginMessages messages = CurrentPlatform.getMessages();

    static Map<AccountID, UUID> uuid_storage = new ConcurrentHashMap<>();

    private static final String IPV4_REGEX =
            "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
    private static final Pattern IPv4_PATTERN = Pattern.compile(IPV4_REGEX);

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerPing(ServerListPingEvent e) {
        if (!config.isBungeeCord()) {
            Event ipEvent = new PluginIpValidationEvent(e.getAddress(), PluginIpValidationEvent.ValidationProcess.SERVER_PING,
                    PluginIpValidationEvent.ValidationResult.SUCCESS,
                    "Plugin added the IP to the IP validation queue", e);

            ModulePlugin.callEvent(ipEvent);
        }

        if (!Manager.isInitialized()) {
            if (config.showMOTD()) {
                e.setMotd(StringUtils.toColor("&dLockLogin &8&l| &aStarting server"));
            }
        } else {
            if (config.showMOTD() && config.isBungeeCord()) {
                e.setMotd(StringUtils.toColor("&aThis server is being protected by &dLockLogin"));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent e) {
        if (Manager.isInitialized()) {
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

            if (ipEvent.getResult() != validationResult && ipEvent.getHandleOwner() == null) {
                try {
                    Field f = ipEvent.getClass().getDeclaredField("validationResult");
                    f.setAccessible(true);

                    f.set(ipEvent, validationResult); //Deny changes from unknown sources
                } catch (Throwable ignored) {}
            }

            String conn_name = e.getName();
            UUID gen_uuid = UUIDUtil.fetch(conn_name, UUIDType.OFFLINE);
            if (CurrentPlatform.isOnline()) {
                gen_uuid = UUIDUtil.fetch(conn_name, UUIDType.ONLINE);
            }
            UUID tar_uuid = e.getUniqueId();

            if (!ipEvent.isHandled()) {
                switch (ipEvent.getResult()) {
                    case SUCCESS:
                        if (e.getLoginResult().equals(AsyncPlayerPreLoginEvent.Result.ALLOWED)) {
                            if (!config.isBungeeCord()) {
                                if (config.registerOptions().maxAccounts() > 0) {
                                    AccountData data = new AccountData(ip, AccountID.fromUUID(tar_uuid));

                                    if (data.allow(config.registerOptions().maxAccounts())) {
                                        data.save();

                                        int amount = data.getAlts().size();
                                        if (amount > 2) {
                                            for (Player online : plugin.getServer().getOnlinePlayers()) {
                                                User user = new User(online);
                                                if (user.hasPermission(PluginPermissions.info_alt_alert())) {
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

                                if (config.uuidValidator()) {
                                    if (!gen_uuid.equals(tar_uuid)) {
                                        FloodGateUtil util = new FloodGateUtil(tar_uuid);
                                        if (!util.isFloodClient()) {
                                            logger.scheduleLog(Level.GRAVE, "Denied connection from {0} because its UUID ( {1} ) doesn't match with generated one ( {2} )", conn_name, tar_uuid, gen_uuid);

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
                                    if (account.isLocked()) {
                                        String administrator = account.getIssuer();
                                        Instant date = account.getLockDate();
                                        InstantParser parser = new InstantParser(date);
                                        String dateString = parser.getDay() + " " + parser.getMonth() + " " + parser.getYear();

                                        e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, StringUtils.toColor(messages.forcedAccountRemoval(administrator + " [ " + dateString + " ]")));
                                        logger.scheduleLog(Level.WARNING, "Client {0} tried to join, but his account was blocked by {1} on {2}", conn_name, administrator, dateString);
                                        return;
                                    }
                                }

                                if (!ipEvent.getResult().equals(validationResult) && ipEvent.getHandleOwner() != null) {
                                    logger.scheduleLog(Level.WARNING, "Module {0} changed the plugin IP validation result from {1} to {2} with reason {3}",
                                            ipEvent.getHandleOwner().name(), validationResult.name(), ipEvent.getResult().name(), ipEvent.getResult().getReason());
                                }

                                e.allow();
                            } else {
                                SimpleScheduler timer = new SourceScheduler(plugin, 1, SchedulerUnit.SECOND, true);
                                timer.endAction(() -> {
                                    Player online = plugin.getServer().getPlayer(tar_uuid);
                                    if (online != null && online.isOnline()) {
                                        User user = new User(online);
                                        if (!user.getSession().isValid()) {
                                            user.kick(messages.bungeeProxy());
                                        }

                                        timer.cancel();
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
                        if (!ipEvent.getResult().equals(validationResult) && ipEvent.getHandleOwner() != null) {
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
        } else {
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, StringUtils.toColor("&cThe server is starting up!"));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(PlayerLoginEvent e) {
        if (e.getResult().equals(PlayerLoginEvent.Result.ALLOWED)) {
            Player player = e.getPlayer();

            tryAsync(TaskTarget.EVENT, () -> {
                User user = new User(player);
                ModulePlayer sender = new ModulePlayer(
                        player.getName(),
                        player.getUniqueId(),
                        user.getSession(),
                        user.getManager(),
                        (player.getAddress() == null ? null : player.getAddress().getAddress())
                );
                //If bukkit player objects changes module player also changes
                CurrentPlatform.connectPlayer(sender, player);

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

                    OfflinePlayer offline = plugin.getServer().getOfflinePlayer(player.getUniqueId());

                    Event event = new UserJoinEvent(e.getAddress(), player.getUniqueId(), offline.getName(), e);
                    if (!config.isBungeeCord()) {
                        ModulePlugin.callEvent(event);

                        if (event.isHandled()) {
                            sender.requestKick(event.getHandleReason());
                        }
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

        if (ip != null) {
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

            if (ipEvent.getResult() != validationResult && ipEvent.getHandleOwner() == null) {
                try {
                    Field f = ipEvent.getClass().getDeclaredField("validationResult");
                    f.setAccessible(true);

                    f.set(ipEvent, validationResult); //Deny changes from unknown sources
                } catch (Throwable ignored) {}
            }

            if (!ipEvent.isHandled()) {
                switch (ipEvent.getResult()) {
                    case SUCCESS:
                        tryAsync(TaskTarget.EVENT, () -> {
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
                                        visor.toggleView();
                                    }
                                }

                                if (config.enableSpawn()) {
                                    trySync(TaskTarget.TELEPORT, () -> player.teleport(player.getWorld().getSpawnLocation()));

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

                            if (!config.isBungeeCord()) {
                                Event event = new UserPostJoinEvent(user.getModule(), e);
                                ModulePlugin.callEvent(event);

                                if (event.isHandled()) {
                                    user.kick(event.getHandleReason());
                                }
                            }
                        });

                        if (!ipEvent.getResult().equals(validationResult) && ipEvent.getHandleOwner() != null) {
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
        } else {
            user.kick(StringUtils.toColor(messages.ipProxyError()));
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
