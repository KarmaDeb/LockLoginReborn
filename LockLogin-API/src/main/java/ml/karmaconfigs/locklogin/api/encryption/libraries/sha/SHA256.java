package ml.karmaconfigs.locklogin.api.encryption.libraries.sha;

import com.google.common.hash.Hashing;
import ml.karmaconfigs.api.common.utils.StringUtils;

import java.nio.charset.StandardCharsets;

public class SHA256 {

    private final Object password;

    /**
     * Initialize the codification
     *
     * @param value  the value to codify
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
    public final boolean check(final String token) {
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
    public final String hash() {
        String random_salt = StringUtils.randomString(64, StringUtils.StringGen.ONLY_LETTERS, StringUtils.StringType.ALL_UPPER);

        return "$SHA256$" + random_salt + "$" + Hashing.sha256().hashString(password.toString(), StandardCharsets.UTF_8).toString();
    }
}
