package eu.locklogin.api.common.utils.other.name;

import ml.karmaconfigs.api.common.string.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NameSearchResult {

    private final UUID[] uuids;

    /**
     * Initialize the name search result
     *
     * @param ids the uuids search result
     */
    public NameSearchResult(final UUID... ids) {
        List<UUID> fixed_uuids = new ArrayList<>();
        for (UUID id : ids) {
            if (id != null) {
                try {
                    UUID uuid = UUID.fromString(id.toString());
                    if (!StringUtils.isNullOrEmpty(uuid.toString()))
                        fixed_uuids.add(uuid);
                } catch (Throwable ignored) {
                }
            }
        }

        uuids = fixed_uuids.toArray(new UUID[0]);
    }

    /**
     * Get if the result is valid
     *
     * @return if the result is valid
     */
    public final boolean isValidResult() {
        boolean valid = false;

        if (uuids != null) {
            for (UUID uuid : uuids) {
                if (uuid != null) {
                    valid = true;
                    break;
                }
            }
        }

        return valid;
    }

    /**
     * Get if the result contains multiple uuids
     * z
     *
     * @return if the result contains multiple uuids
     */
    public final boolean singleResult() {
        if (isValidResult()) {
            return uuids.length == 1;
        }

        return true;
    }

    /**
     * Get the unique id
     *
     * @return the unique id
     */
    public final UUID getUniqueId() {
        return uuids[0];
    }

    /**
     * Get the uuids
     *
     * @return the uuids
     */
    @SuppressWarnings("unused")
    public final UUID[] getUniqueIDs() {
        return uuids;
    }
}
