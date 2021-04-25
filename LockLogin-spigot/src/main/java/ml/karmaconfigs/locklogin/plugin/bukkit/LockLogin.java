package ml.karmaconfigs.locklogin.plugin.bukkit;

import ml.karmaconfigs.api.bukkit.Logger;
import ml.karmaconfigs.api.common.KarmaPlugin;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.modules.JavaModuleLoader;
import ml.karmaconfigs.locklogin.plugin.common.utils.ASCIIArtGenerator;
import ml.karmaconfigs.locklogin.plugin.common.utils.FileInfo;
import ml.karmaconfigs.locklogin.plugin.common.utils.plugin.Messages;
import ml.karmaconfigs.locklogin.plugin.common.utils.version.VersionID;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Files;

public interface LockLogin {

    Main plugin = (Main) JavaPlugin.getProvidingPlugin(Main.class);

    String name = KarmaPlugin.getters.getName(plugin);
    String update = FileInfo.getUpdateName(new File(Main.class.getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .getPath()));
    String version = KarmaPlugin.getters.getVersion(plugin);

    String versionID = new VersionID(version, update).generate().get();

    File lockloginFile = new File(Main.class.getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .getPath());

    Logger logger = new Logger(plugin);

    Messages properties = new Messages();

    ASCIIArtGenerator artGen = new ASCIIArtGenerator();

    static JavaModuleLoader getLoader() {
        File modulesFolder = new File(plugin.getDataFolder() + File.separator + "plugin", "modules");

        if (!modulesFolder.exists())
            try {
                Files.createDirectories(modulesFolder.getParentFile().toPath());
            } catch (Throwable ignored) {}

        return new JavaModuleLoader(modulesFolder);
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
}
