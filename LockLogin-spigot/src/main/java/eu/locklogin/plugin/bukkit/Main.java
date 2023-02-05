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

import eu.locklogin.api.common.security.BackupTask;
import eu.locklogin.api.common.utils.FileInfo;
import eu.locklogin.api.common.web.ChecksumTables;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.api.util.platform.Platform;
import ml.karmaconfigs.api.bukkit.KarmaPlugin;
import ml.karmaconfigs.api.common.karma.KarmaAPI;
import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.version.checker.VersionUpdater;
import ml.karmaconfigs.api.common.version.comparator.VersionComparator;
import org.bukkit.configuration.file.YamlConfiguration;

import java.net.URL;

public final class Main extends KarmaPlugin {

    private final MainBootstrap plugin;

    private boolean unloaded = false;

    public Main() throws Throwable {
        super(false);

        String required_api_version = FileInfo.getKarmaVersion(getSourceFile());
        String current_api_version = KarmaAPI.getVersion();

        String pl_version = current_api_version;
        String re_version = required_api_version;
        VersionComparator plugin_comparator = new VersionComparator(VersionComparator.createBuilder()
                .currentVersion(current_api_version)
                .checkVersion(required_api_version));;
        try {
            pl_version = KarmaAPI.getPluginVersion();
            re_version = FileInfo.getKarmaPluginVersion(getSourceFile());

            plugin_comparator = new VersionComparator(VersionComparator.createBuilder()
                    .currentVersion(pl_version.replace("-", "."))
                    .checkVersion(re_version.replace("-", ".")));
        } catch (Throwable ex) {
            ex.printStackTrace();
        }

        if (required_api_version.equals(current_api_version) && plugin_comparator.isUpToDate()) {
            CurrentPlatform.setMain(Main.class);
            CurrentPlatform.setPlatform(Platform.BUKKIT);

            ChecksumTables tables = new ChecksumTables();
            tables.checkTables();

            plugin = new MainBootstrap(this);
        } else {
            if (required_api_version.equals(current_api_version)) {
                console().send("KarmaAPI found version is not the minimum required. You have: {0} | Required: {1}", Level.GRAVE, pl_version, re_version);
            } else {
                console().send("KarmaAPI found version is not the minimum required. You have: {0} | Required: {1}", Level.GRAVE, current_api_version, required_api_version);
            }

            console().send("You can update from: {0}", Level.INFO, "https://www.spigotmc.org/resources/karmaapi-platform.98542/");
            plugin = null;
            onDisable();
        }
    }

    @Override
    public void enable() {
        if (plugin != null) {
            plugin.enable();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                BackupTask.performBackup();
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
        /*URL url = FileInfo.versionHost(null);
        if (url != null)
            return url.toString();

        return null;*/

        return "https://raw.githubusercontent.com/KarmaDeb/updates/master/LockLogin/version/snapshot.kup";
    }
}
