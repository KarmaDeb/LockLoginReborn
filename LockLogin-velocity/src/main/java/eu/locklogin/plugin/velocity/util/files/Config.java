package eu.locklogin.plugin.velocity.util.files;

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
import ml.karmaconfigs.api.common.karmafile.karmayaml.FileCopy;
import ml.karmaconfigs.api.common.karmafile.karmayaml.KarmaYamlManager;
import ml.karmaconfigs.api.common.karmafile.karmayaml.YamlReloader;
import ml.karmaconfigs.api.common.utils.StringUtils;

import java.io.File;

import static eu.locklogin.plugin.velocity.LockLogin.source;

public final class Config extends PluginConfiguration {

    private final static File cfg_file = new File(source.getDataPath().toFile(), "config.yml");
    private static KarmaYamlManager cfg = null;

    /**
     * Initialize configuration
     */
    public Config() {
        if (cfg == null) {
            if (!cfg_file.exists()) {
                FileCopy copy = new FileCopy(source, "cfg/config.yml");
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
        return true;
    }

    @Override
    public final String serverName() {
        return cfg.getString("ServerName", StringUtils.randomString(8, StringUtils.StringGen.ONLY_LETTERS, StringUtils.StringType.ALL_LOWER));
    }

    @Override
    public final RegisterConfig registerOptions() {
        boolean boss = cfg.getBoolean("Login.Boss", true);
        boolean blind = cfg.getBoolean("Register.Blind", false);
        boolean nausea = cfg.getBoolean("Register.Nausea", false);
        int timeout = cfg.getInt("Register.TimeOut", 60);
        int max = cfg.getInt("Register.Max", 2);
        int interval = cfg.getInt("MessagesInterval.Registration", 5);

        return new RegisterConfig(boss, blind, nausea, timeout, max, interval);
    }

    @Override
    public final LoginConfig loginOptions() {
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
    public final CaptchaConfig captchaOptions() {
        boolean enabled = cfg.getBoolean("Captcha.Enabled", true);
        int length = cfg.getInt("Captcha.Difficulty.Length", 8);
        boolean letters = cfg.getBoolean("Captcha.Difficulty.Letters", true);
        boolean strike = cfg.getBoolean("Captcha.Strikethrough.Enabled", true);
        boolean randomStrike = cfg.getBoolean("Captcha.Strikethrough.Random", true);

        return new CaptchaConfig(enabled, length, letters, strike, randomStrike);
    }

    @Override
    public final HashType passwordEncryption() {
        String value = cfg.getString("Encryption.Passwords", "SHA512");
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
            case "512":
            case "sha512":
            default:
                return HashType.SHA512;
        }
    }

    @Override
    public final HashType pinEncryption() {
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
            case "512":
            case "sha512":
            default:
                return HashType.SHA512;
        }
    }

    /**
     * Get if the plugin should block the player
     * login/register when he has an invalid password.
     * <p>
     * Forcing him to change it until it's safe
     *
     * @return if the plugin should block unsafe passwords
     */
    @Override
    public boolean blockUnsafePasswords() {
        return cfg.getBoolean("BlockUnsafePasswords", true);
    }

    @Override
    public final BruteForceConfig bruteForceOptions() {
        int tries = cfg.getInt("BruteForce", 3);
        int time = cfg.getInt("BruteForce.BlockTime", 30);

        return new BruteForceConfig(tries, time);
    }

    @Override
    public final boolean antiBot() {
        return cfg.getBoolean("AntiBot", true);
    }

    @Override
    public final boolean allowSameIP() {
        return cfg.getBoolean("AllowSameIp", true);
    }

    @Override
    public final boolean enablePin() {
        return cfg.getBoolean("Pin", true);
    }

    @Override
    public final boolean enable2FA() {
        return cfg.getBoolean("2FA", true);
    }

    @Override
    public final UpdaterConfig getUpdaterOptions() {
        String channel = cfg.getString("Updater.Channel", "RELEASE");
        assert channel != null;

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
    public final boolean enableSpawn() {
        return cfg.getBoolean("Spawn.Manage", false);
    }

    @Override
    public final boolean takeBack() {
        return cfg.getBoolean("Spawn.Back", false);
    }

    @Override
    public final boolean clearChat() {
        return cfg.getBoolean("ClearChat", false);
    }

    @Override
    public final int accountsPerIP() {
        return cfg.getInt("AccountsPerIp", 2);
    }

    @Override
    public final boolean checkNames() {
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

    @Override
    public final Lang getLang() {
        String val = cfg.getString("Lang", "en_EN");
        assert val != null;

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
            case "de_de":
            case "german":
                return Lang.GERMAN;
            case "zh_cn":
            case "chinese simplified":
            case "simplified chinese":
                return Lang.CHINESE_SIMPLIFIED;
            default:
                return Lang.COMMUNITY;
        }
    }

    @Override
    public final String getLangName() {
        return cfg.getString("Lang", "en_EN");
    }

    @Override
    public final String getModulePrefix() {
        String value = cfg.getString("ModulePrefix", "$");
        assert value != null;

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
            try {
                YamlReloader reloader = cfg.getReloader();
                if (reloader != null) {
                    reloader.reload();
                    return true;
                }

                return false;
            } catch (Throwable ex) {
                return false;
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

            String account_system = cfg.getString("AccountSys", "File");

            String module_prefix = cfg.getString("ModulePrefix", "$");

            assert server_name != null;
            assert password_encryption != null;
            assert pin_encryption != null;
            assert update_channel != null;
            assert account_system != null;
            assert module_prefix != null;

            if (server_name.replaceAll("\\s", "").isEmpty()) {
                server_name = StringUtils.randomString(8, StringUtils.StringGen.ONLY_LETTERS, StringUtils.StringType.ALL_LOWER);
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
                case "sha512":
                case "sha256":
                case "bcrypt":
                case "argon2i":
                case "argon2id":
                    break;
                default:
                    password_encryption = "SHA512";
                    cfg.set("Encryption.Passwords", password_encryption);

                    changes = true;
            }

            switch (pin_encryption.toLowerCase()) {
                case "sha512":
                case "sha256":
                case "bcrypt":
                case "argon2i":
                case "argon2id":
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

            switch (account_system.toLowerCase()) {
                case "file":
                case "mysql":
                case "sql":
                    break;
                default:
                    account_system = "File";
                    cfg.set("AccountSys", account_system);

                    changes = true;
            }

            if (module_prefix.replaceAll("\\s", "").isEmpty()) {
                module_prefix = "$";
                cfg.set("ModulePrefix", module_prefix);

                changes = true;
            }

            if (changes) {
                cfg.save(cfg_file, source, "cfg/config.yml");
            }
        }

        /**
         * Parse the yaml file into a string, read-able by
         * spigot
         */
        static String getConfiguration() {
            return cfg.toString();
        }
    }
}
