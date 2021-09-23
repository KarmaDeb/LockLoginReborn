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
    private final String[] aliases;

    /**
     * Initialize the bungee like command
     *
     * @param label the command label
     * @param alias the command aliases
     */
    public BungeeLikeCommand(final String label, final String... alias) {
        arg = label;
        aliases = alias;
    }

    public abstract void execute(final CommandSource sender, final String[] args);

    public final String getArg() {
        return arg;
    }

    public final String[] getAliasses() {
        return aliases;
    }
}
