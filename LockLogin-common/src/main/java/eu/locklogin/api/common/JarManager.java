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
import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.karma.KarmaSource;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;

/**
 * LockLogin jar manager, from KarmaAPI
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

        Set<PluginDependency> success = new HashSet<>();
        Set<PluginDependency> error = new HashSet<>();

        for (PluginDependency download : downloadTable) {
            lockLogin.console().send("&aTrying to download dependency " + download.getName());

            String url = download.getDownloadURL();
            File jarFile = download.getLocation();

            InputStream is = null;
            ReadableByteChannel rbc = null;
            FileOutputStream fos = null;
            try {
                URL download_url = new URL(url);
                URLConnection connection = download_url.openConnection();
                connection.connect();

                if (!jarFile.getParentFile().exists())
                    Files.createDirectories(jarFile.getParentFile().toPath());

                if (!jarFile.exists())
                    Files.createFile(jarFile.toPath());

                TrustManager[] trustManagers = new TrustManager[]{new NvbTrustManager()};
                final SSLContext context = SSLContext.getInstance("TLSv1.2");
                context.init(null, trustManagers, null);

                // Set connections to use lenient TrustManager and HostnameVerifier
                HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
                HttpsURLConnection.setDefaultHostnameVerifier(new NvbHostnameVerifier());

                is = download_url.openStream();
                rbc = Channels.newChannel(is);
                fos = new FileOutputStream(jarFile);

                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            } catch (Throwable ex) {
                error.add(download);
            } finally {
                try {
                    if (rbc != null) {
                        rbc.close();
                    }
                    if (fos != null) {
                        fos.close();
                    }
                    if (is != null) {
                        is.close();
                    }
                } catch (Throwable ignored) {}

                success.add(download);
            }
        }

        for (PluginDependency valid : success)
            lockLogin.console().send("&aDownloaded plugin dependency " + valid.getName());

        for (PluginDependency failed : error)
            lockLogin.console().send("&cFailed to download plugin dependency " + failed.getName());
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

    /**
     * Simple <code>TrustManager</code> that allows unsigned certificates.
     */
    private static final class NvbTrustManager implements TrustManager, X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }

    /**
     * Simple <code>HostnameVerifier</code> that allows any hostname and session.
     */
    private static final class NvbHostnameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }
}