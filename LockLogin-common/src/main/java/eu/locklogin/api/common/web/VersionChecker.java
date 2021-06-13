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

import com.google.gson.*;
import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.api.common.utils.StringUtils;
import eu.locklogin.api.util.enums.UpdateChannel;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * LockLogin version checker
 */
public final class VersionChecker {

    private static String latest_version = "";
    private static String latest_changelog = "";
    static String download_url = "";
    private static boolean can_check = true;
    private final String current_version;

    /**
     * Initialize the version checker
     *
     * @param version the current version
     */
    public VersionChecker(final String version) {
        current_version = version;
    }

    /**
     * Check for a new version
     *
     * @param channel the current plugin channel
     */
    public final void checkVersion(final UpdateChannel channel) {
        if (can_check) {
            can_check = false;

            int wait = (int) TimeUnit.MINUTES.convert(5, TimeUnit.MILLISECONDS);

            //Avoid plugin instances to flood the update channel
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    can_check = true;
                }
            }, wait);

            String name;
            switch (channel) {
                case SNAPSHOT:
                    name = "snapshot";
                    break;
                case RC:
                    name = "rc";
                    break;
                case RELEASE:
                default:
                    name = "release";
                    break;
            }

            String check_url = "https://locklogin.eu/version/?channel=" + name;
            try {
                URL url = new URL(check_url);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                //connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");

                int response = connection.getResponseCode();
                if (response == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    JsonObject value = gson.fromJson(reader, JsonObject.class);

                    latest_version = value.get("version").getAsString();
                    download_url = value.get("download").getAsString();
                    JsonArray changelog = value.getAsJsonArray("changelog");
                    List<String> lines = new ArrayList<>();
                    for (JsonElement object : changelog)
                        lines.add(object.getAsString().replace("_", "&"));

                    latest_changelog = StringUtils.listToString(lines,false);
                } else {
                    can_check = true;
                    Console.send("&cFailed to check for updates at locklogin.eu host, response code: ({0} - {1})", response, connection.getResponseMessage());
                }
            } catch (Throwable ex) {
                can_check = true;
                ex.printStackTrace();
            }
        }
    }

    /**
     * Check if the plugin is outdated
     *
     * @return if the plugin is outdated
     */
    public final boolean isOutdated() {
        if (latest_version.replaceAll("\\s", "").isEmpty())
            return false;

        return !latest_version.equals(current_version);
    }

    /**
     * Get the plugin latest version id
     *
     * @return the plugin latest version id
     */
    public final String getLatestVersion() {
        return latest_version;
    }

    /**
     * Get the latest plugin changelog
     *
     * @return the latest plugin changelog
     */
    public final String getChangelog() {
        return latest_changelog;
    }

    /**
     * Get the latest download url
     *
     * @return the plugin latest download url
     */
    public final String getDownloadURL() {
        return download_url;
    }
}
