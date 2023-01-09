package eu.locklogin.api.file.pack.sub;

import eu.locklogin.api.file.pack.LanguagePack;
import eu.locklogin.api.file.pack.key.DefaultKey;
import eu.locklogin.api.file.pack.key.PackKey;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Default sub language pack
 */
public final class DefaultSubPack extends SubPack {

    private final LanguagePack parent;
    private final String name;
    private final Map<String, Object> keys = new HashMap<>();

    /**
     * Initialize the default language sub pack
     *
     * @param p the language pack owner
     * @param n the language pack name
     */
    public DefaultSubPack(final LanguagePack p, final String n) {
        parent = p;
        name = n;
    }

    /**
     * Set the pack key value
     *
     * @param key the pack key
     * @param value the value
     */
    public void setKey(final String key, final Object value) {
        keys.put(key, value);
    }

    /**
     * Get the language pack that owns this sub pack
     *
     * @return the language pack owner
     */
    @Override
    public LanguagePack getPack() {
        return parent;
    }

    /**
     * Get the sub pack name
     *
     * @return the sub pack name
     */
    @Override
    public String getName() {
        return name;
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
        Object value = keys.getOrDefault(key, null);
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
            SubPack sub = new DefaultSubPack(parent, key);
            for (Object obj : map.keySet()) {
                if (obj instanceof String) {
                    String str = (String) obj;
                    sub.setKey(str, map.get(obj));
                }
            }

            return new DefaultKey(sub);
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
    public SubPack getSub(final String name) {
        Object value = keys.getOrDefault(name, null);
        if (value == null) {
            return null;
        }

        if (value instanceof SubPack) {
            return (SubPack) value;
        }
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            SubPack sub = new DefaultSubPack(parent, name);
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
        return keys.keySet();
    }
}
