package eu.locklogin.plugin.bukkit.util.files.client;

/*
 * Private GSA code
 *
 * The use of this code
 * without GSA team authorization
 * will be a violation of
 * terms of use determined
 * in <a href="http://karmaconfigs.cf/license/"> here </a>
 * or (fallback domain) <a href="https://karmaconfigs.github.io/page/license"> here </a>
 */

import eu.locklogin.api.account.AccountID;
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.util.enums.Manager;
import eu.locklogin.api.util.platform.CurrentPlatform;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public final class OfflineClient {

    private final String searching;

    /**
     * Initialize the offline client
     *
     * @param name the client name
     */
    public OfflineClient(final String name) {
        searching = name;
    }

    /**
     * Initialize the offline client
     *
     * @param id the client id
     */
    public OfflineClient(final AccountID id) {
        searching = id.getId();
    }

    /**
     * Get the offline client account
     *
     * @return the offline client account
     */
    @Nullable
    public AccountManager getAccount() {
        AccountManager result = null;

        if (CurrentPlatform.isValidAccountManager()) {
            AccountManager current = CurrentPlatform.getAccountManager(Manager.CUSTOM, null);
            if (current != null) {
                Set<AccountManager> managers = current.getAccounts();

                for (AccountManager manager : managers) {
                    if (manager.getName().equalsIgnoreCase(searching) || manager.getUUID().getId().equals(searching) || manager.getUUID().getId().replace("-", "").equals(searching)) {
                        result = manager;
                        break;
                    }
                }
            }
        }

        return result;
    }
}
