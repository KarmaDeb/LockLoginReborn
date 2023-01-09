package eu.locklogin.api.file.pack.key;

import eu.locklogin.api.file.pack.sub.SubPack;
import ml.karmaconfigs.api.common.string.StringUtils;

public final class DefaultKey extends PackKey {

    private SubPack sub = null;
    private Number num = null;
    private Boolean bool = null;
    private String str = null;
    private Character ch = null;

    /**
     * Initialize the default key
     *
     * @param p the parameter
     */
    public DefaultKey(final SubPack p) {
        sub = p;
    }

    /**
     * Initialize the default key
     *
     * @param p the parameter
     */
    public DefaultKey(final Number p) {
        num = p;
    }

    /**
     * Initialize the default key
     *
     * @param p the parameter
     */
    public DefaultKey(final Boolean p) {
        bool = p;
    }

    /**
     * Initialize the default key
     *
     * @param p the parameter
     */
    public DefaultKey(final String p) {
        str = p;
    }

    /**
     * Initialize the default key
     *
     * @param p the parameter
     */
    public DefaultKey(final Character p) {
        ch = p;
    }

    /**
     * Get if the pack key is a number
     *
     * @return the pack type
     */
    @Override
    public boolean isNumber() {
        return num != null;
    }

    /**
     * Get if the pack key is a boolean
     *
     * @return the pack type
     */
    @Override
    public boolean isBoolean() {
        return bool != null;
    }

    /**
     * Get if the pack key is a string
     *
     * @return the pack type
     */
    @Override
    public boolean isString() {
        return str != null;
    }

    /**
     * Get if the pack key is a character
     *
     * @return the pack type
     */
    @Override
    public boolean isCharacter() {
        return ch != null;
    }

    /**
     * Get if the pack key is a sub pack
     *
     * @return the pack type
     */
    @Override
    public boolean isSub() {
        return sub != null;
    }

    /**
     * Get if the key is a null key
     *
     * @return the pack type
     */
    @Override
    public boolean isNull() {
        return sub != null || num != null || bool != null || str != null || ch != null;
    }

    /**
     * Get the key as integer
     *
     * @return the integer
     * @throws IllegalStateException if the integer cannot be got
     */
    @Override
    public Number getAsNumber() throws IllegalStateException {
        if (num != null) {
            return num;
        } else {
            throw new IllegalStateException("Cannot get number for language pack because the value is not a number");
        }
    }

    /**
     * Get the key as boolean
     *
     * @return the boolean
     * @throws IllegalStateException if the boolean cannot be got
     */
    @Override
    public boolean getAsBool() {
        if (num != null || bool != null || str != null || ch != null) {
            if (bool != null) {
                return bool;
            }
            if (str != null) {
                if (str.equalsIgnoreCase("true") || str.equalsIgnoreCase("1") || str.equalsIgnoreCase("y")) {
                    return true;
                } else {
                    if (str.equalsIgnoreCase("false") || str.equalsIgnoreCase("0") || str.equalsIgnoreCase("n")) {
                        return false;
                    }
                }
            }
            if (num != null) {
                if (num.intValue() == 1) {
                    return true;
                } else {
                    if (num.intValue() == 0) {
                        return false;
                    }
                }
            }
            if (ch != null) {
                if (ch == 'y' || ch == 'Y' || ch == '1') {
                    return true;
                } else {
                    if (ch == 'n' || ch == 'N' || ch == '0') {
                        return false;
                    }
                }
            }
        }

        throw new IllegalStateException("Cannot get boolean for language pack because the value is not true, false, y, Y, n, N, 0 or 1");
    }

    /**
     * Get the key as text
     *
     * @param replaces the text replaces
     * @return the text
     * @throws IllegalStateException if the text cannot be got
     */
    @Override
    public String getAsText(final Object... replaces) {
        if (num != null || bool != null || str != null || ch != null) {
            if (str != null) {
                return StringUtils.formatString(str, replaces);
            }
            if (bool != null) {
                return bool.toString().toLowerCase();
            }
            if (num != null) {
                return num.toString();
            }

            return String.valueOf(ch);
        }

        throw new IllegalStateException("Cannot get string for language pack because the value is a sub key or null");
    }

    /**
     * Get the character
     *
     * @return the character
     * @throws IllegalStateException if the character cannot be got
     */
    @Override
    public char getAsChar() {
        if (ch != null) {
            return ch;
        } else {
            throw new IllegalStateException("Cannot get character for language pack because the value is not a character");
        }
    }
}
