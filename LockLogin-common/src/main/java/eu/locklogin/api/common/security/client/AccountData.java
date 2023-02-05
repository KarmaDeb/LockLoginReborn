package eu.locklogin.api.common.security.client;

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
import eu.locklogin.api.encryption.CryptoFactory;
import eu.locklogin.api.encryption.HashType;
import ml.karmaconfigs.api.common.data.file.FileUtilities;
import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.karma.file.element.KarmaPrimitive;
import ml.karmaconfigs.api.common.karma.file.element.multi.KarmaArray;
import ml.karmaconfigs.api.common.karma.file.element.types.Element;
import ml.karmaconfigs.api.common.karma.file.element.types.ElementPrimitive;
import ml.karmaconfigs.api.common.karma.source.APISource;
import ml.karmaconfigs.api.common.karma.source.KarmaSource;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.utils.enums.Level;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * LockLogin ip account data
 */
public final class AccountData {

    private final KarmaSource plugin = APISource.loadProvider("LockLogin");

    private final String ip;
    private final AccountID uuid;

    private final KarmaMain lib;
    private final KarmaMain rev_lib;

    /**
     * Initialize the account data
     *
     * @param libraryName the player ip
     * @param account     the player account id
     */
    public AccountData(final @Nullable InetAddress libraryName, final AccountID account) {
        uuid = account;

        if (libraryName != null) {
            StringBuilder addressBuilder = new StringBuilder();
            for (byte bytes : libraryName.getAddress())
                addressBuilder.append(bytes);

            CryptoFactory util = CryptoFactory.getBuilder().withPassword(addressBuilder.toString()).build();
            ip = util.hash(HashType.LS_SHA256, true);
        } else {
            ip = "none";
        }

        File libFile = new File(FileUtilities.getProjectFolder("plugins") + File.separator + "LockLogin" + File.separator + "data" + File.separator +
                "ips" + File.separator + "lib", ip + ".library");
        File revLibFile = new File(FileUtilities.getProjectFolder("plugins") + File.separator + "LockLogin" + File.separator + "data" + File.separator +
                "ips" + File.separator + "rev_lib", account.getId() + ".library");

        lib = new KarmaMain(libFile.toPath());
        rev_lib = new KarmaMain(revLibFile.toPath());
    }

    /**
     * Save the player data
     */
    public void save() {
        if (!lib.exists()) {
            KarmaArray array = new KarmaArray(new KarmaPrimitive(uuid.getId()));
            lib.set("assigned", array);
        } else {
            if (lib.isSet("assigned")) {
                Element<?> sets = lib.get("assigned");
                if (sets.isArray()) {
                    KarmaArray array = (KarmaArray) sets.getAsArray();
                    ElementPrimitive check_id = new KarmaPrimitive(uuid.getId());

                    if (!array.contains(check_id)) {
                        array.add(check_id);
                        lib.set("assigned", array);

                        if (!lib.save()) {
                            plugin.console().send("Failed to save player IP data of {0}", Level.GRAVE, uuid.getId());
                            plugin.logger().scheduleLog(Level.GRAVE, "Failed to save player IP data of {0}", uuid.getId());
                        }
                    }
                }
            }
        }

        if (!rev_lib.exists()) {
            KarmaArray array = new KarmaArray(new KarmaPrimitive(ip));
            rev_lib.set("assigned", array);
        } else {
            if (rev_lib.isSet("assigned")) {
                Element<?> sets = rev_lib.get("assigned");
                if (sets.isArray()) {
                    KarmaArray array = (KarmaArray) sets.getAsArray();
                    ElementPrimitive check_ip = new KarmaPrimitive(ip);

                    if (!array.contains(check_ip)) {
                        array.add(check_ip);
                        rev_lib.set("assigned", array);

                        if (!rev_lib.save()) {
                            plugin.console().send("Failed to save player recursive IP data of {0}", Level.GRAVE, uuid.getId());
                            plugin.logger().scheduleLog(Level.GRAVE, "Failed to save player IP data of {0}", uuid.getId());
                        }
                    }
                }
            }
        }
    }

    /**
     * Get if the player can join
     *
     * @param max the maximum amount of accounts
     * @return if the player can join
     */
    public boolean allow(final int max) {
        if (rev_lib.exists()) {
            return true;
        }

        if (lib.exists() && lib.isSet("assigned")) {
            Element<?> sets = lib.get("assigned");
            if (sets.isArray()) {
                KarmaArray array = (KarmaArray) sets.getAsArray();
                KarmaPrimitive check_id = new KarmaPrimitive(uuid.getId());

                if (array.contains(check_id)) {
                    return true;
                } else {
                    return array.getSize() < max;
                }
            }
        }

        return true;
    }

    /**
     * Get all the alt accounts an IP has
     *
     * @return all the alt accounts the IP has
     */
    public Set<AccountID> getAlts() {
        Set<AccountID> accounts = new HashSet<>();

        if (lib.exists() && lib.isSet("assigned")) {
            Element<?> sets = lib.get("assigned");
            if (sets.isArray()) {
                KarmaArray array = (KarmaArray) sets.getAsArray();

                for (ElementPrimitive element : array) {
                    if (element.isString()) {
                        String str = element.asString();

                        if (!StringUtils.isNullOrEmpty(str)) {
                            accounts.add(AccountID.fromString(str));
                        }
                    }
                }
            }
        }

        return accounts;
    }

    /**
     * Get all the alt accounts an IP has
     *
     * @return all the alt accounts the IP has
     */
    public Set<AccountID> getReverseAlts() {
        Set<AccountID> accounts = new HashSet<>();

        if (rev_lib.exists() && rev_lib.isSet("assigned")) {
            Element<?> sets = rev_lib.get("assigned");
            if (sets.isArray()) {
                KarmaArray array = (KarmaArray) sets.getAsArray();

                for (ElementPrimitive libName : array) {
                    Path libFile = plugin.getDataPath().resolve("data").resolve("ips").resolve("lib").resolve(libName + ".kf");
                    if (Files.exists(libFile)) {
                        KarmaMain lb = new KarmaMain(libFile);
                        if (lb.isSet("assigned")) {
                            Element<?> lbAssigned = lb.get("assigned");
                            if (lbAssigned.isArray()) {
                                KarmaArray lbArray = (KarmaArray) lbAssigned.getAsArray();
                                for (ElementPrimitive element : lbArray) {
                                    if (element.isString()) {
                                        accounts.add(AccountID.fromString(element.asString()));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return accounts;
    }
}
