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

import eu.locklogin.api.util.platform.CurrentPlatform;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * LockLogin UUID fetcher/resolver
 */
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
    @SuppressWarnings("deprecation")
    private static UUID retrieveUUID(String name) {
        try {
            String url = "https://api.mojang.com/users/profiles/minecraft/" + name;
            String UUIDJson = IOUtils.toString(new URL(url));

            JSONObject UUIDObject = (JSONObject) JSONValue.parseWithException(UUIDJson);
            try {
                return UUID.fromString(UUIDObject.get("id").toString());
            } catch (Throwable ex) {
                return fixUUID(UUIDObject.get("id").toString());
            }
        } catch (Throwable e) {
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
