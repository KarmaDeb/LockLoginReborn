package ml.karmaconfigs.locklogin.plugin.bukkit.util.inventory.object;

import ml.karmaconfigs.api.bukkit.KarmaFile;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.plugin;

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

    private final KarmaFile sk_cache;

    /**
     * Initialize the skull cache
     *
     * @param owner_name the skull cache owner name
     */
    public SkullCache(final String owner_name) {
        sk_cache = new KarmaFile(plugin, owner_name, "data", "skulls");

        if (!sk_cache.exists())
            sk_cache.create();
    }

    /**
     * Save the skull value
     *
     * @param value the skull value
     */
    public final void save(final String value) {
        if (!sk_cache.exists())
            sk_cache.create();

        Date today = new Date();
        SimpleDateFormat ll_format = new SimpleDateFormat("yyyyMMddHHmmss");
        String timestamp = ll_format.format(today);

        sk_cache.set("Value", value);
        sk_cache.set("Timestamp", timestamp);
    }

    /**
     * Get the cache skull value
     *
     * @return the cache skull value
     */
    @Nullable
    public final String get() {
        if (!sk_cache.exists())
            sk_cache.create();

        String value = sk_cache.getString("Value", "");
        return (value.replaceAll("\\s", "").isEmpty() ? null : value);
    }

    /**
     * Check if the skull needs to reload cache
     *
     * @return if the skull needs to reload cache
     */
    public final boolean needsCache() {
        if (!sk_cache.exists())
            sk_cache.create();

        if (sk_cache.isSet("Timestamp")) {
            Date today = new Date();
            SimpleDateFormat ll_format = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = ll_format.format(today);

            String stored_timestamp = sk_cache.getString("Timestamp", timestamp);
            try {
                Date way_back = ll_format.parse(stored_timestamp);

                return Math.round((today.getTime() - way_back.getTime()) / (double) 86400000) > 1;
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }

        return true;
    }
}
