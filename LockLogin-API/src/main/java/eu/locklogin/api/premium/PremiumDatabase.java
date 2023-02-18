package eu.locklogin.api.premium;


import java.util.UUID;

/**
 * Premium user database
 */
public interface PremiumDatabase {

    /**
     * Get if the UUID is premium
     *
     * @param id the uuid
     * @return if the id is premium
     */
    boolean isPremium(final UUID id);

    /**
     * Set the premium status of a UUID
     *
     * @param id the uuid
     * @param status the new status
     * @return if the database was able to be saved
     */
    boolean setPremium(final UUID id, final boolean status);
}
