package ml.karmaconfigs.locklogin.plugin.velocity;

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

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import ml.karmaconfigs.api.common.utils.ReflectionUtil;
import ml.karmaconfigs.api.velocity.Logger;
import ml.karmaconfigs.api.velocity.Util;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.modules.util.client.ModulePlayer;
import ml.karmaconfigs.locklogin.api.modules.util.javamodule.JavaModuleLoader;
import ml.karmaconfigs.locklogin.plugin.common.utils.FileInfo;
import ml.karmaconfigs.locklogin.plugin.common.utils.other.ASCIIArtGenerator;
import ml.karmaconfigs.locklogin.plugin.common.utils.plugin.Messages;
import ml.karmaconfigs.locklogin.plugin.common.utils.version.VersionID;
import ml.karmaconfigs.locklogin.plugin.velocity.util.player.User;
import org.bstats.velocity.Metrics;

import java.io.File;
import java.net.InetAddress;
import java.nio.file.Files;
import java.util.UUID;

public interface LockLogin {

    PluginContainer plugin = Main.plugin;
    ProxyServer server = Main.server;
    Metrics.Factory factory = Main.factory;
    Main main = Main.get();

    String name = ReflectionUtil.getName(plugin);
    String update = FileInfo.getUpdateName(new File(Main.class.getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .getPath().replaceAll("%20", " ")));
    String version = ReflectionUtil.getVersion(plugin);

    String versionID = new VersionID(version, update).generate().get();

    File lockloginFile = new File(Main.class.getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .getPath().replaceAll("%20", " "));

    Logger logger = new Logger(plugin);

    Messages properties = new Messages();

    ASCIIArtGenerator artGen = new ASCIIArtGenerator();

    static JavaModuleLoader getLoader() {
        Util util = new Util(plugin);

        File modulesFolder = new File(util.getDataFolder() + File.separator + "plugin", "modules");

        if (!modulesFolder.exists())
            try {
                Files.createDirectories(modulesFolder.getParentFile().toPath());
            } catch (Throwable ignored) {
            }

        return new JavaModuleLoader(modulesFolder);
    }

    static ModulePlayer fromPlayer(final com.velocitypowered.api.proxy.Player player) {
        String name = player.getGameProfile().getName();
        UUID uuid = player.getUniqueId();
        ClientSession session = User.getSession(player);
        AccountManager manager = User.getManager(player);
        InetAddress address = player.getRemoteAddress().getAddress();

        return new ModulePlayer(name, uuid, session, manager, address);
    }
}
