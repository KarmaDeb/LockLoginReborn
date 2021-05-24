package ml.karmaconfigs.locklogin.plugin.common.session;

import ml.karmaconfigs.api.common.karmafile.GlobalKarmaFile;
import ml.karmaconfigs.api.common.utils.FileUtilities;
import ml.karmaconfigs.locklogin.api.account.AccountID;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.utils.platform.CurrentPlatform;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
