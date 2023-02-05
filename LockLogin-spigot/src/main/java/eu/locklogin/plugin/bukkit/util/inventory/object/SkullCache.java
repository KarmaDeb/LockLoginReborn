package eu.locklogin.plugin.bukkit.util.inventory.object;

import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.karma.file.element.KarmaPrimitive;
import ml.karmaconfigs.api.common.karma.file.element.types.Element;
import ml.karmaconfigs.api.common.karma.file.element.types.ElementPrimitive;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;

import static eu.locklogin.plugin.bukkit.LockLogin.plugin;

/**
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
public final class SkullCache {

    private final KarmaMain sk_cache;

    /**
     * Initialize the skull cache
     *
     * @param owner_name the skull cache owner name
     */
    public SkullCache(final String owner_name) {
        sk_cache = new KarmaMain(plugin, owner_name, "data", "skulls");

        if (!sk_cache.exists())
            sk_cache.create();
    }

    /**
     * Save the skull value
     *
     * @param value the skull value
     */
    public void save(final String value) {
        if (!sk_cache.exists())
            sk_cache.create();

        Date today = new Date();
        SimpleDateFormat ll_format = new SimpleDateFormat("yyyyMMddHHmmss");
        String timestamp = ll_format.format(today);

        sk_cache.setRaw("value", value);
        sk_cache.setRaw("timestamp", timestamp);

        sk_cache.save();
    }

    /**
     * Get the cache skull value
     *
     * @return the cache skull value
     */
    @Nullable
    public String get() {
        if (!sk_cache.exists())
            sk_cache.create();

        Element<?> value = sk_cache.get("value");
        if (value.isPrimitive()) {
            ElementPrimitive primitive = value.getAsPrimitive();
            if (primitive.isString()) {
                return primitive.asString();
            }
        }

        return null;
    }

    /**
     * Check if the skull needs to reload cache
     *
     * @return if the skull needs to reload cache
     */
    public boolean needsCache() {
        if (!sk_cache.exists())
            sk_cache.create();

        if (sk_cache.isSet("timestamp")) {
            Date today = new Date();
            SimpleDateFormat ll_format = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = ll_format.format(today);

            Element<?> stored_timestamp = sk_cache.get("timestamp", new KarmaPrimitive(timestamp));
            try {
                if (stored_timestamp.isPrimitive()) {
                    ElementPrimitive primitive = stored_timestamp.getAsPrimitive();
                    if (primitive.isString()) {
                        Date way_back = ll_format.parse(primitive.asString());
                        return Math.round((today.getTime() - way_back.getTime()) / (double) 86400000) > 1;
                    }
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }

        return true;
    }
}
