package eu.locklogin.plugin.velocity;

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
import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.utils.KarmaLogger;
import ml.karmaconfigs.api.velocity.KarmaPlugin;
import org.bstats.velocity.Metrics;

import java.io.File;
import java.nio.file.Files;

public interface LockLogin {

    KarmaPlugin plugin = VelocityPlugin.plugin;
    Metrics.Factory factory = VelocityPlugin.factory;

    Console console = APISource.loadProvider("LockLogin").console();

    String name = plugin.name();
    String update = FileInfo.getUpdateName(null);

    VersionID versionID = new VersionID(plugin.version(), update).generate();

    String version = versionID.getVersionID();

    KarmaLogger logger = CurrentPlatform.getLogger();

    PluginProperties properties = new PluginProperties();

    ASCIIArtGenerator artGen = new ASCIIArtGenerator();

    static ModuleLoader getLoader() {
        File modulesFolder = new File(plugin.getDataPath().toFile() + File.separator + "plugin", "modules");

        if (!modulesFolder.exists())
            try {
                Files.createDirectories(modulesFolder.getParentFile().toPath());
            } catch (Throwable ignored) {
            }

        return new ModuleLoader();
    }
}
