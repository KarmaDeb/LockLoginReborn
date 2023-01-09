package eu.locklogin.api.file.pack;

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
import ml.karmaconfigs.api.common.data.path.PathUtilities;
import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.karma.file.element.KarmaArray;
import ml.karmaconfigs.api.common.karma.file.element.KarmaElement;
import ml.karmaconfigs.api.common.karma.source.APISource;
import ml.karmaconfigs.api.common.karma.source.KarmaSource;
import ml.karmaconfigs.api.common.string.StringUtils;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * LockLogin info alias
 */
public final class Alias {

    private final String name;
    private final KarmaMain file;

    /**
     * Initialize the alias class
     *
     * @param _name the alias name
     */
    public Alias(final String _name) {
        name = StringUtils.stripColor(_name.toLowerCase().replace(" ", "_"));
        KarmaSource source = APISource.loadProvider("LockLogin");

        file = source.loadFile(name + ".alias", "data", "aliases");
    }

    /**
     * Create the specified alias
     */
    public void create() {
        if (!file.exists())
            file.create();
    }

    /**
     * Destroy the specified alias
     */
    public void destroy() {
        PathUtilities.destroy(file.getDocument());
    }

    /**
     * Get if the alias exists
     *
     * @return if the alias exists
     */
    public boolean exists() {
        return file.exists();
    }

    /**
     * Add the specified users to the alias users
     *
     * @param accounts the accounts to add
     * @return the already added user ids
     */
    public Set<String> addUsers(final Map<AccountID, String> accounts) {
        KarmaArray array = new KarmaArray();
        if (file.isSet("users")) {
            array = file.get("users").getArray();
        }

        Set<String> already = new LinkedHashSet<>();
        for (AccountID id : accounts.keySet()) {
            if (array.contains(KarmaElement.from(id.getId()))) {
                already.add(accounts.get(id));
            } else {
                array.add(KarmaElement.from(id.getId()));
            }
        }
        file.set("users", array);
        file.save();

        return already;
    }

    /**
     * Remove the specified user from the alias users
     *
     * @param accounts the accounts to remove
     * @return the non-removed ids
     */
    public Set<String> delUsers(final Map<AccountID, String> accounts) {
        KarmaArray array = new KarmaArray();
        if (file.isSet("users")) {
            array = file.get("users").getArray();
        }

        Set<String> not_in = new LinkedHashSet<>();
        for (AccountID id : accounts.keySet()) {
            if (array.contains(KarmaElement.from(id.getId()))) {
                array.remove(KarmaElement.from(id.getId()));
            } else {
                not_in.add(accounts.get(id));
            }
        }

        file.set("users", array);
        file.save();

        return not_in;
    }

    /**
     * Get the user ids
     *
     * @return the alias user ids
     */
    public Set<AccountID> getUsers() {
        KarmaArray array = new KarmaArray();
        if (file.isSet("users")) {
            array = file.get("users").getArray();
        }

        Set<AccountID> ids = new LinkedHashSet<>();

        for (KarmaElement element : array)
            ids.add(AccountID.fromString(element.getObjet().getString()));

        return ids;
    }

    /**
     * Get the alias name
     *
     * @return the alias name
     */
    public String getName() {
        return name;
    }
}
