package eu.locklogin.api.common.web;

import eu.locklogin.api.common.utils.FileInfo;
import eu.locklogin.api.common.utils.dependencies.Dependency;
import eu.locklogin.api.common.utils.dependencies.PluginDependency;
import ml.karmaconfigs.api.common.data.path.PathUtilities;
import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.karma.file.element.KarmaPrimitive;
import ml.karmaconfigs.api.common.karma.file.element.types.Element;
import ml.karmaconfigs.api.common.karma.file.element.types.ElementPrimitive;
import ml.karmaconfigs.api.common.karma.source.APISource;
import ml.karmaconfigs.api.common.karma.source.KarmaSource;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private final static KarmaSource lockLogin = APISource.loadProvider("LockLogin");
    private final static Map<String, Long> adler_tables = new HashMap<>();
    private final static Map<String, Long> crc_tables = new HashMap<>();

    private static boolean can_check = true;

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

    /**
     * Start the checksum tables data fetch
     */
    public void checkTables() {
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

                URL check_url = FileInfo.checksumHost(null);
                if (check_url != null) {
                    HttpURLConnection connection = (HttpURLConnection) check_url.openConnection();
                    connection.setRequestMethod("GET");

                    int response = connection.getResponseCode();
                    if (response == HttpURLConnection.HTTP_OK) {
                        lockLogin.console().send("&aFetching checksum results to keep dependencies safe ( {0} )", check_url);

                        InputStream inputStream = connection.getInputStream();
                        InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                        BufferedReader bf = new BufferedReader(reader);

                        Path dataFile = lockLogin.getDataPath().resolve("cache").resolve("tables.lldb");
                        PathUtilities.create(dataFile);

                        BufferedWriter writer = Files.newBufferedWriter(dataFile, StandardCharsets.UTF_8);
                        String line;
                        while ((line = bf.readLine()) != null) {
                            writer.write(line + "\n");
                        }

                        writer.flush();
                        writer.close();

                        bf.close();
                        reader.close();
                        bf.close();

                        KarmaMain checksum = new KarmaMain(dataFile);
                        checksum.create();

                        for (Dependency dependency : Dependency.values()) {
                            String name = dependency.getAsDependency().getName();
                            Element<?> adler = checksum.get("adler." + name.replace(" ", "_"), new KarmaPrimitive(0L));
                            Element<?> crc = checksum.get("crc." + name.replace(" ", "_"), new KarmaPrimitive(0L));

                            System.out.println("Reading checksum for: " + name.replace(" ", "_"));

                            if (adler.isPrimitive() && crc.isPrimitive()) {
                                ElementPrimitive adlerPrimitive = adler.getAsPrimitive();
                                ElementPrimitive crcPrimitive = crc.getAsPrimitive();

                                if (adlerPrimitive.isNumber() && crcPrimitive.isNumber()) {
                                    adler_tables.put(name, adlerPrimitive.asLong());
                                    crc_tables.put(name, crcPrimitive.asLong());
                                }
                            }
                        }

                        checksum.delete();
                    } else {
                        can_check = true;
                        lockLogin.console().send("&cFailed to retrieve adler checksum from karma repository site, response code: ({0} - {1})", response, connection.getResponseMessage());
                    }
                } else {
                    can_check = true;
                    lockLogin.console().send("&cFailed to retrieve checksums for LockLogin, please make sure you have internet connection (null address)");
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
                can_check = true;
            }
        }
    }
}
