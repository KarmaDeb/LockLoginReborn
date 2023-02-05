package eu.locklogin.api.util.enums;

/**
 * Password check type
 */
public enum CheckType {
    /**
     * If password is unique
     */
    UNIQUE,
    /**
     * If password has the minimum length
     */
    LENGTH,
    /**
     * If password has the minimum special characters
     */
    SPECIAL,
    /**
     * If password has the minimum amount of numbers
     */
    NUMBER,
    /**
     * If password has the minimum amount of lowercase letters
     */
    LOWER,
    /**
     * If password has the minimum amount of uppercase letters
     */
    UPPER
}
