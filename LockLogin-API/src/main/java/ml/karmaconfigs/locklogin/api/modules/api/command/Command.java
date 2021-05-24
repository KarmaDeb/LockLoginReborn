package ml.karmaconfigs.locklogin.api.modules.api.command;

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
    public abstract void processCommand(final String arg, final Object sender, final String... parameters);

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
