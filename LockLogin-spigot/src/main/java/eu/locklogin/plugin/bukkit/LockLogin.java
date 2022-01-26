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

import eu.locklogin.api.common.utils.FileInfo;
import eu.locklogin.api.common.utils.other.ASCIIArtGenerator;
import eu.locklogin.api.common.utils.version.VersionID;
import eu.locklogin.api.file.plugin.PluginProperties;
import eu.locklogin.api.module.plugin.javamodule.ModuleLoader;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.api.common.Logger;
import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Files;

public interface LockLogin {

    Main plugin = (Main) JavaPlugin.getProvidingPlugin(Main.class);

    Console console = APISource.loadProvider("LockLogin").console();

    String name = plugin.name();
    String update = FileInfo.getUpdateName(new File(Main.class.getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .getPath().replaceAll("%20", " ")));
    String version = plugin.version();

    VersionID versionID = new VersionID(version, update).generate();

    File lockloginFile = new File(Main.class.getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .getPath().replaceAll("%20", " "));

    Logger logger = CurrentPlatform.getLogger();

    PluginProperties properties = new PluginProperties();

    ASCIIArtGenerator artGen = new ASCIIArtGenerator();

    static ModuleLoader getLoader() {
        File modulesFolder = new File(plugin.getDataFolder() + File.separator + "plugin", "modules");

        if (!modulesFolder.exists())
            try {
                Files.createDirectories(modulesFolder.getParentFile().toPath());
            } catch (Throwable ignored) {
            }

        return new ModuleLoader();
    }

    static boolean isNullOrEmpty(final String... values) {
        boolean any = false;

        for (String str : values) {
            if (StringUtils.isNullOrEmpty(str)) {
                any = true;
                break;
            }
        }

        return any;
    }

    static void trySync(final TaskTarget target, final Runnable action) {
        try {
            plugin.getServer().getScheduler().runTask(plugin, action);
        } catch (Throwable ex) {
            logger.scheduleLog(Level.GRAVE, ex);
            logger.scheduleLog(Level.INFO, "Failed to schedule async task with identifier {0}. It will run without any special thread configuration!", target.getTaskId());

            console.send("Failed to perform task {0}");
            action.run();
        }
    }

    static void tryAsync(final TaskTarget target, final Runnable action) {
        try {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, action);
        } catch (Throwable ex) {
            logger.scheduleLog(Level.GRAVE, ex);
            logger.scheduleLog(Level.INFO, "Failed to schedule async task with identifier {0}. It will run sync!", target.getTaskId());
            action.run();
        }
    }
}
