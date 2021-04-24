package ml.karmaconfigs.locklogin.plugin.common.security;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Password {

    private final static Set<String> insecure = new HashSet<>();

    private final String password;

    /**
     * Initialize the password util
     *
     * @param key the password that will be under the checks
     */
    public Password(final String key) {
        if (key != null)
            password = key;
        else
            password = "";

        if (insecure.isEmpty()) {
            try {
                InputStream in = getClass().getResourceAsStream("/insecure.txt");
                if (in != null) {
                    InputStreamReader inReader = new InputStreamReader(in, StandardCharsets.UTF_8);
                    BufferedReader reader = new BufferedReader(inReader);

                    String line;
                    while ((line = reader.readLine()) != null)
                        insecure.add(line);

                    in.close();
                    inReader.close();
                    reader.close();
                }
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * Add insecure passwords to the official
     * list
     *
     * @param insecurities the insecure passwords
     */
    public final void addInsecure(final String... insecurities) {
        insecure.addAll(Arrays.asList(insecurities));
    }

    /**
     * Add insecure passwords to the official
     * list
     *
     * @param insecurities the insecure passwords
     */
    public final void addInsecure(final Set<String> insecurities) {
        insecure.addAll(insecurities);
    }

    /**
     * Add insecure passwords to the official
     * list
     *
     * @param insecurities the insecure passwords
     */
    public final void addInsecure(final List<String> insecurities) {
        insecure.addAll(insecurities);
    }

    /**
     * Check if the password is secure
     * or not
     *
     * @return if the password is secure
     */
    public final boolean isSecure() {
        if (password.length() >= 4)
            return insecure.stream().noneMatch(password::contains);

        return false;
    }
}
