package eu.locklogin.api.common.web;

import eu.locklogin.api.common.utils.FileInfo;
import eu.locklogin.api.common.utils.dependencies.Dependency;
import eu.locklogin.api.common.utils.dependencies.PluginDependency;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.api.common.karmafile.KarmaFile;
import ml.karmaconfigs.api.common.utils.FileUtilities;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Checksum tables, to check if the dependencies
 * are valid or need to be re-downloaded
 */
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

                String check_url = "https://locklogin.eu/checksum/" + version + "/checksum.lldb";
                URL url = new URL(check_url);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int response = connection.getResponseCode();
                if (response == HttpURLConnection.HTTP_OK) {
                    Console.send("&aFetching checksum results to keep dependencies safe ( {0} )", check_url);

                    InputStream inputStream = connection.getInputStream();
                    InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                    BufferedReader bf = new BufferedReader(reader);

                    File dataFile = new File(FileUtilities.getProjectFolder() + File.separator + "LockLogin", "tables.lldb");
                    KarmaFile checksum = new KarmaFile(dataFile);
                    checksum.create();

                    BufferedWriter writer = Files.newBufferedWriter(dataFile.toPath(), StandardCharsets.UTF_8);
                    String line;
                    while ((line = bf.readLine()) != null) {
                        writer.write(line + "\n");
                    }

                    writer.flush();
                    writer.close();

                    bf.close();
                    reader.close();
                    bf.close();

                    for (Dependency dependency : Dependency.values()) {
                        String name = dependency.getAsDependency().getName();
                        long adler = checksum.getLong(name + "_adler", 0L);
                        long crc = checksum.getLong(name + "_crc", 0L);

                        adler_tables.put(name, adler);
                        crc_tables.put(name, crc);
                    }

                    checksum.delete();
                } else {
                    can_check = true;
                    Console.send("&cFailed to retrieve adler checksum from locklogin.eu, response code: ({0} - {1})", response, connection.getResponseMessage());
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
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
        return adler_tables.getOrDefault(dependency.getName(), 0L);
    }

    /**
     * Get the CRC check result of the table check
     *
     * @param dependency the dependency
     * @return the crc result of tables
     */
    public static long getCRC(final PluginDependency dependency) {
        return crc_tables.getOrDefault(dependency.getName(), 0L);
    }
}
