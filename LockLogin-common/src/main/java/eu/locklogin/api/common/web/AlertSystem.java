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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * LockLogin alert system
 */
public final class AlertSystem {

    private static String alert_level = "&eLockLogin";
    private static String alert_message = "&7Alerts system is working, LockLogin will send you alerts from here";

    private static String latest_level = "";
    private static String latest_message = "";

    /**
     * Check for new alerts
     */
    public final void checkAlerts() {
        try {
            URL url = new URL("https://karmaconfigs.github.io/updates/LockLogin/alerts.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String word;
            List<String> lines = new ArrayList<>();
            while ((word = reader.readLine()) != null)
                if (!lines.contains(word)) {
                    lines.add(word);
                }
            reader.close();
            List<String> replaced = new ArrayList<>();
            for (String str : lines) {
                if (!replaced.contains(str)) {
                    replaced.add(str
                            .replace("[", "replace-one")
                            .replace("]", "replace-two")
                            .replace(",", "replace-comma")
                            .replace("_", "&"));
                }
            }
            alert_level = replaced.get(0)
                    .replace("replace-one", "[")
                    .replace("replace-two", "]")
                    .replace("replace-comma", ",");
            alert_message = replaced.get(1)
                    .replace("replace-one", "[")
                    .replace("replace-two", "]")
                    .replace("replace-comma", ",");
        } catch (IOException ignore) {
        }
    }

    /**
     * Get the alert message
     *
     * @return the alert message
     */
    public final String getMessage() {
        latest_level = alert_level;
        latest_message = alert_message;

        return alert_level + " " + alert_message;
    }

    /**
     * Check if there's a new alert available
     *
     * @return if there's a new alert available
     */
    public final boolean available() {
        return !latest_level.equals(alert_level) && !latest_message.equals(alert_message);
    }
}
