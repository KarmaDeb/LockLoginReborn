package eu.locklogin.api.common.utils;

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

/**
 * Valid channels
 */
public enum Channel {
    /**
     * Account channel
     */
    ACCOUNT("ll:account"),
    /**
     * Plugin channel
     */
    PLUGIN("ll:plugin"),
    /**
     * Access channel
     */
    ACCESS("ll:access");

    /**
     * Channel name
     */
    private final String name;

    /**
     * Create the channel
     *
     * @param n the channel name
     */
    Channel(final String n) {
        name = n;
    }

    /**
     * Get the channel name
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    public static Channel fromName(final String name) {
        switch (name.toLowerCase()) {
            case "ll:account":
                return Channel.ACCOUNT;
            case "ll:plugin":
                return Channel.PLUGIN;
            case "ll:access":
                return Channel.ACCESS;
        }

        return null;
    }
}
