package eu.locklogin.api.common.utils.other;

/*
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.url.URLUtils;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * LockLogin UUID fetcher/resolver
 *
 * TODO: Will be removed in version 1.13.17
 *
 * @deprecated {@link ml.karmaconfigs.api.common.utils.UUIDUtil} should be used now
 */
@Deprecated
public final class UUIDGen {

    /**
     * Get the UUID of the player name
     *
     * @param name the player name
     * @return the uuid of the player name
     */
    public static UUID getUUID(final String name) {
        if (CurrentPlatform.isOnline()) {
            return retrieveUUID(name);
        } else {
            return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name.replaceAll("\\s", "")).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Get the mojang player uuid
     *
     * @param name the player name
     * @return the mojang uuid from the specified player name
     */
    private static UUID retrieveUUID(String name) {
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
            String response = URLUtils.getResponse(url);

            Gson gson = new GsonBuilder().setLenient().setPrettyPrinting().create();
            JsonObject uuid = gson.fromJson(response, JsonObject.class);
            try {
                return UUID.fromString(uuid.get("id").toString());
            } catch (Throwable ex) {
                return fixUUID(uuid.get("id").toString());
            }
        } catch (Throwable ex) {
            CurrentPlatform.getLogger().scheduleLog(Level.GRAVE, ex);
            CurrentPlatform.getLogger().scheduleLog(Level.INFO, "Failed to fetch client UUID of {0}", name);
            return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Fix the trimmed UUID
     *
     * @param id the trimmed UUID
     * @return the full UUID
     * @throws IllegalArgumentException if the UUID is invalid ( not an UUID )
     */
    private static UUID fixUUID(String id) throws IllegalArgumentException {
        if (id == null) throw new IllegalArgumentException();
        if (!id.contains("-")) {
            StringBuilder builder = new StringBuilder(id.trim());
            try {
                builder.insert(20, "-");
                builder.insert(16, "-");
                builder.insert(12, "-");
                builder.insert(8, "-");
            } catch (StringIndexOutOfBoundsException e) {
                throw new IllegalArgumentException();
            }

            return UUID.fromString(builder.toString());
        }

        return UUID.fromString(id);
    }
}
