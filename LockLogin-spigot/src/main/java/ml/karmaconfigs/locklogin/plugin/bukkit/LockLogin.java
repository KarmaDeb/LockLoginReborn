package ml.karmaconfigs.locklogin.plugin.bukkit;

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

import ml.karmaconfigs.api.bukkit.Logger;
import ml.karmaconfigs.api.common.KarmaPlugin;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.modules.util.client.ModulePlayer;
import ml.karmaconfigs.locklogin.api.modules.util.javamodule.JavaModuleLoader;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.player.User;
import ml.karmaconfigs.locklogin.plugin.common.utils.FileInfo;
import ml.karmaconfigs.locklogin.plugin.common.utils.other.ASCIIArtGenerator;
import ml.karmaconfigs.locklogin.plugin.common.utils.plugin.Messages;
import ml.karmaconfigs.locklogin.plugin.common.utils.version.VersionID;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.net.InetAddress;
import java.nio.file.Files;
import java.util.UUID;

public interface LockLogin {

    Main plugin = (Main) JavaPlugin.getProvidingPlugin(Main.class);

    String name = KarmaPlugin.getters.getName(plugin);
    String update = FileInfo.getUpdateName(new File(Main.class.getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .getPath().replaceAll("%20", " ")));
    String version = KarmaPlugin.getters.getVersion(plugin);

    String versionID = new VersionID(version, update).generate().get();

    File lockloginFile = new File(Main.class.getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .getPath().replaceAll("%20", " "));

    Logger logger = new Logger(plugin);

    Messages properties = new Messages();

    ASCIIArtGenerator artGen = new ASCIIArtGenerator();

    static JavaModuleLoader getLoader() {
        File modulesFolder = new File(plugin.getDataFolder() + File.separator + "plugin", "modules");

        if (!modulesFolder.exists())
            try {
                Files.createDirectories(modulesFolder.getParentFile().toPath());
            } catch (Throwable ignored) {
            }

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

    static ModulePlayer fromPlayer(final org.bukkit.entity.Player player) {
        String name = player.getName();
        UUID uuid = player.getUniqueId();
        ClientSession session = User.getSession(player);
        AccountManager manager = User.getManager(player);
        InetAddress address = null;
        if (player.getAddress() != null) {
            address = player.getAddress().getAddress();
        }

        return new ModulePlayer(name, uuid, session, manager, address);
    }

    static void trySync(final Runnable action) {
        try {
            plugin.getServer().getScheduler().runTask(plugin, action);
        } catch (Throwable ex) {
            action.run();
        }
    }

    static void tryAsync(final Runnable action) {
        try {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, action);
        } catch (Throwable ex) {
            action.run();
        }
    }
}
