package eu.locklogin.api.file;

import eu.locklogin.api.file.options.PasswordConfig;
import eu.locklogin.api.file.pack.Alias;
import eu.locklogin.api.file.pack.PluginProperties;
import eu.locklogin.api.module.plugin.client.permission.PermissionObject;
import eu.locklogin.api.module.plugin.javamodule.sender.ModuleSender;
import eu.locklogin.api.security.PasswordAttribute;
import eu.locklogin.api.util.enums.CheckType;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.karma.file.yaml.FileCopy;
import ml.karmaconfigs.api.common.karma.file.yaml.KarmaYamlManager;
import ml.karmaconfigs.api.common.karma.file.yaml.YamlReloader;
import ml.karmaconfigs.api.common.karma.source.APISource;
import ml.karmaconfigs.api.common.karma.source.KarmaSource;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.utils.enums.Level;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public abstract class PluginMessages {

    private static final PluginProperties properties = new PluginProperties();
    private static KarmaYamlManager msg = null;
    private static boolean alerted = false;

    /**
     * Initialize the plugin messages
     *
     * @param source the plugin messages source
     */
    public PluginMessages(final KarmaSource source) {
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        String country = config.getLang().country(config.getLangName());
        msg = new KarmaYamlManager(source, "messages_" + country, "lang", "v2");

        Object kymSource = msg.getSourceRoot().getSource();
        if (kymSource instanceof File || kymSource instanceof Path) {
            File msg_file;
            if (kymSource instanceof File) {
                msg_file = (File) kymSource;
            } else {
                msg_file = ((Path) kymSource).toFile();
            }

            if (!msg_file.exists()) {
                if (!alerted) {
                    APISource.loadProvider("LockLogin").console().send("Could not find community message pack named {0} in lang_v2 folder, using messages english as default", Level.GRAVE, msg_file.getName());
                    alerted = true;
                }

                msg_file = new File(source.getDataPath().toFile() + File.separator + "lang" + File.separator + "v2", "messages_en.yml");

                if (!msg_file.exists()) {
                    FileCopy copy = new FileCopy(source, "lang/messages_en.yml");

                    try {
                        copy.copy(msg_file);
                    } catch (Throwable ignored) {
                    }
                }

                msg = new KarmaYamlManager(msg_file);
            }
        }
    }

    /**
     * Reload the messages file
     */
    public boolean reload() {
        try {
            if (msg != null) {
                YamlReloader reloader = msg.getReloader();
                if (reloader != null) {
                    reloader.reload();
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }

        return false;
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String prefix() {
        return parse(msg.getString("Prefix", "&eLockLogin &7>> "));
    }

    /**
     * Get a plugin message
     *
     * @param permission message replace
     * @return plugin message
     */
    public String permissionError(final Object permission) {
        Method getName = null;
        try {
            getName = permission.getClass().getMethod("getName");
        } catch (Throwable ex) {
            try {
                getName = permission.getClass().getDeclaredMethod("getName");
            } catch (Throwable ignored) {
            }
        }

        if (getName != null && String.class.isAssignableFrom(getName.getReturnType())) {
            String name = "\"Failed to invoke permission getName method\"";
            try {
                name = (String) getName.invoke(permission);
            } catch (Throwable ignored) {
            }

            return parse(msg.getString("PermissionError", "&5&oYou do not have the permission {permission}").replace("{permission}", name));
        } else {
            return parse(msg.getString("PermissionError", "&5&oYou do not have the permission {permission}").replace("{permission}", "\"permission object doesn't has getName method\""));
        }
    }

    /**
     * Get a plugin message for modules
     *
     * @param permission message replace
     * @return plugin module message
     */
    public String permissionError(final PermissionObject permission) {
        return parse(msg.getString("PermissionError", "&5&oYou do not have the permission {permission}").replace("{permission}", permission.getPermission()));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public final String bungeeProxy() {
        return parse(msg.getString("BungeeProxy", "&5&oPlease, connect through bungee proxy!"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public final String pinTitle() {
        String str = msg.getString("PinTitle", "&eLockLogin pinner");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public final String altTitle() {
        String str = msg.getString("AltTitle", "&8&lAlt accounts lookup");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public final String infoTitle() {
        String str = msg.getString("InfoTitle", "&8&lBundled player info");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public final String nextButton() {
        String str = msg.getString("Next", "&eNext");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public final String backButton() {
        String str = msg.getString("Back", "&eBack");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public final String notVerified(final ModuleSender target) {
        String str = msg.getString("PlayerNotVerified", "&5&oYou can't fight against {player} while he's not logged/registered");

        return parse(str.replace("{player}", StringUtils.stripColor(target.getName())));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public final String alreadyPlaying() {
        String str = msg.getString("AlreadyPlaying", "&5&oThat player is already playing");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @param name message replace
     * @return plugin message
     */
    public String connectionError(final String name) {
        String str = msg.getString("ConnectionError", "&5&oThe player {player} is not online");

        return parse(str.replace("{player}", StringUtils.stripColor(name)));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String infoUsage() {
        String str = msg.getString("PlayerInfoUsage", "&5&oPlease, use /playerinfo <player>");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String lookupUsage() {
        String str = msg.getString("LookUpUsage", "&5&oPlease, use /lookup <player>");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @param name   message replace
     * @param amount message replace
     * @return plugin message
     */
    public String altFound(final String name, int amount) {
        return parse(msg.getString("AltFound", "&5&o{player} could have {alts} alt accounts, type /lookup {player} for more info").replace("{player}", StringUtils.stripColor(name)).replace("{alts}", String.valueOf(amount)));
    }

    /**
     * Get a plugin message
     *
     * @param name message replace
     * @return plugin message
     */
    public String neverPlayer(final String name) {
        return parse(msg.getString("NeverPlayed", "&5&oThe player {player} never played on the server").replace("{player}", StringUtils.stripColor(name)));
    }

    /**
     * Get a plugin message
     *
     * @param name message replace
     * @return plugin message
     */
    public String targetAccessError(final String name) {
        return parse(msg.getString("TargetAccessError", "&5&oThe player {player} isn't logged in/registered").replace("{player}", StringUtils.stripColor(name)));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String incorrectPassword() {
        return parse(msg.getString("IncorrectPassword", "&5&oThe provided password is not correct!"));
    }

    /**
     * Get a plugin message
     *
     * @param code message replace
     * @return plugin message
     */
    public String captcha(String code) {
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

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String invalidCaptcha() {
        String str = msg.getString("InvalidCaptcha", "&5&oThe specified captcha code is not valid or correct");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String sessionServerDisabled() {
        String str = msg.getString("SessionServerDisabled", "&5&oPersistent sessions are disabled in this server");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String sessionEnabled() {
        String str = msg.getString("SessionEnabled", "&dEnabled persistent session for your account ( &e-security&c )");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String sessionDisabled() {
        String str = msg.getString("SessionDisabled", "&5&oDisabled persistent session for your account ( &e+security&c )");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String register() {
        String str = msg.getString("Register", "&5&oPlease, use /register <password> <password> <captcha>");

        return parse(str.replace("{captcha}", "<captcha>"));
    }

    /**
     * Get a plugin message
     *
     * @param color message replace
     * @param time  message replace
     * @return plugin message
     */
    public String registerBar(final String color, long time) {
        String str = msg.getString("RegisterBar", "{color}You have &7{time}{color} to register");

        long minutes = TimeUnit.SECONDS.toMinutes(time);
        long hours = TimeUnit.SECONDS.toHours(time);

        String format;
        if (time <= 59) {
            format = time + " " + StringUtils.stripColor(properties.getProperty("second", "second(s)"));
        } else {
            if (minutes <= 59) {
                format = minutes + " " + StringUtils.stripColor(properties.getProperty("minute", "minute(s)")) + " and " + Math.abs((minutes * 60) - time) + " " + StringUtils.stripColor(properties.getProperty("second_short", "sec(s)"));
            } else {
                format = hours + " " + StringUtils.stripColor(properties.getProperty("hour", "hour(s)")) + Math.abs((hours * 60) - minutes) + " " + StringUtils.stripColor(properties.getProperty("minute_short", "min(s)"));
            }
        }

        return parse(str.replace("{color}", color).replace("{time}", format));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String registered() {
        String str = msg.getString("Registered", "&dRegister completed, thank for playing");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String alreadyRegistered() {
        String str = msg.getString("AlreadyRegistered", "&5&oYou are already registered!");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String registerError() {
        String str = msg.getString("RegisterError", "&5&oThe provided passwords does not match!");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String passwordInsecure() {
        String str = msg.getString("PasswordInsecure", "&5&oThe specified password is not secure!");

        return parse(str);
    }

    public String passwordWarning() {
        String str = msg.getString("PasswordWarning", "&5&oThe client {player} is using an unsafe password");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String registerTimeOut() {
        String str = msg.getString("RegisterOut", "&5&oYou took too much time to register");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @param time message replace
     * @return plugin message
     */
    public String registerTitle(final long time) {
        String str = msg.getString("RegisterTitle", "&7You have");

        long minutes = TimeUnit.SECONDS.toMinutes(time);
        long hours = TimeUnit.SECONDS.toHours(time);

        String format;
        if (time <= 59) {
            format = time + " " + StringUtils.stripColor(properties.getProperty("second", "second(s)"));
        } else {
            if (minutes <= 59) {
                format = minutes + StringUtils.stripColor(properties.getProperty("minute", "minute(s)")) + " and " + Math.abs((minutes * 60) - time) + " " + StringUtils.stripColor(properties.getProperty("second_short", "sec(s)"));
            } else {
                format = hours + StringUtils.stripColor(properties.getProperty("hour", "hour(s)")) + " and " + Math.abs((hours * 60) - minutes) + " " + StringUtils.stripColor(properties.getProperty("minute_short", "min(s)"));
            }
        }

        return parse(str.replace("{time}", format));
    }

    /**
     * Get a plugin message
     *
     * @param time message replace
     * @return plugin message
     */
    public String registerSubtitle(final long time) {
        String str = msg.getString("RegisterSubtitle", "&b{time} &7to register");

        long minutes = TimeUnit.SECONDS.toMinutes(time);
        long hours = TimeUnit.SECONDS.toHours(time);

        String format;
        if (time <= 59) {
            format = time + " " + StringUtils.stripColor(properties.getProperty("second", "second(s)"));
        } else {
            if (minutes <= 59) {
                format = minutes + StringUtils.stripColor(properties.getProperty("minute", "minute(s)")) + " and " + Math.abs((minutes * 60) - time) + " " + StringUtils.stripColor(properties.getProperty("second_short", "sec(s)"));
            } else {
                format = hours + StringUtils.stripColor(properties.getProperty("hour", "hour(s)")) + " and " + Math.abs((hours * 60) - minutes) + " " + StringUtils.stripColor(properties.getProperty("minute_short", "min(s)"));
            }
        }

        return parse(str.replace("{time}", format));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String maxRegisters() {
        List<String> messages = msg.getStringList("MaxRegisters");
        StringBuilder builder = new StringBuilder();

        PluginConfiguration config = CurrentPlatform.getConfiguration();
        for (String str : messages) {
            builder.append(str
                    .replace("{ServerName}", config.serverName())).append("\n");
        }

        return parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String login() {
        String str = msg.getString("Login", "&5&oPlease, use /login <password> <captcha>");

        return parse(str.replace("{captcha}", "<captcha>"));
    }

    /**
     * Get a plugin message
     *
     * @param color message replace
     * @param time  message replace
     * @return plugin message
     */
    public String loginBar(final String color, long time) {
        String str = msg.getString("LoginBar", "{color}You have &7{time}{color} to login");

        long minutes = TimeUnit.SECONDS.toMinutes(time);
        long hours = TimeUnit.SECONDS.toHours(time);

        String format;
        if (time <= 59) {
            format = time + " " + StringUtils.stripColor(properties.getProperty("second", "second(s)"));
        } else {
            if (minutes <= 59) {
                format = minutes + " " + StringUtils.stripColor(properties.getProperty("minute", "minute(s)")) + " and " + Math.abs((minutes * 60) - time) + " " + StringUtils.stripColor(properties.getProperty("second_short", "sec(s)"));
            } else {
                format = hours + " " + StringUtils.stripColor(properties.getProperty("hour", "hour(s)")) + Math.abs((hours * 60) - minutes) + " " + StringUtils.stripColor(properties.getProperty("minute_short", "min(s)"));
            }
        }

        return parse(str.replace("{color}", color).replace("{time}", format));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String logged() {
        String str = msg.getString("Logged", "&dLogged-in, welcome back &7{player}");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String alreadyLogged() {
        String str = msg.getString("AlreadyLogged", "&5&oYou are already logged!");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String loginInsecure() {
        String str = msg.getString("LoginInsecure", "&5&oYour password has been classified as not secure and you must change it");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String loginTimeOut() {
        String str = msg.getString("LoginOut", "&5&oYou took too much time to log-in");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @param time message replace
     * @return plugin message
     */
    public String loginTitle(final long time) {
        String str = msg.getString("RegisterTitle", "&7You have");

        long minutes = TimeUnit.SECONDS.toMinutes(time);
        long hours = TimeUnit.SECONDS.toHours(time);

        String format;
        if (time <= 59) {
            format = time + " " + StringUtils.stripColor(properties.getProperty("second", "second(s)"));
        } else {
            if (minutes <= 59) {
                format = minutes + " " + StringUtils.stripColor(properties.getProperty("minute", "minute(s)")) + " and " + Math.abs((minutes * 60) - time) + " " + StringUtils.stripColor(properties.getProperty("second_short", "sec(s)"));
            } else {
                format = hours + " " + StringUtils.stripColor(properties.getProperty("hour", "hour(s)")) + " and " + Math.abs((hours * 60) - minutes) + " " + StringUtils.stripColor(properties.getProperty("minute_short", "min(s)"));
            }
        }

        return parse(str.replace("{time}", format));
    }

    /**
     * Get a plugin message
     *
     * @param time message replace
     * @return plugin message
     */
    public String loginSubtitle(final long time) {
        String str = msg.getString("LoginSubtitle", "&b{time} &7to login");

        long minutes = TimeUnit.SECONDS.toMinutes(time);
        long hours = TimeUnit.SECONDS.toHours(time);

        String format;
        if (time <= 59) {
            format = time + " " + StringUtils.stripColor(properties.getProperty("second", "second(s)"));
        } else {
            if (minutes <= 59) {
                format = minutes + " " + StringUtils.stripColor(properties.getProperty("minute", "minute(s)")) + " and " + Math.abs((minutes * 60) - time) + " " + StringUtils.stripColor(properties.getProperty("second_short", "sec(s)"));
            } else {
                format = hours + " " + StringUtils.stripColor(properties.getProperty("hour", "hour(s)")) + " and " + Math.abs((hours * 60) - minutes) + " " + StringUtils.stripColor(properties.getProperty("minute_short", "min(s)"));
            }
        }

        return parse(str.replace("{time}", format));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String premiumEnabled() {
        List<String> messages = msg.getStringList("PremiumEnabled");
        StringBuilder builder = new StringBuilder();

        for (String str : messages) builder.append(str).append("\n");
        return parse(builder.toString());
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String premiumDisabled() {
        List<String> messages = msg.getStringList("PremiumDisabled");
        StringBuilder builder = new StringBuilder();

        for (String str : messages) builder.append(str).append("\n");
        return parse(builder.toString());
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String premiumError() {
        String message = msg.getString("PremiumError", "&5&oAn error occurred while disabling/enabling premium in your account");
        return parse(message);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String premiumAuth() {
        String message = msg.getString("PremiumAuth", "&dAuthenticated using premium authentication");
        return parse(message);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String premiumServer() {
        String message = msg.getString("PremiumServer", "&5&lThis server is for premium users only!");
        return parse(message);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String premiumWarning() {
        List<String> messages = msg.getStringList("PremiumWarning");
        StringBuilder builder = new StringBuilder();

        for (String str : messages) builder.append(str).append("\n");
        return parse(builder.toString());
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String premiumFailAuth() {
        String message = msg.getString("PremiumFails.AuthenticationError", "&5&oFailed to validate your connection. Are you authenticated through mojang?");
        return parse(message);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String premiumFailInternal() {
        String message = msg.getString("PremiumFails.InternalError", "&5&oAn internal server error occurred");
        return parse(message);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String premiumFailConnection() {
        String message = msg.getString("PremiumFails.ConnectionError", "&5&oFailed to validate your connection with this server");
        return parse(message);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String premiumFailAddress() {
        String message = msg.getString("AddressError", "&5&oFailed to authenticate your address");
        return parse(message);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String premiumFailEncryption() {
        String message = msg.getString("PremiumFails.EncryptionError", "&5&oInvalid encryption provided");
        return parse(message);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String premiumFailPrecocious() {
        String message = msg.getString("PremiumFails.PrecociousEncryption", "&5&oTried to authenticate before the server does!");
        return parse(message);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String premiumFailSession() {
        String message = msg.getString("PremiumFails.SessionError", "&5&oInvalid session");
        return parse(message);
    }

    /**
     * Get a plugin message
     *
     * @param type message type
     * @param result message argument
     * @return plugin message
     */
    public String checkSuccess(final CheckType type, final PasswordAttribute result) {
        String str = "";
        switch (type) {
            case UNIQUE:
                str = msg.getString("Password.Success.Unique", "&e- &aYour password is mostly unique");
                break;
            case LENGTH:
                str = msg.getString("Password.Success.Length", "&e- &aYour password has at least {min_length} characters");
                break;
            case SPECIAL:
                str = msg.getString("Password.Success.Special", "&e- &aYour password has at least {min_special} special characters");
                break;
            case NUMBER:
                str = msg.getString("Password.Success.Numbers", "&e- &aYour password has at least {min_number} numbers");
                break;
            case LOWER:
                str = msg.getString("Password.Success.Uppers", "&e- &aYour password has at least {min_lower} lowercase characters");
                break;
            case UPPER:
                str = msg.getString("Password.Success.Lowers", "&e- &aYour password has at least {min_upper} uppercase characters");
                break;
        }

        PluginConfiguration config = CurrentPlatform.getConfiguration();
        PasswordConfig passwordConfig = config.passwordConfig();

        return parse(str
                .replace("{min_length}", String.valueOf(passwordConfig.min_length()))
                .replace("{min_special}", String.valueOf(passwordConfig.min_characters()))
                .replace("{min_number}", String.valueOf(passwordConfig.min_numbers()))
                .replace("{min_lower}", String.valueOf(passwordConfig.min_lower()))
                .replace("{min_upper}", String.valueOf(passwordConfig.min_upper()))
                .replace("{length}", String.valueOf(result.getLength()))
                .replace("{special}", String.valueOf(result.getSpecialCharacters()))
                .replace("{number}", String.valueOf(result.getNumbers()))
                .replace("{lower}", String.valueOf(result.getLowerCase()))
                .replace("{upper}", String.valueOf(result.getUpperCase())));
    }

    /**
     * Get a plugin message
     *
     * @param type message type
     * @param result message argument
     * @return plugin message
     */
    public String checkFailed(final CheckType type, final PasswordAttribute result) {
        String str = "";
        switch (type) {
            case UNIQUE:
                str = msg.getString("Password.Failed.Unique", "&e- &cYour password is similar to known ones");
                break;
            case LENGTH:
                str = msg.getString("Password.Failed.Length", "&e- &cYour password has {length} of {min_length} characters");
                break;
            case SPECIAL:
                str = msg.getString("Password.Failed.Special", "&e- &cYour password has {special} of {min_special} characters");
                break;
            case NUMBER:
                str = msg.getString("Password.Failed.Numbers", "&e- &cYour password has {number} of {min_number} numbers");
                break;
            case LOWER:
                str = msg.getString("Password.Failed.Uppers", "&e- &cYour password has {lower} of {min_lower} lowercase characters");
                break;
            case UPPER:
                str = msg.getString("Password.Failed.Lowers", "&e- &cYour password has {upper} of {min_upper} uppercase characters");
                break;
        }

        PluginConfiguration config = CurrentPlatform.getConfiguration();
        PasswordConfig passwordConfig = config.passwordConfig();

        return parse(str
                .replace("{min_length}", String.valueOf(passwordConfig.min_length()))
                .replace("{min_special}", String.valueOf(passwordConfig.min_characters()))
                .replace("{min_number}", String.valueOf(passwordConfig.min_numbers()))
                .replace("{min_lower}", String.valueOf(passwordConfig.min_lower()))
                .replace("{min_upper}", String.valueOf(passwordConfig.min_upper()))
                .replace("{length}", String.valueOf(result.getLength()))
                .replace("{special}", String.valueOf(result.getSpecialCharacters()))
                .replace("{number}", String.valueOf(result.getNumbers()))
                .replace("{lower}", String.valueOf(result.getLowerCase()))
                .replace("{upper}", String.valueOf(result.getUpperCase())));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String pinUsages() {
        return parse(msg.getString("PinUsages", "&5&oValid pin sub-arguments: &e<setup>&7, &e<remove>&7, &e<change>"));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String pinSet() {
        String str = msg.getString("PinSet", "&dYour pin has been set successfully");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String pinReseted() {
        String str = msg.getString("PinReseted", "&5&oPin removed, your account is now less secure");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String pinChanged() {
        String str = msg.getString("PinChanged", "&dYour pin has been changed successfully");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String alreadyPin() {
        String str = msg.getString("AlreadyPin", "&5&oYou already have set your pin!");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String noPin() {
        String str = msg.getString("NoPin", "&5&oYou don't have a pin!");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String setPin() {
        String str = msg.getString("SetPin", "&5&oPlease, use /pin setup <pin>");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String resetPin() {
        String str = msg.getString("ResetPin", "&5&oPlease, use /pin reset <pin>");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String changePin() {
        String str = msg.getString("ChangePin", "&5&oPlease, use /pin change <pin> <new pin>");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String pinDisabled() {
        String str = msg.getString("PinDisabled", "&5&oPins are disabled");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public final String pinLength() {
        String str = msg.getString("PinLength", "&5&oPin must have 4 digits");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String incorrectPin() {
        String str = msg.getString("IncorrectPin", "&5&oThe specified pin is not correct!");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String gAuthUsages() {
        String str = msg.getString("2FaUsages", "&5&oValid 2FA sub-arguments: &e<setup>&7, &e<remove>&7, &e<2fa code>");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String gAuthSetupUsage() {
        String str = msg.getString("2FaSetupUsage", "&5&oPlease, use /2fa setup <password>");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String gAuthRemoveUsage() {
        String str = msg.getString("2FaRemoveUsage", "&5&oPlease, use /2fa remove <password> <2fa code>");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String gAuthenticate() {
        String str = msg.getString("2FaAuthenticate", "&5&oPlease, use /2fa to start playing");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String gAuthCorrect() {
        String str = msg.getString("2FaLogged", "&d2FA code validated");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String gAuthAlready() {
        String str = msg.getString("2FaAlreadyLogged", "&5&oYou are already authenticated with 2FA!");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String gAuthIncorrect() {
        String str = msg.getString("2FaIncorrect", "&5&oIncorrect 2FA code");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String gAuthSetupAlready() {
        String str = msg.getString("2FaAlready", "&5&oYou already have setup your 2FA!");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String gAuthToggleError() {
        String str = msg.getString("ToggleFAError", "&5&oError while trying to toggle 2FA ( incorrect password/code )");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String gAuthDisabled() {
        String str = msg.getString("Disabled2FA", "&5&o2FA disabled, your account is now less secure");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String gAuthEnabled() {
        String str = msg.getString("Enabled2FA", "&d2FA enabled, your account is secure again");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String gAuthNotEnabled() {
        String str = msg.getString("2FaAccountDisabled", "&5&o2FA is disabled in your account");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String gAuthServerDisabled() {
        String str = msg.getString("2FAServerDisabled", "&5&o2FA is currently disabled in this server");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String gauthLocked() {
        String str = msg.getString("2FaLocked", "&5&oThis server wants you to have 2FA enabled");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @param codes message replace
     * @return plugin message
     */
    public String gAuthScratchCodes(final List<Integer> codes) {
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

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String gAuthInstructions() {
        List<String> messages = msg.getStringList("2FaInstructions");
        StringBuilder builder = new StringBuilder();

        PluginConfiguration config = CurrentPlatform.getConfiguration();

        for (String str : messages)
            builder.append(str
                    .replace("{message}", gAuthLink() + StringUtils.getLastColor(str))
                    .replace("{account}", "{player} (" + config.serverName() + ")")).append("\n");

        return parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String gAuthLink() {
        String str = msg.getString("2FaLink", "&bClick here to get your 2FA QR code");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String panicLogin() {
        String str = msg.getString("PanicLogin", "&5&oPlease, use /panic <token>");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String panicTitle() {
        String str = msg.getString("PanicTitle", "&cPANIC MODE");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String panicSubtitle() {
        String str = msg.getString("PanicSubtitle", "&cRUN &4/panic <token>");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String panicMode() {
        String str = msg.getString("PanicMode", "&5&oThe account entered in panic mode, you have 1 token login try before being IP-blocked");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @SuppressWarnings("unused")
    public String panicDisabled() {
        String str = msg.getString("PanicDisabled", "&5&oThe server is not protected against brute force attacks");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String panicAlready() {
        String str = msg.getString("PanicAlready", "&dYou already have a brute force token, your account is secure");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String panicRequested() {
        List<String> messages = msg.getStringList("PanicRequested");
        StringBuilder builder = new StringBuilder();

        for (String str : messages)
            builder.append(str).append("\n");

        return parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @SuppressWarnings("unused")
    public String panicEnabled() {
        List<String> messages = msg.getStringList("PanicEnabled");
        StringBuilder builder = new StringBuilder();

        for (String str : messages)
            builder.append(str).append("\n");

        return parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    @SuppressWarnings("unused")
    public String tokenLink() {
        String str = msg.getString("TokenLink", "&bClick to reveal the token");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String accountArguments() {
        String str = msg.getString("AccountArguments", "&5&oValid account sub-arguments: &e<change>&7, &e<unlock>&7, &e<close>&7, &e<remove>");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String change() {
        String str = msg.getString("Change", "&5&oPlease, use /account change <password> <new password>");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String changeSame() {
        String str = msg.getString("ChangeSame", "&5&oYour password can't be the same as old!");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String changeDone() {
        String str = msg.getString("ChangeDone", "&dYour password has changed!");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String accountUnLock() {
        String str = msg.getString("AccountUnlock", "&5&oPlease, use /account unlock <player>");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @param target message replace
     * @return plugin message
     */
    public String accountUnLocked(final String target) {
        String str = msg.getString("AccountUnlocked", "&dAccount of {player} has been unlocked");

        return parse(str.replace("{player}", StringUtils.stripColor(target)));
    }

    /**
     * Get a plugin message
     *
     * @param target message replace
     * @return plugin message
     */
    public String accountNotLocked(final String target) {
        String str = msg.getString("AccountNotLocked", "&5&oAccount of {player} is not locked!");

        return parse(str.replace("{player}", StringUtils.stripColor(target)));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String close() {
        String str = msg.getString("Close", "&5&oPlease, use /account close [player]");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String closed() {
        String str = msg.getString("Closed", "&5&oSession closed, re-login now!");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String forcedClose() {
        String str = msg.getString("ForcedClose", "&5&oYour session have been closed by an admin, login again");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @param target message replace
     * @return plugin message
     */
    public String forcedCloseAdmin(final ModuleSender target) {
        String str = msg.getString("ForcedCloseAdmin", "&dSession of {player} closed");

        return parse(str.replace("{player}", StringUtils.stripColor(target.getName())));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String remove() {
        String str = msg.getString("Remove", "&5&oPlease, use /account remove <password|player> [password]");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String removeAccountMatch() {
        String str = msg.getString("RemoveAccountMatch", "&5&oThe provided passwords does not match");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String accountRemoved() {
        String str = msg.getString("AccountRemoved", "&5&oYour account have been deleted");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @param administrator message replace
     * @return plugin message
     */
    public String forcedAccountRemoval(final String administrator) {
        List<String> messages = msg.getStringList("ForcedAccountRemoval");
        StringBuilder builder = new StringBuilder();

        for (String str : messages)
            builder.append(str
                    .replace("{player}", StringUtils.stripColor(administrator))).append("\n");

        return parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    /**
     * Get a plugin message
     *
     * @param target message replace
     * @return plugin message
     */
    public String forcedAccountRemovalAdmin(final String target) {
        String str = msg.getString("ForcedAccountRemovalAdmin", "&dAccount of {player} removed, don't forget to run /account unlock {player}!");

        return parse(str.replace("{player}", StringUtils.stripColor(target)));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public final String spawnSet() {
        String str = msg.getString("SpawnSet", "&dThe login spawn location have been set");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public final String locationsReset() {
        String str = msg.getString("LocationsReset", "&dAll last locations have been reset");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public final String locationReset(final String name) {
        String str = msg.getString("LocationReset", "&dLast location of {player} has been reset");

        return parse(str.replace("{player}", StringUtils.stripColor(name)));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public final String locationsFixed() {
        String str = msg.getString("LocationsFixed", "&dAll last locations have been fixed");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public final String locationFixed(final String name) {
        String str = msg.getString("LocationFixed", "&dLocation of {player} has been fixed");

        return parse(str.replace("{player}", StringUtils.stripColor(name)));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public final String resetLocUsage() {
        String str = msg.getString("ResetLastLocUsage", "&5&oPlease, use /locations [player|@all|@me] <remove|fix>");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String alias() {
        String str = msg.getString("AliasArguments", "&5&oValid alias sub-arguments: &e<create>&7, &e<destroy>&7, &e<add>&7, &e<remove> [alias] [player(s)]");

        return parse(str);
    }

    /**
     * Get a plugin message
     *
     * @param alias message replace
     * @return plugin message
     */
    public String aliasCreated(final Alias alias) {
        String str = msg.getString("AliasCreated", "&dAlias {alias} created successfully");

        return parse(str.replace("{alias}", alias.getName()));
    }

    /**
     * Get a plugin message
     *
     * @param alias message replace
     * @return plugin message
     */
    public String aliasDestroyed(final Alias alias) {
        String str = msg.getString("AliasDestroyed", "&5&oAlias {alias} has been destroyed");

        return parse(str.replace("{alias}", alias.getName()));
    }

    /**
     * Get a plugin message
     *
     * @param alias message replace
     * @return plugin message
     */
    public String aliasExists(final Alias alias) {
        String str = msg.getString("AliasExists", "&5&oAlias {alias} already exists!");

        return parse(str.replace("{alias}", alias.getName()));
    }

    /**
     * Get a plugin message
     *
     * @param alias message replace
     * @return plugin message
     */
    public String aliasNotFound(final String alias) {
        String str = msg.getString("AliasNotFound", "&5&oCouldn't find any alias called {alias}");

        return parse(str.replace("{alias}", StringUtils.stripColor(alias.toUpperCase().replace(" ", "_"))));
    }

    /**
     * Get a plugin message
     *
     * @param alias   message replace
     * @param players message replace
     * @return plugin message
     */
    public String addedPlayer(final Alias alias, String... players) {
        String str = msg.getString("AddedPlayer", "&dAdded {player} to {alias}");

        StringBuilder builder = new StringBuilder();
        for (String player : players)
            builder.append(player).append(", ");

        return parse(str
                .replace("{player}",
                        StringUtils.replaceLast(builder.toString(), ", ", ""))
                .replace("{alias}", alias.getName()));
    }

    /**
     * Get a plugin message
     *
     * @param alias   message replace
     * @param players message replace
     * @return plugin message
     */
    public String removedPlayer(final Alias alias, String... players) {
        String str = msg.getString("RemovedPlayer", "&dRemoved {player} from {alias}");

        StringBuilder builder = new StringBuilder();
        for (String player : players)
            builder.append(player).append(", ");

        return parse(str
                .replace("{player}",
                        StringUtils.replaceLast(builder.toString(), ", ", ""))
                .replace("{alias}", alias.getName()));
    }

    /**
     * Get a plugin message
     *
     * @param alias   message replace
     * @param players message replace
     * @return plugin message
     */
    public String playerNotIn(final Alias alias, String... players) {
        String str = msg.getString("PlayerNotIn", "&5&o{player} is not in {alias}!");

        StringBuilder builder = new StringBuilder();
        for (String player : players)
            builder.append(player).append(", ");

        return parse(str
                .replace("{player}",
                        StringUtils.replaceLast(builder.toString(), ", ", ""))
                .replace("{alias}", alias.getName()));
    }

    /**
     * Get a plugin message
     *
     * @param alias   message replace
     * @param players message replace
     * @return plugin message
     */
    public String playerAlreadyIn(final Alias alias, String... players) {
        String str = msg.getString("PlayerAlreadyIn", "&5&o{player} is already in {alias}!");

        StringBuilder builder = new StringBuilder();
        for (String player : players)
            builder.append(player).append(", ");

        return parse(str
                .replace("{player}",
                        StringUtils.replaceLast(builder.toString(), ", ", ""))
                .replace("{alias}", alias.getName()));
    }

    /**
     * Get a plugin message
     *
     * @param time message replace
     * @return plugin message
     */
    public String ipBlocked(final long time) {
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

    /**
     * Get a plugin message
     *
     * @param chars message replace
     * @return plugin message
     */
    public String illegalName(final String chars) {
        List<String> messages = msg.getStringList("IllegalName");
        StringBuilder builder = new StringBuilder();

        for (String str : messages) builder.append(str.replace("{chars}", chars)).append("\n");

        return parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    /**
     * Get a plugin message
     *
     * @param name message replace
     * @return plugin message
     */
    public String similarName(final String name) {
        List<String> messages = msg.getStringList("SimilarName");
        StringBuilder builder = new StringBuilder();

        for (String str : messages) builder.append(str.replace("{player}", name)).append("\n");

        return parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    /**
     * Get a plugin message
     *
     * @param name       messages replace
     * @param knownNames message replace
     * @return plugin message
     */
    public String multipleNames(final String name, final String... knownNames) {
        List<String> messages = msg.getStringList("MultipleNames");
        StringBuilder nameBuilder = new StringBuilder();

        for (String known : knownNames)
            nameBuilder.append("&d").append(known).append("&5, ");

        String names = StringUtils.replaceLast(nameBuilder.toString(), "&5, ", "");

        StringBuilder builder = new StringBuilder();

        for (String str : messages)
            builder.append(str.replace("{player}", name).replace("{players}", names)).append("\n");

        return parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String uuidFetchError() {
        List<String> messages = msg.getStringList("UUIDFetchError");
        StringBuilder builder = new StringBuilder();

        for (String str : messages) builder.append(str).append("\n");

        return parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    /**
     * Get a plugin message
     *
     * @return plugin message
     */
    public String ipProxyError() {
        List<String> messages = msg.getStringList("IpProxyError");
        StringBuilder builder = new StringBuilder();

        for (String str : messages)
            builder.append(str).append("\n");

        return parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    public String bedrockJava() {
        List<String> messages = msg.getStringList("BedrockJavaError");
        StringBuilder builder = new StringBuilder();

        for (String str : messages)
            builder.append(str).append("\n");

        return parse(StringUtils.replaceLast(builder.toString(), "\n", ""));
    }

    /**
     * Get the plugin messages manager
     *
     * @return the plugin messages manager
     */
    protected final KarmaYamlManager getManager() {
        return msg;
    }

    /**
     * Returns a string representation of the object. In general, the
     * {@code toString} method returns a string that
     * "textually represents" this object. The result should
     * be a concise but informative representation that is easy for a
     * person to read.
     * It is recommended that all subclasses override this method.
     * <p>
     * The {@code toString} method for class {@code Object}
     * returns a string consisting of the name of the class of which the
     * object is an instance, the at-sign character `{@code @}', and
     * the unsigned hexadecimal representation of the hash code of the
     * object. In other words, this method returns a string equal to the
     * value of:
     * <blockquote>
     * <pre>
     * getClass().getName() + '@' + Integer.toHexString(hashCode())
     * </pre></blockquote>
     *
     * @return a string representation of the object.
     */
    @Override
    public final String toString() {
        return Base64.getEncoder().encodeToString(msg.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Parse the message
     *
     * @param original the original string
     * @return the parsed message
     */
    protected abstract String parse(final String original);

    /**
     * Load the messages from the specified yaml
     * text
     *
     * @param yaml the yaml to load
     */
    public abstract void loadString(final String yaml);
}
