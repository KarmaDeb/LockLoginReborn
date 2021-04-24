package ml.karmaconfigs.locklogin.plugin.bukkit.plugin.bungee;

import ml.karmaconfigs.locklogin.api.encryption.CryptType;
import ml.karmaconfigs.locklogin.api.encryption.CryptoUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class BungeeDataStorager {

    private final static String key = "";

    private static int registered_accounts = 0;
    private static int logged_accounts = 0;

    private final String provided;

    /**
     * Initialize the bungee data storager
     *
     * @param providedKey the bungeecord key
     */
    public BungeeDataStorager(final String providedKey) {
        provided = providedKey;

        if (key.replaceAll("\\s", "").isEmpty()) {
            try {
                Class<?> storager = BungeeDataStorager.class;
                Field sessionsField = storager.getDeclaredField("key");

                CryptoUtil util = new CryptoUtil(providedKey, null);

                sessionsField.setAccessible(true);
                sessionsField.setInt(sessionsField, sessionsField.getModifiers() & ~Modifier.FINAL);
                sessionsField.set(storager, util.hash(CryptType.SHA512, true));
                sessionsField.setInt(sessionsField, sessionsField.getModifiers() & Modifier.FINAL);
            } catch (Throwable ignored) {}
        }
    }

    /**
     * Validate the specified bungee storager key
     *
     * @return if the specified key is valid
     */
    public final boolean validate() {
        CryptoUtil util = new CryptoUtil(provided, key);
        return util.validate();
    }

    /**
     * Set the registered accounts amount
     *
     * @param amount the registered accounts amount
     */
    public final void setRegisteredAccounts(final int amount) {
        registered_accounts = amount;
    }

    /**
     * Set the logged accounts amount
     *
     * @param amount the logged accounts amount
     */
    public final void setLoggedAccounts(final int amount) {
        logged_accounts = amount;
    }

    /**
     * Add a login
     */
    public final void addLogin() {
        logged_accounts++;
    }

    /**
     * Rest a login
     */
    public final void restLogin() {
        logged_accounts--;
    }

    /**
     * Get the registered accounts amount
     *
     * @return the registered accounts amount
     */
    public final int getRegisteredAccounts() {
        return registered_accounts;
    }

    /**
     * Get the logged accounts amount
     *
     * @return the logged accounts amount
     */
    public final int getLoggedAccounts() {
        return logged_accounts;
    }
}
