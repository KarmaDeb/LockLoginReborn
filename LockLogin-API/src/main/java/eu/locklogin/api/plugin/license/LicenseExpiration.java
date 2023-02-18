package eu.locklogin.api.plugin.license;

import java.time.Instant;

/**
 * Owner of a license
 */
public interface LicenseExpiration {

    /**
     * Get when the license was granted
     *
     * @return when the license was granted
     */
    Instant granted();

    /**
     * Get when the license will expire
     *
     * @return when the license will expire
     */
    Instant expiration();

    /**
     * Get if the license has expired
     *
     * @return if the license is expired
     */
    boolean isExpired();

    /**
     * Get if the license has an expiration
     * date
     *
     * @return if the license has an expiration date
     */
    boolean hasExpiration();

    /**
     * Get the amount of years before
     * the license expires
     *
     * @return the license years lifetime
     */
    int expireYears();

    /**
     * Get the amount of months before
     * the license expires
     *
     * @return the license months lifetime
     */
    int expireMonths();

    /**
     * Get the amount of weeks before
     * the license expires
     *
     * @return the license weeks lifetime
     */
    int expireWeeks();

    /**
     * Get the amount of days before
     * the license expires
     *
     * @return the license days lifetime
     */
    int expireDays();

    /**
     * Get the amount of hours before
     * the license expires
     *
     * @return the license hours lifetime
     */
    int expireHours();

    /**
     * Get the amount of minutes before
     * the license expires
     *
     * @return the license minutes lifetime
     */
    int expireMinutes();

    /**
     * Get the amount of seconds before
     * the license expires
     *
     * @return the license seconds lifetime
     */
    int expireSeconds();
}
