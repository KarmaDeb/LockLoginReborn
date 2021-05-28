package ml.karmaconfigs.locklogin.api.modules.util;

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

import ml.karmaconfigs.api.common.Console;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.security.cert.X509Certificate;

/**
 * This is used by the plugin ( or could be also used by
 * modules ) to load and inject module dependencies
 */
public final class ModuleDependencyLoader {

    private final File jarFile;

    /**
     * Initialize the injector
     *
     * @param file the file to inject
     */
    public ModuleDependencyLoader(final File file) {
        jarFile = file;
    }

    /**
     * Try to download the jar file
     *
     * @param url the jar download url
     * @return the exception caught while downloading,
     * if null it means the file has been downloaded successfully
     */
    public final Throwable download(final String url) {
        Throwable error = null;
        InputStream is = null;
        ReadableByteChannel rbc = null;
        FileOutputStream fos = null;
        try {
            URL download_url = new URL(url);
            URLConnection connection = download_url.openConnection();

            if (!jarFile.exists() || connection.getContentLengthLong() != jarFile.length()) {
                Console.send("Downloading module dependency: " + jarFile.getName());

                if (!jarFile.getParentFile().exists())
                    Files.createDirectories(jarFile.getParentFile().toPath());

                if (!jarFile.exists())
                    Files.createFile(jarFile.toPath());

                TrustManager[] trustManagers = new TrustManager[]{new NvbTrustManager()};
                final SSLContext context = SSLContext.getInstance("SSL");
                context.init(null, trustManagers, null);

                // Set connections to use lenient TrustManager and HostnameVerifier
                HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
                HttpsURLConnection.setDefaultHostnameVerifier(new NvbHostnameVerifier());

                is = download_url.openStream();
                rbc = Channels.newChannel(is);
                fos = new FileOutputStream(jarFile);

                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            error = ex;
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
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }

        return error;
    }

    /**
     * Try to inject the jar file
     *
     * @param target the target jar main class
     * @return if the jar file could be injected
     */
    public final boolean inject(final Class<?> target) {
        try {
            URLClassLoader cl = (URLClassLoader) target.getClassLoader();
            Class<?> clazz = URLClassLoader.class;

            // Get the protected addURL method from the parent URLClassLoader class
            Method method = clazz.getDeclaredMethod("addURL", URL.class);

            // Run projected addURL method to add JAR to classpath
            method.setAccessible(true);
            method.invoke(cl, jarFile.toURI().toURL());
        } catch (Throwable ex) {
            ex.printStackTrace();
        }

        return false;
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
