package eu.locklogin.api.file.plugin;

import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.plugin.key.PackKey;
import eu.locklogin.api.file.plugin.sub.SubPack;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.karmafile.karmayaml.KarmaYamlManager;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Default language pack
 */
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

        internals.put("command_not_available", "&5&oThis command is not available for console");
        internals.put("could_not_create_user", "&5&oWe're sorry, but we couldn't create your account");
        internals.put("session_not_valid", "&5&oYour session is invalid, try leaving and joining the server again");
        internals.put("command_register_problem", "Failed to register command(s): {0}");
        internals.put("command_2fa_hover", "&eClick here to scan the QR code!");
        internals.put("file_register_problem", "Failed to setup/check file(s): {0}. The plugin will use defaults, you can try to create files later by running /locklogin reload");
        internals.put("plugin_error_disabling", "Disabling plugin due an internal error");
        internals.put("plugin_error_cache_save", "Failed to save cache object {0} ( {1} )");
        internals.put("plugin_error_cache_load", "Failed to load cache object {0} ( {1} )");
        internals.put("plugin_filter_initialize", "Initializing console filter to protect user data");
        internals.put("plugin_filter_error", "An error occurred while initializing console filter, check logs for more info");
        internals.put("reload_config", "&dReloaded config file");
        internals.put("reload_proxy", "&dReloaded proxy configuration");
        internals.put("reload_messages", "&dReloaded messages file");
        internals.put("restart_systems", "&dRestarting version checker and plugin alert system");
        internals.put("ascii_art_size", 10);
        internals.put("ascii_art_character", '*');
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
        for (String k : needed_keys) {
            PackKey value = get(k, null);
            if (value == null) {
                return false;
            }
        }

        return false;
    }

    /**
     * Get a key
     *
     * @param key the message key
     * @param def the default value (SHOULD NEVER BE NULL, EVEN THOUGH THE API ALLOWS IT)
     * @return the message
     */
    @Override
    public PackKey get(String key, @Nullable PackKey def) {
        return null;
    }

    /**
     * Get a sub pack
     *
     * @param name the sub pack name
     * @return the sub pack
     */
    @Override
    public SubPack get(String name) {
        return null;
    }

    /**
     * Get pack sub packs
     *
     * @return a list of pack sub packs name
     */
    @Override
    public Set<String> getSubs() {
        return null;
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
