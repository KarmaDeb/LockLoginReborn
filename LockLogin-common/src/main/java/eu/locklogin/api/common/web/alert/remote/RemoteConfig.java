package eu.locklogin.api.common.web.alert.remote;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import eu.locklogin.api.encryption.HashType;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.options.*;
import eu.locklogin.api.util.enums.Lang;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.karma.file.yaml.KarmaYamlManager;

import java.util.Map;

/**
 * Remote plugin configuration
 */
public class RemoteConfig extends PluginConfiguration {

    private final KarmaYamlManager manager;

    /**
     * Initialize the remote config
     *
     * @param simple_json the remote configuration
     */
    public RemoteConfig(final String simple_json) {
        Gson gson = new GsonBuilder().setLenient().create();
        Map<String, Object> map = gson.fromJson(simple_json, new TypeToken<Map<String, Object>>(){}.getType());

        KarmaYamlManager tmp = new KarmaYamlManager(map);
        KarmaYamlManager server = tmp.getSection("server");
        if (server.getKeySet().size() > 0) {
            tmp = server;
        }

        manager = tmp;
    }

    /**
     * Check if bungee cord mode is enabled
     *
     * @return if bungee mode is enabled
     */
    @Override
    public boolean isBungeeCord() {
        return manager.getBoolean("BungeeCord", false);
    }

    /**
     * Get the current server name
     *
     * @return the current server name
     */
    @Override
    public String serverName() {
        return manager.getString("ServerName", CurrentPlatform.getRealConfiguration().serverName());
    }

    /**
     * Get if bedrock players login automatically
     *
     * @return if bedrock players log in automatically
     */
    @Override
    public boolean bedrockLogin() {
        return manager.getBoolean("BedrockLogin", CurrentPlatform.getConfiguration().bedrockLogin());
    }

    /**
     * Get if the plugin share statistics with bStats
     *
     * @return if the plugin will share his statistics
     */
    @Override
    public boolean shareBStats() {
        return true;
    }

    /**
     * Get if the plugin share statistics with official server
     *
     * @return if the plugin will share statistics wil official web server
     */
    @Override
    public boolean sharePlugin() {
        return true;
    }

    /**
     * Get the plugin backup configuration
     *
     * @return the plugin backup configuration
     */
    @Override
    public BackupConfig backup() {
        return new BackupConfig(true, 5, 30, 7);
    }

    /**
     * Get if the premium support should be
     * enabled for this server
     *
     * @return if the plugin implements premium support
     */
    @Override
    public boolean enablePremium() {
        return true;
    }

    /**
     * Get if the plugin should try to fix UUIDs for
     * premium users
     *
     * @return if the plugin should fix UUIDs
     */
    @Override
    public boolean fixUUIDs() {
        return true;
    }

    /**
     * Get if the plugin should replace the server MOTD while in
     * bungeecord mode
     *
     * @return if the server should replace MOTD
     */
    @Override
    public boolean showMOTD() {
        return manager.getBoolean("BungeeMotd", CurrentPlatform.getRealConfiguration().showMOTD());
    }

    /**
     * Get the assigned LockLogin web panel
     * server key
     *
     * @return the panel server key
     */
    @Override
    public String serverKey() {
        return manager.getString("ServerKey", CurrentPlatform.getServerHash());
    }

    /**
     * Get the communication key
     *
     * @return the communication key
     */
    @Override
    public String comKey() {
        return manager.getString("BungeeKey", "");
    }

    /**
     * Get the registration options
     *
     * @return the registration options
     */
    @Override
    public RegisterConfig registerOptions() {
        RegisterConfig config = CurrentPlatform.getRealConfiguration().registerOptions();

        boolean boss = manager.getBoolean("Register.Boss", config.hasBossBar());
        boolean blind = manager.getBoolean("Register.Blind", config.blindEffect());
        boolean nausea = manager.getBoolean("Register.Nausea", config.nauseaEffect());

        int timeout = manager.getInt("Register.TimeOut", config.timeOut());
        int max = manager.getInt("Register.Max", config.maxAccounts());
        int interval = manager.getInt("MessagesInterval.Registration", config.getMessageInterval());

        return new RegisterConfig(boss, blind, nausea, timeout, max, interval);
    }

    /**
     * Get the login options
     *
     * @return the login options
     */
    @Override
    public LoginConfig loginOptions() {
        LoginConfig config = CurrentPlatform.getRealConfiguration().loginOptions();

        boolean boss = manager.getBoolean("Login.Boss", config.hasBossBar());
        boolean blind = manager.getBoolean("Login.Blind", config.blindEffect());
        boolean nausea = manager.getBoolean("Login.Nausea", config.nauseaEffect());

        int timeout = manager.getInt("Login.TimeOut", config.timeOut());
        int max = manager.getInt("Login.MaxTries", config.maxTries());
        int interval = manager.getInt("MessagesInterval.Logging", config.getMessageInterval());

        return new LoginConfig(boss, blind, nausea, timeout, max, interval);
    }

    /**
     * Get if the plugin has sessions enabled
     *
     * @return if the plugin has sessions
     */
    @Override
    public boolean enableSessions() {
        return manager.getBoolean("Sessions.Enabled", CurrentPlatform.getRealConfiguration().enableSessions());
    }

    /**
     * Get if the plugin should check if an
     * IP is healthy for the server integrity
     *
     * @return if the plugin has IP checks enabled
     */
    @Override
    public boolean ipHealthCheck() {
        return manager.getBoolean("IpHealthCheck", CurrentPlatform.getRealConfiguration().ipHealthCheck());
    }

    /**
     * Get if the plugin should validate UUIDs
     *
     * @return if the plugin should validate
     * UUIDs
     */
    @Override
    public boolean uuidValidator() {
        return manager.getBoolean("UUIDValidator", CurrentPlatform.getRealConfiguration().uuidValidator());
    }

    /**
     * Get the session life time
     *
     * @return the session life time
     */
    @Override
    public int sessionTime() {
        return manager.getInt("Sessions.Time", CurrentPlatform.getRealConfiguration().sessionTime());
    }

    /**
     * Get if the non-logged players
     * should be hidden from logged players
     * and logged players from non-logged
     *
     * @return if the non-logged players should
     * be hidden
     */
    @Override
    public boolean hideNonLogged() {
        return manager.getBoolean("HideNonLogged", CurrentPlatform.getRealConfiguration().hideNonLogged());
    }

    /**
     * Get the captcha options
     *
     * @return the captcha options
     */
    @Override
    public CaptchaConfig captchaOptions() {
        CaptchaConfig config = CurrentPlatform.getRealConfiguration().captchaOptions();

        boolean enable = manager.getBoolean("Captcha.Enabled", config.isEnabled());
        int length = manager.getInt("Captcha.Difficulty.Length", config.getLength());
        boolean letters = manager.getBoolean("Captcha.Difficulty.Letters", config.hasLetters());
        boolean strikethrough = manager.getBoolean("Captcha.Strikethrough.Enabled", config.enableStrike());
        boolean random_strikethrough = manager.getBoolean("Captcha.Strikethrough.Random", config.randomStrike());

        return new CaptchaConfig(enable, length, letters, strikethrough, random_strikethrough);
    }

    /**
     * Get the password hash type
     *
     * @return the password hash type
     */
    @Override
    public HashType passwordEncryption() {
        return HashType.valueOf(manager.getString("Encryption.Passwords", CurrentPlatform.getRealConfiguration().passwordEncryption().name()).toUpperCase());
    }

    /**
     * Get the pin encryption type
     *
     * @return the pin encryption type
     */
    @Override
    public HashType pinEncryption() {
        return HashType.valueOf(manager.getString("Encryption.Pins", CurrentPlatform.getRealConfiguration().pinEncryption().name()).toUpperCase());
    }

    /**
     * Get if the plugin should encrypt the passwords
     * in Base64
     *
     * @return if the plugin should encrypt in Base64
     */
    @Override
    public boolean encryptBase64() {
        return manager.getBoolean("Encryption.Encrypt", CurrentPlatform.getRealConfiguration().encryptBase64());
    }

    /**
     * Get if the plugin should encrypt the password
     * using a virtual ID
     *
     * @return if the plugin should hash password using
     * a virtual ID
     */
    @Override
    public boolean useVirtualID() {
        return manager.getBoolean("Encryption.VirtualID", CurrentPlatform.getRealConfiguration().useVirtualID());
    }

    /**
     * Get the permission configuration
     *
     * @return the permission configuration
     */
    @Override
    public PermissionConfig permissionConfig() {
        return null;
    }

    /**
     * Get the password configuration
     *
     * @return the password configuration
     */
    @Override
    public PasswordConfig passwordConfig() {
        return null;
    }

    /**
     * Get anti-brute force system options
     *
     * @return the brute force system options
     */
    @Override
    public BruteForceConfig bruteForceOptions() {
        BruteForceConfig config = CurrentPlatform.getRealConfiguration().bruteForceOptions();

        int tries = manager.getInt("BruteForce.Tries", config.getMaxTries());
        int time = manager.getInt("BruteForce.BlockTime", config.getBlockTime());

        return new BruteForceConfig(tries, time);
    }

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
    @Override
    public boolean allowSameIP() {
        return manager.getBoolean("AllowSameIp", CurrentPlatform.getRealConfiguration().allowSameIP());
    }

    /**
     * Get if the pin system is enabled
     *
     * @return if the pin system is enabled
     */
    @Override
    public boolean enablePin() {
        return manager.getBoolean("Pin", CurrentPlatform.getRealConfiguration().enablePin());
    }

    /**
     * Get if the 2FA system is enabled
     *
     * @return if the 2FA system is enabled
     */
    @Override
    public boolean enable2FA() {
        return manager.getBoolean("2FA", CurrentPlatform.getRealConfiguration().enable2FA());
    }

    /**
     * Get updater options
     *
     * @return updater options
     */
    @Override
    public UpdaterConfig getUpdaterOptions() {
        UpdaterConfig config = CurrentPlatform.getRealConfiguration().getUpdaterOptions();

        String channel = manager.getString("Updater.Channel", config.getChannel().name());
        boolean check = manager.getBoolean("Updater.Check", config.isEnabled());
        int interval = manager.getInt("Updater.CheckTime", config.getInterval());

        return new UpdaterConfig(channel, check, interval);
    }

    /**
     * Get if the spawn is enabled
     *
     * @return if the spawn is enabled
     */
    @Override
    public boolean enableSpawn() {
        return manager.getBoolean("Spawn.Manager", CurrentPlatform.getRealConfiguration().enableSpawn());
    }

    /**
     * Get if the take back after login feature is
     * enabled
     *
     * @return if the player should be took back after
     * logging in
     */
    @Override
    public boolean takeBack() {
        return manager.getBoolean("Spawn.TakeBack", CurrentPlatform.getRealConfiguration().takeBack());
    }

    /**
     * Get the minimum distance between the spawn and the
     * player to store his last location
     *
     * @return the minimum distance between the spawn and player
     * to store last location
     */
    @Override
    public int spawnDistance() {
        return manager.getInt("Spawn.SpawnDistance", CurrentPlatform.getRealConfiguration().spawnDistance());
    }

    /**
     * Get if the plugin should clear player chat
     *
     * @return if the player chat should be cleared when
     * he joins
     */
    @Override
    public boolean clearChat() {
        return manager.getBoolean("ClearChat", CurrentPlatform.getRealConfiguration().clearChat());
    }

    /**
     * Get if the plugin should check player names
     * before they join
     *
     * @return if the plugin should check player names
     * before they join
     */
    @Override
    public boolean checkNames() {
        return manager.getBoolean("CheckNames", CurrentPlatform.getRealConfiguration().checkNames());
    }

    /**
     * Get if the plugin should check if player
     * names are similar, to avoid similar names
     * in the server
     *
     * @return if the plugin should check also
     * for similar player names
     */
    @Override
    public boolean enforceNameCheck() {
        return manager.getBoolean("EnforceNameCheck", CurrentPlatform.getRealConfiguration().enforceNameCheck());
    }

    /**
     * Get the name check protocol
     *
     * @return the name check protocol
     */
    @Override
    public int nameCheckProtocol() {
        return manager.getInt("NameCheckProtocol", CurrentPlatform.getRealConfiguration().nameCheckProtocol());
    }

    /**
     * Get the current plugin language
     *
     * @return the plugin language
     */
    @Override
    public Lang getLang() {
        return Lang.valueOf(manager.getString("Lang", CurrentPlatform.getRealConfiguration().getLang().name()).toUpperCase());
    }

    /**
     * Get the current plugin language name
     *
     * @return the plugin language name
     */
    @Override
    public String getLangName() {
        return manager.getString("Lang", CurrentPlatform.getRealConfiguration().getLangName());
    }

    /**
     * Get the current modules prefix for
     * commands
     *
     * @return the module commands prefix
     */
    @Override
    public String getModulePrefix() {
        return manager.getString("ModulePrefix", CurrentPlatform.getRealConfiguration().getModulePrefix());
    }
}
