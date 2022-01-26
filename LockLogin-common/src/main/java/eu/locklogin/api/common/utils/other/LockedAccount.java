package eu.locklogin.api.common.utils.other;

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
import eu.locklogin.api.account.LockManager;
import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.karma.KarmaSource;
import ml.karmaconfigs.api.common.karmafile.KarmaFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/**
 * Simple LockLogin locked account
 */
public final class LockedAccount extends LockManager {

    private final KarmaFile lockedFile;

    /**
     * Initialize the locked account
     *
     * @param accId the account id
     */
    public LockedAccount(final AccountID accId) {
        super(accId);

        KarmaSource source = APISource.loadProvider("LockLogin");

        Path file = source.getDataPath().resolve("data").resolve("accounts").resolve(accId.getId().replace("-", "") + ".locked");
        lockedFile = new KarmaFile(file);
    }

    /**
     * Lock the account
     */
    @Override
    public void lock(final String administrator) {
        lockedFile.create();

        lockedFile.set("ISSUER", administrator);
        lockedFile.set("DATE", Instant.now());
    }

    /**
     * Unlock the account
     */
    @Override
    public boolean release() {
        try {
            return Files.deleteIfExists(lockedFile.getFile().toPath());
        } catch (Throwable ex) {
            return false;
        }
    }

    /**
     * Get the lock issuer
     *
     * @return the lock issuer
     */
    @Override
    public String getIssuer() {
        return lockedFile.getString("ISSUER", "");
    }

    /**
     * Get the lock date
     *
     * @return the lock date
     */
    @Override
    public Instant getLockDate() {
        return Instant.parse(lockedFile.getString("DATE", Instant.now().toString()));
    }

    /**
     * Get if the account is locked
     *
     * @return if the account is locked
     */
    @Override
    public boolean isLocked() {
        return (lockedFile.exists() && !lockedFile.getFile().isDirectory());
    }
}
