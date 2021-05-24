package ml.karmaconfigs.locklogin.plugin.common.utils.plugin;

import ml.karmaconfigs.api.common.karmafile.GlobalKarmaFile;
import ml.karmaconfigs.api.common.utils.FileUtilities;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.account.AccountID;

import java.io.File;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Alias {

    private final String name;
    private final GlobalKarmaFile file;

    /**
     * Initialize the alias class
     *
     * @param _name the alias name
     */
    public Alias(final String _name) {
        name = StringUtils.stripColor(_name.toLowerCase().replace(" ", "_"));

        File aliases = new File(FileUtilities.getPluginsFolder() + File.separator + "LockLogin" + File.separator + "data", "aliases");
        File alias = new File(aliases, name + ".alias");

        file = new GlobalKarmaFile(alias);
    }

    /**
     * Create the specified alias
     */
    public final void create() {
        if (!file.exists())
            file.create();
    }

    /**
     * Destroy the specified alias
     */
    public final void destroy() {
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
    public final boolean exists() {
        return file.exists();
    }

    /**
     * Add the specified users to the alias users
     *
     * @param accounts the accounts to add
     * @return the already added user ids
     */
    public final Set<String> addUsers(final Map<AccountID, String> accounts) {
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
    public final Set<String> delUsers(final Map<AccountID, String> accounts) {
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
    public final Set<AccountID> getUsers() {
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
    public final String getName() {
        return name;
    }
}
