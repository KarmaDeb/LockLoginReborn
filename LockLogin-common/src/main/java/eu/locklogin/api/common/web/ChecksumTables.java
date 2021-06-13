package eu.locklogin.api.common.web;

import com.google.gson.*;
import eu.locklogin.api.common.utils.FileInfo;
import eu.locklogin.api.common.utils.dependencies.Dependency;
import eu.locklogin.api.common.utils.dependencies.PluginDependency;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.Console;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

public final class ChecksumTables {

    private final static Map<String, Long> adler_tables = new HashMap<>();
    private final static Map<String, Long> crc_tables = new HashMap<>();

    private static boolean can_check = true;

    /**
     * Start the checksum tables data fetch
     */
    public final void checkTables() {
        if (can_check) {
            can_check = false;

            try {
                int wait = (int) TimeUnit.MINUTES.convert(5, TimeUnit.MILLISECONDS);

                //Avoid plugin instances to flood the update channel
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        can_check = true;
                    }
                }, wait);

                String version = FileInfo.getJarVersion(new File(CurrentPlatform.getMain().getProtectionDomain().getCodeSource().getLocation().getPath().replaceAll("%20", " ")));

                String check_url = "https://locklogin.eu/checksum/" + version + "/?type=adler";
                URL url = new URL(check_url);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                //connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");

                int response = connection.getResponseCode();
                if (response == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    JsonObject value = gson.fromJson(reader, JsonObject.class);

                    for (Dependency dependency : Dependency.values()) {
                        String name = dependency.name();
                        long adler = value.get(name).getAsLong();

                        adler_tables.put(name, adler);
                    }
                } else {
                    can_check = true;
                    Console.send("&cFailed to retrieve adler checksum from locklogin.eu, response code: ({0} - {1})", response, connection.getResponseMessage());
                }

                check_url = "https://locklogin.eu/checksum/?type=crc";
                url = new URL(check_url);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                //connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");

                response = connection.getResponseCode();
                if (response == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    JsonObject value = gson.fromJson(reader, JsonObject.class);

                    for (Dependency dependency : Dependency.values()) {
                        String name = dependency.getAsDependency().getName();
                        long adler = value.get(name).getAsLong();

                        crc_tables.put(name, adler);
                    }
                } else {
                    can_check = true;
                    Console.send("&cFailed to retrieve crc checksum from locklogin.eu, response code: ({0} - {1})", response, connection.getResponseMessage());
                }
            } catch (Throwable ex) {
                can_check = true;
            }
        }
    }

    /**
     * Get the adler check result of the table check
     *
     * @param dependency the dependency
     * @return the adler result of tables
     */
    public static long getAdler(final PluginDependency dependency) {
        return adler_tables.getOrDefault(dependency.getName(), dependency.getAdlerCheck());
    }

    /**
     * Get the CRC check result of the table check
     *
     * @param dependency the dependency
     * @return the crc result of tables
     */
    public static long getCRC(final PluginDependency dependency) {
        return crc_tables.getOrDefault(dependency.getName(), dependency.getCRCCheck());
    }
}
