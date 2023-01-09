package eu.locklogin.api.file.pack;

import eu.locklogin.api.file.pack.key.PackKey;
import eu.locklogin.api.file.pack.sub.SubPack;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * LockLogin plugin language pack.
 */
@SuppressWarnings("unused")
public abstract class LanguagePack {

    /**
     * Get the language pack locale
     *
     * @return the language pack locale
     */
    public abstract Locale getLocale();

    /**
     * Get the language pack name
     *
     * @return the language pack name
     */
    public abstract String getName();

    /**
     * Get the language pack authors
     *
     * @return the authors of the language pack
     */
    public abstract String[] getAuthors();

    /**
     * Get the pack version
     *
     * @return the pack version
     */
    public abstract String getVersion();

    /**
     * Get the pack minimum plugin version
     *
     * @return the pack minimum plugin version
     */
    public abstract String getMinimumVersion();

    /**
     * Get if the language pack is compatible with
     * the current plugin version
     *
     * @return if the language is compatible with the plugin
     */
    public abstract boolean isCompatible();

    /**
     * Get a key
     *
     * @param key the message key
     * @param def the default value (SHOULD NEVER BE NULL, EVEN THOUGH THE API ALLOWS IT)
     * @return the message
     */
    public abstract PackKey get(final String key, final @Nullable PackKey def);

    /**
     * Get a sub pack
     *
     * @param name the sub pack name
     * @return the sub pack
     */
    public abstract SubPack get(final String name);

    /**
     * Get pack sub packs
     *
     * @return a set of pack sub packs name
     */
    public abstract Set<String> getSubs();

    /**
     * Get the plugin messages (messages_xx.yml) file of the
     * pack
     *
     * @return the pack messages
     */
    public abstract String getPluginMessages();

    final Set<String> fetchKeys(final Map<?, ?> keys) {
        Set<String> data = new LinkedHashSet<>();
        for (Object key : keys.keySet()) {
            if (key instanceof String) {
                String str = (String) key;

                Object value = keys.get(key);
                if (value instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) value;
                    fetchKeys(map).forEach((fetched) -> data.add(str + "." + fetched));
                } else {
                    data.add(str);
                }
            }
        }

        return data;
    }

    static String[] needed_keys = {
            "diff.year",
            "diff.years",
            "diff.month",
            "diff.months",
            "diff.day",
            "diff.days",
            "diff.hour",
            "diff.hours",
            "diff.minute",
            "diff.minutes",
            "diff.milli",
            "diff.millis",
            "diff.spacer",
            "diff.terminator",
            "time.second",
            "time.minute",
            "time.hour",
            "time.short.minute",
            "time.short.second",
            "command.not_available",
            "command.register_problem",
            "command.2fa_hover",
            "error.create_user",
            "error.files",
            "session.invalid",
            "plugin.error_disabling",
            "plugin.filter_initialize",
            "plugin.filter_error",
            "reload.config",
            "reload.messages",
            "reload.proxy",
            "reload.systems",
            "ascii.size",
            "ascii.character"
    };
}
