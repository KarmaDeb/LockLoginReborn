package ml.karmaconfigs.locklogin.plugin.velocity;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import ml.karmaconfigs.api.common.utils.ReflectionUtil;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.api.velocity.Logger;
import ml.karmaconfigs.api.velocity.Util;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.modules.client.Player;
import ml.karmaconfigs.locklogin.api.modules.javamodule.JavaModuleLoader;
import ml.karmaconfigs.locklogin.plugin.common.utils.ASCIIArtGenerator;
import ml.karmaconfigs.locklogin.plugin.common.utils.FileInfo;
import ml.karmaconfigs.locklogin.plugin.common.utils.plugin.Messages;
import ml.karmaconfigs.locklogin.plugin.common.utils.version.VersionID;
import ml.karmaconfigs.locklogin.plugin.velocity.util.player.User;

import java.io.File;
import java.net.InetAddress;
import java.nio.file.Files;
import java.util.UUID;

public interface LockLogin {

    PluginContainer plugin = Main.plugin;
    ProxyServer server = Main.server;

    String name = ReflectionUtil.getName(plugin);
    String update = FileInfo.getUpdateName(new File(Main.class.getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .getPath()));
    String version = ReflectionUtil.getVersion(plugin);

    String versionID = new VersionID(version, update).generate().get();

    File lockloginFile = new File(Main.class.getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .getPath());

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

    static Player fromPlayer(final com.velocitypowered.api.proxy.Player player) {
        User user = new User(player);

        String name = player.getGameProfile().getName();
        UUID uuid = player.getUniqueId();
        ClientSession session = user.getSession();
        AccountManager manager = user.getManager();
        InetAddress address = player.getRemoteAddress().getAddress();

        return new Player(name, uuid, session, manager, address);
    }
}
