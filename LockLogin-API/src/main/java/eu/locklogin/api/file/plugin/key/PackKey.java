package eu.locklogin.api.file.plugin.key;

public abstract class PackKey {

    /**
     * Get if the pack key is a number
     *
     * @return the pack type
     */
    public abstract boolean isNumber();

    /**
     * Get if the pack key is a boolean
     *
     * @return the pack type
     */
    public abstract boolean isBoolean();

    /**
     * Get if the pack key is a string
     *
     * @return the pack type
     */
    public abstract boolean isString();

    /**
     * Get if the pack key is a character
     *
     * @return the pack type
     */
    public abstract boolean isCharacter();

    /**
     * Get if the pack key is a sub pack
     *
     * @return the pack type
     */
    public abstract boolean isSub();

    /**
     * Get if the key is a null key
     *
     * @return the pack type
     */
    public abstract boolean isNull();

    /**
     * Get the key as integer
     *
     * @return the integer
     * @throws IllegalStateException if the integer cannot be got
     */
    public abstract int getAsInt() throws IllegalStateException;

    /**
     * Get the key as boolean
     *
     * @return the boolean
     * @throws IllegalStateException if the boolean cannot be got
     */
    public abstract boolean getAsBool();

    /**
     * Get the key as text
     *
     * @param replaces the text replaces
     * @return the text
     * @throws IllegalStateException if the text cannot be got
     */
    public abstract String getAsText(final Object... replaces);

    /**
     * Get the character
     *
     * @return the character
     * @throws IllegalStateException if the character cannot be got
     */
    public abstract char getAsChar();
}
