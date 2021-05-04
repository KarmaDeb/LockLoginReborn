package ml.karmaconfigs.locklogin.api.account;

import ml.karmaconfigs.api.common.karmafile.GlobalKarmaFile;
import ml.karmaconfigs.api.common.utils.FileUtilities;

import java.io.File;

/**
 * LockLogin AzuriomID memory
 * <p>
 * As azuriom generates random UUIDs, instead of
 * using bukkit "OfflinePlayer:[name]" uuid conversion,
 * LockLogin must memorize to which account is assigned each
 * id. This is mostly used by PlayerFile, but could be
 * used by any other plugin or module.
 */
public final class AzuriomId {

    private final String uuid;

    private final GlobalKarmaFile idData = new GlobalKarmaFile(new File(FileUtilities.getPluginsFolder() + File.separator + "LockLogin" + File.separator + "data" + File.separator + "azuriom", "ids.lldb"));

    /**
     * Initialize the azuriom id memory
     *
     * @param id the azuriom account id
     */
    public AzuriomId(final AccountID id) {
        uuid = id.getId();
    }

    /**
     * Assign the uuid to an account
     *
     * @param name the account name
     */
    public final void assignTo(final String name) {
        idData.set(name, uuid);
    }

    /**
     * Get the account file
     *
     * @return the azuriom id account file
     */
    public final File getAccountFile() {
        return new File(FileUtilities.getPluginsFolder() + File.separator + "LockLogin" + File.separator + "data" + File.separator + "accounts", uuid + ".lldb");
    }
}
