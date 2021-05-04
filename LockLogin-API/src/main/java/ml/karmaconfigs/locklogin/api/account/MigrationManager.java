package ml.karmaconfigs.locklogin.api.account;

/**
 * Private GSA code
 * <p>
 * The use of this code
 * without GSA team authorization
 * will be a violation of
 * terms of use determined
 * in <a href="https://karmaconfigs.ml/license/"> here </a>
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
