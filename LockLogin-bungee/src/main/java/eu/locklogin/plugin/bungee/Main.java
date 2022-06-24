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
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.api.util.platform.Platform;
import ml.karmaconfigs.api.bungee.KarmaPlugin;
import net.md_5.bungee.api.ProxyServer;

import java.net.URL;

public final class Main extends KarmaPlugin {

    private static boolean status = false;
    private static MainBootstrap plugin;

    private boolean unloaded = false;

    public Main() throws Throwable {
        super(false);
        CurrentPlatform.setPlatform(Platform.BUNGEE);
        CurrentPlatform.setMain(Main.class);

        ChecksumTables tables = new ChecksumTables();
        tables.checkTables();

        plugin = new MainBootstrap(this);
    }

    @Override
    public void enable() {
        status = true;
        plugin.enable();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!unloaded) {
                onDisable();
            }
        })); //Make sure the plugin shuts down correctly.
        CurrentPlatform.setOnline(ProxyServer.getInstance().getConfig().isOnlineMode());
    }

    @Override
    public void onDisable() {
        status = false;
        plugin.disable();
        stopTasks();

        unloaded = true;
    }

    public boolean enabled() {
        return status;
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
}
