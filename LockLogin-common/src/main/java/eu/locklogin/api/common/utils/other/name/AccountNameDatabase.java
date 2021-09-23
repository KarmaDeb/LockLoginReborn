package eu.locklogin.api.common.utils.other.name;

import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.karmafile.KarmaFile;
import ml.karmaconfigs.api.common.karmafile.Key;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class AccountNameDatabase {

    private final static KarmaFile nameDatabase = new KarmaFile(APISource.getSource(), "names.lldb", "data");
    private final UUID uuid;

    /**
     * Initialize the account name database
     *
     * @param id the account id
     */
    public AccountNameDatabase(final UUID id) {
        uuid = id;
    }

    /**
     * Assign a name to the user ID
     *
     * @param name the name
     */
    public void assign(final String name) {
        List<String> names = nameDatabase.getStringList(uuid.toString());
        if (!names.contains(name)) {
            names.add(name);
            nameDatabase.set(uuid.toString(), names);
        }
    }

    /**
     * Find the uuid of that name
     *
     * @param name the username/nickname
     * @return the name search result
     */
    public static NameSearchResult find(final String name) {
        Set<UUID> ids = new LinkedHashSet<>();

        for (Key key : nameDatabase.getKeys(false)) {
            String path = key.getPath();
            List<String> values = nameDatabase.getStringList(path);

            if (values.contains(name)) {
                try {
                    ids.add(UUID.fromString(path));
                } catch (Throwable ignored) {}
            }
        }

        return new NameSearchResult(ids.toArray(new UUID[0]));
    }

    /**
     * Find the other possible names of the specified name
     *
     * @param name the name
     * @return the other possible names
     */
    public static String[] otherPossible(final String name) {
        Set<String> names = new LinkedHashSet<>();

        for (Key key : nameDatabase.getKeys(false)) {
            String path = key.getPath();
            List<String> values = nameDatabase.getStringList(path);

            if (values.contains(name)) {
                try {
                    names.addAll(values);
                } catch (Throwable ignored) {}
            }
        }
        names.remove(name);

        return names.toArray(new String[0]);
    }
}
