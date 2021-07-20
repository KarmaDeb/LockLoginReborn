package eu.locklogin.api.common.web;

import com.google.gson.*;
import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.api.common.utils.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Donor names fetcher
 */
public final class STFetcher {

    private final static Set<String> special_thanks = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static boolean can_check = true;

    /**
     * Start the checksum tables data fetch
     */
    public final void check() {
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

                String check_url = "https://locklogin.eu/stf/";
                URL url = new URL(check_url);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int response = connection.getResponseCode();
                if (response == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    JsonObject value = gson.fromJson(reader, JsonObject.class);
                    JsonArray array = value.get("donors").getAsJsonArray();

                    for (JsonElement element : array) special_thanks.add(element.getAsString());
                } else {
                    can_check = true;
                    Console.send("&cFailed to retrieve adler checksum from locklogin.eu, response code: ({0} - {1})", response, connection.getResponseMessage());
                }
            } catch (Throwable ex) {
                can_check = true;
            }
        }
    }

    /**
     * Get a list with the plugin contributors
     *
     * @return the adler result of tables
     */
    public static String getDonors() {
        StringBuilder builder = new StringBuilder();

        for (String donor : special_thanks)
            builder.append(donor).append(", ");

        return StringUtils.replaceLast(builder.toString(), ", ", "");
    }
}
