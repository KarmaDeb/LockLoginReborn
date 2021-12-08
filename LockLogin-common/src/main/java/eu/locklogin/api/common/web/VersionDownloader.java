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

import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.karma.KarmaSource;
import ml.karmaconfigs.api.common.timer.scheduler.LateScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.worker.FixedLateScheduler;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.file.FileUtilities;
import ml.karmaconfigs.api.common.version.VersionFetchResult;
import ml.karmaconfigs.api.common.version.util.VersionType;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.CompletableFuture;

/**
 * LockLogin version downloader
 */
public final class VersionDownloader {

    private final static KarmaSource lockLogin = APISource.loadProvider("LockLogin");
    private static boolean downloading = false;
    private final VersionFetchResult result;

    /**
     * Initialize the version downloader
     *
     * @param res the version check fetch result
     */
    public VersionDownloader(final VersionFetchResult res) {
        result = res;
    }

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
     *
     * @return the download result
     */
    public LateScheduler<File> download() {
        LateScheduler<File> future = new FixedLateScheduler<>();

        CompletableFuture.runAsync(() -> {
            File dest_file = new File(FileUtilities.getProjectFolder("plugins") + File.separator + "LockLogin" + File.separator + "plugin" + File.separator + "updater" + File.separator + result.resolve(VersionType.LATEST), "LockLogin.jar");
            dest_file = FileUtilities.getFixedFile(dest_file);

            downloading = true;

            Throwable error = null;
            try {
                URL url = new URL(result.getUpdateURL());
                URLConnection connection = url.openConnection();
                connection.connect();

                if (!dest_file.getParentFile().exists()) {
                    if (dest_file.getParentFile().mkdirs()) {
                        lockLogin.console().send("Created update folder for LockLogin new update", Level.INFO);
                    } else {
                        lockLogin.console().send("An unknown error occurred while creating update folder", Level.GRAVE);
                    }
                }

                InputStream input = new BufferedInputStream(url.openStream(), 1024);
                OutputStream output = new FileOutputStream(dest_file);

                byte[] dataBuffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = input.read(dataBuffer, 0, 1024)) != -1) output.write(dataBuffer, 0, bytesRead);

                output.flush();

                output.close();
                input.close();
            } catch (Throwable ex) {
                error = ex;
            } finally {
                future.complete(dest_file, error);
                downloading = false;
            }
        });

        return future;
    }
}
