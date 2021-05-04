package ml.karmaconfigs.locklogin.plugin.bungee;

import ml.karmaconfigs.api.bungee.Logger;
import ml.karmaconfigs.api.common.KarmaPlugin;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.modules.client.Player;
import ml.karmaconfigs.locklogin.api.modules.javamodule.JavaModuleLoader;
import ml.karmaconfigs.locklogin.plugin.bungee.util.player.User;
import ml.karmaconfigs.locklogin.plugin.common.utils.ASCIIArtGenerator;
import ml.karmaconfigs.locklogin.plugin.common.utils.FileInfo;
import ml.karmaconfigs.locklogin.plugin.common.utils.plugin.Messages;
import ml.karmaconfigs.locklogin.plugin.common.utils.version.VersionID;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.util.UUID;

public interface LockLogin {

    Main plugin = (Main) ProxyServer.getInstance().getPluginManager().getPlugin("LockLogin");

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
            } catch (Throwable ignored) {
            }

        return new JavaModuleLoader(modulesFolder);
    }

    static Player fromPlayer(final ProxiedPlayer player) {
        User user = new User(player);

        String name = player.getName();
        UUID uuid = player.getUniqueId();
        ClientSession session = user.getSession();
        AccountManager manager = user.getManager();
        InetAddress address = getIp(player.getSocketAddress());

        return new Player(name, uuid, session, manager, address);
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
