package eu.locklogin.api.file.plugin.sub;

import eu.locklogin.api.file.plugin.LanguagePack;
import eu.locklogin.api.file.plugin.key.PackKey;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public final class DefaultSubPack extends SubPack {

    /**
     * Get the language pack that owns this sub pack
     *
     * @return the language pack owner
     */
    @Override
    public LanguagePack getPack() {
        return null;
    }

    /**
     * Get the sub pack name
     *
     * @return the sub pack name
     */
    @Override
    public String getName() {
        return null;
    }

    /**
     * Get a message
     *
     * @param key the message key
     * @param def the default value (SHOULD NEVER BE NULL, EVEN THOUGH THE API ALLOWS IT)
     * @return the message
     */
    @Override
    public PackKey get(final String key, @Nullable final PackKey def) {
        return null;
    }

    /**
     * Get a sub pack
     *
     * @param name the sub pack name
     * @return the sub pack
     */
    @Override
    public SubPack getSub(String name) {
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
}
