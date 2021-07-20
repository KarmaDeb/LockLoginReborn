package eu.locklogin.api.encryption;

import ml.karmaconfigs.api.common.karmafile.KarmaFile;
import ml.karmaconfigs.api.common.utils.FileUtilities;

import java.io.File;

/**
 * LockLogin salts data for DynamicBungeeAuth
 * SHA512 auth API
 */
public final class SaltData {

    private final static KarmaFile file = new KarmaFile(new File(FileUtilities.getProjectFolder() + File.separator + "LockLogin" + File.separator + "cache", "dba.salt"));
    private final String password;

    /**
     * Initialize the salt data
     *
     * @param pwd the password
     */
    public SaltData(final String pwd) {
        password = pwd;
    }

    /**
     * Assign the salt to the password
     *
     * @param salt the password salt
     */
    public final void assing(final String salt) {
        file.set(password, salt);
    }

    /**
     * Get the salt from the password
     *
     * @return the password salt
     */
    public final String getSalt() {
        return file.getString(password, "");
    }
}
