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
     * Get if bedrock players login automatically
     *
     * @return if bedrock players log in automatically
     */
    public abstract boolean bedrockLogin();

    /**
     * Get if the plugin share statistics with bStats
     *
     * @return if the plugin will share his statistics
     */
    public boolean shareBStats() {
        return true; //TODO: Make this method abstract
    }

    /**
     * Get if the plugin share statistics with official server
     *
     * @return if the plugin will share statistics wil official web server
     */
    public boolean sharePlugin() {
        return true; //TODO: Make this method abstract
    }

    /**
     * Get if the plugin should replace the server MOTD while in
     * bungeecord mode
     *
     * @return if the server should replace MOTD
     */
    public abstract boolean showMOTD();

    /**
     * Get the assigned LockLogin web panel
     * server key
     *
     * @return the panel server key
     */
    public abstract String serverKey();

    /**
     * Get the communication key
     *
     * @return the communication key
     */
    public abstract String comKey();

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
     * Get the session life time
     *
     * @return the session life time
     */
    public abstract int sessionTime();

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
     * Get if the plugin should encrypt the password
     * using a virtual ID
     *
     * @return if the plugin should hash password using
     *         a virtual ID
     */
    public abstract boolean useVirtualID();

    /**
     * Get the permission configuration
     *
     * @return the permission configuration
     */
    public abstract PermissionConfig permissionConfig();

    /**
     * Get the password configuration
     *
     * @return the password configuration
     */
    public abstract PasswordConfig passwordConfig();

    /**
     * Get anti-brute force system options
     *
     * @return the brute force system options
     */
    public abstract BruteForceConfig bruteForceOptions();

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
