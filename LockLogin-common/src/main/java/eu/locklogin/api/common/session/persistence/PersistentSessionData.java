package eu.locklogin.api.common.session.persistence;

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

import eu.locklogin.api.account.AccountID;
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.util.enums.ManagerType;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.data.file.FileUtilities;
import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.karma.file.element.KarmaPrimitive;
import ml.karmaconfigs.api.common.karma.file.element.multi.KarmaArray;
import ml.karmaconfigs.api.common.karma.file.element.types.Element;
import ml.karmaconfigs.api.common.karma.file.element.types.ElementPrimitive;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * LockLogin persistent session data
 */
public final class PersistentSessionData {

    private final static File folder = new File(FileUtilities.getProjectFolder("plugins") + File.separator + "LockLogin", "data");
    private final static File file = new File(folder, "sessions.lldb");

    private final static KarmaMain sessions = new KarmaMain(file.toPath());

    private final AccountID id;

    /**
     * Initialize the persistent session data
     *
     * @param uuid the account id
     */
    public PersistentSessionData(final AccountID uuid) {
        id = uuid;

        if (!sessions.exists())
            sessions.create();
    }

    /**
     * Get a list of persistent accounts
     *
     * @return a set of persistent accounts
     */
    public static Set<AccountManager> getPersistentAccounts() {
        Set<AccountManager> accounts = new LinkedHashSet<>();
        AccountManager tmp_manager = CurrentPlatform.getAccountManager(ManagerType.CUSTOM, null);

        if (tmp_manager != null) {
            Set<AccountManager> tmp_accounts = tmp_manager.getAccounts();
            if (sessions.isSet("persistent")) {
                Element<?> element = sessions.get("persistent");

                if (element.isArray()) {
                    KarmaArray array = (KarmaArray) element.getAsArray();

                    for (AccountManager manager : tmp_accounts) {
                        if (manager != null) {
                            ElementPrimitive check_id = new KarmaPrimitive(manager.getUUID().getId());

                            if (array.contains(check_id))
                                accounts.add(manager);
                        }
                    }
                }
            }
        }

        return accounts;
    }

    /**
     * Toggle the user persistent session
     * status
     *
     * @return the user persistent session
     * status
     */
    public boolean toggleSession() {
        boolean result = false;

        if (sessions.isSet("persistent")) {
            Element<?> element = sessions.get("persistent");

            if (element.isArray()) {
                KarmaArray array = (KarmaArray) element.getAsArray();
                ElementPrimitive check_id = new KarmaPrimitive(id.getId());

                if (array.contains(check_id)) {
                    array.remove(check_id);
                } else {
                    array.add(check_id);
                    result = true;
                }

                sessions.set("persistent", array);
                sessions.save();
            }
        } else {
            KarmaArray array = new KarmaArray();
            array.add(id.getId());

            sessions.set("persistent", array);
            return sessions.save();
        }

        return result;
    }

    /**
     * Get if the user account is persistent or not
     *
     * @return if the user account is persistent
     */
    public boolean isPersistent() {
        if (sessions.isSet("persistent")) {
            Element<?> element = sessions.get("persistent");

            if (element.isArray()) {
                KarmaArray array = (KarmaArray) element.getAsArray();
                ElementPrimitive check_id = new KarmaPrimitive(id.getId());
                return array.contains(check_id);
            }
        }

        return false;
    }
}
