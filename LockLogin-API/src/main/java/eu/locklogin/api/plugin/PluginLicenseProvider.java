package eu.locklogin.api.plugin;

import eu.locklogin.api.plugin.license.License;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Plugin license
 */
public interface PluginLicenseProvider {

    /**
     * Check the license
     *
     * @param license the license file
     * @return the license if the signature is valid
     */
    License fetch(final File license);

    /**
     * Check the license
     *
     * @param license the license file
     * @return the license if the signature is valid
     */
    default License fetch(final Path license) {
        return fetch(license.toFile());
    }

    /**
     * Synchronize an existing license
     *
     * @param key the license key
     * @return the license
     */
    License sync(final String key);

    /**
     * Synchronize an existing license
     *
     * @param key the license key
     * @param username the license username
     * @param password the license password
     * @return the license
     */
    License sync(final String key, final String username, final String password);

    /**
     * Request a new free license
     *
     * @return the license
     */
    License request();

    /**
     * Request a new private license
     *
     * @param id       the license ID
     * @param username the private license name
     * @param password the private license password
     * @return the new private license
     */
    License request(final UUID id, final String username, final String password);
}
