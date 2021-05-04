package ml.karmaconfigs.locklogin.plugin.bukkit.plugin.bungee.data;

import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.encryption.CryptoUtil;
import ml.karmaconfigs.locklogin.api.encryption.HashType;
import ml.karmaconfigs.locklogin.plugin.common.JarManager;
import ml.karmaconfigs.locklogin.plugin.common.session.SessionDataContainer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class BungeeDataStorager {

    private final static Set<AccountManager> accounts = new HashSet<>();
    @SuppressWarnings("FieldMayBeFinal")
    //This could be modified by the cache loader or when a bungeecord message has been received for the first time, so it can't be final
    private static String key = "";
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
                String token = CryptoUtil.getBuilder().withPassword(providedKey).build().hash(HashType.SHA512, true);

                JarManager.changeField(BungeeDataStorager.class, "key", true, token);
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Validate the specified bungee storager key
     *
     * @return if the specified key is valid
     */
    public final boolean validate() {
        CryptoUtil util = CryptoUtil.getBuilder().withPassword(provided).withToken(key).build();
        return util.validate();
    }

    /**
     * Add and update the specified bungee account instances
     *
     * @param bungeeAccounts the bungee account instances
     */
    public final void updateAccounts(final AccountManager... bungeeAccounts) {
        accounts.clear();
        accounts.addAll(Arrays.asList(bungeeAccounts));
    }

    /**
     * Get the registered accounts amount
     *
     * @return the registered accounts amount
     */
    public final int getRegisteredAccounts() {
        return SessionDataContainer.getRegistered();
    }

    /**
     * Set the registered accounts amount
     *
     * @param amount the registered accounts amount
     */
    public final void setRegisteredAccounts(final int amount) {
        SessionDataContainer.setRegistered(amount);
    }

    /**
     * Get the logged accounts amount
     *
     * @return the logged accounts amount
     */
    public final int getLoggedAccounts() {
        return SessionDataContainer.getLogged();
    }

    /**
     * Set the logged accounts amount
     *
     * @param amount the logged accounts amount
     */
    public final void setLoggedAccounts(final int amount) {
        SessionDataContainer.setLogged(amount);
    }
}
