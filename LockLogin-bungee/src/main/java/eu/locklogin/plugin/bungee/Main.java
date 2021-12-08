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

import eu.locklogin.api.common.web.ChecksumTables;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.api.util.platform.Platform;
import ml.karmaconfigs.api.bungee.KarmaPlugin;
import ml.karmaconfigs.api.common.utils.URLUtils;
import net.md_5.bungee.api.ProxyServer;

public final class Main extends KarmaPlugin {

    private static boolean status = false;
    private static MainBootstrap plugin;

    public Main() throws Throwable {
        CurrentPlatform.setPlatform(Platform.BUNGEE);
        CurrentPlatform.setMain(Main.class);
        CurrentPlatform.setOnline(ProxyServer.getInstance().getConfig().isOnlineMode());

        ChecksumTables tables = new ChecksumTables();
        tables.checkTables();

        plugin = new MainBootstrap(this);
    }

    @Override
    public void enable() {
        status = true;

        plugin.enable();
    }

    @Override
    public void onDisable() {
        status = false;

        plugin.disable();
        stopTasks();
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
        String host = "https://karmarepo.000webhostapp.com/locklogin/version/";
        if (!URLUtils.exists(host)) {
            host = "https://karmaconfigs.github.io/updates/LockLogin/version/";
        }

        PluginConfiguration config = CurrentPlatform.getConfiguration();
        if (config != null) {
            switch (config.getUpdaterOptions().getChannel()) {
                case SNAPSHOT:
                    return host + "snapshot.kupdter";
                case RC:
                    return host + "candidate.kupdter";
                case RELEASE:
                default:
                    return host + "release.kupdter";
            }
        }

        return host + "release.kupdter";
    }
}
