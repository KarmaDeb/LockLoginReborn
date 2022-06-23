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

import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.karma.KarmaSource;
import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.karma.file.element.KarmaElement;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * LockLogin allowed commands manager
 */
public final class AllowedCommand {

    private final static KarmaSource plugin = APISource.loadProvider("LockLogin");
    private final static Set<String> allowed = new HashSet<>();

    /**
     * Scan for allowed passwords
     */
    public static void scan() {
        KarmaMain allowedFile = new KarmaMain(plugin, "allowed.lldb")
                .internal(AllowedCommand.class.getResourceAsStream("/security/allowed.lldb"));

        if (!allowedFile.exists())
            allowedFile.exportDefaults();

        KarmaElement a = allowedFile.get("allowed");

        if (a != null && a.isArray()) {
            a.getArray().forEach((entry) -> allowed.add(entry.getObjet().textValue()));
        }
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
    public static boolean notAllowed(final String command) {
        return allowed.stream().noneMatch(command::startsWith);
    }
}
