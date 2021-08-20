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
import ml.karmaconfigs.api.common.ResourceDownloader;
import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.karma.KarmaSource;
import ml.karmaconfigs.api.common.karma.loader.KarmaBootstrap;
import ml.karmaconfigs.api.common.karma.loader.SubJarLoader;
import ml.karmaconfigs.api.common.utils.URLUtils;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.File;

public final class Main extends Plugin implements KarmaSource {

    private final KarmaBootstrap plugin;

    private final static File lockloginFile = new File(Main.class.getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .getPath().replaceAll("%20", " "));

    public Main() throws Throwable {
        APISource.setSourceProvider(this);
        CurrentPlatform.setPlatform(Platform.BUNGEE);
        CurrentPlatform.setMain(Main.class);
        CurrentPlatform.setOnline(ProxyServer.getInstance().getConfig().isOnlineMode());

        ChecksumTables tables = new ChecksumTables();
        tables.checkTables();

        String downloadURL = URLUtils.getOrBackup(
                "https://locklogin.eu/assets/" + getDescription().getVersion() + "/LockLoginC.jar",
                "https://karmaconfigs.github.io/updates/LockLogin/assets/" + getDescription().getVersion() + "/LockLoginC.jar").toString();
        ResourceDownloader downloader = ResourceDownloader.toCache(
                this,
                "locklogin.injar",
                downloadURL,
                "plugin", getDescription().getVersion());
        if (!downloader.isDownloaded())
            downloader.download();

        SubJarLoader loader = new SubJarLoader(getClass().getClassLoader(), downloader.getDestFile());
        plugin = loader.instantiate("eu.locklogin.plugin.bungee.MainBootstrap", Plugin.class, this);
    }

    @Override
    public void onEnable() {
        plugin.enable();
    }

    @Override
    public void onDisable() {
        plugin.disable();
        stopTasks();
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
        String host = "https://locklogin.eu/version/";
        if (!URLUtils.exists(host)) {
            host = "https://karmaconfigs.github.io/updates/LockLogin/";
        }

        PluginConfiguration config = CurrentPlatform.getConfiguration();
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
}
