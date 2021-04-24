package ml.karmaconfigs.locklogin.plugin.bukkit.util.files.client;

import ml.karmaconfigs.locklogin.api.account.AccountID;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.plugin.common.utils.platform.CurrentPlatform;
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
    public final AccountManager getAccount() throws IllegalStateException {
        if (CurrentPlatform.isValidAccountManager()) {
            AccountManager current = CurrentPlatform.getAccountManager(null);
            if (current != null) {
                Set<AccountManager> managers = current.getAccounts();

                for (AccountManager manager : managers) {
                    if (manager.getName().equals(searching) || manager.getUUID().getId().equals(searching))
                        return manager;
                }
            } else {
                throw new IllegalStateException("Couldn't continue with plugin task: Fetch offline accounts. Because account manager is null, maybe it doesn't has an empty constructor?");
            }
        } else {
            throw new IllegalStateException("Couldn't continue with plugin task: Fetch offline accounts. Because account manager is not valid");
        }

        return null;
    }
}
