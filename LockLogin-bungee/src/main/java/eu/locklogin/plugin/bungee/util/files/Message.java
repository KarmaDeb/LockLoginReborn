package eu.locklogin.plugin.bungee.util.files;

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

import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.api.common.karmafile.karmayaml.FileCopy;
import ml.karmaconfigs.api.common.karmafile.karmayaml.KarmaYamlManager;
import ml.karmaconfigs.api.common.karmafile.karmayaml.YamlReloader;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.rgb.RGBTextComponent;
import ml.karmaconfigs.api.common.utils.StringUtils;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bungee.Main;
import eu.locklogin.plugin.bungee.permissibles.Permission;
import eu.locklogin.api.common.utils.plugin.Alias;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static eu.locklogin.plugin.bungee.LockLogin.plugin;

public final class Message {

    private static File msg_file = new File(plugin.getDataFolder() + File.separator + "lang" + File.separator + "v2", "messages_en.yml");
    private static KarmaYamlManager msg = null;

    private static boolean alerted = false;

    /**
     * Initialize messages file
     */
    public Message() {
        if (msg == null) {
            PluginConfiguration config = CurrentPlatform.getConfiguration();

            String country = config.getLang().country(config.getLangName());
            msg_file = new File(plugin.getDataFolder() + File.separator + "lang" + File.separator + "v2", "messages_" + country + ".yml");
            msg = new KarmaYamlManager(msg_file);

            InputStream internal = Main.class.getResourceAsStream("/lang/messages_" + country + ".yml");
            //Check if the file exists inside the plugin as an official language
            if (internal != null) {
                if (!msg_file.exists()) {
                    FileCopy copy = new FileCopy(plugin, "lang/messages_" + country + ".yml");

                    try {
                        copy.copy(msg_file);
                        msg = new KarmaYamlManager(msg_file);
                    } catch (Throwable ignored) {
                    }
                }
            } else {
                if (!msg_file.exists()) {
                    if (!alerted) {
                        Console.send(plugin, "Could not find community message pack named {0} in lang_v2 folder, using messages english as default", Level.GRAVE, msg_file.getName());
                        alerted = true;
                    }

                    msg_file = new File(plugin.getDataFolder() + File.separator + "lang" + File.separator + "v2", "messages_en.yml");

                    if (!msg_file.exists()) {
                        FileCopy copy = new FileCopy(plugin, "lang/messages_en.yml");

                        try {
                            copy.copy(msg_file);
                        } catch (Throwable ignored) {
                        }
                    }

                    msg = new KarmaYamlManager(msg_file);
                }
            }
        }
    }

    public final String prefix() {
        return parse(msg.getString("Prefix", "&eLockLogin &7>> "));
    }

    public final String permissionError(final Permission permission) {
        return parse(msg.getString("PermissionError", "&5&oYou do not have the permission {permission}").replace("{permission}", permission.getName()));
    }

    public final String maxIP() {
        String str = msg.getString("MaxIp", "&5&oMax account per IP reached on the server");

        return parse(str);
    }

    public final String connectionError(final String name) {
        String str = msg.getString("ConnectionError", "&5&oThe player {player} is not online");

        return parse(str.replace("{player}", StringUtils.stripColor(name)));
    }

    public final String infoUsage() {
        String str = msg.getString("PlayerInfoUsage", "&5&oPlease, use /playerinfo <player>");

        return parse(str);
    }

    public final String lookupUsage() {
        String str = msg.getString("LookUpUsage", "&5&oPlease, use /lookup <player>");

        return parse(str);
    }

    public final String altFound(final String name, final int amount) {
        return parse(msg.getString("AltFound", "&5&o{player} could have {alts} alt accounts, type /lookup {player} for more info").replace("{player}", StringUtils.stripColor(name)).replace("{alts}", String.valueOf(amount)));
    }

    public final String neverPlayer(final String name) {
        return parse(msg.getString("NeverPlayed", "&5&oThe player {player} never played on the server").replace("{player}", StringUtils.stripColor(name)));
    }

    public final String targetAccessError(final String name) {
        return parse(msg.getString("TargetAccessError", "&5&oThe player {player} isn't logged in/registered").replace("{player}", StringUtils.stripColor(name)));
    }

    public final String incorrectPassword() {
        return parse(msg.getString("IncorrectPassword", "&5&oThe provided password is not correct!"));
    }

    public final String captcha(String code) {
        String str = msg.getString("Captcha", "&7Your captcha code is: &e{captcha}");

        PluginConfiguration config = CurrentPlatform.getConfiguration();

        if (config.captchaOptions().enableStrike()) {
            if (config.captchaOptions().randomStrike()) {
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

    public final String invalidCaptcha() {
        String str = msg.getString("InvalidCaptcha", "&5&oThe specified captcha code is not valid or correct");

        return parse(str);
    }

    public final String sessionServerDisabled() {
        String str = msg.getString("SessionServerDisabled", "&5&oPersistent sessions are disabled in this server");

        return parse(str);
    }

    public final String sessionEnabled() {
        String str = msg.getString("SessionEnabled", "&dEnabled persistent session for your account ( &e-security&c )");

        return parse(str);
    }

    public final String sessionDisabled() {
        String str = msg.getString("SessionDisabled", "&5&oDisabled persistent session for your account ( &e+security&c )");

        return parse(str);
    }

    public final String register() {
        String str = msg.getString("Register", "&5&oPlease, use /register <password> <password> <captcha>");

        return parse(str.replace("{captcha}", "<captcha>"));
    }

    public final String registerBar(final String color, final int time) {
        String str = msg.getString("RegisterBar", "{color}You have &7{time}{color} to register");

        return parse(str.replace("{color}", color).replace("{time}", String.valueOf(time)));
    }

    public final String registered() {
        String str = msg.getString("Registered", "&dRegister completed, thank for playing");

        return parse(str);
    }

    public final String alreadyRegistered() {
        String str = msg.getString("AlreadyRegistered", "&5&oYou are already registered!");

        return parse(str);
    }

    public final String registerError() {
        String str = msg.getString("RegisterError", "&5&oThe provided passwords does not match!");

        return parse(str);
    }

    public final String passwordInsecure() {
        String str = msg.getString("PasswordInsecure", "&5&oThe specified password is not secure!");

        return parse(str);
    }

    public final String registerTimeOut() {
        String str = msg.getString("RegisterOut", "&5&oYou took too much time to register");

        return parse(str);
    }

    public final String registerTitle(final int time) {
        String str = msg.getString("RegisterTitle", "&7You have");

        return parse(str.replace("{time}", String.valueOf(time)));
    }

    public final String registerSubtitle(final int time) {
        String str = msg.getString("RegisterSubtitle", "&b{time} &7to register");

        return parse(str.replace("{time}", String.valueOf(time)));
    }

    public final String maxRegisters() {
        List<String> messages = msg.getStringList("MaxRegisters");
        StringBuilder builder = new StringBuilder();

        PluginConfiguration config = CurrentPlatform.getConfiguration();
        for (String str : messages) {
            builder.append(str
                    .replace("{ServerName}", config.serverName())).append("\n");
        }

        return parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    public final String login() {
        String str = msg.getString("Login", "&5&oPlease, use /login <password> <captcha>");

        return parse(str.replace("{captcha}", "<captcha>"));
    }

    public final String loginBar(final String color, final int time) {
        String str = msg.getString("LoginBar", "{color}You have &7{time}{color} to login");

        return parse(str.replace("{color}", color).replace("{time}", String.valueOf(time)));
    }

    public final String logged() {
        String str = msg.getString("Logged", "&dLogged-in, welcome back &7{player}");

        return parse(str);
    }

    public final String alreadyLogged() {
        String str = msg.getString("AlreadyLogged", "&5&oYou are already logged!");

        return parse(str);
    }

    public final String loginInsecure() {
        String str = msg.getString("LoginInsecure", "&5&oYour password has been classified as not secure and you must change it");

        return parse(str);
    }

    public final String loginTimeOut() {
        String str = msg.getString("LoginOut", "&5&oYou took too much time to log-in");

        return parse(str);
    }

    public final String loginTitle(final int time) {
        String str = msg.getString("LoginTitle", "&7You have");

        return parse(str.replace("{time}", String.valueOf(time)));
    }

    public final String loginSubtitle(final int time) {
        return parse(msg.getString("LoginSubtitle", "&b{time} &7to login").replace("{time}", String.valueOf(time)));
    }

    public final String pinUsages() {
        return parse(msg.getString("PinUsages", "&5&oValid pin sub-arguments: &e<setup>&7, &e<remove>&7, &e<change>"));
    }

    public final String pinSet() {
        String str = msg.getString("PinSet", "&dYour pin has been set successfully");

        return parse(str);
    }

    public final String pinReseted() {
        String str = msg.getString("PinReseted", "&5&oPin removed, your account is now less secure");

        return parse(str);
    }

    public final String pinChanged() {
        String str = msg.getString("PinChanged", "&dYour pin has been changed successfully");

        return parse(str);
    }

    public final String alreadyPin() {
        String str = msg.getString("AlreadyPin", "&5&oYou already have set your pin!");

        return parse(str);
    }

    public final String noPin() {
        String str = msg.getString("NoPin", "&5&oYou don't have a pin!");

        return parse(str);
    }

    public final String setPin() {
        String str = msg.getString("SetPin", "&5&oPlease, use /pin setup <pin>");

        return parse(str);
    }

    public final String resetPin() {
        String str = msg.getString("ResetPin", "&5&oPlease, use /pin reset <pin>");

        return parse(str);
    }

    public final String changePin() {
        String str = msg.getString("ChangePin", "&5&oPlease, use /pin change <pin> <new pin>");

        return parse(str);
    }

    public final String pinDisabled() {
        String str = msg.getString("PinDisabled", "&5&oPins are disabled");

        return parse(str);
    }

    public final String incorrectPin() {
        String str = msg.getString("IncorrectPin", "&5&oThe specified pin is not correct!");

        return parse(str);
    }

    public final String gAuthUsages() {
        String str = msg.getString("2FaUsages", "&5&oValid 2FA sub-arguments: &e<setup>&7, &e<remove>&7, &e<2fa code>");

        return parse(str);
    }

    public final String gAuthSetupUsage() {
        String str = msg.getString("2FaSetupUsage", "&5&oPlease, use /2fa setup <password>");

        return parse(str);
    }

    public final String gAuthRemoveUsage() {
        String str = msg.getString("2FaRemoveUsage", "&5&oPlease, use /2fa remove <password> <2fa code>");

        return parse(str);
    }

    public final String gAuthenticate() {
        String str = msg.getString("2FaAuthenticate", "&5&oPlease, use /2fa to start playing");

        return parse(str);
    }

    public final String gAuthCorrect() {
        String str = msg.getString("2FaLogged", "&d2FA code validated");

        return parse(str);
    }

    public final String gAuthAlready() {
        String str = msg.getString("2FaAlreadyLogged", "&5&oYou are already authenticated with 2FA!");

        return parse(str);
    }

    public final String gAuthIncorrect() {
        String str = msg.getString("2FaIncorrect", "&5&oIncorrect 2FA code");

        return parse(str);
    }

    public final String gAuthSetupAlready() {
        String str = msg.getString("2FaAlready", "&5&oYou already have setup your 2FA!");

        return parse(str);
    }

    public final String gAuthToggleError() {
        String str = msg.getString("ToggleFAError", "&5&oError while trying to toggle 2FA ( incorrect password/code )");

        return parse(str);
    }

    public final String gAuthDisabled() {
        String str = msg.getString("Disabled2FA", "&5&o2FA disabled, your account is now less secure");

        return parse(str);
    }

    public final String gAuthEnabled() {
        String str = msg.getString("Enabled2FA", "&d2FA enabled, your account is secure again");

        return parse(str);
    }

    public final String gAuthNotEnabled() {
        String str = msg.getString("2FaAccountDisabled", "&5&o2FA is disabled in your account");

        return parse(str);
    }

    public final String gAuthServerDisabled() {
        String str = msg.getString("2FAServerDisabled", "&5&o2FA is currently disabled in this server");

        return parse(str);
    }

    public final String gauthLocked() {
        String str = msg.getString("2FaLocked", "&5&oThis server wants you to have 2FA enabled");

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

        PluginConfiguration config = CurrentPlatform.getConfiguration();

        for (String str : messages)
            builder.append(str
                    .replace("{message}", gAuthLink() + StringUtils.getLastColor(str))
                    .replace("{account}", "{player} (" + config.serverName() + ")")).append("\n");

        return parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    public final String gAuthLink() {
        String str = msg.getString("2FaLink", "&bClick here to get your 2FA QR code");

        return parse(str);
    }

    public final String accountArguments() {
        String str = msg.getString("AccountArguments", "&5&oValid account sub-arguments: &e<change>&7, &e<unlock>&7, &e<close>&7, &e<remove>");

        return parse(str);
    }

    public final String change() {
        String str = msg.getString("Change", "&5&oPlease, use /account change <password> <new password>");

        return parse(str);
    }

    public final String changeSame() {
        String str = msg.getString("ChangeSame", "&5&oYour password can't be the same as old!");

        return parse(str);
    }

    public final String changeDone() {
        String str = msg.getString("ChangeDone", "&dYour password has changed!");

        return parse(str);
    }

    public final String accountUnLock() {
        String str = msg.getString("AccountUnlock", "&5&oPlease, use /account unlock <player>");

        return parse(str);
    }

    public final String accountUnLocked(final String target) {
        String str = msg.getString("AccountUnlocked", "&dAccount of {player} has been unlocked");

        return parse(str.replace("{player}", StringUtils.stripColor(target)));
    }

    public final String accountNotLocked(final String target) {
        String str = msg.getString("AccountNotLocked", "&5&oAccount of {player} is not locked!");

        return parse(str.replace("{player}", StringUtils.stripColor(target)));
    }

    public final String close() {
        String str = msg.getString("Close", "&5&oPlease, use /account close [player]");

        return parse(str);
    }

    public final String closed() {
        String str = msg.getString("Closed", "&5&oSession closed, re-login now!");

        return parse(str);
    }

    public final String forcedClose() {
        String str = msg.getString("ForcedClose", "&5&oYour session have been closed by an admin, login again");

        return parse(str);
    }

    public final String forcedCloseAdmin(final ProxiedPlayer target) {
        String str = msg.getString("ForcedCloseAdmin", "&dSession of {player} closed");

        return parse(str.replace("{player}", StringUtils.stripColor(target.getDisplayName())));
    }

    public final String remove() {
        String str = msg.getString("Remove", "&5&oPlease, use /account remove <password|player> [password]");

        return parse(str);
    }

    public final String removeAccountMatch() {
        String str = msg.getString("RemoveAccountMatch", "&5&oThe provided passwords does not match");

        return parse(str);
    }

    public final String accountRemoved() {
        String str = msg.getString("AccountRemoved", "&5&oYour account have been deleted");

        return parse(str);
    }

    public final String forcedAccountRemoval(final String administrator) {
        List<String> messages = msg.getStringList("ForcedAccountRemoval");
        StringBuilder builder = new StringBuilder();

        for (String str : messages)
            builder.append(str
                    .replace("{player}", StringUtils.stripColor(administrator))).append("\n");

        return parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    public final String forcedAccountRemovalAdmin(final String target) {
        String str = msg.getString("ForcedAccountRemovalAdmin", "&dAccount of {player} removed, don't forget to run /account unlock {player}!");

        return parse(str.replace("{player}", StringUtils.stripColor(target)));
    }

    public final String alias() {
        String str = msg.getString("AliasArguments", "&5&oValid alias sub-arguments: &e<create>&7, &e<destroy>&7, &e<add>&7, &e<remove> [alias] [player(s)]");

        return parse(str);
    }

    public final String aliasCreated(final Alias alias) {
        String str = msg.getString("AliasCreated", "&dAlias {alias} created successfully");

        return parse(str.replace("{alias}", alias.getName()));
    }

    public final String aliasDestroyed(final Alias alias) {
        String str = msg.getString("AliasDestroyed", "&5&oAlias {alias} has been destroyed");

        return parse(str.replace("{alias}", alias.getName()));
    }

    public final String aliasExists(final Alias alias) {
        String str = msg.getString("AliasExists", "&5&oAlias {alias} already exists!");

        return parse(str.replace("{alias}", alias.getName()));
    }

    public final String aliasNotFound(final String alias) {
        String str = msg.getString("AliasNotFound", "&5&oCouldn't find any alias called {alias}");

        return parse(str.replace("{alias}", StringUtils.stripColor(alias.toUpperCase().replace(" ", "_"))));
    }

    public final String addedPlayer(final Alias alias, final String... players) {
        String str = msg.getString("AddedPlayer", "&dAdded {player} to {alias}");

        StringBuilder builder = new StringBuilder();
        for (String player : players)
            builder.append(player).append(", ");

        return parse(str
                .replace("{player}",
                        StringUtils.replaceLast(builder.toString(), ", ", ""))
                .replace("{alias}", alias.getName()));
    }

    public final String removedPlayer(final Alias alias, final String... players) {
        String str = msg.getString("RemovedPlayer", "&dRemoved {player} from {alias}");

        StringBuilder builder = new StringBuilder();
        for (String player : players)
            builder.append(player).append(", ");

        return parse(str
                .replace("{player}",
                        StringUtils.replaceLast(builder.toString(), ", ", ""))
                .replace("{alias}", alias.getName()));
    }

    public final String playerNotIn(final Alias alias, final String... players) {
        String str = msg.getString("PlayerNotIn", "&5&o{player} is not in {alias}!");

        StringBuilder builder = new StringBuilder();
        for (String player : players)
            builder.append(player).append(", ");

        return parse(str
                .replace("{player}",
                        StringUtils.replaceLast(builder.toString(), ", ", ""))
                .replace("{alias}", alias.getName()));
    }

    public final String playerAlreadyIn(final Alias alias, final String... players) {
        String str = msg.getString("PlayerAlreadyIn", "&5&o{player} is already in {alias}!");

        StringBuilder builder = new StringBuilder();
        for (String player : players)
            builder.append(player).append(", ");

        return parse(str
                .replace("{player}",
                        StringUtils.replaceLast(builder.toString(), ", ", ""))
                .replace("{alias}", alias.getName()));
    }

    public final String ipBlocked(final long time) {
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
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        RGBTextComponent component = new RGBTextComponent(true, true);
        return component.parse(original.replace("{ServerName}", config.serverName()));
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
            PluginConfiguration config = CurrentPlatform.getConfiguration();

            String country = config.getLang().country(config.getLangName());
            msg_file = new File(plugin.getDataFolder() + File.separator + "lang" + File.separator + "v2", "messages_" + country + ".yml");

            InputStream internal = Main.class.getResourceAsStream("/lang/messages_" + country + ".yml");
            //Check if the file exists inside the plugin as an official language
            if (internal != null) {
                if (!msg_file.exists()) {
                    FileCopy copy = new FileCopy(plugin, "lang/messages_" + country + ".yml");

                    try {
                        copy.copy(msg_file);
                        msg = new KarmaYamlManager(msg_file);
                    } catch (Throwable ignored) {}

                    return true;
                } else {
                    YamlReloader reloader = msg.getReloader();
                    if (reloader != null) {
                        reloader.reload();
                        return true;
                    }

                    return false;
                }
            } else {
                if (!msg_file.exists()) {
                    if (!alerted) {
                        Console.send(plugin, "Could not find community message pack named {0} in lang_v2 folder, using messages english as default", Level.GRAVE, msg_file.getName());
                        alerted = true;
                    }

                    msg_file = new File(plugin.getDataFolder() + File.separator + "lang" + File.separator + "v2", "messages_en.yml");

                    if (!msg_file.exists()) {
                        FileCopy copy = new FileCopy(plugin, "lang/messages_en.yml");

                        try {
                            copy.copy(msg_file);
                        } catch (Throwable ignored) {
                        }
                    }

                    try {
                        msg = new KarmaYamlManager(msg_file);
                    } catch (Throwable ignored) {}
                } else {
                    YamlReloader reloader = msg.getReloader();
                    if (reloader != null) {
                        reloader.reload();
                        return true;
                    }

                    return false;
                }
            }

            return false;
        }

        /**
         * Parse the yaml file into a string, read-able by
         * spigot
         */
        static String getMessages() {
            return msg.toString();
        }
    }
}
