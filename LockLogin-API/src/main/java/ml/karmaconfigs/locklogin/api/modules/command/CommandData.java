package ml.karmaconfigs.locklogin.api.modules.command;

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
