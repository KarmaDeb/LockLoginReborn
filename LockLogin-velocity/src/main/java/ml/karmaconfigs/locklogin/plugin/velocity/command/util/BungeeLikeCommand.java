package ml.karmaconfigs.locklogin.plugin.velocity.command.util;

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
