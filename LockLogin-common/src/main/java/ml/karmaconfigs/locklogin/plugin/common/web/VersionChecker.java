package ml.karmaconfigs.locklogin.plugin.common.web;

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

import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.utils.enums.UpdateChannel;

import java.io.BufferedReader;
import java.io.InputStreamReader;
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

            String name = "release/latest.txt";
            switch (channel) {
                case SNAPSHOT:
                    name = "snapshot/latest.txt";
                    break;
                case RC:
                    name = "rc/latest.txt";
                    break;
                default:
                    break;
            }

            try {
                URL url = new URL("https://karmaconfigs.github.io/updates/LockLogin/" + name);
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                String word;
                List<String> lines = new ArrayList<>();
                while ((word = reader.readLine()) != null) {
                    lines.add((word.replaceAll("\\s", "").isEmpty() ? "&f" : word));
                }

                reader.close();
                StringBuilder changelog_builder = new StringBuilder();
                for (int i = 1; i < lines.size(); i++) {
                    String line = lines.get(i);
                    changelog_builder.append(line.replace("_", "&")).append("\n");
                }

                latest_changelog = StringUtils.replaceLast(changelog_builder.toString(), "\n", "");
                latest_version = StringUtils.stripColor(lines.get(0));
            } catch (Throwable ex) {
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
}
