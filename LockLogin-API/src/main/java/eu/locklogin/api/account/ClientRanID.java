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

import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.karma.file.element.types.Element;
import ml.karmaconfigs.api.common.karma.file.element.types.ElementPrimitive;
import ml.karmaconfigs.api.common.karma.source.APISource;
import ml.karmaconfigs.api.common.karma.source.KarmaSource;
import ml.karmaconfigs.api.common.timer.scheduler.LateScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.worker.AsyncLateScheduler;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * LockLogin LocalId memory
 * <p>
 * Originally created for Azuriom ID random UUID generation system.
 * This system is now used to retrieve a player account file using its UUID
 * instead of his player variable
 */
@SuppressWarnings("unused")
public final class ClientRanID {

    private final String uuid;

    private final static KarmaSource source = APISource.loadProvider("LockLogin");
    private final static KarmaMain idData = new KarmaMain(source.getDataPath().resolve("data").resolve("local").resolve("ids.lldb"));

    /**
     * Initialize the azuriom id memory
     *
     * @param id the azuriom account id
     */
    public ClientRanID(final AccountID id) {
        uuid = id.getId();
    }

    /**
     * Assign the uuid to an account
     *
     * @param name the account name
     */
    public void assignTo(final String name) {
        idData.setRaw(name, name);
        idData.save();
    }

    /**
     * Get the name the UUID is assigned to
     *
     * @return the name of the UUID is assigned
     * to. Returns null if the UUID is not assigned
     */
    public LateScheduler<String> getAssigned() {
        LateScheduler<String> result = new AsyncLateScheduler<>();

        source.async().queue("load_assigned_ids", () -> {
            boolean response = false;
            for (String key : idData.getKeys()) {
                Element<?> tmpVal = idData.get(key);

                if (tmpVal.isPrimitive()) {
                    ElementPrimitive primitive = tmpVal.getAsPrimitive();

                    if (primitive.isString()) {
                        String id = primitive.asString();
                        if (uuid.equals(id)) {
                            result.complete(key);

                            response = true;
                            break;
                        }
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
        Element<?> id = idData.get(name);
        if (id.isPrimitive()) {
            ElementPrimitive primitive = id.getAsPrimitive();
            if (primitive.isString()) {
                return AccountID.fromString(primitive.asString());
            }
        }

        return null;
    }
}
