package ml.karmaconfigs.locklogin.plugin.common.session;

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

import ml.karmaconfigs.api.common.karmafile.GlobalKarmaFile;
import ml.karmaconfigs.api.common.utils.FileUtilities;
import ml.karmaconfigs.locklogin.api.account.AccountID;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.utils.platform.CurrentPlatform;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * LockLogin persistent session data
 */
public final class PersistentSessionData {

    private final static File folder = new File(FileUtilities.getPluginsFolder() + File.separator + "LockLogin", "data");
    private final static File file = new File(folder, "sessions.lldb");

    private final static GlobalKarmaFile sessions = new GlobalKarmaFile(file);

    private final AccountID id;

    /**
     * Initialize the persistent session data
     *
     * @param uuid the account id
     */
    public PersistentSessionData(final AccountID uuid) {
        id = uuid;
    }

    /**
     * Toggle the user persistent session
     * status
     *
     * @return the user persistent session
     * status
     */
    public final boolean toggleSession() {
        List<String> ids = sessions.getStringList("PERSISTENT");

        boolean result;
        if (ids.contains(id.getId())) {
            ids.remove(id.getId());
            result = false;
        } else {
            ids.add(id.getId());
            result = true;
        }

        sessions.set("PERSISTENT", ids);
        return result;
    }

    /**
     * Get if the user account is persistent or not
     *
     * @return if the user account is persistent
     */
    public final boolean isPersistent() {
        List<String> ids = sessions.getStringList("PERSISTENT");
        return ids.contains(id.getId());
    }

    /**
     * Get a list of persistent accounts
     *
     * @return a list of persistent accounts
     */
    public static Set<AccountManager> getPersistentAccounts() {
        Set<AccountManager> accounts = new LinkedHashSet<>();
        AccountManager tmp_manager = CurrentPlatform.getAccountManager(null);

        if (tmp_manager != null) {
            Set<AccountManager> tmp_accounts = tmp_manager.getAccounts();
            List<String> ids = sessions.getStringList("PERSISTENT");
            for (AccountManager manager : tmp_accounts) {
                if (manager != null) {
                    if (ids.contains(manager.getUUID().getId()))
                        accounts.add(manager);
                }
            }
        }

        return accounts;
    }
}
