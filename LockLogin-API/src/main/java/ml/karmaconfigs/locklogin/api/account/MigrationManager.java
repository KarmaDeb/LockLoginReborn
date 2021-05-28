package ml.karmaconfigs.locklogin.api.account;

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

/**
 * LockLogin migration manager, used by external modules
 * to migrate from an account manager to a new one
 */
public final class MigrationManager {

    private final AccountManager current;
    private final AccountManager target;

    /**
     * Initialize the migration account
     *
     * @param manager the current manager
     * @param migrate the target manager
     */
    public MigrationManager(final AccountManager manager, final AccountManager migrate) {
        current = manager;
        target = migrate;
    }

    /**
     * Initialize the migration
     */
    public final void startMigration() {
        AccountID uuid = current.getUUID();
        String name = current.getName();
        String password = current.getPassword();
        String token = current.getGAuth();
        String pin = current.getPin();
        boolean fa = current.has2FA();

        if (!target.exists())
            target.create();

        target.saveUUID(uuid);
        target.setName(name);
        target.setPassword(password);
        target.setGAuth(token);
        target.setPin(pin);
        target.set2FA(fa);
    }
}
