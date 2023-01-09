package eu.locklogin.api.encryption.libraries.sha;

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

import com.google.common.hash.Hashing;
import ml.karmaconfigs.api.common.string.random.RandomString;
import ml.karmaconfigs.api.common.string.text.TextContent;
import ml.karmaconfigs.api.common.string.text.TextType;

import java.nio.charset.StandardCharsets;

/**
 * SHA256 utilities
 */
public final class SHA256 {

    private final Object password;

    /**
     * Initialize the codification
     *
     * @param value the value to codify
     */
    public SHA256(final Object value) {
        this.password = value;
    }

    /**
     * Check if the specified password
     * is correct
     *
     * @param token the provided token password
     * @return a boolean
     */
    public boolean check(final String token) {
        try {
            String[] data = token.split("\\$");
            String salt = data[2];

            String generated = hash();
            String generated_salt = generated.split("\\$")[2];
            generated = generated.replaceFirst(generated_salt, salt);

            return generated.equals(token);
        } catch (Throwable ex) {
            //Add compatibility with old SHA256 generation

            String old_token = token;
            if (token.contains("\\$")) {
                String[] data = token.split("\\$");

                int max = data.length - 1;

                StringBuilder removeFrom = new StringBuilder();
                for (int i = 0; i < max; i++) {
                    removeFrom.append("$").append(data[i]);
                }

                old_token = token.replace(removeFrom.toString(), "");
            }

            String hashed = hash();
            String salt = hashed.split("\\$")[2];

            hashed = hashed.replaceFirst("\\$SHA256\\$" + salt + "\\$", "");

            return old_token.equals(hashed);
        }
    }

    /**
     * hashEncrypted and encrypt the value
     *
     * @return a hashed value
     */
    @SuppressWarnings("all")
    public String hash() {
        String random_salt = new RandomString(RandomString.createBuilder().
                withSize(64)
                .withContent(TextContent.ONLY_LETTERS)
                .withType(TextType.ALL_UPPER)).create();

        return "$SHA256$" + random_salt + "$" + Hashing.sha256().hashString(password.toString(), StandardCharsets.UTF_8).toString();
    }
}
