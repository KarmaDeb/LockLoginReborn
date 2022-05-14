package eu.locklogin.api.common.web;

/*
 * Private GSA code
 *
 * The use of this code
 * without GSA team authorization
 * will be a violation of
 * terms of use determined
 * in <a href="http://karmaconfigs.cf/license/"> here </a>
 * or (fallback domain) <a href="https://karmaconfigs.github.io/page/license"> here </a>
 */

import eu.locklogin.api.common.utils.FileInfo;
import eu.locklogin.api.file.plugin.PluginProperties;
import eu.locklogin.api.module.plugin.client.permission.PermissionDefault;
import eu.locklogin.api.module.plugin.client.permission.PermissionObject;
import eu.locklogin.api.module.plugin.client.permission.SimplePermission;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.api.util.platform.ModuleServer;
import ml.karmaconfigs.api.common.ResourceDownloader;
import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.karma.KarmaSource;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.file.FileUtilities;
import ml.karmaconfigs.api.common.utils.file.PathUtilities;
import ml.karmaconfigs.api.common.utils.security.token.TokenGenerator;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;

/**
 * LockLogin version downloader
 */
public final class VersionDownloader {

    private final static KarmaSource lockLogin = APISource.loadProvider("LockLogin");
    private static boolean downloading = false;

    /**
     * Get if the downloader is already downloading
     * the latest version
     *
     * @return if the downloader is already downloading the
     * latest version
     */
    public static boolean canDownload() {
        return !downloading;
    }

    /**
     * Get if the server owner wants the plugin to download
     * automatically the new jars
     *
     * @return if the server owner wants to download
     * automatically the new jars
     */
    public static boolean downloadUpdates() {
        File no_download = new File(FileUtilities.getProjectFolder("plugins") + File.separator + "LockLogin" + File.separator + "plugin" + File.separator + "updater", ".no_download");
        return !no_download.exists();
    }

    /**
     * Download the latest version
     */
    public static void download() {
        String random = TokenGenerator.generateLiteral(16);
        Path dest_file = lockLogin.getDataPath().resolve("plugin").resolve("updater").resolve(random + ".jar");

        dest_file = PathUtilities.getFixedPath(dest_file);

        downloading = true;

        PluginProperties properties = new PluginProperties();

        URL update_url = FileInfo.updateHost(null);
        if (update_url != null) {
            lockLogin.console().send("Downloading latest LockLogin version from {0}", Level.INFO, update_url);
            ResourceDownloader downloader = new ResourceDownloader(dest_file, update_url.toString());

            downloader.downloadAsync().whenComplete((downloaded, error) -> {
                if (downloaded) {
                    lockLogin.console().send(properties.getProperty("updater_downloaded", "Downloaded latest version plugin instance, to apply the updates run /locklogin applyUpdates"), Level.OK);
                } else {
                    lockLogin.logger().scheduleLog(Level.GRAVE, error);
                    lockLogin.logger().scheduleLog(Level.INFO, "Failed to download latest version of LockLogin");

                    lockLogin.console().send(properties.getProperty("updater_download_fail", "Failed to download latest LockLogin update ( {0} )"), Level.GRAVE, error.getMessage());
                }

                downloading = false;
                ModuleServer server = CurrentPlatform.getServer();
                PermissionObject applyUpdates = new SimplePermission("locklogin.applyupdates", PermissionDefault.FALSE);

                server.getOnlinePlayers().forEach((player) -> {
                    if (player.hasPermission(applyUpdates)) {
                        player.sendMessage(CurrentPlatform.getMessages().prefix() + properties.getProperty("updater_downloaded", "Downloaded latest version plugin instance, to apply the updates run /locklogin applyUpdates"));
                    }
                });
            });
        } else {
            lockLogin.console().send("Failed to download latest version of LockLogin", Level.GRAVE);
        }
    }
}
