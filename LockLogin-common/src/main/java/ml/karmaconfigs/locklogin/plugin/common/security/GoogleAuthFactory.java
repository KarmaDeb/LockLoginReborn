package ml.karmaconfigs.locklogin.plugin.common.security;

/*
 * GNU LESSER GENERAL PUBLIC LICENSE
 * Version 2.1, February 1999
 * <p>
 * Copyright (C) 1991, 1999 Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * Everyone is permitted to copy and distribute verbatim copies
 * of this license document, but changing it is not allowed.
 * <p>
 * [This is the first released version of the Lesser GPL.  It also counts
 * as the successor of the GNU Library Public License, version 2, hence
 * the version number 2.1.]
 */

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorException;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.warrenstrange.googleauth.GoogleAuthenticator.SCRATCH_CODE_MODULUS;

/**
 * LockLogin 2fa factory
 */
public final class GoogleAuthFactory {

    private final static GoogleAuthenticator authenticator = new GoogleAuthenticator();

    private final static Map<UUID, List<Integer>> recovery_codes = new LinkedHashMap<>();

    private final UUID playerID;
    private final String player;

    /**
     * Initialize the google auth factory
     *
     * @param owner the google auth factory
     *              player target
     * @param name  the google auth factory
     *              player target name
     */
    public GoogleAuthFactory(final UUID owner, final String name) {
        playerID = owner;
        player = name;
    }

    /**
     * Generate a new google auth token
     * for the user
     *
     * @return the user token
     */
    public final String generateToken() {
        GoogleAuthenticatorKey credentials = authenticator.createCredentials();

        recovery_codes.put(playerID, credentials.getScratchCodes());
        return credentials.getKey();
    }

    /**
     * Get the user recovery codes
     *
     * @return the user recovery codes
     */
    public final List<Integer> getRecoveryCodes() {
        return recovery_codes.getOrDefault(playerID, Collections.emptyList());
    }

    /**
     * Validate the user code
     *
     * @return if the user code is valid
     */
    public final boolean validate(final String token, final int code) {
        return authenticator.authorize(token, code);
    }

    /**
     * Get the scratch code generator
     */
    public static class ScratchGenerator {

        private static final String DEFAULT_RANDOM_NUMBER_ALGORITHM = "SHA1PRNG";
        private static final String DEFAULT_RANDOM_NUMBER_ALGORITHM_PROVIDER = "SUN";

        /**
         * @return the default random number generator algorithm.
         * @since 0.5.0
         */
        private static String getRandomNumberAlgorithm() {
            return System.getProperty(
                    GoogleAuthenticator.RNG_ALGORITHM,
                    DEFAULT_RANDOM_NUMBER_ALGORITHM);
        }

        /**
         * @return the default random number generator algorithm provider.
         * @since 0.5.0
         */
        private static String getRandomNumberAlgorithmProvider() {
            return System.getProperty(
                    GoogleAuthenticator.RNG_ALGORITHM_PROVIDER,
                    DEFAULT_RANDOM_NUMBER_ALGORITHM_PROVIDER);
        }

        /**
         * Get a list of new scratch codes
         *
         * @return a list of new scratch codes
         */
        public static List<Integer> generate() {
            final List<Integer> scratchCodes = new ArrayList<>();

            for (int i = 0; i < 6; ++i) {
                scratchCodes.add(generateScratchCode());
            }

            return scratchCodes;
        }

        /**
         * This method calculates a scratch code from a random byte buffer of
         * suitable size <code>#BYTES_PER_SCRATCH_CODE</code>.
         *
         * @param scratchCodeBuffer a random byte buffer whose minimum size is
         *                          <code>#BYTES_PER_SCRATCH_CODE</code>.
         * @return the scratch code.
         */
        private static int calculateScratchCode(byte[] scratchCodeBuffer) {
            if (scratchCodeBuffer.length < 4) {
                throw new IllegalArgumentException(
                        String.format(
                                "The provided random byte buffer is too small: %d.",
                                scratchCodeBuffer.length));
            }

            int scratchCode = 0;

            for (int i = 0; i < 4; ++i) {
                scratchCode = (scratchCode << 8) + (scratchCodeBuffer[i] & 0xff);
            }

            scratchCode = (scratchCode & 0x7FFFFFFF) % SCRATCH_CODE_MODULUS;

            if (validateScratchCode(scratchCode)) {
                return scratchCode;
            } else {
                return -1;
            }
        }

        /**
         * Validate the scratch code
         *
         * @param scratchCode the scratch code
         * @return if the scratch code is valid
         */
        private static boolean validateScratchCode(int scratchCode) {
            return (scratchCode >= SCRATCH_CODE_MODULUS / 10);
        }

        /**
         * This method creates a new random byte buffer from which a new scratch
         * code is generated. This function is invoked if a scratch code generated
         * from the main buffer is invalid because it does not satisfy the scratch
         * code restrictions.
         *
         * @return A valid scratch code.
         */
        private static int generateScratchCode() {
            ReseedingSecureRandom secureRandom = new ReseedingSecureRandom(getRandomNumberAlgorithm(), getRandomNumberAlgorithmProvider());

            while (true) {
                byte[] scratchCodeBuffer = new byte[4];
                secureRandom.nextBytes(scratchCodeBuffer);

                int scratchCode = calculateScratchCode(scratchCodeBuffer);

                if (scratchCode != -1) {
                    return scratchCode;
                }
            }
        }

        static class ReseedingSecureRandom {

            private static final int MAX_OPERATIONS = 1_000_000;
            private final String provider;
            private final String algorithm;
            private final AtomicInteger count = new AtomicInteger(0);
            private volatile SecureRandom secureRandom;

            ReseedingSecureRandom(String algorithm, String provider) {
                if (algorithm == null) {
                    throw new IllegalArgumentException("Algorithm cannot be null.");
                }

                if (provider == null) {
                    throw new IllegalArgumentException("Provider cannot be null.");
                }

                this.algorithm = algorithm;
                this.provider = provider;

                buildSecureRandom();
            }

            private void buildSecureRandom() {
                try {
                    if (this.algorithm == null && this.provider == null) {
                        this.secureRandom = new SecureRandom();
                    } else if (this.provider == null) {
                        this.secureRandom = SecureRandom.getInstance(this.algorithm);
                    } else {
                        this.secureRandom = SecureRandom.getInstance(this.algorithm, this.provider);
                    }
                } catch (NoSuchAlgorithmException e) {
                    throw new GoogleAuthenticatorException(
                            String.format(
                                    "Could not initialise SecureRandom with the specified algorithm: %s. " +
                                            "Another provider can be chosen setting the %s system property.",
                                    this.algorithm,
                                    GoogleAuthenticator.RNG_ALGORITHM
                            ), e
                    );
                } catch (NoSuchProviderException e) {
                    throw new GoogleAuthenticatorException(
                            String.format(
                                    "Could not initialise SecureRandom with the specified provider: %s. " +
                                            "Another provider can be chosen setting the %s system property.",
                                    this.provider,
                                    GoogleAuthenticator.RNG_ALGORITHM_PROVIDER
                            ), e
                    );
                }
            }

            void nextBytes(byte[] bytes) {
                if (count.incrementAndGet() > MAX_OPERATIONS) {
                    synchronized (this) {
                        if (count.get() > MAX_OPERATIONS) {
                            buildSecureRandom();
                            count.set(0);
                        }
                    }
                }

                this.secureRandom.nextBytes(bytes);
            }
        }
    }
}
