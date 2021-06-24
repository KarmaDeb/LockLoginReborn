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

import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.api.common.utils.FileUtilities;
import ml.karmaconfigs.api.common.utils.enums.Level;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.time.Instant;
import java.util.function.Consumer;

import static ml.karmaconfigs.api.common.version.VersionUpdater.VersionFetchResult;
import static ml.karmaconfigs.api.common.version.VersionUpdater.VersionFetchResult.*;

/**
 * LockLogin version downloader
 */
public final class VersionDownloader {

    private static double percentage = 0D;
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
     * Get the downloader download percentage
     *
     * @return the downloader download percentage
     */
    public static double getPercentage() {
        return percentage;
    }

    /**
     * Get if the downloader is already downloading
     * the latest version
     *
     * @return if the downloader is already downloading the
     * latest version
     */
    public static boolean isDownloading() {
        return downloading;
    }

    /**
     * Download the latest version
     *
     * @param onDownload when the downloader downloads
     *                   the latest version
     * @param onFail     when the downloader fails to download
     */
    public final void download(final Consumer<File> onDownload, final Consumer<Throwable> onFail) {
        String time = Instant.now().toString().replace(":", ";");
        File dest_file = new File(FileUtilities.getProjectFolder() + File.separator + "LockLogin" + File.separator + "plugin" + File.separator + "updater", "LockLogin_" + result.resolve(VersionType.LATEST) + ".jar");

        dest_file = FileUtilities.getFixedFile(dest_file);
        downloading = true;

        try {
            URL url = new URL(result.getUpdateURL());
            URLConnection connection = url.openConnection();

            int size = connection.getContentLength();
            connection.connect();

            if (!dest_file.getParentFile().exists()) {
                if (dest_file.getParentFile().mkdirs()) {
                    Console.send("Created update folder for LockLogin new update", Level.INFO);
                } else {
                    Console.send("An unknown error occurred while creating update folder", Level.GRAVE);
                }
            }

            InputStream input = new BufferedInputStream(url.openStream(), 1024);
            OutputStream output = new FileOutputStream(dest_file);

            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            double sumCount = 0.0;
            while ((bytesRead = input.read(dataBuffer, 0, 1024)) != -1) {
                output.write(dataBuffer, 0, bytesRead);

                sumCount += bytesRead;
                percentage = (sumCount / size * 100.0);
            }

            output.flush();

            output.close();
            input.close();
        } catch (Throwable ex) {
            if (onFail != null) {
                onFail.accept(ex);
            } else {
                ex.printStackTrace();
            }
        } finally {
            if (onDownload != null) {
                onDownload.accept(dest_file);
            }

            downloading = false;
        }
    }

    /**
     * Get if the server owner wants the plugin to download
     * automatically the new jars
     *
     * @return if the server owner wants to download
     * automatically the new jars
     */
    public static boolean downloadUpdates() {
        File no_download = new File(FileUtilities.getProjectFolder() + File.separator + "LockLogin" + File.separator + "plugin" + File.separator + "updater", ".no_download");
        return !no_download.exists();
    }
}
