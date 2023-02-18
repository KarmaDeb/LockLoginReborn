package eu.locklogin.api.plugin.license;

/**
 * Owner of a license
 */
public interface LicenseOwner {

    /**
     * Get the license owner contact
     *
     * @return the license owner contact
     */
    String contact();

    /**
     * Get the license owner name
     *
     * @return the license name
     */
    String name();
}
