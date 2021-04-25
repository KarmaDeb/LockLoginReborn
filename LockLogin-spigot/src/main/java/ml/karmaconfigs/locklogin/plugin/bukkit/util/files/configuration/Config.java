package ml.karmaconfigs.locklogin.plugin.bukkit.util.files.configuration;

import ml.karmaconfigs.api.bukkit.karmayaml.FileCopy;
import ml.karmaconfigs.api.bukkit.karmayaml.YamlReloader;
import ml.karmaconfigs.api.common.utils.FileUtilities;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.encryption.CryptType;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.configuration.options.*;
import ml.karmaconfigs.locklogin.plugin.common.utils.enums.AccountSystem;
import ml.karmaconfigs.locklogin.plugin.common.utils.enums.Lang;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.*;

public final class Config {

    private final static File cfg_file = new File(plugin.getDataFolder(), "config.yml");
    private static YamlConfiguration cfg = YamlConfiguration.loadConfiguration(cfg_file);

    /**
     * Initialize configuration
     */
    public Config() {
        if (!cfg_file.exists()) {
            FileCopy copy = new FileCopy(plugin, "config_spigot.yml");
            try {
                copy.copy(cfg_file);
                if (!manager.reload())
                    cfg = YamlConfiguration.loadConfiguration(cfg_file);
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }

        //Check values everytime the configuration is called
        //this will help avoid issues
        manager.checkValues();
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
                YamlReloader reloader = new YamlReloader(plugin, cfg_file, "config_spigot.yml");
                if (reloader.reloadAndCopy()) {
                    cfg.loadFromString(reloader.getYamlString());
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
                try {
                    cfg.save(cfg_file);

                    YamlReloader reloader = new YamlReloader(plugin, cfg_file, "config_spigot.yml");
                    if (reloader.reloadAndCopy())
                        cfg.loadFromString(reloader.getYamlString());
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Check if bungee cord mode is enabled
     *
     * @return if bungee mode is enabled
     */
    public final boolean isBungeeCord() {
        File spigot = new File(FileUtilities.getServerFolder(), "spigot.yml");

        if (spigot.exists()) {
            YamlConfiguration spigot_yml = YamlConfiguration.loadConfiguration(spigot);
            return spigot_yml.getBoolean("settings.bungeecord", false);
        }

        return false;
    }

    public final String serverName() {
        return cfg.getString("ServerName", StringUtils.randomString(8, StringUtils.StringGen.ONLY_LETTERS, StringUtils.StringType.ALL_LOWER));
    }

    public final RegisterConfig registerOptions() {
        boolean blind = cfg.getBoolean("Register.Blind", false);
        boolean nausea = cfg.getBoolean("Register.Nausea", false);
        int timeout = cfg.getInt("Register.TimeOut", 60);
        int max = cfg.getInt("Register.Max", 2);
        int interval = cfg.getInt("MessagesInterval.Registration", 5);

        return new RegisterConfig(blind, nausea, timeout, max, interval);
    }

    public final LoginConfig loginOptions() {
        boolean blind = cfg.getBoolean("Login.Blind", false);
        boolean nausea = cfg.getBoolean("Login.Nausea", false);
        int timeout = cfg.getInt("Login.TimeOut", 60);
        int max = cfg.getInt("Login.MaxTries", 2);
        int interval = cfg.getInt("MessagesInterval.Logging", 5);

        return new LoginConfig(blind, nausea, timeout, max, interval);
    }

    public final CaptchaConfig captchaOptions() {
        boolean enabled = cfg.getBoolean("Captcha.Enabled", true);
        int length = cfg.getInt("Captcha.Difficulty.Length", 8);
        boolean letters = cfg.getBoolean("Captcha.Difficulty.Letters", true);
        boolean strike = cfg.getBoolean("Captcha.Strikethrough.Enabled", true);
        boolean randomStrike = cfg.getBoolean("Captcha.Strikethrough.Random", true);

        return new CaptchaConfig(enabled, length, letters, strike, randomStrike);
    }

    public final CryptType passwordEncryption() {
        String value = cfg.getString("Encryption.Passwords", "SHA512");
        assert value != null;

        switch (value.toLowerCase()) {
            case "256":
            case "sha256":
                return CryptType.SHA256;
            case "bcrypt":
                return CryptType.BCrypt;
            case "argon2i":
                return CryptType.ARGON2I;
            case "argon2id":
                return CryptType.ARGON2ID;
            case "512":
            case "sha512":
            default:
                return CryptType.SHA512;
        }
    }

    public final CryptType pinEncryption() {
        String value = cfg.getString("Encryption.Pin", "SHA512");
        assert value != null;

        switch (value.toLowerCase()) {
            case "256":
            case "sha256":
                return CryptType.SHA256;
            case "bcrypt":
                return CryptType.BCrypt;
            case "argon2i":
                return CryptType.ARGON2I;
            case "argon2id":
                return CryptType.ARGON2ID;
            case "512":
            case "sha512":
            default:
                return CryptType.SHA512;
        }
    }

    public final boolean restrictAzuriom() {
        return cfg.getBoolean("Azuriom.Restrict", false);
    }

    public final boolean semiPremium() {
        return cfg.getBoolean("Azuriom.SemiPremium", false);
    }

    public final BruteForceConfig bruteForceOptions() {
        int tries = cfg.getInt("BruteForce", 3);
        int time = cfg.getInt("BruteForce.BlockTime", 30);

        return new BruteForceConfig(tries, time);
    }

    public final boolean antiBot() {
        return cfg.getBoolean("AntiBot", true);
    }

    public final boolean allowSameIP() {
        return cfg.getBoolean("AllowSameIp", true);
    }

    public final boolean enablePin() {
        return cfg.getBoolean("Pin", true);
    }

    public final boolean enable2FA() {
        return cfg.getBoolean("2FA", true);
    }

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

    public final boolean enableSpawn() {
        return cfg.getBoolean("Spawn.Manage", false);
    }

    public final boolean takeBack() {
        return cfg.getBoolean("Spawn.Back", false);
    }

    public final boolean clearChat() {
        return cfg.getBoolean("ClearChat", false);
    }

    public final int accountsPerIP() {
        return cfg.getInt("AccountsPerIp", 2);
    }

    public final boolean checkNames() {
        return cfg.getBoolean("CheckNames", true);
    }

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
            default:
                return Lang.COMMUNITY;
        }
    }

    public final String getLangName() {
        return cfg.getString("Lang", "en_EN");
    }

    public final AccountSystem getAccountSystem() {
        String str = cfg.getString("AccountSys", "File");
        assert str != null;

        switch (str.toLowerCase()) {
            case "mysql":
            case "sql":
                return AccountSystem.SQL;
            default:
                return AccountSystem.FILE;
        }
    }

    public final String getModulePrefix() {
        String value = cfg.getString("ModulePrefix", "$");
        assert value != null;

        if (value.replaceAll("\\s", "").isEmpty())
            value = "$";

        return value.replaceAll("\\s", "").substring(0, 1);
    }
}
