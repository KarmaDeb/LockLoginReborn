package ml.karmaconfigs.locklogin.plugin.common.security;

import ml.karmaconfigs.api.common.karmafile.GlobalKarmaFile;
import ml.karmaconfigs.api.common.utils.FileUtilities;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class AllowedCommand {

    private final static Set<String> allowed = new HashSet<>();

    /**
     * Scan for allowed passwords
     */
    public static void scan() {
        File file = new File(FileUtilities.getPluginsFolder() + File.separator + "LockLogin", "allowed.lldb");
        GlobalKarmaFile allowedFile = new GlobalKarmaFile(file);

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
