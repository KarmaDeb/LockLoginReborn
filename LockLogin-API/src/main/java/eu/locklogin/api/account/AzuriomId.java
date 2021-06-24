package eu.locklogin.api.account;

/*
 * GNU LESSER GENERAL PUBLIC LICENSE
 * Version 2.1, February 1999
 * <p>
 * Copyright (C) 1991, 1999 Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * Everyone is permitted to copy and distribute verbatim copies
 * of this license document, but changing it is not allowed.
 * <p>
 * [This is the first released version of the Lesser GPL.  It also counts
 * as the successor of the GNU Library Public License, version 2, hence
 * the version number 2.1.]
 */

import ml.karmaconfigs.api.common.karmafile.KarmaFile;
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

    private final KarmaFile idData = new KarmaFile(new File(FileUtilities.getProjectFolder() + File.separator + "LockLogin" + File.separator + "data" + File.separator + "azuriom", "ids.lldb"));

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
        return new File(FileUtilities.getProjectFolder() + File.separator + "LockLogin" + File.separator + "data" + File.separator + "accounts", uuid + ".lldb");
    }
}
