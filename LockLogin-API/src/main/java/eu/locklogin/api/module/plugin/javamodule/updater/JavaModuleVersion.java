package eu.locklogin.api.module.plugin.javamodule.updater;

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

import eu.locklogin.api.module.PluginModule;
import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.timer.SchedulerUnit;
import ml.karmaconfigs.api.common.timer.SourceScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.LateScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.worker.AsyncLateScheduler;
import ml.karmaconfigs.api.common.utils.url.URLUtils;
import ml.karmaconfigs.api.common.version.LegacyVersionUpdater;
import ml.karmaconfigs.api.common.version.VersionFetchResult;
import ml.karmaconfigs.api.common.version.VersionUpdater;
import ml.karmaconfigs.api.common.version.util.VersionCheckType;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LockLogin module version manager,
 * this also contains a version checker to
 * allow the server owner know when to update
 */
public final class JavaModuleVersion {

    private final static Set<PluginModule> recently_cached = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final PluginModule module;
    private final VersionUpdater updater;

    /**
     * Initialize the java module version
     *
     * @param owner the module owner
     */
    public JavaModuleVersion(final PluginModule owner) {
        module = owner;

        if (URLUtils.exists(module.updateURL())) {
            updater = LegacyVersionUpdater.createNewBuilder(module).withVersionType(VersionCheckType.NUMBER).build();
        } else {
            updater = null;
        }
    }

    /**
     * Fetch the latest module version info
     *
     * @return the latest module version info
     */
    public LateScheduler<VersionFetchResult> fetch() {
        LateScheduler<VersionFetchResult> result = new AsyncLateScheduler<>();

        OfflineResult backup = new OfflineResult(module);
        APISource.loadProvider("LockLogin").async().queue(() -> {
            if (updater != null) {
                if (recently_cached.contains(module)) {
                    updater.get().whenComplete((getResult, error) -> {
                        if (error == null) {
                            result.complete(getResult);
                        } else {
                            result.complete(backup);
                        }
                    });
                } else {
                    recently_cached.add(module);

                    SimpleScheduler scheduler = new SourceScheduler(module, 5, SchedulerUnit.MINUTE, false).multiThreading(true);
                    scheduler.restartAction(() -> recently_cached.remove(module)).start();

                    updater.fetch(true).whenComplete((fetchResult, error) -> {
                        if (error == null) {
                            result.complete(fetchResult);
                        } else {
                            result.complete(backup);
                        }
                    });
                }
            } else {
                result.complete(backup);
            }
        });

        return result;
    }

    /**
     * Offline fetch result for modules that doesn't have a working updater
     */
    private static class OfflineResult extends VersionFetchResult {

        /**
         * Initialize the offline fetch result
         *
         * @param module the module
         */
        public OfflineResult(PluginModule module) {
            super(module, module.version(), module.updateURL(), new String[]{}, null);
        }
    }
}