package ml.karmaconfigs.locklogin.plugin.bukkit.plugin;

import ml.karmaconfigs.api.bukkit.KarmaFile;
import ml.karmaconfigs.api.common.utils.FileUtilities;
import ml.karmaconfigs.locklogin.api.encryption.CryptType;
import ml.karmaconfigs.locklogin.api.encryption.CryptoUtil;

import java.io.File;

public final class ConsoleAccount {

    private final KarmaFile accountFile = new KarmaFile(new File(FileUtilities.getServerFolder(), "server"));

    /**
     * Initialize the console account
     */
    public ConsoleAccount() {
        if (!accountFile.exists())
            accountFile.create();
    }

    /**
     * Set the console account password
     *
     * For security terms, the password won't be able to be changed
     * without the last password
     *
     * @param password the console account password
     */
    public final void setPassword(final String password) {
        if (accountFile.getString("PASSWORD", "").replaceAll("\\s", "").isEmpty()) {
            CryptoUtil util = new CryptoUtil(password, null);

            accountFile.set("PASSWORD", util.hash(CryptType.SHA512, true));
        }
    }

    /**
     * Change the console password
     *
     * @param provided the current password
     * @param password the new password
     */
    public final boolean changePassword(final String provided, final String password) {
        String set = accountFile.getString("PASSWORD", "").replaceAll("\\s", "");
        if (set.replaceAll("\\s", "").isEmpty()) {
            setPassword(password);
            return true;
        }

        CryptoUtil util = new CryptoUtil(provided, set);
        if (util.validate()) {
            CryptoUtil newCrypt = new CryptoUtil(password, null);
            accountFile.set("PASSWORD", newCrypt.hash(CryptType.SHA512, true));

            return true;
        }

        return false;
    }

    /**
     * Validate the console access
     *
     * @param password the provided password
     * @return if the access has been granted
     */
    public final boolean validate(final String password) {
        if (isRegistered()) {
            String set = accountFile.getString("PASSWORD", "").replaceAll("\\s", "");

            CryptoUtil util = new CryptoUtil(password, set);
            return util.validate();
        }

        return false;
    }

    /**
     * Check if the console is registered
     *
     * @return if the console is registered
     */
    public final boolean isRegistered() {
        return !accountFile.getString("PASSWORD", "").replaceAll("\\s", "").isEmpty();
    }
}
