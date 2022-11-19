package eu.locklogin.api.common.session;

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
import eu.locklogin.api.util.enums.Manager;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.karma.file.element.KarmaArray;
import ml.karmaconfigs.api.common.karma.file.element.KarmaElement;
import ml.karmaconfigs.api.common.karma.file.element.KarmaKeyArray;
import ml.karmaconfigs.api.common.karma.file.element.KarmaObject;
import ml.karmaconfigs.api.common.utils.file.FileUtilities;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * LockLogin persistent session data
 */
public final class PersistentSessionData {

    private final static File folder = new File(FileUtilities.getProjectFolder("plugins") + File.separator + "LockLogin", "data");
    private final static File file = new File(folder, "sessions.lldb");

    private final static KarmaMain sessions = new KarmaMain(APISource.loadProvider("LockLogin"), file.toPath());

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
     * @return a list of persistent accounts
     */
    public static Set<AccountManager> getPersistentAccounts() {
        Set<AccountManager> accounts = new LinkedHashSet<>();
        AccountManager tmp_manager = CurrentPlatform.getAccountManager(Manager.CUSTOM, null);

        if (tmp_manager != null) {
            Set<AccountManager> tmp_accounts = tmp_manager.getAccounts();
            if (sessions.isSet("persistent")) {
                KarmaElement element = sessions.get("persistent");

                if (element.isArray()) {
                    KarmaArray array = element.getArray();

                    for (AccountManager manager : tmp_accounts) {
                        if (manager != null) {
                            if (array.contains(new KarmaObject(manager.getUUID().getId())))
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
            KarmaElement element = sessions.get("persistent");

            if (element.isArray()) {
                KarmaArray array = element.getArray();
                KarmaObject d = new KarmaObject(id.getId());

                if (array.contains(d)) {
                    array.remove(d);
                } else {
                    array.add(d);
                    result = true;
                }

                sessions.set("persistent", array);
                sessions.save();
            }
        } else {
            KarmaArray array = new KarmaArray();
            array.add(new KarmaObject(id.getId()));

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
            KarmaElement element = sessions.get("persistent");

            if (element.isArray()) {
                KarmaArray array = element.getArray();
                return array.contains(new KarmaObject(id.getId()));
            }
        }

        return false;
    }

    /**
     * Set the current session id
     *
     * @param session_id the new session id
     * @return the session id
     */
    public boolean setSessionId(final AccountID session_id) {
        KarmaKeyArray map = new KarmaKeyArray();
        if (sessions.isSet("sessions")) {
            KarmaElement element = sessions.get("sessions");
            if (element.isKeyArray())
                map = element.getKeyArray();
        }

        map.add(id.getId(), new KarmaObject(session_id.getId()), true);
        sessions.set("sessions", map);
        return sessions.save();
    }

    /**
     * Get the client current session name
     *
     * @return the client session name
     */
    public AccountID sessionId() {
        if (sessions.isSet("sessions")) {
            KarmaElement element = sessions.get("sessions");

            if (element.isKeyArray()) {
                KarmaKeyArray map = element.getKeyArray();

                for (String key : map.getKeys()) {
                    if (key.equals(id.getId())) {
                        KarmaElement valueElement = map.get(key);
                        if (valueElement.isObject()) {
                            KarmaObject object = valueElement.getObjet();
                            if (object.isString()) {
                                String value = object.getString();
                                return AccountID.fromUUID(UUID.nameUUIDFromBytes(("LockLoginAccount: " + value).getBytes()));
                            }
                        }
                    }
                }
            }
        }

        return id;
    }
}
