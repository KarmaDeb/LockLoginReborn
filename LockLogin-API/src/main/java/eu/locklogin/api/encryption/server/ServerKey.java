package eu.locklogin.api.encryption.server;

import eu.locklogin.api.util.platform.CurrentPlatform;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * Server key to decrypt web panel data
 *
 * TODO: I love encrypting things  ~ :flushed: ~
 */
public final class ServerKey {

    /**
     * Get the key to decrypt the data.
     *
     * @return the assigned key
     */
    public static SecretKey getKey() {
        String key = CurrentPlatform.getConfiguration().serverKey();
        String decodedKey = new String(Base64.getDecoder().decode(key));
        byte[] decoded = decodedKey.split(";")[0].getBytes();

        return new SecretKeySpec(decoded, 0, decoded.length, "AES");
    }

    /**
     * Get the spec to make the key work
     *
     * @return the key spec
     */
    public static IvParameterSpec getSpec() {
        String key = CurrentPlatform.getConfiguration().serverKey();
        String decodedKey = new String(Base64.getDecoder().decode(key));
        byte[] decoded = decodedKey.split(";")[1].getBytes();

        return new IvParameterSpec(decoded);
    }
}
