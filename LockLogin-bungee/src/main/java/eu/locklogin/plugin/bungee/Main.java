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
import eu.locklogin.api.common.web.ChecksumTables;
import eu.locklogin.api.module.plugin.javamodule.server.TargetServer;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.api.util.platform.ModuleServer;
import eu.locklogin.api.util.platform.Platform;
import eu.locklogin.plugin.bungee.util.files.cache.TargetServerStorage;
import ml.karmaconfigs.api.bungee.KarmaPlugin;
import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.karma.file.element.KarmaElement;
import ml.karmaconfigs.api.common.karma.file.element.KarmaObject;
import ml.karmaconfigs.api.common.utils.enums.Level;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Main extends KarmaPlugin {

    private static MainBootstrap plugin;

    private boolean unloaded = false;

    public Main() throws Throwable {
        super(false);
        try {
            Class.forName("com.imaginarycode.minecraft.redisbungee.RedisBungee");
            //CurrentPlatform.setPlatform(Platform.REDIS);
            CurrentPlatform.setPlatform(Platform.BUNGEE);
            console().send("RedisBungeecord has been detected. This is not currently supported by LockLogin, buy may be in a future", Level.WARNING);
        } catch (Throwable ex) {
            CurrentPlatform.setPlatform(Platform.BUNGEE);
        }

        CurrentPlatform.setMain(Main.class);

        ChecksumTables tables = new ChecksumTables();
        tables.checkTables();

        plugin = new MainBootstrap(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void enable() {
        plugin.enable();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!unloaded) {
                onDisable();
            }
        })); //Make sure the plugin shuts down correctly.
        CurrentPlatform.setOnline(ProxyServer.getInstance().getConfig().isOnlineMode());

        console().send("Loading all servers for API.", Level.INFO);
        Map<String, ServerInfo> info = ProxyServer.getInstance().getServers();
        for (String name : info.keySet()) {
            ServerInfo server = info.get(name);
            TargetServerStorage storage = new TargetServerStorage(name);
            UUID known = storage.load();
            if (known != null) {
                InetSocketAddress socket = (InetSocketAddress) server.getSocketAddress();
                if (socket != null) {
                    InetAddress address = socket.getAddress();
                    if (address != null) {
                        server.ping((result, error) -> {
                            if (error == null && result != null) {
                                console().send("Server {0} is online!", Level.OK, name);
                                TargetServer target_server = new TargetServer(name, known, address, socket.getPort(), true);
                                TargetServer stored = CurrentPlatform.getServer().getServer(name);

                                try {
                                    Field f = ModuleServer.class.getDeclaredField("servers");
                                    f.setAccessible(true);
                                    Set<TargetServer> stored_set = (Set<TargetServer>) f.get(ModuleServer.class);
                                    if (stored != null) {
                                        //Remove from stored servers
                                        stored_set.remove(stored);
                                    }
                                    stored_set.add(target_server);
                                } catch (Throwable ignored) {}
                            } else {
                                console().send("Server {0} is offline!", Level.WARNING, name);

                                TargetServer target_server = new TargetServer(name, known, address, socket.getPort(), false);
                                TargetServer stored = CurrentPlatform.getServer().getServer(name);

                                try {
                                    Field f = ModuleServer.class.getDeclaredField("servers");
                                    f.setAccessible(true);
                                    Set<TargetServer> stored_set = (Set<TargetServer>) f.get(ModuleServer.class);
                                    if (stored != null) {
                                        //Remove from stored servers
                                        stored_set.remove(stored);
                                    }
                                    stored_set.add(target_server);
                                } catch (Throwable ignored) {}
                            }
                        });
                    }
                }
            }
        }
    }

    @Override
    public void onDisable() {
        plugin.disable();
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
        return new String[]{getDescription().getAuthor()};
    }

    @Override
    public String updateURL() {
        URL url = FileInfo.versionHost(null);
        if (url != null)
            return url.toString();

        return null;
    }

    @Override
    public String getIdentifier() {
        KarmaMain container = new KarmaMain(this, "", "/cache/stats.kf");
        if (!container.exists())
            container.create();

        KarmaElement element = container.get("identifier", new KarmaObject(UUID.randomUUID().toString()));
        String id = element.getObjet().getString();
        if (!container.isSet("identifier")) {
            container.set("identifier", new KarmaObject(id));
            container.save();
        }

        return id;
    }
}
