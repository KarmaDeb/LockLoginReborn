package eu.locklogin.api.file.plugin;

import eu.locklogin.api.file.plugin.key.PackKey;
import eu.locklogin.api.file.plugin.sub.SubPack;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Set;

/**
 * LockLogin plugin language pack.
 */
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
     * @return a list of pack sub packs name
     */
    public abstract Set<String> getSubs();

    /**
     * Get the plugin messages (messages_xx.yml) file of the
     * pack
     *
     * @return the pack messages
     */
    public abstract String getPluginMessages();

    public static String[] needed_keys = {

    };
}
