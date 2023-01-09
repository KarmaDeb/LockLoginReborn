package eu.locklogin.api.encryption;

import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.karma.file.element.KarmaElement;
import ml.karmaconfigs.api.common.karma.source.APISource;

/**
 * LockLogin salts data for DynamicBungeeAuth
 * SHA512 auth API
 */
public final class SaltData {

    private final static KarmaMain file = new KarmaMain(APISource.loadProvider("LockLogin"), "dba.kf", "cache");
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
    @SuppressWarnings("unused")
    public void assign(final String salt) {
        file.set(password, KarmaElement.from(salt));
    }

    /**
     * Get the salt from the password
     *
     * @return the password salt
     */
    public String getSalt() {
        return file.get(password, KarmaElement.from("")).getObjet().getString();
    }
}
