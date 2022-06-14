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
import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.karma.KarmaSource;
import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.karma.file.element.KarmaArray;
import ml.karmaconfigs.api.common.karma.file.element.KarmaElement;
import ml.karmaconfigs.api.common.karma.file.element.KarmaObject;
import ml.karmaconfigs.api.common.karmafile.KarmaFile;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.file.FileUtilities;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    @SuppressWarnings("deprecation")
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

        File libFileOld = new File(FileUtilities.getProjectFolder("plugins") + File.separator + "LockLogin" + File.separator + "data" + File.separator +
                "ips" + File.separator + "lib", ip + ".library");
        File revLibFileOld = new File(FileUtilities.getProjectFolder("plugins") + File.separator + "LockLogin" + File.separator + "data" + File.separator +
                "ips" + File.separator + "rev_lib", account.getId() + ".library");

        File libFile = new File(FileUtilities.getProjectFolder("plugins") + File.separator + "LockLogin" + File.separator + "data" + File.separator +
                "ips" + File.separator + "lib", ip + ".kf");

        File revLibFile = new File(FileUtilities.getProjectFolder("plugins") + File.separator + "LockLogin" + File.separator + "data" + File.separator +
                "ips" + File.separator + "rev_lib", account.getId() + ".kf");

        if (libFileOld.exists()) {
            try {
                KarmaMain mn = KarmaMain.fromLegacy(new KarmaFile(libFileOld));
                if (mn.save(libFile.toPath())) {
                    mn.delete();
                    plugin.console().send("Updated legacy KarmaFile {0} to modern KarmaMain file", Level.OK, FileUtilities.getPrettyFile(libFileOld));
                } else {
                    plugin.console().send("Failed to update legacy KarmaFile {0} to modern KarmaMain file", Level.WARNING, FileUtilities.getPrettyFile(libFileOld));
                }
            } catch (Throwable ex) {
                plugin.logger().scheduleLog(Level.GRAVE, ex);
                plugin.logger().scheduleLog(Level.INFO, "Failed to update KarmaFile {0} to KarmaMain", FileUtilities.getPrettyFile(libFileOld));
                plugin.console().send("Failed to update legacy KarmaFile {0} to modern KarmaMain file", Level.GRAVE, FileUtilities.getPrettyFile(libFileOld));
            }
        }
        if (revLibFileOld.exists()) {
            try {
                KarmaMain mn = KarmaMain.fromLegacy(new KarmaFile(revLibFileOld));
                if (mn.save(revLibFile.toPath())) {
                    mn.delete();
                    plugin.console().send("Updated legacy KarmaFile {0} to modern KarmaMain file", Level.OK, FileUtilities.getPrettyFile(revLibFileOld));
                } else {
                    plugin.console().send("Failed to update legacy KarmaFile {0} to modern KarmaMain file", Level.WARNING, FileUtilities.getPrettyFile(revLibFileOld));
                }
            } catch (Throwable ex) {
                plugin.logger().scheduleLog(Level.GRAVE, ex);
                plugin.logger().scheduleLog(Level.INFO, "Failed to update KarmaFile {0} to KarmaMain", FileUtilities.getPrettyFile(revLibFileOld));
                plugin.console().send("Failed to update legacy KarmaFile {0} to modern KarmaMain file", Level.GRAVE, FileUtilities.getPrettyFile(revLibFileOld));
            }
        }

        lib = new KarmaMain(libFile.toPath());
        rev_lib = new KarmaMain(revLibFile.toPath());
    }

    /**
     * Save the player data
     */
    public void save() {
        if (!lib.exists()) {
            List<KarmaElement> sets = new ArrayList<>();
            sets.add(new KarmaObject(uuid.getId()));

            lib.set("assigned", new KarmaArray(sets.toArray(new KarmaElement[0])));
        } else {
            if (lib.isSet("assigned")) {
                KarmaElement sets = lib.get("assigned");
                if (sets.isArray()) {
                    KarmaArray array = sets.getArray();
                    KarmaObject id = new KarmaObject(uuid.getId());

                    if (!array.contains(id)) {
                        array.add(id);
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
            List<KarmaElement> sets = new ArrayList<>();
            sets.add(new KarmaObject(ip));

            rev_lib.set("assigned", new KarmaArray(sets.toArray(new KarmaElement[0])));
        } else {
            if (rev_lib.isSet("assigned")) {
                KarmaElement sets = rev_lib.get("assigned");
                if (sets.isArray()) {
                    KarmaArray array = sets.getArray();
                    KarmaObject id = new KarmaObject(ip);

                    if (!array.contains(id)) {
                        array.add(id);
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
            KarmaElement sets = lib.get("assigned");
            if (sets.isArray()) {
                KarmaArray array = sets.getArray();
                if (array.contains(new KarmaObject(uuid.getId()))) {
                    return true;
                } else {
                    return array.size() < max;
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
            KarmaElement sets = lib.get("assigned");
            if (sets.isArray()) {
                KarmaArray array = sets.getArray();

                for (KarmaElement element : array.getElements()) {
                    if (element.isString()) {
                        String str = element.getObjet().getString();

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
            KarmaElement sets = rev_lib.get("assigned");
            if (sets.isArray()) {
                KarmaArray array = sets.getArray();

                for (KarmaElement libName : array.getElements()) {
                    Path libFile = plugin.getDataPath().resolve("data").resolve("ips").resolve("lib").resolve(libName + ".kf");
                    if (Files.exists(libFile)) {
                        KarmaMain lb = new KarmaMain(libFile);
                        if (lb.isSet("assigned")) {
                            KarmaElement lbAssigned = lb.get("assigned");
                            if (lbAssigned.isArray()) {
                                KarmaArray lbArray = lbAssigned.getArray();
                                for (KarmaElement element : lbArray) {
                                    if (element.isString()) {
                                        accounts.add(AccountID.fromString(element.getObjet().getString()));
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
