package eu.locklogin.plugin.bungee;

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

import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.common.utils.FileInfo;
import eu.locklogin.api.common.utils.other.ASCIIArtGenerator;
import eu.locklogin.api.common.utils.version.VersionID;
import eu.locklogin.api.file.plugin.PluginProperties;
import eu.locklogin.api.module.plugin.client.ModulePlayer;
import eu.locklogin.api.module.plugin.javamodule.ModuleLoader;
import eu.locklogin.plugin.bungee.util.player.User;
import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.api.common.Logger;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

public interface LockLogin {

    Main plugin = (Main) ProxyServer.getInstance().getPluginManager().getPlugin("LockLogin");

    Console console = new Console(plugin);

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

    Logger logger = new Logger(plugin);

    PluginProperties properties = new PluginProperties();

    ASCIIArtGenerator artGen = new ASCIIArtGenerator();

    UUID rsa_id = UUID.nameUUIDFromBytes(("locklogin_rsa_local").getBytes(StandardCharsets.UTF_8));
    UUID aes_id = UUID.nameUUIDFromBytes(("locklogin_aes_local").getBytes(StandardCharsets.UTF_8));

    static UUID generateForServer(final String proxy) {
        return UUID.nameUUIDFromBytes(("KeyFor:" + proxy).getBytes(StandardCharsets.UTF_8));
    }

    static ModuleLoader getLoader() {
        File modulesFolder = new File(plugin.getDataFolder() + File.separator + "plugin", "modules");

        if (!modulesFolder.exists())
            try {
                Files.createDirectories(modulesFolder.getParentFile().toPath());
            } catch (Throwable ignored) {
            }

        return new ModuleLoader();
    }

    static ModulePlayer fromPlayer(final ProxiedPlayer player) {
        String name = player.getName();
        UUID uuid = player.getUniqueId();
        ClientSession session = User.getSession(player);
        AccountManager manager = User.getManager(player);
        InetAddress address = getIp(player.getSocketAddress());

        return new ModulePlayer(name, uuid, session, manager, address);
    }

    @Nullable
    static InetAddress getIp(final SocketAddress connection) {
        try {
            InetSocketAddress address = (InetSocketAddress) connection;
            return address.getAddress();
        } catch (Throwable ex) {
            return null;
        }
    }

    @Nullable
    static InetSocketAddress getSocketIp(final SocketAddress connection) {
        try {
            return (InetSocketAddress) connection;
        } catch (Throwable ex) {
            return null;
        }
    }
}
