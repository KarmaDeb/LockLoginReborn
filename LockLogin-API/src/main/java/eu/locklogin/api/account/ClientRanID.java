package eu.locklogin.api.account;

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

import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.karma.KarmaSource;
import ml.karmaconfigs.api.common.karmafile.KarmaFile;
import ml.karmaconfigs.api.common.karmafile.Key;
import ml.karmaconfigs.api.common.timer.scheduler.LateScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.worker.AsyncLateScheduler;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

/**
 * LockLogin LocalId memory
 * <p>
 * Originally created for Azuriom ID random UUID generation system.
 *
 * This system is now used to retrieve a player account file using its UUID
 * instead of his player variable
 */
@SuppressWarnings("unused")
public final class ClientRanID {

    private final String uuid;

    private final static KarmaSource source = APISource.loadProvider("LockLogin");
    private final static KarmaFile idData = new KarmaFile(source.getDataPath().resolve("data").resolve("local").resolve("ids.lldb"));

    /**
     * Initialize the azuriom id memory
     *
     * @param id the azuriom account id
     */
    public ClientRanID(final AccountID id) {
        Path oldFile = source.getDataPath().resolve("data").resolve("azuriom").resolve("ids.lldb");
        if (Files.exists(oldFile)) {
            source.console().send("Found legacy client random ID storage ( azuriom/ids.lldb ). Starting migration to the new system", Level.INFO);

            KarmaFile tmpMigration = new KarmaFile(oldFile);
            tmpMigration.getKeys(false).forEach((key) -> idData.set(key.getPath(), key.getValue()));

            tmpMigration.delete();
        }

        uuid = id.getId();
    }

    /**
     * Assign the uuid to an account
     *
     * @param name the account name
     */
    public void assignTo(final String name) {
        idData.set(name, uuid);
    }

    /**
     * Get the name the UUID is assigned to
     *
     * @return the name of the UUID is assigned
     * to. Returns null if the UUID is not assigned
     */
    public LateScheduler<String> getAssigned() {
        LateScheduler<String> result = new AsyncLateScheduler<>();

        source.async().queue(() -> {
            Set<Key> keys = idData.getKeys(false);

            boolean response = false;
            for (Key key : keys) {
                Object tmpVal = key.getValue();

                if (tmpVal instanceof UUID) {
                    UUID value = (UUID) tmpVal;
                    if (value.toString().equals(uuid)) {
                        result.complete(key.getPath());

                        response = true;
                        break;
                    }
                }
            }

            if (!response)
                result.complete(null);
        });

        return result;
    }

    /**
     * Get the account file
     *
     * @return the azuriom id account file
     */
    public Path getAccountFile() {
        return source.getDataPath().resolve("data").resolve("accounts").resolve(uuid.replace("-", "") + ".lldb");
    }

    /**
     * Get the assigned UUID of the specified name
     *
     * @param name the name to search for
     * @return the assigned id to the specified name.
     * Returns null if there's no UUID specified on that
     * player
     */
    @Nullable
    public static AccountID getAssigned(final String name) {
        String id = idData.getString(name, "");
        if (!StringUtils.isNullOrEmpty(id)) {
            return AccountID.fromString(id);
        } else {
            return null;
        }
    }
}
