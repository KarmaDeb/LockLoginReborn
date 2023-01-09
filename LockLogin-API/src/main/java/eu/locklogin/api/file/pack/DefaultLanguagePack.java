package eu.locklogin.api.file.pack;

import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.pack.key.DefaultKey;
import eu.locklogin.api.file.pack.key.PackKey;
import eu.locklogin.api.file.pack.sub.DefaultSubPack;
import eu.locklogin.api.file.pack.sub.SubPack;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.karma.file.yaml.KarmaYamlManager;
import ml.karmaconfigs.api.common.karma.source.APISource;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Default language pack
 */
@SuppressWarnings("unused")
public final class DefaultLanguagePack extends LanguagePack {

    private final Map<String, Object> internals = new HashMap<>();

    /**
     * Initialize the default language pack
     */
    DefaultLanguagePack() {
        Map<String, String> diff = new HashMap<>();
        diff.put("year", "year");
        diff.put("years", "years");
        diff.put("month", "month");
        diff.put("months", "months");
        diff.put("day", "day");
        diff.put("days", "days");
        diff.put("hour", "hour");
        diff.put("hours", "hours");
        diff.put("minute", "minute");
        diff.put("minutes", "minutes");
        diff.put("milli", "milli");
        diff.put("millis", "millis");
        diff.put("spacer", ", ");
        diff.put("terminator", "and ");
        internals.put("diff", diff);

        Map<String, Object> times = new HashMap<>();
        times.put("second", "second(s)");
        times.put("minute", "minute(s)");
        times.put("hour", "hour(s)");
        Map<String, String> times_short = new HashMap<>();
        times_short.put("minute", "min(s)");
        times_short.put("second", "sec(s)");
        times.put("short", times_short);
        internals.put("time", times);

        Map<String, String> commands = new HashMap<>();
        commands.put("not_available", "&5&oThis command is not available for console");
        commands.put("register_problem", "Failed to register command(s): {0}");
        commands.put("2fa_hover", "&eClick here to scan the QR code!");
        internals.put("command", commands);

        Map<String, String> errors = new HashMap<>();
        errors.put("create_user", "&5&oWe're sorry, but we couldn't create your account");
        errors.put("files", "Failed to setup/check file(s): {0}. The plugin will use defaults, you can try to create files later by running /locklogin reload");
        internals.put("error", errors);

        Map<String, String> sessions = new HashMap<>();
        sessions.put("invalid", "&5&oYour session is invalid, try leaving and joining the server again");
        internals.put("session", sessions);

        Map<String, String> plugins = new HashMap<>();
        plugins.put("error_disabling", "Disabling plugin due an internal error");
        plugins.put("filter_initialize", "Initializing console filter to protect user data");
        plugins.put("filter_error", "An error occurred while initializing console filter, check logs for more info");
        internals.put("plugin", plugins);

        Map<String, String> reload = new HashMap<>();
        reload.put("config", "&dReloaded config file");
        reload.put("messages", "&dReloaded messages file");
        reload.put("proxy", "&dReloaded proxy configuration");
        reload.put("system", "&dRestarting version checker and plugin alert system");
        internals.put("reload", reload);

        Map<String, Object> ascii = new HashMap<>();
        ascii.put("size", 10);
        ascii.put("character", '*');
        internals.put("ascii", ascii);
    }

    /**
     * Get the language pack locale
     *
     * @return the language pack locale
     */
    @Override
    public Locale getLocale() {
        return Locale.ENGLISH;
    }

    /**
     * Get the language pack name
     *
     * @return the language pack name
     */
    @Override
    public String getName() {
        return "LockLogin";
    }

    /**
     * Get the language pack authors
     *
     * @return the authors of the language pack
     */
    @Override
    public String[] getAuthors() {
        return new String[]{
                "KarmaDev"
        };
    }

    /**
     * Get the pack version
     *
     * @return the pack version
     */
    @Override
    public String getVersion() {
        return APISource.loadProvider("LockLogin").version();
    }

    /**
     * Get the pack minimum plugin version
     *
     * @return the pack minimum plugin version
     */
    @Override
    public String getMinimumVersion() {
        return APISource.loadProvider("LockLogin").version();
    }

    /**
     * Get if the language pack is compatible with
     * the current plugin version
     *
     * @return if the language is compatible with the plugin
     */
    @Override
    public boolean isCompatible() {
        Set<String> keys = fetchKeys(internals);
        for (String needed : needed_keys) {
            if (!keys.contains(needed)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Get a key
     *
     * @param key the message key
     * @param def the default value (SHOULD NEVER BE NULL, EVEN THOUGH THE API ALLOWS IT)
     * @return the message
     * @throws IllegalStateException if the language key is not valid
     */
    @Override
    public PackKey get(final String key, @Nullable final PackKey def) throws IllegalStateException {
        Object value = internals.getOrDefault(key, null);
        if (value == null) {
            return def;
        }

        if (value instanceof PackKey) {
            return (PackKey) value;
        }
        if (value instanceof String) {
            return new DefaultKey((String) value);
        }
        if (value instanceof Boolean) {
            return new DefaultKey((Boolean) value);
        }
        if (value instanceof Character) {
            return new DefaultKey((Character) value);
        }
        if (value instanceof Number) {
            return new DefaultKey((Number) value);
        }
        if (value instanceof SubPack) {
            return new DefaultKey((SubPack) value);
        }
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            SubPack sub = new DefaultSubPack(this, key);
            for (Object obj : map.keySet()) {
                if (obj instanceof String) {
                    String str = (String) obj;
                    sub.setKey(str, map.get(obj));
                }
            }
        }

        throw new IllegalStateException("Cannot retrieve language pack key for unsupported type: " + value.getClass().getSimpleName());
    }

    /**
     * Get a sub pack
     *
     * @param name the sub pack name
     * @return the sub pack
     */
    @Override
    public SubPack get(final String name) {
        Object value = internals.getOrDefault(name, null);
        if (value == null) {
            return null;
        }

        if (value instanceof SubPack) {
            return (SubPack) value;
        }
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            SubPack sub = new DefaultSubPack(this, name);
            for (Object obj : map.keySet()) {
                if (obj instanceof String) {
                    String str = (String) obj;
                    sub.setKey(str, map.get(obj));
                }
            }

            return sub;
        }

        throw new IllegalStateException("Cannot retrieve language pack sub keys for unsupported type: " + value.getClass().getSimpleName());
    }

    /**
     * Get pack sub packs
     *
     * @return a set of pack sub packs name
     */
    @Override
    public Set<String> getSubs() {
        return internals.keySet();
    }


    /**
     * Get the plugin messages (messages_xx.yml) file of the
     * pack
     *
     * @return the pack messages
     */
    @Override
    public String getPluginMessages() {
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        String country = config.getLang().country(config.getLangName());
        KarmaYamlManager msg = new KarmaYamlManager(APISource.loadProvider("LockLogin"), "messages_" + country, "lang", "v2");

        return Base64.getEncoder().encodeToString(msg.toString().getBytes());
    }
}
