package ml.karmaconfigs.locklogin.plugin.bukkit.util.files.messages;

import ml.karmaconfigs.api.bukkit.Console;
import ml.karmaconfigs.api.bukkit.karmayaml.FileCopy;
import ml.karmaconfigs.api.bukkit.karmayaml.YamlReloader;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.account.AccountID;
import ml.karmaconfigs.locklogin.plugin.bukkit.Main;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.configuration.Config;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.*;

public final class Message {

    private static File msg_file = new File(plugin.getDataFolder() + File.separator + "lang_v2", "messages_en.yml");
    private static YamlConfiguration msg = YamlConfiguration.loadConfiguration(msg_file);

    private static boolean alerted = false;

    /**
     * Initialize messages file
     */
    public Message() {
        Config config = new Config();

        String country = config.getLang().country(config.getLangName());
        msg_file = new File(plugin.getDataFolder() + File.separator + "lang_v2", "messages_" + country + ".yml");

        InputStream internal = Main.class.getResourceAsStream("/lang/messages_" + country + ".yml");
        //Check if the file exists inside the plugin as an official language
        if (internal != null) {
            if (!msg_file.exists()) {
                FileCopy copy = new FileCopy(plugin, "messages_" + country + ".yml");

                try {
                    copy.copy(msg_file);
                    msg = YamlConfiguration.loadConfiguration(msg_file);
                } catch (Throwable ignored) {}
            }
        } else {
            if (!msg_file.exists()) {
                if (!alerted) {
                    Console.send(plugin, "Could not find community message pack named {0} in lang_v2 folder, using messages english as default", Level.GRAVE, msg_file.getName());
                    alerted = true;
                }

                msg_file = new File(plugin.getDataFolder() + File.separator + "lang_v2", "messages_en.yml");

                if (!msg_file.exists()) {
                    FileCopy copy = new FileCopy(plugin, "lang/messages_en.yml");

                    try {
                        copy.copy(msg_file);
                    } catch (Throwable ignored) {
                    }
                }

                msg = YamlConfiguration.loadConfiguration(msg_file);
            }
        }
    }

    /**
     * Get the messages manager
     */
    public interface manager {

        /**
         * Tries to reload messages file
         *
         * @return if the messages file could be reloaded
         */
        static boolean reload() {
            Config config = new Config();

            String country = config.getLang().country(config.getLangName());
            msg_file = new File(plugin.getDataFolder() + File.separator + "lang_v2", "messages_" + country + ".yml");

            InputStream internal = Main.class.getResourceAsStream("/lang/messages_" + country + ".yml");
            //Check if the file exists inside the plugin as an official language
            if (internal != null) {
                if (!msg_file.exists()) {
                    FileCopy copy = new FileCopy(plugin, "messages_" + country + ".yml");

                    try {
                        copy.copy(msg_file);
                        msg = YamlConfiguration.loadConfiguration(msg_file);
                    } catch (Throwable ignored) {}

                    return true;
                } else {
                    try {
                        YamlReloader reloader = new YamlReloader(plugin, msg_file, "lang/messages_" + country + ".yml");
                        if (reloader.reloadAndCopy()) {
                            msg.loadFromString(reloader.getYamlString());
                            return true;
                        }
                    } catch (Throwable ignored) {}
                }
            } else {
                if (!msg_file.exists()) {
                    if (!alerted) {
                        Console.send(plugin, "Could not find community message pack named {0} in lang_v2 folder, using messages english as default", Level.GRAVE, msg_file.getName());
                        alerted = true;
                    }

                    msg_file = new File(plugin.getDataFolder() + File.separator + "lang_v2", "messages_en.yml");

                    if (!msg_file.exists()) {
                        FileCopy copy = new FileCopy(plugin, "lang/messages_en.yml");

                        try {
                            copy.copy(msg_file);
                        } catch (Throwable ignored) {
                        }
                    }

                    msg = YamlConfiguration.loadConfiguration(msg_file);
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
            Config config = new Config();
            if (config.isBungeeCord()) {
                try {
                    msg.loadFromString(yaml);
                } catch (Throwable ex) {
                    try {
                        Path tmp_file = Files.createTempFile(plugin.getDataFolder().toPath(), "bungeecord", "messages");
                        Files.write(tmp_file, yaml.getBytes(StandardCharsets.UTF_8), StandardOpenOption.DSYNC);
                        File file = tmp_file.toFile();

                        msg = YamlConfiguration.loadConfiguration(file);
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
    }

    public final String prefix() {
        String str = msg.getString("Prefix", "&eLockLogin &7>> ");
        assert str != null;

        return parse(str);
    }

    public final String permissionError(final Permission permission) {
        String str = msg.getString("PermissionError", "&5&oYou do not have the permission {permission}");
        assert str != null;

        return parse(str.replace("{permission}", permission.getName()));
    }

    public final String bungeeProxy() {
        String str = msg.getString("BungeeProxy", "&5&oPlease, connect through bungee proxy!");
        assert str != null;

        return parse(str);
    }

    public final String notVerified() {
        String str = msg.getString("PlayerNotVerified", "&5&oYou can't fight against {player} while he's not logged/registered");
        assert str != null;

        return parse(str);
    }

    public final String alreadyPlaying() {
        String str = msg.getString("AlreadyPlaying", "&5&oThat player is already playing");
        assert str != null;

        return parse(str);
    }

    public final String minChar() {
        String str = msg.getString("PasswordMinChar", "&5&oPassword must have 4 chars min");
        assert str != null;

        return parse(str);
    }

    public final String maxIP() {
        String str = msg.getString("MaxIp", "&5&oMax account per IP reached on the server");
        assert str != null;

        return parse(str);
    }

    public final String connectionError(final String name) {
        String str = msg.getString("ConnectionError", "&5&oThe player {player} is not online");
        assert str != null;

        return parse(str.replace("{player}", name));
    }

    public final String infoUsage() {
        String str = msg.getString("PlayerInfoUsage", "&5&oPlease, use /playerinfo <player>");
        assert str != null;

        return parse(str);
    }

    public final String lookupUsage(){
        String str = msg.getString("LookUpUsage", "&5&oPlease, use /lookup <player>");
        assert str != null;

        return parse(str);
    }

    public final String altFound(final Player player, final int amount) {
        String str = msg.getString("AltFound", "&5&o{player} could have {alts} alt accounts, type /lookup {player} for more info");
        assert str != null;

        return parse(str.replace("{player}", StringUtils.stripColor(player.getDisplayName())).replace("{alts}", String.valueOf(amount)));
    }

    public final String neverPlayer(final String name) {
        String str = msg.getString("NeverPlayed", "&5&oThe player {player} never played on the server");
        assert str != null;

        return parse(str.replace("{player}", name));
    }

    public final String targetAccessError(final String name) {
        String str = msg.getString("TargetAccessError", "&5&oThe player {player} isn't logged in/registered");
        assert str != null;

        return parse(str.replace("{player}", name));
    }

    public final String incorrectPassword() {
        String str = msg.getString("IncorrectPassword", "&5&oThe provided password is not correct!");
        assert str != null;

        return parse(str);
    }

    public final String onlyAzuriom() {
        List<String> messages = msg.getStringList("OnlyAzuriom");
        StringBuilder builder = new StringBuilder();

        Config config = new Config();
        for (String str : messages) {
            builder.append(str
                    .replace("{ServerName}", config.serverName())).append("\n");
        }

        return parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    public final String captchaMessage(String code) {
        String str = msg.getString("Captcha", "&7Your captcha code is: &e{captcha}");
        assert str != null;

        Config cfg = new Config();

        if (cfg.captchaOptions().enableStrike()) {
            if (cfg.captchaOptions().randomStrike()) {
                String last_color = StringUtils.getLastColor(str);

                StringBuilder builder = new StringBuilder();

                for (int i = 0; i < code.length(); i++) {
                    int random = new Random().nextInt(100);

                    if (random > 50) {
                        builder.append(last_color).append("&m").append(code.charAt(i)).append("&r");
                    } else {
                        builder.append(last_color).append(code.charAt(i)).append("&r");
                    }
                }

                code = builder.toString();
            } else {
                code = "&m" + code;
            }
        }

        return parse(str.replace("{captcha}", code));
    }

    public final String typeCaptcha() {
        String str = msg.getString("TypeCaptcha", "&5&oPlease, use /captcha <captcha>");
        assert str != null;

        return parse(str.replace("{captcha}", "<captcha>"));
    }

    public final String captchaTimeout() {
        String str = msg.getString("CaptchaTimeOut", "&5&oYou took too much time to write captcha!");
        assert str != null;

        return parse(str);
    }

    public final String specifyCaptcha() {
        String str = msg.getString("SpecifyCaptcha", "&5&oPlease provide a valid captcha code");
        assert str != null;

        return parse(str);
    }

    public final String captchaValidated() {
        String str = msg.getString("CaptchaValidated", "&dCaptcha code validated");
        assert str != null;

        return parse(str);
    }

    public final String invalidCaptcha() {
        String str = msg.getString("InvalidCaptcha", "&5&oThe specified captcha code is not valid or correct");
        assert str != null;

        return parse(str);
    }

    public final String alreadyCaptcha() {
        String str = msg.getString("AlreadyCaptcha", "&5&oYour captcha code is already verified!");
        assert str != null;

        return parse(str);
    }

    public final String register() {
        String str = msg.getString("Register", "&5&oPlease, use /register <password> <password> <captcha>");
        assert str != null;

        return parse(str.replace("{captcha}", "<captcha>"));
    }

    public final String registered() {
        String str = msg.getString("Registered", "&dRegister completed, thank for playing");
        assert str != null;

        return parse(str);
    }

    public final String alreadyRegistered() {
        String str = msg.getString("AlreadyRegistered", "&5&oYou are already registered!");
        assert str != null;

        return parse(str);
    }

    public final String registerError() {
        String str = msg.getString("RegisterError", "&5&oThe provided passwords does not match!");
        assert str != null;

        return parse(str);
    }

    public final String passwordInsecure() {
        String str = msg.getString("PasswordInsecure", "&5&oThe specified password is not secure!");
        assert str != null;

        return parse(str);
    }

    public final String registerTimeOut() {
        String str = msg.getString("RegisterOut", "&5&oYou took too much time to register");
        assert str != null;

        return parse(str);
    }

    public final String registerTitle(final int time) {
        String str = msg.getString("RegisterTitle", "&7You have");
        assert str != null;

        return parse(str.replace("{time}", String.valueOf(time)));
    }

    public final String registerSubtitle(final int time) {
        String str = msg.getString("RegisterSubtitle", "&b{time} &7to register");
        assert str != null;

        return parse(str.replace("{time}", String.valueOf(time)));
    }

    public final String maxRegisters() {
        List<String> messages = msg.getStringList("MaxRegisters");
        StringBuilder builder = new StringBuilder();

        Config config = new Config();
        for (String str : messages) {
            builder.append(str
                    .replace("{ServerName}", config.serverName())).append("\n");
        }

        return parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    public final String login() {
        String str = msg.getString("Login", "&5&oPlease, use /login <password> <captcha>");
        assert str != null;

        return parse(str.replace("{captcha}", "<captcha>"));
    }

    public final String logged() {
        String str = msg.getString("Logged", "&dLogged-in, welcome back &7{player}");
        assert str != null;

        return parse(str);
    }

    public final String alreadyLogged() {
        String str = msg.getString("AlreadyLogged", "&5&oYou are already logged!");
        assert str != null;

        return parse(str);
    }

    public final String loginInsecure() {
        String str = msg.getString("LoginInsecure", "&5&oYour password has been classified as not secure and you must change it");
        assert str != null;

        return parse(str);
    }

    public final String loginTimeOut() {
        String str = msg.getString("LoginOut", "&5&oYou took too much time to log-in");
        assert str != null;

        return parse(str);
    }

    public final String loginTitle(final int time) {
        String str = msg.getString("LoginTitle", "&7You have");
        assert str != null;

        return parse(str.replace("{time}", String.valueOf(time)));
    }

    public final String loginSubtitle(final int time) {
        String str = msg.getString("LoginSubtitle", "&b{time} &7to login");
        assert str != null;

        return parse(str.replace("{time}", String.valueOf(time)));
    }

    public final String pinTitle() {
        String str = msg.getString("PinTitle", "&eLockLogin pinner");
        assert str != null;

        return parse(str);
    }

    public final String pinSet() {
        String str = msg.getString("PinSet", "&dYour pin has been set successfully");
        assert str != null;

        return parse(str);
    }

    public final String alreadyPin() {
        String str = msg.getString("AlreadyPin", "&5&oYou already have set your pin!");
        assert str != null;

        return parse(str);
    }

    public final String noPin() {
        String str = msg.getString("NoPin", "&5&oYou don't have a pin!");
        assert str != null;

        return parse(str);
    }

    public final String pinUsage() {
        String str = msg.getString("SetPin", "&5&oPlease, use /pin setup <pin>");
        assert str != null;

        return parse(str);
    }

    public final String resetPin() {
        String str = msg.getString("ResetPin", "&5&oPlease, use /pin reset <pin>");
        assert str != null;

        return parse(str);
    }

    public final String pinDisabled() {
        String str = msg.getString("PinDisabled", "&5&oPins are disabled");
        assert str != null;

        return parse(str);
    }

    public final String pinLength() {
        String str = msg.getString("PinLength", "&5&oPin must have 4 digits");
        assert str != null;

        return parse(str);
    }

    public final String incorrectPin() {
        String str = msg.getString("IncorrectPin", "&5&oThe specified pin is not correct!");
        assert str != null;

        return parse(str);
    }

    public final String gAuthUsages() {
        String str = msg.getString("2FaUsages", "&5&oValid 2FA sub-arguments: &e<setup>&7, &e<remove>&7, &e<2fa code>");
        assert str != null;

        return parse(str);
    }

    public final String gAuthSetupUsage() {
        String str = msg.getString("2FaSetupUsage", "&5&oPlease, use /2fa setup <password>");
        assert str != null;

        return parse(str);
    }

    public final String gAuthRemoveUsage() {
        String str = msg.getString("2FaRemoveUsage", "&5&oPlease, use /2fa remove <password> <2fa code>");
        assert str != null;

        return parse(str);
    }

    public final String gAuthenticate() {
        String str = msg.getString("2FaAuthenticate", "&5&oPlease, use /2fa to start playing");
        assert str != null;

        return parse(str);
    }

    public final String gAuthCorrect() {
        String str = msg.getString("2FaLogged", "&d2FA code validated");
        assert str != null;

        return parse(str);
    }

    public final String gAuthAlready() {
        String str = msg.getString("2FaAlreadyLogged", "&5&oYou are already authenticated with 2FA!");
        assert str != null;

        return parse(str);
    }

    public final String gAuthIncorrect() {
        String str = msg.getString("2FaIncorrect", "&5&oIncorrect 2FA code");
        assert str != null;

        return parse(str);
    }

    public final String gAuthSetupAlready() {
        String str = msg.getString("2FaAlready", "&5&oYou already have setup your 2FA!");
        assert str != null;

        return parse(str);
    }

    public final String gAuthToggleError() {
        String str = msg.getString("ToggleFAError", "&5&oError while trying to toggle 2FA ( incorrect password/code )");
        assert str != null;

        return parse(str);
    }

    public final String gAuthDisabled() {
        String str = msg.getString("Disabled2FA", "&5&o2FA disabled, your account is now less secure");
        assert str != null;

        return parse(str);
    }

    public final String gAuthEnabled() {
        String str = msg.getString("Enabled2FA", "&d2FA enabled, your account is secure again");
        assert str != null;

        return parse(str);
    }

    public final String gAuthNotEnabled() {
        String str = msg.getString("2FaAccountDisabled", "&5&o2FA is disabled in your account");
        assert str != null;

        return parse(str);
    }

    public final String gAuthServerDisabled() {
        String str = msg.getString("2FAServerDisabled", "&5&o2FA is currently disabled in this server");
        assert str != null;

        return parse(str);
    }

    public final String gAuthScratchCodes(final List<Integer> codes) {
        List<String> messages = msg.getStringList("2FAScratchCodes");
        StringBuilder builder = new StringBuilder();

        for (String str : messages) builder.append(str).append("\n");

        builder.append("{newline}&r ");

        int added = 0;
        for (int i = 0; i < codes.size(); i++) {
            int code = codes.get(i);
            builder.append("&e").append(code).append((added == 2 ? "\n" : (i != codes.size() - 1 ? "&7, " : "")));
            added++;
        }

        return parse(builder.toString());
    }

    public final String gAuthInstructions() {
        List<String> messages = msg.getStringList("2FaInstructions");
        StringBuilder builder = new StringBuilder();

        for (String str : messages) builder.append(str).append("\n");

        return parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    public final String gAuthLink() {
        String str = msg.getString("2FaLink", "&bClick here to get your 2FA QR code");
        assert str != null;

        return parse(str);
    }

    public final String changePassword() {
        String str = msg.getString("ChangePass", "&5&oPlease, use /{arg} <password> <new password>");
        assert str != null;

        return parse(str);
    }

    public final String changeSame() {
        String str = msg.getString("ChangeSame", "&5&oYour password can't be the same as old!");
        assert str != null;

        return parse(str);
    }

    public final String changeDone() {
        String str = msg.getString("ChangeDone", "&dYour password has changed!");
        assert str != null;

        return parse(str);
    }

    public final String unLogUsage() {
        String str = msg.getString("UnLog", "&5&oPlease, use /{arg}");
        assert str != null;

        return parse(str);
    }

    public final String unLogged() {
        String str = msg.getString("UnLogged", "&5&oSession closed, re-login now!");
        assert str != null;

        return parse(str);
    }

    public final String forcedUnLog() {
        String str = msg.getString("ForcedUnLog", "&5&oYour session have been closed by an admin, login again");
        assert str != null;

        return parse(str);
    }

    public final String forcedUnLogAdmin(final Player target) {
        String str = msg.getString("ForcedUnLogAdmin", "&dSession of {player} closed");
        assert str != null;

        return parse(str.replace("{player}", StringUtils.stripColor(target.getDisplayName())));
    }

    public final String delAccountUsage(final String argument) {
        String str = msg.getString("DelAccount", "&5&oPlease, use /{arg} <password|player> <password>");
        assert str != null;

        return parse(str.replace("{arg}", argument));
    }

    public final String delAccountMatch() {
        String str = msg.getString("DelAccountMatch", "&5&oThe provided passwords does not match");
        assert str != null;

        return parse(str);
    }

    public final String accountDeleted() {
        String str = msg.getString("AccountDeleted", "&5&oYour account have been deleted");
        assert str != null;
        
        return parse(str);
    }

    public final String forcedDelAccount(final String administrator) {
        List<String> messages = msg.getStringList("ForcedDelAccount");
        StringBuilder builder = new StringBuilder();

        for (String str : messages) builder.append(str
                .replace("{player}", administrator)).append("\n");

        return parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    public final String forcedDelAccountAdmin(final String target) {
        String str = msg.getString("ForcedDelAccountAdmin", "&dAccount of {player} removed");
        assert str != null;

        return parse(str.replace("{player}", target));
    }

    public final String accountUnLock() {
        String str = msg.getString("AccountUnlock", "&5&oPlease, use /unblock <player>");
        assert str != null;

        return parse(str);
    }

    public final String accountUnLocked(final String target) {
        String str = msg.getString("AccountUnlocked", "&dAccount of {player} has been unlocked");
        assert str != null;

        return parse(str.replace("{player}", target));
    }

    public final String accountNotLocked(final String target) {
        String str = msg.getString("AccountNotLocked", "&5&oAccount of {player} is not locked!");
        assert str != null;

        return parse(str.replace("{player}", target));
    }

    public final String spawnSet() {
        String str = msg.getString("SpawnSet", "&dThe login spawn location have been set");
        assert str != null;
        
        return parse(str);
    }
    
    public final String locationsReset() {
        String str = msg.getString("LocationsReset", "&dAll last locations have been reset");
        assert str != null;
        
        return parse(str);
    }

    public final String locationReset(final String name) {
        String str = msg.getString("LocationReset", "&dLast location of {player} has been reset");
        assert str != null;

        return parse(str.replace("{player}", name));
    }
    
    public final String locationsFixed() {
        String str = msg.getString("LocationsFixed", "&dAll last locations have been fixed");
        assert str != null;
        
        return parse(str);
    }
    
    public final String locationFixed(final String name) {
        String str = msg.getString("LocationFixed", "&dLocation of {player} has been fixed");
        assert str != null;

        return parse(str.replace("{player}", name));
    }

    public final String noLastLocation(final String name) {
        String str = msg.getString("NoLastLocation", "&5&oThe player {player} doesn't has last location");
        assert str != null;

        return parse(str.replace("{player}", name));
    }

    public final String resetLocUsage() {
        String str = msg.getString("RestLastLocUsage", "&5&oPlease, use /locations [player|@all|@me] <remove|fix>");
        assert str != null;
        
        return parse(str);
    }

    public final String migrating(final AccountID id, final String type) {
        String str = msg.getString("Migrating", "&dMigrating account of {player} to {type}");
        assert str != null;

        return parse(str.replace("{player}", id.getId()).replace("{type}", type));
    }

    public final String migratingAll(final String type) {
        String str = msg.getString("MigratingAll", "&dMigrating accounts of everyone to {type}");
        assert str != null;

        return parse(str.replace("{type}", type));
    }

    public final String migrated() {
        String str = msg.getString("Migrated", "&dAccount(s) migration ended");
        assert str != null;
        
        return parse(str);
    }

    public final String migrationUsage() {
        String str = msg.getString("MigrationUsage", "&5&oPlease, use /migrate");
        assert str != null;
        
        return parse(str);
    }

    public final String migrationError(final String type) {
        String str = msg.getString("MigrationConnectionError", "&5&oThere was an error while trying to connect to {type}");
        assert str != null;

        return parse(str.replace("{type}", type));
    }

    public final String ipBlocked(final int time) {
        List<String> messages = msg.getStringList("IpBlocked");
        StringBuilder builder = new StringBuilder();

        long seconds = TimeUnit.SECONDS.toSeconds(time);
        long minutes = TimeUnit.SECONDS.toMinutes(time);
        long hours = TimeUnit.SECONDS.toHours(time);

        String format;
        long final_time;
        if (seconds <= 59) {
            format = "sec(s)";
            final_time = seconds;
        } else {
            if (minutes <= 59) {
                format = "min(s) and " + Math.abs((minutes * 60) - seconds) + " sec(s)";
                final_time = minutes;
            } else {
                format = "hour(s) " + Math.abs((hours * 60) - minutes) + " min(s)";
                final_time = hours;
            }
        }

        for (String str : messages) {
            builder.append(str
                    .replace("{time}", String.valueOf(final_time))
                    .replace("{time_format}", format)).append("\n");
        }

        return parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    public final String antiBot() {
        List<String> messages = msg.getStringList("AntiBot");
        StringBuilder builder = new StringBuilder();

        for (String str : messages) builder.append(str).append("\n");

        return parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    public final String illegalName(final String chars) {
        List<String> messages = msg.getStringList("AntiBot");
        StringBuilder builder = new StringBuilder();

        for (String str : messages) builder.append(str.replace("{chars}", chars)).append("\n");

        return parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    public final String uuidFetchError() {
        List<String> messages = msg.getStringList("UUIDFetchError");
        StringBuilder builder = new StringBuilder();

        for (String str : messages) builder.append(str).append("\n");

        return parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    public final String ipProxyError() {
        List<String> messages = msg.getStringList("IpProxyError");
        StringBuilder builder = new StringBuilder();

        for (String str : messages)
            builder.append(str).append("\n");

        return parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }
    
    public final String parse(final String original) {
        Config config = new Config();
        
        return original.replace("{ServerName}", config.serverName());
    }
}
