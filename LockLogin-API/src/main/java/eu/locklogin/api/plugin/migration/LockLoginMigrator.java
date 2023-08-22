package eu.locklogin.api.plugin.migration;

import eu.locklogin.api.account.AccountManager;
import ml.karmaconfigs.api.common.timer.scheduler.LateScheduler;

import java.util.Set;

/**
 * LockLogin migrator is the interface that allows any
 * plugin to migrate from LockLogin, or allows LockLogin
 * to migrate from a plugin
 */
public interface LockLoginMigrator {

    /**
     * Start the migration process
     *
     * @return the migration
     */
    LateScheduler<Set<AccountManager>> migrate();

    /**
     * Get the source migration
     *
     * @return the migration target
     */
    MigrationTarget source();
}
