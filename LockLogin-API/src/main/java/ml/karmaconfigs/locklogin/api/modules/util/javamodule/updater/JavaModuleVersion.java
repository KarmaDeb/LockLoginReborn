package ml.karmaconfigs.locklogin.api.modules.util.javamodule.updater;

import ml.karmaconfigs.locklogin.api.modules.PluginModule;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public final class JavaModuleVersion {

    private final PluginModule module;
    private static boolean updater_enabled = false;
    private static int last_try = 0;

    /**
     * Initialize the java module version
     *
     * @param owner the module owner
     */
    public JavaModuleVersion(final PluginModule owner) {
        module = owner;
    }

    /**
     * Get the current module version
     *
     * @return the current module version
     */
    public final String getVersion() {
        return module.version();
    }

    /**
     * Get the latest module version
     *
     * @return the latest module version
     */
    public final String getLatest() {
        try {
            if (updaterEnabled().get()) {
                String update_url = module.update_url().replaceAll("\\s", "") + "latest.txt";
                URL url = new URL(update_url);

                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                String first = reader.readLine();

                if (first != null) {
                    return first;
                }
            }

            return getVersion();
        } catch (Throwable ignored) {}

        return getVersion();
    }

    /**
     * Get the latest module version
     *
     * @return the latest module version
     */
    public final String getDownloadURL() {
        try {
            if (updaterEnabled().get()) {
                String update_url = module.update_url().replaceAll("\\s", "") + "latest.txt";
                URL url = new URL(update_url);

                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                reader.readLine();
                String download_url = reader.readLine();

                if (download_url != null) {
                    return download_url;
                }
            }

            return "";
        } catch (Throwable ignored) {}

        return "";
    }

    /**
     * Get if the module has updater enabled
     *
     * @return if the module has the updater
     * enabled
     */
    public final Future<Boolean> updaterEnabled() {
        CompletableFuture<Boolean> result = new CompletableFuture<>();

        if (last_try <= 0) {
            String update_url = module.update_url().replaceAll("\\s", "");

            new Thread(() -> {
                try {
                    TrustManager[] trustManagers = new TrustManager[]{new HttpsTrustManager.NvbTrustManager()};
                    final SSLContext context = SSLContext.getInstance("SSL");
                    context.init(null, trustManagers, null);

                    HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
                    HttpsURLConnection.setDefaultHostnameVerifier(new HttpsTrustManager.NvbHostnameVerifier());

                    HttpURLConnection connection = (HttpURLConnection) new URL(update_url).openConnection();
                    connection.setConnectTimeout(2500);
                    connection.setReadTimeout(2500);
                    connection.setRequestMethod("HEAD");
                    int responseCode = connection.getResponseCode();

                    boolean status = (200 <= responseCode && responseCode <= 399);

                    result.complete(status);
                    updater_enabled = status;
                } catch (Throwable ex) {
                    result.complete(false);
                    updater_enabled = false;
                }
            }).start();

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                int back = 59;
                @Override
                public void run() {
                    if (back <= 0) {
                        last_try--;
                        if (last_try <= 0)
                            timer.cancel();
                        back = 60;
                    }

                    back--;
                }
            }, 0, 1000);

            return result;
        } else {
            result.complete(updater_enabled);
            return result;
        }
    }
}

class HttpsTrustManager {

    /**
     * Simple <code>TrustManager</code> that allows unsigned certificates.
     */
    static final class NvbTrustManager implements TrustManager, X509TrustManager {
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
    static final class NvbHostnameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }
}