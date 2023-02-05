package eu.locklogin.api.common;

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

import eu.locklogin.api.common.utils.dependencies.PluginDependency;
import ml.karmaconfigs.api.common.ResourceDownloader;
import ml.karmaconfigs.api.common.karma.source.APISource;
import ml.karmaconfigs.api.common.karma.source.KarmaSource;
import ml.karmaconfigs.api.common.utils.enums.Level;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * LockLogin jar manager, from KarmaAPI
 * THIS IS TOO OLD, THIS DOES NOT EVEN EXIST IN KARMA-API NOW DAYS
 */
public final class JarManager {

    private final static KarmaSource lockLogin = APISource.loadProvider("LockLogin");
    private final static Set<PluginDependency> downloadTable = new HashSet<>();
    private final PluginDependency pluginDependency;

    /**
     * Initialize the injector
     *
     * @param file the file to inject
     */
    public JarManager(final PluginDependency file) {
        pluginDependency = file;
    }

    /**
     * Change the filed value of the specified class
     *
     * @param clazz     the class
     * @param fieldName the field name
     * @param value     the field value
     * @throws Throwable to catch any possible error
     */
    public synchronized static void changeField(final Class<?> clazz, final String fieldName, final Object value) throws Throwable {
        Field field;

        field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(clazz, value);
    }

    /**
     * Try to download the dependencies from the download table
     */
    public static void downloadAll() {
        KarmaSource lockLogin = APISource.loadProvider("LockLogin");

        Set<String> error = new HashSet<>();

        for (PluginDependency download : downloadTable) {
            lockLogin.console().send("&aTrying to download dependency " + download.getName());

            URL download_url = download.getDownloadURL();
            if (download_url != null) {
                File jarFile = download.getLocation();

                ResourceDownloader downloader = new ResourceDownloader(jarFile, download_url.toString());
                downloader.download();
            } else {
                lockLogin.logger().scheduleLog(Level.GRAVE, "Failed to download dependency {0} because its download URL was null", download.getName());
                error.add(download.getName());
            }
        }

        for (String failed : error)
            lockLogin.console().send("&cFailed to download plugin dependency " + failed);
    }

    /**
     * Process the dependency status
     *
     * @param clearOld clear old download table
     */
    public void process(final boolean clearOld) {
        if (clearOld)
            downloadTable.clear();

        if (pluginDependency.isValid()) {
            downloadTable.remove(pluginDependency);
        } else {
            lockLogin.console().send("&cDependency " + pluginDependency.getName() + " is invalid or is not downloaded and will be downloaded");
            downloadTable.add(pluginDependency);
        }
    }
}