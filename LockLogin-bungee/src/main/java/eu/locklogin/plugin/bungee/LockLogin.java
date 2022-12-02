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

import eu.locklogin.api.common.utils.FileInfo;
import eu.locklogin.api.common.utils.other.ASCIIArtGenerator;
import eu.locklogin.api.common.utils.plugin.MessageQueue;
import eu.locklogin.api.common.utils.version.VersionID;
import eu.locklogin.api.file.plugin.PluginProperties;
import eu.locklogin.api.module.plugin.javamodule.ModuleLoader;
import eu.locklogin.api.module.plugin.javamodule.server.TargetServer;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.utils.KarmaLogger;
import net.md_5.bungee.api.ProxyServer;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LockLogin {

    private final static Map<String, MessageQueue> queues = new ConcurrentHashMap<>();

    public static Main plugin = (Main) ProxyServer.getInstance().getPluginManager().getPlugin("LockLogin");

    public static Console console = APISource.loadProvider("LockLogin").console();

    public static String name = plugin.name();
    public static String update = FileInfo.getUpdateName(null);

    public static MessageQueue fetchQueue(final TargetServer server) {
        MessageQueue stored = queues.getOrDefault(server.getName(), null);
        if (stored == null) {
            stored = new MessageQueue(server);
            queues.put(server.getName(), stored);
        }

        return stored;
    }

    public static VersionID versionID = new VersionID(plugin.version(), update).generate();

    public static String version = versionID.getVersionID();

    public static File lockloginFile = new File(Main.class.getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .getPath().replaceAll("%20", " "));

    public static KarmaLogger logger = CurrentPlatform.getLogger();

    public static PluginProperties properties = new PluginProperties();

    public static ASCIIArtGenerator artGen = new ASCIIArtGenerator();

    public static ModuleLoader getLoader() {
        File modulesFolder = new File(plugin.getDataFolder() + File.separator + "plugin", "modules");

        if (!modulesFolder.exists())
            try {
                Files.createDirectories(modulesFolder.getParentFile().toPath());
            } catch (Throwable ignored) {
            }

        return new ModuleLoader();
    }

    @Nullable
    public static InetAddress getIp(final SocketAddress connection) {
        try {
            InetSocketAddress address = (InetSocketAddress) connection;
            return address.getAddress();
        } catch (Throwable ex) {
            return null;
        }
    }

    @Nullable
    public static InetSocketAddress getSocketIp(final SocketAddress connection) {
        try {
            return (InetSocketAddress) connection;
        } catch (Throwable ex) {
            return null;
        }
    }
}