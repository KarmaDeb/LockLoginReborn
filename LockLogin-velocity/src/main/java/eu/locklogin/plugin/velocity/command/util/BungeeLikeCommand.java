package eu.locklogin.plugin.velocity.command.util;

/*
 * Private GSA code
 *
 * The use of this code
 * without GSA team authorization
 * will be a violation of
 * terms of use determined
 * in <a href="http://karmaconfigs.cf/license/"> here </a>
 * or (fallback domain) <a href="https://karmaconfigs.github.io/page/license"> here </a>
 */

import com.velocitypowered.api.command.CommandSource;

public abstract class BungeeLikeCommand {

    private final String arg;

    /**
     * Initialize the bungee like command
     *
     * @param label the command label
     */
    public BungeeLikeCommand(final String label) {
        arg = label;
    }

    public abstract void execute(final CommandSource sender, final String[] args);

    public final String getArg() {
        return arg;
    }
}
