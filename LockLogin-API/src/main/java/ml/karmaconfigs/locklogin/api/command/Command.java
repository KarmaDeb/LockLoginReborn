package ml.karmaconfigs.locklogin.api.command;

/**
 * LockLogin command initializer
 */
public abstract class Command {

    private final String[] args;
    private final String desc;

    public Command(final String description, final String... arguments) {
        desc = description;
        args = arguments;
    }

    /**
     * Process the command when
     * its fired
     *
     * @param arg the used argument
     * @param sender the command sender
     * @param parameters the command parameters
     */
    public abstract void processCommand(final String arg, final Object sender, final String... parameters);

    /**
     * Get the valid command arguments
     *
     * @return the valid command arguments
     */
    public final String[] validArguments() {
        return args;
    }

    /**
     * Get the command description
     *
     * @return the command description
     */
    public final String getDescription() {
        return desc;
    }
}
