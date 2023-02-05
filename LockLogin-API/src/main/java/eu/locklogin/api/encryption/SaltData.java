package eu.locklogin.api.encryption;

import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.karma.file.element.KarmaPrimitive;
import ml.karmaconfigs.api.common.karma.file.element.types.Element;
import ml.karmaconfigs.api.common.karma.file.element.types.ElementPrimitive;
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
        file.setRaw(password, salt);
    }

    /**
     * Get the salt from the password
     *
     * @return the password salt
     */
    public String getSalt() {
        Element<?> element = file.get(password, new KarmaPrimitive(""));
        if (element.isPrimitive()) {
            ElementPrimitive primitive = element.getAsPrimitive();
            if (primitive.isString()) return primitive.asString();
        }

        return "";
    }
}
