package ml.karmaconfigs.locklogin.api.modules.api.command;

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
 * LockLogin command data
 */
public final class CommandData {

    private final Command command;
    private final String[] arguments;
    private final String description;

    /**
     * Initialize the command data
     *
     * @param main the main command
     */
    public CommandData(final Command main) {
        command = main;
        arguments = main.validAliases();
        description = main.getDescription();
    }

    /**
     * Get the command owning this data
     *
     * @return the command that owns this data
     */
    public final Command getOwner() {
        return command;
    }

    /**
     * Get the command arguments
     *
     * @return the command valid arguments
     */
    public String[] getArguments() {
        return arguments;
    }

    /**
     * Get the command description
     *
     * @return the command description
     */
    public String getDescription() {
        return description;
    }
}
