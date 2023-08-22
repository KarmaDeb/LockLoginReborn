package eu.locklogin.plugin.bukkit;

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

import eu.locklogin.api.common.security.backup.BackupTask;
import eu.locklogin.api.common.utils.FileInfo;
import eu.locklogin.api.common.web.ChecksumTables;
import eu.locklogin.api.module.SourcePlugin;
import eu.locklogin.api.security.backup.BackupScheduler;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.api.util.platform.Platform;
import ml.karmaconfigs.api.bukkit.KarmaPlugin;
import ml.karmaconfigs.api.common.karma.KarmaAPI;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.version.comparator.VersionComparator;

import java.net.URL;

public final class Main extends KarmaPlugin implements SourcePlugin {

    private final MainBootstrap plugin;

    private boolean unloaded = false;

    public Main() throws Throwable {
        super(false);

        String required_api_version = FileInfo.getKarmaPluginVersion(getSourceFile()).replace("-", ".");
        String current_api_version = KarmaAPI.getPluginVersion().replace("-", ".");

        VersionComparator plugin_comparator = new VersionComparator(VersionComparator.createBuilder()
                .currentVersion(current_api_version)
                .checkVersion(required_api_version));

        if (plugin_comparator.isUpToDate()) {
            CurrentPlatform.setMain(Main.class);
            CurrentPlatform.setPlatform(Platform.BUKKIT);
            CurrentPlatform.init(getServer().getPort());

            ChecksumTables tables = new ChecksumTables();
            tables.checkTables();

            console().send("KarmaAPI found version is up to date with the minimum required. Required: {0} | Current: {1}", Level.OK, StringUtils.replaceLast(required_api_version, StringUtils.escapeString("."), "-"), StringUtils.replaceLast(current_api_version, StringUtils.escapeString("."), "-"));
            plugin = new MainBootstrap(this);
        } else {
            console().send("KarmaAPI found version is not the minimum required. You have: {0} | Required: {1}", Level.GRAVE, current_api_version, required_api_version);
            console().send("You can update from: {0}", Level.INFO, "https://www.spigotmc.org/resources/karmaapi-platform.98542/");
            plugin = null;
            onDisable();
        }
    }

    @Override
    public void enable() {
        if (plugin != null) {
            CurrentPlatform.setBackupScheduler(new BackupTask());

            plugin.enable();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                BackupScheduler scheduler = CurrentPlatform.getBackupScheduler();
                scheduler.performBackup().whenComplete((id, error) -> {
                    if (error != null) {
                        if (!error.getMessage().equals("Cannot backup without accounts")) {
                            logger().scheduleLog(Level.GRAVE, error);
                            logger().scheduleLog(Level.INFO, "Failed to save backup {0}", id);
                            console().send("Failed to save backup {0}. See logs for more information", Level.GRAVE, id);
                        }
                    } else {
                        console().send("Successfully created backup with id {0}", Level.INFO, id);
                    }
                });
                if (!unloaded) {
                    onDisable();
                }
            })); //Make sure the plugin shuts down correctly.

            CurrentPlatform.setOnline(getServer().getOnlineMode());
        }
    }

    @Override
    public void onDisable() {
        if (plugin != null) {
            plugin.disable();
        } else {
            console().send("The plugin won't start unless the problems have been fixed!", Level.WARNING);
        }

        stopTasks();
        unloaded = true;
    }

    @Override
    public String name() {
        return getDescription().getName();
    }

    @Override
    public String version() {
        return getDescription().getVersion();
    }

    @Override
    public String description() {
        return getDescription().getDescription();
    }

    @Override
    public String[] authors() {
        return getDescription().getAuthors().toArray(new String[0]);
    }

    @Override
    public String updateURL() {
        URL url = FileInfo.versionHost(null);
        if (url != null)
            return url.toString();

        return null;
    }
}
