package eu.locklogin.api.file.plugin;

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
import ml.karmaconfigs.api.common.karmafile.KarmaFile;
import ml.karmaconfigs.api.common.utils.FileUtilities;
import ml.karmaconfigs.api.common.utils.StringUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * LockLogin info alias
 */
public final class Alias {

    private final String name;
    private final KarmaFile file;

    /**
     * Initialize the alias class
     *
     * @param _name the alias name
     */
    public Alias(final String _name) {
        name = StringUtils.stripColor(_name.toLowerCase().replace(" ", "_"));

        File aliases = new File(FileUtilities.getProjectFolder() + File.separator + "LockLogin" + File.separator + "data", "aliases");
        File alias = new File(aliases, name + ".alias");

        file = new KarmaFile(alias);
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
        try {
            if (file.exists())
                Files.delete(file.getFile().toPath());
        } catch (Throwable ignored) {
        }
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
        List<String> set = file.getStringList("USERS");
        Set<String> already = new LinkedHashSet<>();
        for (AccountID id : accounts.keySet()) {
            if (!set.contains(id.getId()))
                set.add(id.getId());
            else
                already.add(accounts.get(id));
        }

        file.set("USERS", set);
        return already;
    }

    /**
     * Remove the specified user from the alias users
     *
     * @param accounts the accounts to remove
     * @return the non-removed ids
     */
    public Set<String> delUsers(final Map<AccountID, String> accounts) {
        List<String> set = file.getStringList("USERS");
        Set<String> not_in = new LinkedHashSet<>();
        for (AccountID id : accounts.keySet()) {
            if (set.contains(id.getId()))
                set.remove(id.getId());
            else
                not_in.add(accounts.get(id));
        }

        file.set("USERS", set);
        return not_in;
    }

    /**
     * Get the user ids
     *
     * @return the alias user ids
     */
    public Set<AccountID> getUsers() {
        List<String> set = file.getStringList("USERS");
        Set<AccountID> ids = new LinkedHashSet<>();

        for (String str : set)
            ids.add(AccountID.fromTrimmed(str));

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
