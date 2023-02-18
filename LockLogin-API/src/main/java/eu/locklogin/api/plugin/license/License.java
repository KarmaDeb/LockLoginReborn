package eu.locklogin.api.plugin.license;

import java.nio.file.Path;

/**
 * License
 */
public abstract class License {

    protected Path installLocation;

    /**
     * Get the license location
     *
     * @return the license location
     */
    public final Path getLocation() {
        return installLocation;
    }

    /**
     * Set the license install location
     *
     * @param path the path location
     */
    public final void setInstallLocation(final Path path) {
        installLocation = path;
    }

    /**
     * Get the license base64 value
     *
     * @return the license
     */
    public abstract String base();

    /**
     * Get the license synchronization key
     *
     * @return the license synchronization key
     */
    public abstract String syncKey();

    /**
     * Get the license communication key
     *
     * @return the license communication key
     */
    public abstract String comKey();

    /**
     * Get the license version
     *
     * @return the license version
     */
    public abstract String version();

    /**
     * Get the license owner
     *
     * @return the license owner
     */
    public abstract LicenseOwner owner();

    /**
     * Get license expiration information
     *
     * @return the license expiration information
     */
    public abstract LicenseExpiration expiration();

    /**
     * Get the amount of proxies this license
     * can handle
     *
     * @return the amount of proxies for this
     * license
     */
    public abstract int max_proxies();

    /**
     * Get the backup quota for this license
     *
     * @return the backup quota
     */
    public abstract long backup_storage();

    /**
     * Get if this license is free
     *
     * @return if the license is free
     */
    public abstract boolean isFree();

    /**
     * Get if the license is installed
     *
     * @return if the license is installed
     */
    public abstract boolean installed();

    /**
     * Install this license (creates the .dat file)
     *
     * @return if the license could be installed
     */
    public abstract boolean install();
}
