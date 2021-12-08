package eu.locklogin.api.module.plugin.api.command;

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

import eu.locklogin.api.module.plugin.javamodule.sender.ModuleSender;

/**
 * LockLogin command initializer
 */
public abstract class Command {

    private final String[] alias;
    private final String desc;
    private boolean hide_call = false;

    /**
     * Initialize the command
     *
     * @param description the command description
     * @param aliases     the command aliases
     */
    public Command(final String description, final String... aliases) {
        desc = description;
        alias = aliases;
    }

    /**
     * Initialize the command
     *
     * @param description the command description
     * @param sensitive   if the command should be parsed as sensitive command
     * @param aliases     the command valid aliases
     */
    public Command(final String description, final boolean sensitive, final String... aliases) {
        desc = description;
        hide_call = sensitive;
        alias = aliases;
    }

    /**
     * Process the command when
     * its fired
     *
     * @param arg        the used argument
     * @param sender     the command sender
     * @param parameters the command parameters
     */
    public abstract void processCommand(final String arg, final ModuleSender sender, final String... parameters);

    /**
     * Process the command when
     * its fired
     *
     * @param arg        the used argument
     * @param sender     the command sender
     * @param parameters the command parameters
     * @deprecated It's better to use {@link Command#processCommand(String, ModuleSender, String...)}
     * instead as this contains directly the command issuers
     */
    @Deprecated
    public void processCommand(final String arg, final Object sender, final String... parameters) {
    }

    /**
     * Get the valid command arguments
     *
     * @return the valid command arguments
     */
    public final String[] validAliases() {
        return alias;
    }

    /**
     * Get the command description
     *
     * @return the command description
     */
    public final String getDescription() {
        return desc;
    }

    /**
     * Get if the command is a sensitive command
     *
     * @return if the command is sensitive
     */
    public final boolean isSensitive() {
        return hide_call;
    }
}
