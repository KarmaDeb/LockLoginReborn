package eu.locklogin.api.file.pack.sub;

import eu.locklogin.api.file.pack.LanguagePack;
import eu.locklogin.api.file.pack.key.PackKey;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

@SuppressWarnings("unused")
public abstract class SubPack {

    /**
     * Get the language pack that owns this sub pack
     *
     * @return the language pack owner
     */
    public abstract LanguagePack getPack();

    /**
     * Get the sub pack name
     *
     * @return the sub pack name
     */
    public abstract String getName();

    /**
     * Set the pack key value
     *
     * @param key the pack key
     * @param value the value
     */
    public abstract void setKey(final String key, final Object value);

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
    public abstract SubPack getSub(final String name);

    /**
     * Get pack sub packs
     *
     * @return a set of pack sub packs name
     */
    public abstract Set<String> getSubs();
}
