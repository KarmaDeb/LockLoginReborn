package eu.locklogin.api.common.security;

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

import ml.karmaconfigs.api.common.karmafile.KarmaFile;
import ml.karmaconfigs.api.common.utils.FileUtilities;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * LockLogin allowed commands manager
 */
public final class AllowedCommand {

    private final static Set<String> allowed = new HashSet<>();

    /**
     * Scan for allowed passwords
     */
    public static void scan() {
        File file = new File(FileUtilities.getPluginsFolder() + File.separator + "LockLogin", "allowed.lldb");
        KarmaFile allowedFile = new KarmaFile(file);

        if (!allowedFile.exists())
            allowedFile.exportFromFile(AllowedCommand.class.getResourceAsStream("/security/allowed.lldb"));

        allowed.addAll(allowedFile.getStringList("ALLOWED", "recovery"));
    }

    /**
     * Add all the specified commands to allowed
     * commands
     *
     * @param commands the commands to allow
     */
    public static void add(final List<String> commands) {
        allowed.addAll(commands);
    }

    /**
     * Get if the command is allowed
     *
     * @param command the command
     * @return if it's allowed
     */
    public static boolean isAllowed(final String command) {
        return allowed.stream().anyMatch(command::startsWith);
    }
}
