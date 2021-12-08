package eu.locklogin.plugin.bukkit.util.player;

/*
 * Private GSA code
 *
 * The use of this code
 * without GSA team authorization
 * will be a violation of
 * terms of use determined
 * in <a href="http://karmaconfigs.cf/license/"> here </a>
 * or (fallback domain) <a href="https://karmaconfigs.github.io/page/license"> here </a>
 */

import eu.locklogin.api.account.AccountID;
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.common.security.GoogleAuthFactory;
import eu.locklogin.api.common.session.SessionCheck;
import eu.locklogin.api.common.utils.other.name.AccountNameDatabase;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.file.options.LoginConfig;
import eu.locklogin.api.file.options.RegisterConfig;
import eu.locklogin.api.module.plugin.api.event.user.SessionInitializationEvent;
import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import eu.locklogin.api.util.platform.CurrentPlatform;
import me.clip.placeholderapi.PlaceholderAPI;
import ml.karmaconfigs.api.bukkit.reflection.BossMessage;
import ml.karmaconfigs.api.bukkit.reflection.TitleMessage;
import ml.karmaconfigs.api.common.boss.BossColor;
import ml.karmaconfigs.api.common.boss.BossProvider;
import ml.karmaconfigs.api.common.boss.ProgressiveBar;
import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.karma.KarmaSource;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static eu.locklogin.plugin.bukkit.LockLogin.*;

/**
 * Initialize the user
 */
public final class User {

    private final static KarmaSource lockLogin = APISource.loadProvider("LockLogin");
    private final static Set<UUID> registered = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final static Map<UUID, Collection<PotionEffect>> effects = new ConcurrentHashMap<>();
    private final static Map<UUID, AccountManager> managers = new ConcurrentHashMap<>();
    private final static Map<UUID, SessionCheck<Player>> sessionChecks = new ConcurrentHashMap<>();
    @SuppressWarnings("FieldMayBeFinal") //This is modified by cache loader
    private static Map<UUID, ClientSession> sessions = new ConcurrentHashMap<>();
    @SuppressWarnings("FieldMayBeFinal") //This is modified by cache loader
    private static Map<UUID, GameMode> temp_spectator = new ConcurrentHashMap<>();
    private final Player player;

    /**
     * Initialize the user
     *
     * @param _player the player
     * @throws IllegalStateException as part of session and account managers setup, if some of
     *                               they are null or can't be initialized, this exception will be thrown and the plugin will be
     *                               instantly disabled
     */
    public User(final Player _player) throws IllegalStateException {
        player = _player;

        User loaded = UserDatabase.loadUser(player);
        if (loaded == null) {
            if (CurrentPlatform.isValidAccountManager()) {
                AccountManager manager = CurrentPlatform.getAccountManager(new Class[]{OfflinePlayer.class}, plugin.getServer().getOfflinePlayer(player.getUniqueId()));

                if (manager == null) {
                    plugin.getPluginLoader().disablePlugin(plugin);
                    throw new IllegalStateException("Cannot initialize user with a null player account manager");
                } else {
                    AccountNameDatabase database = new AccountNameDatabase(player.getUniqueId());
                    lockLogin.async().queue(() -> {
                        database.assign(StringUtils.stripColor(player.getName()));
                        database.assign(StringUtils.stripColor(player.getDisplayName()));
                        database.assign(StringUtils.stripColor(player.getPlayerListName()));
                    });

                    //Try to fix the empty manager values that are
                    //required
                    if (manager.exists()) {
                        String name = manager.getName();
                        AccountID id = manager.getUUID();

                        if (StringUtils.isNullOrEmpty(name))
                            manager.setName(StringUtils.stripColor(player.getDisplayName()));

                        if (StringUtils.isNullOrEmpty(id.getId()))
                            manager.saveUUID(AccountID.fromUUID(player.getUniqueId()));
                    }

                    managers.put(player.getUniqueId(), manager);
                }
            } else {
                plugin.getPluginLoader().disablePlugin(plugin);
                throw new IllegalStateException("Cannot initialize user with an invalid player account manager");
            }

            if (!sessions.containsKey(player.getUniqueId())) {
                if (CurrentPlatform.isValidSessionManager()) {
                    ClientSession session = CurrentPlatform.getSessionManager(null);

                    if (session == null) {
                        plugin.getPluginLoader().disablePlugin(plugin);
                        throw new IllegalStateException("Cannot initialize user with a null session manager");
                    } else {
                        session.initialize();

                        InetSocketAddress ip = player.getAddress();

                        ModulePlayer modulePlayer = new ModulePlayer(
                                player.getName(),
                                player.getUniqueId(),
                                session,
                                managers.get(player.getUniqueId()),
                                (ip == null ? null : ip.getAddress()));
                        CurrentPlatform.connectPlayer(modulePlayer, player);

                        Event event = new SessionInitializationEvent(modulePlayer, session, null);
                        ModulePlugin.callEvent(event);

                        sessions.put(player.getUniqueId(), session);
                    }
                } else {
                    plugin.getPluginLoader().disablePlugin(plugin);
                    throw new IllegalStateException("Cannot initialize user with a null session manager");
                }
            }

            UserDatabase.insert(player, this);
        } else {
            ModulePlayer modulePlayer = CurrentPlatform.getServer().getPlayer(player.getUniqueId());
            if (modulePlayer == null || modulePlayer.getAddress() == null) {
                InetSocketAddress ip = player.getAddress();

                modulePlayer = new ModulePlayer(
                        player.getName(),
                        player.getUniqueId(),
                        sessions.get(player.getUniqueId()),
                        managers.get(player.getUniqueId()),
                        (ip == null ? null : ip.getAddress()));
                CurrentPlatform.connectPlayer(modulePlayer, player);
            }
        }
    }

    /**
     * Get the map of sessions
     *
     * @return the map of sessions
     */
    public static Map<UUID, ClientSession> getSessionMap() {
        return new HashMap<>(sessions);
    }

    /**
     * Get the map of sessions
     *
     * @return the map of sessions
     */
    public static Map<UUID, GameMode> getSpectatorMap() {
        return new HashMap<>(temp_spectator);
    }

    /**
     * Apply the user the "LockLoginUser" metadata
     * value
     */
    public synchronized void applyLockLoginUser() {
        trySync(() -> player.setMetadata("LockLoginUser", new FixedMetadataValue(plugin, player.getUniqueId().toString())));
    }

    /**
     * Set the player in temp spectator status
     *
     * @param status the temp spectator status
     */
    public synchronized void setTempSpectator(final boolean status) {
        trySync(() -> {
            if (status) {
                temp_spectator.put(player.getUniqueId(), player.getGameMode());
                player.setGameMode(GameMode.SPECTATOR);
            } else {
                if (temp_spectator.containsKey(player.getUniqueId()))
                    player.setGameMode(temp_spectator.getOrDefault(player.getUniqueId(), GameMode.SURVIVAL));
            }
        });
    }

    /**
     * Save the current player potion effects
     */
    public synchronized void savePotionEffects() {
        trySync(() -> {
            if (!effects.containsKey(player.getUniqueId()))
                effects.put(player.getUniqueId(), player.getActivePotionEffects());
        });
    }

    /**
     * Apply the session potion effect
     * types
     */
    public synchronized void applySessionEffects() {
        trySync(() -> {
            PluginConfiguration config = CurrentPlatform.getConfiguration();
            List<PotionEffect> apply = new ArrayList<>();
            if (getManager().isRegistered()) {
                LoginConfig login = config.loginOptions();

                if (login.blindEffect()) {
                    PotionEffect blind = new PotionEffect(PotionEffectType.BLINDNESS, 20 * (login.timeOut() + 4), 5, false, false);
                    if (login.nauseaEffect()) {
                        PotionEffect nausea = new PotionEffect(PotionEffectType.CONFUSION, 20 * (login.timeOut() + 4), 5, false, false);
                        apply.add(nausea);
                    }
                    apply.add(blind);
                }
            } else {
                RegisterConfig register = config.registerOptions();

                if (register.blindEffect()) {
                    PotionEffect blind = new PotionEffect(PotionEffectType.BLINDNESS, 20 * (register.timeOut() + 4), 5, false, false);
                    if (register.nauseaEffect()) {
                        PotionEffect nausea = new PotionEffect(PotionEffectType.CONFUSION, 20 * (register.timeOut() + 4), 5, false, false);
                        apply.add(nausea);
                    }
                    apply.add(blind);
                }
            }

            if (!apply.isEmpty())
                player.addPotionEffects(apply);
        });
    }

    /**
     * Restore the player potion effects
     */
    public synchronized void restorePotionEffects() {
        trySync(() -> {
            player.getActivePotionEffects().forEach(effect -> {
                try {
                    player.removePotionEffect(effect.getType());
                } catch (Throwable ex) {
                    logger.scheduleLog(Level.GRAVE, ex);
                    String name = "null";
                    if (effect != null) {
                        name = effect.getType().getName();
                    }

                    logger.scheduleLog(Level.INFO, "Failed to remove potion effect {0} from {1}", name, player.getUniqueId().toString());
                }
            });

            if (effects.containsKey(player.getUniqueId()) && !effects.getOrDefault(player.getUniqueId(), Collections.emptySet()).isEmpty())
                player.addPotionEffects(effects.remove(player.getUniqueId()));
        });
    }

    /**
     * Remove the user the "LockLoginUser" metadata
     * value
     */
    public void removeLockLoginUser() {
        trySync(() -> player.removeMetadata("LockLoginUser", plugin));
    }

    /**
     * Send a message to the player
     *
     * @param message the message to send
     */
    public void send(final String message) {
        String[] parsed = parseMessage(message);

        PluginMessages messages = CurrentPlatform.getMessages();

        if (parsed.length > 1) {
            for (String str : parsed)
                player.sendMessage(StringUtils.toColor(str));
        } else {
            //Make sure to avoid null messages
            if (parsed.length == 1) {
                if (!StringUtils.isNullOrEmpty(parsed[0].replace(messages.prefix(), ""))) {
                    player.sendMessage(StringUtils.toColor(parsed[0]));
                }
            }
        }
    }

    /**
     * Send a message to the player
     *
     * @param message the message to send
     */
    public void send(final TextComponent message) {
        player.spigot().sendMessage(message);
    }

    /**
     * Send a title and subtitle to the player
     *
     * @param title    the title to send
     * @param subtitle the subtitle to send
     */
    public void send(final String title, final String subtitle) {
        TitleMessage titleMessage = new TitleMessage(player, title, subtitle);
        titleMessage.send(0, 5, 0);
    }

    /**
     * Kick the player with the specified reason
     *
     * @param reason the reason of the kick
     */
    public synchronized void kick(final String reason) {
        trySync(() -> {
            String[] parsed = parseMessage(reason);

            if (parsed.length > 1) {
                StringBuilder kickBuilder = new StringBuilder();
                for (String string : parsed)
                    kickBuilder.append(string).append("\n");

                player.kickPlayer(StringUtils.toColor(StringUtils.replaceLast(kickBuilder.toString(), "\n", "")));
            } else {
                player.kickPlayer(StringUtils.toColor(parsed[0]));
            }
        });
    }

    /**
     * Remove the user session check
     */
    public void removeSessionCheck() {
        sessionChecks.remove(player.getUniqueId());
    }

    /**
     * Get the client session checker
     *
     * @return the client session checker
     */
    public SessionCheck<Player> getChecker() {
        SessionCheck<Player> checker = sessionChecks.getOrDefault(player.getUniqueId(), null);
        if (checker == null) {
            ModulePlayer sender = getModule();

            if (sender == null) {
                InetSocketAddress ip = player.getAddress();

                sender = new ModulePlayer(
                        player.getName(),
                        player.getUniqueId(),
                        getSession(),
                        managers.get(player.getUniqueId()),
                        (ip != null ? ip.getAddress() : null));

                CurrentPlatform.connectPlayer(sender, player);
            }

            /*
            So... there was a bug in where sometimes the boss bar would be visible
            even while disabled, that's because I was literally creating the boss
            bar ignoring that option, and then hiding it if disabled.

            The best solution is to just not create it if specified
             */
            BossProvider<Player> message = null;
            int time = CurrentPlatform.getConfiguration().loginOptions().timeOut();
            if (getManager().isRegistered()) {
                time = CurrentPlatform.getConfiguration().registerOptions().timeOut();

                if (CurrentPlatform.getConfiguration().registerOptions().hasBossBar()) {
                    message = new BossMessage(plugin, CurrentPlatform.getMessages().registerBar("&a", time), time).color(BossColor.GREEN).progress(ProgressiveBar.DOWN);
                }
            } else {
                if (CurrentPlatform.getConfiguration().loginOptions().hasBossBar()) {
                    message = new BossMessage(plugin, CurrentPlatform.getMessages().loginBar("&a", time), time).color(BossColor.GREEN).progress(ProgressiveBar.DOWN);
                }
            }

            checker = new SessionCheck<>(plugin, sender, message);
            sessionChecks.put(player.getUniqueId(), checker);
        }

        return checker;
    }

    /**
     * Get the module player of this player
     *
     * @return this player module player
     */
    public ModulePlayer getModule() {
        return CurrentPlatform.getServer().getPlayer(player.getUniqueId());
    }

    /**
     * Get the current player account manager
     *
     * @return the player account manager
     * @throws IllegalStateException if the current manager is null
     */
    @NotNull
    public AccountManager getManager() throws IllegalStateException {
        return managers.get(player.getUniqueId());
    }

    /**
     * Get the current player session
     *
     * @return the player session
     */
    @NotNull
    public ClientSession getSession() {
        return sessions.get(player.getUniqueId());
    }

    /**
     * Get the user google auth token factory
     *
     * @return the user google auth token
     * factory
     */
    public GoogleAuthFactory getTokenFactory() {
        return new GoogleAuthFactory(player.getUniqueId(), StringUtils.toColor(player.getDisplayName()));
    }

    /**
     * Get if the current player is a
     * LockLogin user, this is used to avoid
     * NPC problems
     *
     * @return if the current player is LockLogin user
     */
    public boolean isLockLoginUser() {
        return player.hasMetadata("LockLoginUser");
    }

    public void setRegistered(final boolean status) {
        PluginConfiguration config = CurrentPlatform.getConfiguration();
        if (config.isBungeeCord()) {
            if (status) {
                registered.add(player.getUniqueId());
            } else {
                registered.remove(player.getUniqueId());
            }
        }
    }

    /**
     * Parse the message, replacing "{newline}"
     * with "\n"
     *
     * @return the parsed message
     */
    private String[] parseMessage(final String official) {
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        if (official.contains("{newline}")) {
            String messageData = official.replace("{newline}", "\n");
            String[] messages = messageData.split("\n");

            for (int i = 0; i < messages.length; i++) {
                String message = messages[i];

                ClientSession session = getSession();
                if (session.isCaptchaLogged()) {
                    message = message.replace("{captcha}", "").replace("<captcha>", "");
                } else {
                    message = message.replace("{captcha}", "<captcha>");
                }
                message = message
                        .replace("{player}", StringUtils.stripColor(player.getDisplayName()))
                        .replace("{ServerName}", config.serverName());

                try {
                    if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null)
                        message = PlaceholderAPI.setPlaceholders(player, message);
                } catch (Throwable ignored) {
                }

                messages[i] = message;
            }

            return messages;
        } else {
            ClientSession session = getSession();

            String message = official;

            try {
                if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null)
                    message = PlaceholderAPI.setPlaceholders(player, message);
            } catch (Throwable ignored) {
            }

            return new String[]{message
                    .replace("{captcha}", (session.isCaptchaLogged() ? "" : "<captcha>"))
                    .replace("<captcha>", (session.isCaptchaLogged() ? "" : "<captcha>"))
                    .replace("{player}", StringUtils.stripColor(player.getDisplayName()))
                    .replace("{ServerName}", config.serverName())};
        }
    }
}
