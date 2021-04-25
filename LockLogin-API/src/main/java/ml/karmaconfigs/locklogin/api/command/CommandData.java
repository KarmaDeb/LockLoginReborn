package ml.karmaconfigs.locklogin.api.command;

/**
 * LockLogin command data
 */
public final class CommandData {

    private final String[] arguments;
    private final String description;

    /**
     * Initialize the command data
     *
     * @param main the main command
     */
    public CommandData(final Command main) {
        arguments = main.validArguments();
        description = main.getDescription();
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
