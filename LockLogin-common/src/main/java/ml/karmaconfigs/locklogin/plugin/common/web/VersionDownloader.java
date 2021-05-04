package ml.karmaconfigs.locklogin.plugin.common.web;

import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.api.common.utils.FileUtilities;
import ml.karmaconfigs.locklogin.api.utils.enums.UpdateChannel;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.time.Instant;
import java.util.function.Consumer;

public final class VersionDownloader {

    private static String download_url = "https://karmaconfigs.github.io/updates/LockLogin/release/LockLogin.jar";
    private static double percentage = 0D;
    private static boolean downloading = false;

    private final VersionChecker checker;

    /**
     * Initialize the version downloader
     *
     * @param version the current version id
     * @param channel the version update channel
     */
    public VersionDownloader(final String version, final UpdateChannel channel) {
        String name = "release/LockLogin.jar";
        switch (channel) {
            case SNAPSHOT:
                name = "snapshot/LockLogin.jar";
                break;
            case RC:
                name = "rc/LockLogin.jar";
                break;
            default:
                break;
        }

        checker = new VersionChecker(version);
        checker.checkVersion(channel);

        download_url = "https://karmaconfigs.github.io/updates/LockLogin/" + name;
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
        File dest_file = new File(FileUtilities.getPluginsFolder() + File.separator + "plugin" + File.separator + "updater", "LockLogin_" + time + "_" + checker.getLatestVersion() + ".jar");

        if (getDownloaded() == null) {
            try {
                URL url = new URL(download_url);
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

                    downloading = true;
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
            }
        } else {
            if (onDownload != null)
                onDownload.accept(getDownloaded());
        }
    }

    /**
     * Get the downloaded file instance
     *
     * @return the downloaded file instance
     */
    @Nullable
    private File getDownloaded() {
        File updater_folder = new File(FileUtilities.getPluginsFolder() + File.separator + "plugin" + File.separator + "updater");
        File[] files = updater_folder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (!file.isDirectory() && file.getName().endsWith(".jar")) {
                    if (file.getName().endsWith(checker.getLatestVersion() + ".jar")) {
                        return file;
                    }
                }
            }
        }

        return null;
    }
}
