package eu.locklogin.api.file;

import eu.locklogin.api.encryption.HashType;
import eu.locklogin.api.file.options.*;
import eu.locklogin.api.util.enums.Lang;

/**
 * LockLogin configuration
 */
public abstract class PluginConfiguration {

    /**
     * Check if bungee cord mode is enabled
     *
     * @return if bungee mode is enabled
     */
    public abstract boolean isBungeeCord();

    /**
     * Get the current server name
     *
     * @return the current server name
     */
    public abstract String serverName();

    /**
     * Get the registration options
     *
     * @return the registration options
     */
    public abstract RegisterConfig registerOptions();

    /**
     * Get the login options
     *
     * @return the login options
     */
    public abstract LoginConfig loginOptions();

    /**
     * Get if the plugin has sessions enabled
     *
     * @return if the plugin has sessions
     */
    public abstract boolean enableSessions();

    /**
     * Get if the plugin should check if an
     * IP is healthy for the server integrity
     *
     * @return if the plugin has IP checks enabled
     */
    public abstract boolean ipHealthCheck();

    /**
     * Get if the plugin should validate UUIDs
     *
     * @return if the plugin should validate
     * UUIDs
     */
    public abstract boolean uuidValidator();

    /**
     * Get the session life time
     *
     * @return the session life time
     */
    public abstract int sessionTime();

    /**
     * Get if the non-logged players
     * should be hidden from logged players
     * and logged players from non-logged
     *
     * @return if the non-logged players should
     * be hidden
     */
    public abstract boolean hideNonLogged();

    /**
     * Get the captcha options
     *
     * @return the captcha options
     */
    public abstract CaptchaConfig captchaOptions();

    /**
     * Get the password hash type
     *
     * @return the password hash type
     */
    public abstract HashType passwordEncryption();

    /**
     * Get the pin encryption type
     *
     * @return the pin encryption type
     */
    public abstract HashType pinEncryption();

    /**
     * Get if the plugin should encrypt the passwords
     * in Base64
     *
     * @return if the plugin should encrypt in Base64
     */
    public abstract boolean encryptBase64();

    /**
     * Get if the plugin should block the player
     * login/register when he has an invalid password.
     *
     * Forcing him to change it until it's safe
     *
     * @return if the plugin should block unsafe passwords
     */
    public abstract boolean blockUnsafePasswords();

    /**
     * Get anti-brute force system options
     *
     * @return the brute force system options
     */
    public abstract BruteForceConfig bruteForceOptions();

    /**
     * Get if the anti bot is enabled
     *
     * @return if the anti bot is enabled
     */
    public abstract boolean antiBot();

    /**
     * Get if the plugin should allow players
     * with the same IP join if they are already
     * in, very util in cases in where the players
     * has been frozen due lag, and can't join the
     * server
     *
     * @return if the plugin should allow connections
     * of the same IP when the player is already
     * connected
     */
    public abstract boolean allowSameIP();

    /**
     * Get if the pin system is enabled
     *
     * @return if the pin system is enabled
     */
    public abstract boolean enablePin();

    /**
     * Get if the 2FA system is enabled
     *
     * @return if the 2FA system is enabled
     */
    public abstract boolean enable2FA();

    /**
     * Get updater options
     *
     * @return updater options
     */
    public abstract UpdaterConfig getUpdaterOptions();

    /**
     * Get if the spawn is enabled
     *
     * @return if the spawn is enabled
     */
    public abstract boolean enableSpawn();

    /**
     * Get if the take back after login feature is
     * enabled
     *
     * @return if the player should be took back after
     * logging in
     */
    public abstract boolean takeBack();

    /**
     * Get the minimum distance between the spawn and the
     * player to store his last location
     *
     * @return the minimum distance between the spawn and player
     * to store last location
     */
    public abstract int spawnDistance();

    /**
     * Get if the plugin should clear player chat
     *
     * @return if the player chat should be cleared when
     * he joins
     */
    public abstract boolean clearChat();

    /**
     * Get the amount of same IPs can be
     * simultaneously in the server
     *
     * @return the amount of players with
     * same IP that can be in the server
     */
    public abstract int accountsPerIP();

    /**
     * Get if the plugin should check player names
     * before they join
     *
     * @return if the plugin should check player names
     * before they join
     */
    public abstract boolean checkNames();

    /**
     * Get if the plugin should check if player
     * names are similar, to avoid similar names
     * in the server
     *
     * @return if the plugin should check also
     * for similar player names
     */
    public abstract boolean enforceNameCheck();

    /**
     * Get the name check protocol
     *
     * @return the name check protocol
     */
    public abstract int nameCheckProtocol();

    /**
     * Get the current plugin language
     *
     * @return the plugin language
     */
    public abstract Lang getLang();

    /**
     * Get the current plugin language name
     *
     * @return the plugin language name
     */
    public abstract String getLangName();

    /**
     * Get the current modules prefix for
     * commands
     *
     * @return the module commands prefix
     */
    public abstract String getModulePrefix();
}
