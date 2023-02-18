package eu.locklogin.plugin.bungee.util.files.client;

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
import eu.locklogin.api.util.enums.ManagerType;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.timer.scheduler.LateScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.worker.AsyncLateScheduler;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import static eu.locklogin.plugin.bungee.LockLogin.plugin;

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
    public AccountManager getAccount() throws IllegalStateException {
        if (CurrentPlatform.isValidAccountManager()) {
            AccountManager current = CurrentPlatform.getAccountManager(ManagerType.CUSTOM, null);
            if (current != null) {
                Set<AccountManager> managers = current.getAccounts();

                for (AccountManager manager : managers) {
                    if (manager.getName().equalsIgnoreCase(searching) || manager.getUUID().getId().equals(searching) || manager.getUUID().getId().replace("-", "").equals(searching))
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

    /**
     * Get the offline client account
     *
     * @return the offline client account
     */
    public LateScheduler<AccountManager> getAccountAsync() {
        LateScheduler<AccountManager> result = new AsyncLateScheduler<>();

        plugin.async().queue("fetch_account", () -> {
            if (CurrentPlatform.isValidAccountManager()) {
                AccountManager current = CurrentPlatform.getAccountManager(ManagerType.CUSTOM, null);
                if (current != null) {
                    Set<AccountManager> managers = current.getAccounts();

                    for (AccountManager manager : managers) {
                        if (manager.getName().equalsIgnoreCase(searching) || manager.getUUID().getId().equals(searching) || manager.getUUID().getId().replace("-", "").equals(searching)) {
                            result.complete(manager);
                            return;
                        }
                    }
                } else {
                    result.complete(null, new IllegalStateException("Couldn't continue with plugin task: Fetch offline accounts. Because account manager is null, maybe it doesn't has an empty constructor?"));
                }
            } else {
                result.complete(null, new IllegalStateException("Couldn't continue with plugin task: Fetch offline accounts. Because account manager is not valid"));
            }
        });


        return result;
    }
}
