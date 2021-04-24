package ml.karmaconfigs.locklogin.plugin.bukkit.util.player;

import ml.karmaconfigs.api.bukkit.reflections.TitleMessage;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.LockLoginListener;
import ml.karmaconfigs.locklogin.api.account.AccountID;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.event.user.SessionInitializationEvent;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.configuration.Config;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.configuration.options.LoginConfig;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.configuration.options.RegisterConfig;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.common.security.GoogleAuthFactory;
import ml.karmaconfigs.locklogin.plugin.common.utils.platform.CurrentPlatform;
import net.md_5.bungee.api.chat.TextComponent;
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

    private final AccountManager manager;

    private final Player player;

    private final static Map<UUID, ClientSession> sessions = new HashMap<>();
    private final static Map<UUID, Collection<PotionEffect>> effects = new HashMap<>();

    /**
     * Initialize the user
     *
     * @param _player the player
     * @throws IllegalStateException as part of session and account managers setup, if some of
     * they are null or can't be initialized, this exception will be thrown and the plugin will be
     * instantly disabled
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

                    SessionInitializationEvent event = new SessionInitializationEvent(player, session, null);
                    LockLoginListener.callEvent(event);

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
     * Apply the user the "LockLoginUser" metadata
     * value
     */
    public final void applyLockLoginUser() {
        player.setMetadata("LockLoginUser", new FixedMetadataValue(plugin, player.getUniqueId().toString()));
    }

    /**
     * Save the current player potion effects
     */
    public final void savePotionEffects() {
        if (!effects.containsKey(player.getUniqueId()))
            effects.put(player.getUniqueId(), player.getActivePotionEffects());
    }

    /**
     * Apply the session potion effect
     * types
     */
    public final void applySessionEffects() {
        Config config = new Config();
        List<PotionEffect> apply = new ArrayList<>();
        if (isRegistered()) {
            RegisterConfig register = config.registerOptions();

            if (register.blindEffect()) {
                PotionEffect blind = new PotionEffect(PotionEffectType.BLINDNESS, 20 * register.timeOut(), 5, false, false);
                if (register.nauseaEffect()) {
                    PotionEffect nausea = new PotionEffect(PotionEffectType.CONFUSION, 20 * register.timeOut(), 5, false, false);
                    apply.add(nausea);
                }
                apply.add(blind);
            }
        } else {
            LoginConfig login = config.loginOptions();

            if (login.blindEffect()) {
                PotionEffect blind = new PotionEffect(PotionEffectType.BLINDNESS, 20 * login.timeOut(), 5, false, false);
                if (login.nauseaEffect()) {
                    PotionEffect nausea = new PotionEffect(PotionEffectType.CONFUSION, 20 * login.timeOut(), 5, false, false);
                    apply.add(nausea);
                }
                apply.add(blind);
            }
        }

        if (!apply.isEmpty())
            player.addPotionEffects(apply);
    }

    /**
     * Apply the session potion effect
     * types
     *
     * @param applyNausea apply nausea effect
     */
    public final void applySessionEffects(final boolean applyNausea) {
        Config config = new Config();
        List<PotionEffect> apply = new ArrayList<>();
        if (config.isBungeeCord()) {
            PotionEffect blind = new PotionEffect(PotionEffectType.BLINDNESS, 20 * 60, 5, false, false);
            if (applyNausea) {
                PotionEffect nausea = new PotionEffect(PotionEffectType.CONFUSION, 20 * 60, 5, false, false);
                apply.add(nausea);
            }
            apply.add(blind);
        }

        if (!apply.isEmpty())
            player.addPotionEffects(apply);
    }

    /**
     * Restore the player potion effects
     */
    public final void restorePotionEffects() {
        player.getActivePotionEffects().forEach(effect -> {
            if (effect != null && player.hasPotionEffect(effect.getType()))
                player.removePotionEffect(effect.getType());
        });

        if (effects.containsKey(player.getUniqueId()) && !effects.getOrDefault(player.getUniqueId(), Collections.emptyList()).isEmpty())
            player.addPotionEffects(effects.remove(player.getUniqueId()));
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
     * @param title the title to send
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
    public final void kick(final String reason) {
        String[] parsed = parseMessage(reason);

        if (parsed.length > 1) {
            StringBuilder kickBuilder = new StringBuilder();
            for (String string : parsed)
                kickBuilder.append(string).append("\n");

            player.kickPlayer(StringUtils.toColor(StringUtils.replaceLast(kickBuilder.toString(), "\n", "")));
        } else {
            player.kickPlayer(StringUtils.toColor(parsed[0]));
        }
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
        AccountManager manager = getManager();
        String password = manager.getPassword();

        return !password.replaceAll("\\s", "").isEmpty();
    }

    /**
     * Parse the message, replacing "{newline}"
     * with "\n"
     *
     * @return the parsed message
     */
    private String[] parseMessage(final String official) {
        Config config = new Config();

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

                messages[i] = message;
            }

            return messages;
        } else {
            ClientSession session = getSession();

            return new String[]{official
                    .replace("{captcha}", (session.isCaptchaLogged() ? "" : "<captcha>"))
                    .replace("<captcha>", (session.isCaptchaLogged() ? "" : "<captcha>"))
                    .replace("{player}", StringUtils.stripColor(player.getDisplayName()))
                    .replace("{ServerName}", config.serverName())};
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
}
