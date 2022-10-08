package eu.locklogin.api.common.utils.other.name;

import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.karma.KarmaSource;
import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.karma.file.element.KarmaArray;
import ml.karmaconfigs.api.common.karma.file.element.KarmaElement;
import ml.karmaconfigs.api.common.karma.file.element.KarmaObject;
import ml.karmaconfigs.api.common.timer.scheduler.LateScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.worker.AsyncLateScheduler;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class AccountNameDatabase {

    private final static KarmaSource lockLogin = APISource.loadProvider("LockLogin");
    private final static KarmaMain nameDatabase = new KarmaMain(lockLogin, "names.lldb", "data");

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
     * Find the uuid of that name
     *
     * @param name the username/nickname
     * @return the name search result
     */
    public static LateScheduler<NameSearchResult> find(final String name) {
        LateScheduler<NameSearchResult> result = new AsyncLateScheduler<>();

        KarmaObject nm = new KarmaObject(name);
        lockLogin.async().queue("find_user_name", () -> {
            Set<UUID> ids = new LinkedHashSet<>();

            for (String key : nameDatabase.getKeys()) {
                KarmaElement value = nameDatabase.get(key);
                if (value.isArray() && value.getArray().contains(nm)) {
                    ids.add(UUID.fromString(key.replaceFirst("main\\.", "")));
                }
            }

            NameSearchResult nsr = new NameSearchResult(ids.toArray(new UUID[0]));
            result.complete(nsr);
        });

        return result;
    }

    /**
     * Find the other possible names of the specified name
     *
     * @param name the name
     * @return the other possible names
     */
    public static LateScheduler<String[]> otherPossible(final String name) {
        LateScheduler<String[]> result = new AsyncLateScheduler<>();

        KarmaObject nm = new KarmaObject(name);
        lockLogin.async().queue("find_user_names", () -> {
            Set<String> names = new LinkedHashSet<>();

            for (String key : nameDatabase.getKeys()) {
                KarmaElement value = nameDatabase.get(key);

                if (value.isArray()) {
                    KarmaArray array = value.getArray();
                    if (array.contains(nm)) {
                        array.forEach((sub) -> names.add(sub.getObjet().getString()));
                    }
                }
            }

            names.remove(name);

            String[] rs = names.toArray(new String[0]);
            result.complete(rs);
        });

        return result;
    }

    /**
     * Assign a name to the user ID
     *
     * @param name the name
     */
    public void assign(final String name) {
        KarmaArray array = null;

        if (nameDatabase.isSet(uuid.toString())) {
            KarmaElement element = nameDatabase.get(uuid.toString());
            if (element.isArray()) {
                array = element.getArray();
            }
        }

        if (array == null) {
            array = new KarmaArray(
                    new KarmaObject(name)
            );
        }

        KarmaObject object = new KarmaObject(name);
        if (!array.contains(object)) {
            array.add(object);
        }

        nameDatabase.set(uuid.toString(), array);
        nameDatabase.save();
    }
}
