package eu.locklogin.plugin.bukkit.util.files;

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

import eu.locklogin.api.encryption.HashType;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.options.*;
import eu.locklogin.api.util.enums.Lang;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.data.file.FileUtilities;
import ml.karmaconfigs.api.common.karma.file.yaml.FileCopy;
import ml.karmaconfigs.api.common.karma.file.yaml.KarmaYamlManager;
import ml.karmaconfigs.api.common.karma.file.yaml.YamlReloader;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.string.random.RandomString;
import ml.karmaconfigs.api.common.string.text.TextContent;
import ml.karmaconfigs.api.common.string.text.TextType;
import ml.karmaconfigs.api.common.utils.enums.Level;
import org.bukkit.Bukkit;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static eu.locklogin.plugin.bukkit.LockLogin.plugin;

public final class Config extends PluginConfiguration {

    private final static File cfg_file = new File(plugin.getDataFolder(), "config.yml");
    private static KarmaYamlManager cfg = null;

    private static boolean advised = false;

    /**
     * Initialize configuration
     */
    public Config() {
        if (cfg == null) {
            if (!cfg_file.exists()) {
                FileCopy copy = new FileCopy(plugin, "cfg/config.yml");
                try {
                    copy.copy(cfg_file);
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }

            cfg = new KarmaYamlManager(cfg_file);
        }
    }

    /**
     * Check if bungee cord mode is enabled
     *
     * @return if bungee mode is enabled
     */
    @Override
    public boolean isBungeeCord() {
        File force_bungee = new File(plugin.getDataFolder(), "force_bungee.yml");
        File spigot_yml = new File(Bukkit.getWorldContainer(), "spigot.yml");

        if (!spigot_yml.exists()) {
            if (!advised) {
                advised = true;

                plugin.console().send("Failed to locate file {0} which should contain bungeecord information. Using {1} instead.", Level.GRAVE,
                        FileUtilities.getPrettyFile(spigot_yml),
                        FileUtilities.getPrettyFile(force_bungee));
            }

            if (!force_bungee.exists()) {
                FileCopy copy = new FileCopy(plugin, "force_bungee.yml");
                try {
                    copy.copy(force_bungee);
                } catch (Throwable ignored) {}
            }

            KarmaYamlManager yaml = new KarmaYamlManager(force_bungee);
            if (!yaml.isSet("settings.bungeecord")) {
                yaml.set("settings.bungeecord", false);
                yaml.save(force_bungee);
                return false;
            }

            return yaml.getBoolean("settings.bungeecord");
        } else {
            if (advised) {
                plugin.console().send("Found {0} containing bungeecord information. Using it", Level.OK, FileUtilities.getPrettyFile(spigot_yml));
                advised = false;
            }
        }

        KarmaYamlManager yaml = new KarmaYamlManager(spigot_yml);
        return yaml.getBoolean("settings.bungeecord", false);
    }

    @Override
    public String serverName() {
        String server_name = cfg.getString("ServerName", "");
        if (StringUtils.isNullOrEmpty(server_name)) {
            server_name = new RandomString(
                    RandomString.createBuilder()
                            .withType(TextType.ALL_LOWER)
                            .withContent(TextContent.ONLY_LETTERS)
                            .withSize(8)
            ).create();

            cfg.set("ServerName", server_name);
            cfg = cfg.save(cfg_file, plugin, "cfg/config.yml");

            YamlReloader reloader = cfg.getReloader();
            if (reloader != null) {
                reloader.reload();
            }
        }

        return server_name;
    }

    /**
     * Get if bedrock players login automatically
     *
     * @return if bedrock players log in automatically
     */
    @Override
    public boolean bedrockLogin() {
        return cfg.getBoolean("BedrockLogin", false);
    }

    /**
     * Get if the plugin share statistics with bStats
     *
     * @return if the plugin will share his statistics
     */
    @Override
    public boolean shareBStats() {
        return cfg.getBoolean("Statistics.bStats", true);
    }

    /**
     * Get if the plugin share statistics with official server
     *
     * @return if the plugin will share statistics wil official web server
     */
    @Override
    public boolean sharePlugin() {
        return cfg.getBoolean("Statistics.plugin", true);
    }

    /**
     * Get the plugin backup configuration
     *
     * @return the plugin backup configuration
     */
    @Override
    public BackupConfig backup() {
        boolean enable = cfg.getBoolean("Backup.Enable", true);
        int max = cfg.getInt("Backup.Max", 5);
        int period = cfg.getInt("Backup.Period", 30);
        int purge = cfg.getInt("Backup.Purge", 7);

        return new BackupConfig(enable, max, period, purge);
    }

    /**
     * Get if the premium support should be
     * enabled for this server
     *
     * @return if the plugin implements premium support
     */
    @Override
    public boolean enablePremium() {
        return !CurrentPlatform.isOnline() &&  cfg.getBoolean("Premium.Enable", true);
    }

    /**
     * Get if the plugin should try to fix UUIDs for
     * premium users
     *
     * @return if the plugin should fix UUIDs
     */
    @Override
    public boolean fixUUIDs() {
        return cfg.getBoolean("Premium.ForceUUID", true);
    }

    /**
     * Get if the plugin should replace the server MOTD while in
     * bungeecord mode
     *
     * @return if the server should replace MOTD
     */
    @Override
    public boolean showMOTD() {
        return cfg.getBoolean("BungeeMotd", true);
    }

    /**
     * Get the assigned LockLogin web panel
     * server key
     *
     * @return the panel server key
     */
    @Override
    public String serverKey() {
        return cfg.getString("ServerKey", "");
    }

    /**
     * Get the communication key
     *
     * @return the communication key
     */
    @Override
    public String comKey() {
        return cfg.getString("BungeeKey", "");
    }

    @Override
    public RegisterConfig registerOptions() {
        boolean boss = cfg.getBoolean("Login.Boss", true);
        boolean blind = cfg.getBoolean("Register.Blind", false);
        boolean nausea = cfg.getBoolean("Register.Nausea", false);
        int timeout = cfg.getInt("Register.TimeOut", 60);
        int max = cfg.getInt("Register.Max", 2);
        int interval = cfg.getInt("MessagesInterval.Registration", 5);

        return new RegisterConfig(boss, blind, nausea, timeout, max, interval);
    }

    @Override
    public LoginConfig loginOptions() {
        boolean boss = cfg.getBoolean("Login.Boss", true);
        boolean blind = cfg.getBoolean("Login.Blind", false);
        boolean nausea = cfg.getBoolean("Login.Nausea", false);
        int timeout = cfg.getInt("Login.TimeOut", 60);
        int max = cfg.getInt("Login.MaxTries", 2);
        int interval = cfg.getInt("MessagesInterval.Logging", 5);

        return new LoginConfig(boss, blind, nausea, timeout, max, interval);
    }

    /**
     * Get if the plugin has sessions enabled
     *
     * @return if the plugin has sessions
     */
    @Override
    public boolean enableSessions() {
        return cfg.getBoolean("Sessions.Enabled", false);
    }

    /**
     * Get if the plugin should check if an
     * IP is healthy for the server integrity
     *
     * @return if the plugin has IP checks enabled
     */
    @Override
    public boolean ipHealthCheck() {
        return cfg.getBoolean("IpHealthCheck", true);
    }

    /**
     * Get if the plugin should validate UUIDs
     *
     * @return if the plugin should validate
     * UUIDs
     */
    @Override
    public boolean uuidValidator() {
        return cfg.getBoolean("UUIDValidator", true);
    }

    /**
     * Get the session life time
     *
     * @return the session life time
     */
    @Override
    public int sessionTime() {
        int time = cfg.getInt("Sessions.Time", 5);
        if (time <= 0)
            time = 1;
        if (time > 30)
            time = 5;

        return time;
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
        return cfg.getBoolean("HideNonLogged", false);
    }

    @Override
    public CaptchaConfig captchaOptions() {
        boolean enabled = cfg.getBoolean("Captcha.Enabled", true);
        int length = cfg.getInt("Captcha.Difficulty.Length", 8);
        boolean letters = cfg.getBoolean("Captcha.Difficulty.Letters", true);
        boolean strike = cfg.getBoolean("Captcha.Strikethrough.Enabled", true);
        boolean randomStrike = cfg.getBoolean("Captcha.Strikethrough.Random", true);

        return new CaptchaConfig(enabled, length, letters, strike, randomStrike);
    }

    @Override
    public HashType passwordEncryption() {
        String value = cfg.getString("Encryption.Passwords", "SHA512");

        switch (value.toLowerCase()) {
            case "256":
            case "sha256":
                return HashType.SHA256;
            case "bcrypt":
                return HashType.BCrypt;
            case "argon2i":
                return HashType.ARGON2I;
            case "argon2id":
                return HashType.ARGON2ID;
            case "authme":
            case "authmesha":
                return HashType.AUTHME_SHA;
            case "wordpress":
                return HashType.WORDPRESS;
            case "512":
            case "sha512":
            default:
                return HashType.SHA512;
        }
    }

    @Override
    public HashType pinEncryption() {
        String value = cfg.getString("Encryption.Pins", "SHA512");
        assert value != null;

        switch (value.toLowerCase()) {
            case "256":
            case "sha256":
                return HashType.SHA256;
            case "bcrypt":
                return HashType.BCrypt;
            case "argon2i":
                return HashType.ARGON2I;
            case "argon2id":
                return HashType.ARGON2ID;
            case "authme":
            case "authmesha":
                return HashType.AUTHME_SHA;
            case "wordpress":
                return HashType.WORDPRESS;
            case "512":
            case "sha512":
            default:
                return HashType.SHA512;
        }
    }

    /**
     * Get if the plugin should encrypt the passwords
     * in Base64
     *
     * @return if the plugin should encrypt in Base64
     */
    @Override
    public boolean encryptBase64() {
        return cfg.getBoolean("Encryption.Encrypt", true);
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
        return cfg.getBoolean("Encryption.VirtualID", false);
    }

    /**
     * Get the permission configuration
     *
     * @return the permission configuration
     */
    @Override
    public PermissionConfig permissionConfig() {
        return new PermissionConfig(
                cfg.getBoolean("Permission.BlockOperator", true),
                cfg.getBoolean("Permission.RemoveEverything", true),
                cfg.getBoolean("Permission.AllowWildcard", false)
        );
    }

    /**
     * Get the password configuration
     *
     * @return the password configuration
     */
    @Override
    public PasswordConfig passwordConfig() {
        return new PasswordConfig(
                cfg.getBoolean("Password.PrintSuccess", true),
                cfg.getBoolean("Password.BlockUnsafe", true),
                cfg.getBoolean("Password.WarnUnsafe", true),
                cfg.getBoolean("Password.IgnoreCommon", false),
                cfg.getInt("Password.Safety.MinLength", 10),
                cfg.getInt("Password.Safety.Characters", 1),
                cfg.getInt("Password.Safety.Numbers", 2),
                cfg.getInt("Password.Safety.Letters.Upper", 2),
                cfg.getInt("Password.Safety.Letters.Lower", 5)
        );
    }

    @Override
    public BruteForceConfig bruteForceOptions() {
        int tries = cfg.getInt("BruteForce", 3);
        int time = cfg.getInt("BruteForce.BlockTime", 30);

        return new BruteForceConfig(tries, time);
    }

    @Override
    public boolean allowSameIP() {
        return cfg.getBoolean("AllowSameIp", true);
    }

    @Override
    public boolean enablePin() {
        return cfg.getBoolean("Pin", true);
    }

    @Override
    public boolean enable2FA() {
        return cfg.getBoolean("2FA", true);
    }

    @Override
    public UpdaterConfig getUpdaterOptions() {
        String channel = cfg.getString("Updater.Channel", "RELEASE");

        boolean check = cfg.getBoolean("Updater.Check", true);
        int interval = cfg.getInt("Updater.CheckTime", 10);

        switch (channel.toLowerCase()) {
            case "release_candidate":
            case "releasecandidate":
                channel = "rc";
        }

        return new UpdaterConfig(channel, check, interval);
    }

    @Override
    public boolean enableSpawn() {
        return cfg.getBoolean("Spawn.Manage", false);
    }

    @Override
    public boolean takeBack() {
        return cfg.getBoolean("Spawn.TakeBack", false);
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
        return cfg.getInt("Spawn.SpawnDistance", 30);
    }

    @Override
    public boolean clearChat() {
        return cfg.getBoolean("ClearChat", false);
    }

    @Override
    public boolean checkNames() {
        return cfg.getBoolean("CheckNames", true);
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
        return cfg.getBoolean("EnforceNameCheck", false);
    }

    /**
     * Get the name check protocol
     *
     * @return the name check protocol
     */
    @Override
    public int nameCheckProtocol() {
        return cfg.getInt("NameCheckProtocol", 2);
    }

    @Override
    public Lang getLang() {
        String val = cfg.getString("Lang", "en_EN");

        switch (val.toLowerCase()) {
            case "en_en":
            case "english":
                return Lang.ENGLISH;
            case "es_es":
            case "spanish":
                return Lang.SPANISH;
            case "fr_fr":
            case "french":
                return Lang.FRENCH;
            case "zh_cn":
            case "chinese simplified":
            case "simplified chinese":
                return Lang.CHINESE_SIMPLIFIED;
            case "polish":
            case "poland":
            case "pl":
                return Lang.POLISH;
            case "turkish":
            case "turkey":
            case "tr":
                return Lang.TURKISH;
            default:
                return Lang.COMMUNITY;
        }
    }

    @Override
    public String getLangName() {
        return cfg.getString("Lang", "en_EN");
    }

    @Override
    public String getModulePrefix() {
        String value = cfg.getString("ModulePrefix", "$");

        if (value.replaceAll("\\s", "").isEmpty())
            value = "$";

        return value.replaceAll("\\s", "").substring(0, 1);
    }

    /**
     * Get the configuration manager
     */
    public interface manager {

        /**
         * Reload the configuration
         *
         * @return if the configuration could be reloaded
         */
        static boolean reload() {
            File force_bungee = new File(plugin.getDataFolder(), "force_bungee.yml");

            boolean bungeecord = false;
            try {
                bungeecord = plugin.getServer().spigot().getConfig().getBoolean("settings.bungeecord", false);
            } catch (Throwable ignored) {
            }

            if (!bungeecord) {
                try {
                    FileCopy copy = new FileCopy(plugin, "force_bungee.yml");
                    copy.copy(force_bungee);
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }

                KarmaYamlManager manager = new KarmaYamlManager(force_bungee);
                bungeecord = manager.getBoolean("settings.bungeecord", false);
            }

            if (bungeecord) {
                List<String> sets = new ArrayList<>(cfg.getKeySet());
                sets.remove("Spawn");
                sets.remove("CheckNames");

                YamlReloader reloader = cfg.getReloader();
                if (reloader != null) {
                    reloader.reload(sets.toArray(new String[0]));
                    return true;
                }
            } else {
                YamlReloader reloader = cfg.getReloader();
                if (reloader != null) {
                    reloader.reload();
                    return true;
                }
            }

            return false;
        }

        /**
         * Tries to load bungeecord messages
         *
         * @param yaml the bungeecord messages yml
         */
        static void loadBungee(final String yaml) {
            String data = new String(Base64.getDecoder().decode(yaml.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
            PluginConfiguration config = CurrentPlatform.getConfiguration();
            if (config.isBungeeCord()) {
                KarmaYamlManager bungee = new KarmaYamlManager(data, false);
                cfg.update(bungee, true, "Spawn", "CheckNames");
            }
        }

        /**
         * Check configuration values
         */
        static void checkValues() {
            boolean changes = false;

            String server_name = cfg.getString("ServerName", "");

            int register_timeout = cfg.getInt("Register.TimeOut", 60);

            int login_timeout = cfg.getInt("Login.TimeOut", 30);

            int register_message_interval = cfg.getInt("MessagesInterval.Registration", 5);

            int login_message_interval = cfg.getInt("MessagesInterval.Logging", 5);

            int session_life_time = cfg.getInt("Sessions.Time", 5);

            int captcha_length = cfg.getInt("Captcha.Length", 8);

            String password_encryption = cfg.getString("Encryption.Passwords", "SHA512");
            String pin_encryption = cfg.getString("Encryption.Pin", "SHA512");

            String update_channel = cfg.getString("Updater.Channel", "RELEASE");

            int spawnDist = cfg.getInt("Spawn.SpawnDistance", 30);

            int nameCheckProtocol = cfg.getInt("NameCheckProtocol", 2);

            String module_prefix = cfg.getString("ModulePrefix", "$");

            if (StringUtils.isNullOrEmpty(server_name)) {
                server_name = new RandomString(
                        RandomString.createBuilder()
                                .withType(TextType.ALL_LOWER)
                                .withContent(TextContent.ONLY_LETTERS)
                                .withSize(8)
                ).create();
                cfg.set("ServerName", server_name);

                changes = true;
            }

            if (register_timeout < 15) {
                register_timeout = 15;
                cfg.set("Register.TimeOut", register_timeout);

                changes = true;
            }

            if (login_timeout < 15) {
                login_timeout = 15;
                cfg.set("Login.TimeOut", login_timeout);

                changes = true;
            }

            if (register_message_interval < 5 || register_message_interval > register_timeout) {
                register_message_interval = 5;
                cfg.set("MessagesInterval.Registration", register_message_interval);

                changes = true;
            }

            if (login_message_interval < 5 || login_message_interval > login_timeout) {
                login_message_interval = 5;
                cfg.set("MessagesInterval.Logging", login_message_interval);

                changes = true;
            }

            if (session_life_time <= 0 || session_life_time > 30) {
                session_life_time = 5;
                cfg.set("Sessions.Time", session_life_time);

                changes = true;
            }

            if (captcha_length < 8 || captcha_length > 16) {
                captcha_length = 8;
                cfg.set("Captcha.Length", captcha_length);

                changes = true;
            }

            switch (password_encryption.toLowerCase()) {
                case "256":
                case "sha256":
                case "bcrypt":
                case "argon2i":
                case "argon2id":
                case "authme":
                case "authmesha":
                case "512":
                case "sha512":
                    break;
                default:
                    password_encryption = "SHA512";
                    cfg.set("Encryption.Passwords", password_encryption);

                    changes = true;
            }

            switch (pin_encryption.toLowerCase()) {
                case "256":
                case "sha256":
                case "bcrypt":
                case "argon2i":
                case "argon2id":
                case "authme":
                case "authmesha":
                case "512":
                case "sha512":
                    break;
                default:
                    pin_encryption = "SHA512";
                    cfg.set("Encryption.Pin", pin_encryption);

                    changes = true;
            }

            switch (update_channel.toLowerCase()) {
                case "release":
                case "release_candidate":
                case "releasecandidate":
                case "rc":
                case "snapshot":
                    break;
                default:
                    update_channel = "RELEASE";
                    cfg.set("Updater.Channel", update_channel);

                    changes = true;
            }

            if (spawnDist < 0) {
                spawnDist = 0;
                cfg.set("Spawn.SpawnDistance", spawnDist);

                changes = true;
            }

            if (nameCheckProtocol != 1 && nameCheckProtocol != 2) {
                nameCheckProtocol = 2;

                cfg.set("NameCheckProtocol", nameCheckProtocol);
                changes = true;
            }

            if (module_prefix.replaceAll("\\s", "").isEmpty()) {
                module_prefix = "$";
                cfg.set("ModulePrefix", module_prefix);

                changes = true;
            }

            if (changes) {
                cfg = cfg.save(cfg_file, plugin, "cfg/config.yml");

                YamlReloader reloader = cfg.getReloader();
                if (reloader != null) {
                    reloader.reload();
                }
            }
        }
    }
}
