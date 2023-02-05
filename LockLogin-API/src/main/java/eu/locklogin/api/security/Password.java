package eu.locklogin.api.security;

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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * LockLogin password strength test factory
 */
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
                InputStream in = getClass().getResourceAsStream("/security/insecure.txt");
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
    public void addInsecure(final String... insecurities) {
        insecure.addAll(Arrays.asList(insecurities));
    }

    /**
     * Add insecure passwords to the official
     * list
     *
     * @param insecurities the insecure passwords
     */
    public void addInsecure(final Set<String> insecurities) {
        insecure.addAll(insecurities);
    }

    /**
     * Add insecure passwords to the official
     * list
     *
     * @param insecurities the insecure passwords
     */
    public void addInsecure(final List<String> insecurities) {
        insecure.addAll(insecurities);
    }

    /**
     * Check if the password is secure
     * or not
     *
     * @return if the password is secure
     */
    public boolean isSecure() {
        if (password.length() >= 4)
            return insecure.stream().noneMatch(password::contains);

        return false;
    }
}
