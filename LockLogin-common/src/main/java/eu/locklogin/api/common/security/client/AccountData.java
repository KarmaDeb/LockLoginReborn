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
import ml.karmaconfigs.api.common.karmafile.KarmaFile;
import ml.karmaconfigs.api.common.utils.file.FileUtilities;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * LockLogin ip account data
 */
public final class AccountData {

    private final String ip;
    private final AccountID uuid;

    private final KarmaFile lib;
    private final KarmaFile rev_lib;

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

        lib = new KarmaFile(libFile);
        rev_lib = new KarmaFile(revLibFile);
    }

    /**
     * Save the player data
     */
    public final void save() {
        if (!lib.exists()) {
            lib.create();

            List<String> sets = new ArrayList<>();
            sets.add(uuid.getId());

            lib.set("ASSIGNED", sets);
        } else {
            List<String> sets = lib.getStringList("ASSIGNED");
            if (!sets.contains(uuid.getId())) {
                sets.add(uuid.getId());

                lib.set("ASSIGNED", sets);
            }
        }

        if (!rev_lib.exists()) {
            rev_lib.create();

            List<String> sets = new ArrayList<>();
            sets.add(ip);

            rev_lib.set("ASSIGNED", sets);
        } else {
            List<String> sets = rev_lib.getStringList("ASSIGNED");
            if (!sets.contains(ip)) {
                sets.add(ip);

                rev_lib.set("ASSIGNED", sets);
            }
        }
    }

    /**
     * Get if the player can join
     *
     * @param max the maximum amount of accounts
     * @return if the player can join
     */
    public final boolean allow(final int max) {
        if (rev_lib.exists()) {
            return true;
        }

        if (lib.exists()) {
            if (lib.getStringList("ASSIGNED").contains(uuid.getId())) {
                return true;
            } else {
                return lib.getStringList("ASSIGNED").size() < max;
            }
        }

        return true;
    }

    /**
     * Get all the alt accounts an IP has
     *
     * @return all the alt accounts the IP has
     */
    public final Set<AccountID> getAlts() {
        Set<AccountID> accounts = new HashSet<>();

        if (lib.exists()) {
            for (String str : lib.getStringList("ASSIGNED"))
                accounts.add(AccountID.fromTrimmed(str));
        }

        return accounts;
    }

    /**
     * Get all the alt accounts an IP has
     *
     * @return all the alt accounts the IP has
     */
    public final Set<AccountID> getReverseAlts() {
        Set<AccountID> accounts = new HashSet<>();

        if (rev_lib.exists()) {
            Set<String> address = new HashSet<>(rev_lib.getStringList("ASSIGNED"));

            for (String libName : address) {
                File libFile = new File(FileUtilities.getProjectFolder("plugins") + File.separator + "LockLogin" + File.separator + "data" + File.separator +
                        "ips" + File.separator + "lib", libName + ".library");

                KarmaFile file = new KarmaFile(libFile);
                for (String str : file.getStringList("ASSIGNED"))
                    accounts.add(AccountID.fromTrimmed(str));
            }
        }

        return accounts;
    }
}
