package ml.karmaconfigs.locklogin.plugin.bukkit.util.player;

import me.clip.placeholderapi.PlaceholderAPI;
import ml.karmaconfigs.api.bukkit.reflections.TitleMessage;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.account.AccountID;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.files.PluginConfiguration;
import ml.karmaconfigs.locklogin.api.files.options.LoginConfig;
import ml.karmaconfigs.locklogin.api.files.options.RegisterConfig;
import ml.karmaconfigs.locklogin.api.modules.javamodule.JavaModuleManager;
import ml.karmaconfigs.locklogin.api.modules.event.user.SessionInitializationEvent;
import ml.karmaconfigs.locklogin.api.utils.platform.CurrentPlatform;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.common.security.GoogleAuthFactory;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.*;

/**
 * Initialize the user
 */
public final class User {

    private final static Set<UUID> registered = new HashSet<>();
    private final static Map<UUID, Collection<PotionEffect>> effects = new HashMap<>();
    @SuppressWarnings("FieldMayBeFinal") //This is modified by cache loader
    private static Map<UUID, ClientSession> sessions = new HashMap<>();
    @SuppressWarnings("FieldMayBeFinal") //This is modified by cache loader
    private static Map<UUID, GameMode> temp_spectator = new HashMap<>();
    private final AccountManager manager;
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

        if (!sessions.containsKey(player.getUniqueId())) {
            if (CurrentPlatform.isValidSessionManager()) {
                ClientSession session = CurrentPlatform.getSessionManager(null);

                if (session == null) {
                    plugin.getPluginLoader().disablePlugin(plugin);
                    throw new IllegalStateException("Cannot initialize user with a null session manager");
                } else {
                    session.initialize();

                    SessionInitializationEvent event = new SessionInitializationEvent(fromPlayer(player), session, null);
                    JavaModuleManager.callEvent(event);

                    sessions.put(player.getUniqueId(), session);
                }
            } else {
                plugin.getPluginLoader().disablePlugin(plugin);
                throw new IllegalStateException("Cannot initialize user with a null session manager");
            }
        }

        if (CurrentPlatform.isValidAccountManager()) {
            manager = CurrentPlatform.getAccountManager(new Class[]{OfflinePlayer.class}, plugin.getServer().getOfflinePlayer(player.getUniqueId()));

            if (manager == null) {
                plugin.getPluginLoader().disablePlugin(plugin);
                throw new IllegalStateException("Cannot initialize user with a null player account manager");
            } else {
                //Try to fix the empty manager values that are
                //required
                if (manager.exists()) {
                    String name = manager.getName();
                    AccountID id = manager.getUUID();

                    if (name.replaceAll("\\s", "").isEmpty())
                        manager.setName(StringUtils.stripColor(player.getDisplayName()));

                    if (id.getId().replaceAll("\\s", "").isEmpty())
                        manager.saveUUID(AccountID.fromUUID(player.getUniqueId()));
                }
            }
        } else {
            plugin.getPluginLoader().disablePlugin(plugin);
            throw new IllegalStateException("Cannot initialize user with an invalid player account manager");
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
    public synchronized final void applyLockLoginUser() {
        plugin.getServer().getScheduler().runTask(plugin, () -> player.setMetadata("LockLoginUser", new FixedMetadataValue(plugin, player.getUniqueId().toString())));
    }

    /**
     * Set the player in temp spectator status
     *
     * @param status the temp spectator status
     */
    public synchronized final void setTempSpectator(final boolean status) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
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
    public synchronized final void savePotionEffects() {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!effects.containsKey(player.getUniqueId()))
                effects.put(player.getUniqueId(), player.getActivePotionEffects());
        });
    }

    /**
     * Apply the session potion effect
     * types
     */
    public synchronized final void applySessionEffects() {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            PluginConfiguration config = CurrentPlatform.getConfiguration();
            List<PotionEffect> apply = new ArrayList<>();
            if (isRegistered()) {
                LoginConfig login = config.loginOptions();

                if (login.blindEffect()) {
                    PotionEffect blind = new PotionEffect(PotionEffectType.BLINDNESS, 20 * login.timeOut(), 5, false, false);
                    if (login.nauseaEffect()) {
                        PotionEffect nausea = new PotionEffect(PotionEffectType.CONFUSION, 20 * login.timeOut(), 5, false, false);
                        apply.add(nausea);
                    }
                    apply.add(blind);
                }
            } else {
                RegisterConfig register = config.registerOptions();

                if (register.blindEffect()) {
                    PotionEffect blind = new PotionEffect(PotionEffectType.BLINDNESS, 20 * register.timeOut(), 5, false, false);
                    if (register.nauseaEffect()) {
                        PotionEffect nausea = new PotionEffect(PotionEffectType.CONFUSION, 20 * register.timeOut(), 5, false, false);
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
    public synchronized final void restorePotionEffects() {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
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
    public final void removeLockLoginUser() {
        player.removeMetadata("LockLoginUser", plugin);
    }

    /**
     * Send a message to the player
     *
     * @param message the message to send
     */
    public final void send(final String message) {
        String[] parsed = parseMessage(message);

        Message messages = new Message();

        if (parsed.length > 1) {
            for (String str : parsed)
                player.sendMessage(StringUtils.toColor(str));
        } else {
            //Make sure to avoid null messages
            if (parsed.length == 1) {
                if (!parsed[0].replace(messages.prefix(), "").replaceAll("\\s", "").isEmpty())
                    player.sendMessage(StringUtils.toColor(parsed[0]));
            }
        }
    }

    /**
     * Send a message to the player
     *
     * @param message the message to send
     */
    public final void send(final TextComponent message) {
        player.spigot().sendMessage(message);
    }

    /**
     * Send a title and subtitle to the player
     *
     * @param title    the title to send
     * @param subtitle the subtitle to send
     */
    public final void send(final String title, final String subtitle) {
        TitleMessage titleMessage = new TitleMessage(player, title, subtitle);
        titleMessage.send(0, 5, 0);
    }

    /**
     * Kick the player with the specified reason
     *
     * @param reason the reason of the kick
     */
    public synchronized final void kick(final String reason) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
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
     * Get the current player account manager
     *
     * @return the player account manager
     */
    @NotNull
    public final AccountManager getManager() {
        return manager;
    }

    /**
     * Get the current player session
     *
     * @return the player session
     */
    @NotNull
    public final ClientSession getSession() {
        return sessions.get(player.getUniqueId());
    }

    /**
     * Get the user google auth token factory
     *
     * @return the user google auth token
     * factory
     */
    public final GoogleAuthFactory getTokenFactory() {
        return new GoogleAuthFactory(player.getUniqueId(), StringUtils.toColor(player.getDisplayName()));
    }

    /**
     * Get if the current player is a
     * LockLogin user, this is used to avoid
     * NPC problems
     *
     * @return if the current player is LockLogin user
     */
    public final boolean isLockLoginUser() {
        return player.hasMetadata("LockLoginUser");
    }

    /**
     * Check if the user is registered or not
     *
     * @return if the user is registered
     */
    public final boolean isRegistered() {
        PluginConfiguration config = CurrentPlatform.getConfiguration();
        if (config.isBungeeCord()) {
            return registered.contains(player.getUniqueId());
        } else {
            AccountManager manager = getManager();
            String password = manager.getPassword();

            return !password.replaceAll("\\s", "").isEmpty();
        }
    }

    public final void setRegistered(final boolean status) {
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
                    message = message.replace("{captcha}", "")
                            .replace("<captcha>", "");
                } else {
                    message = message.replace("{captcha}", "<captcha>");
                }
                message = message
                        .replace("{player}", StringUtils.stripColor(player.getDisplayName()))
                        .replace("{ServerName}", config.serverName());

                try {
                    if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI"))
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
                if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI"))
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
