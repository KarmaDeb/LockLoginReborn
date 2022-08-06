package eu.locklogin.api.file.plugin;

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

import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.karma.KarmaSource;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.file.PathUtilities;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
import ml.karmaconfigs.api.common.utils.string.VersionComparator;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.*;

/**
 * LockLogin plugin messages
 */
public final class PluginProperties {

    private final static KarmaSource plugin = APISource.loadProvider("LockLogin");

    /**
     * Get a plugin message property
     *
     * @param name the property name
     * @param def  the default value
     * @return the messages properties
     */
    public String getProperty(final String name, final String def) {
        Path propFile = plugin.getDataPath().resolve("lang").resolve("plugin_messages.properties");
        try {
            if (Files.exists(propFile)) {
                updatePropsVersion();

                Properties properties = new Properties();
                properties.load(Files.newBufferedReader(propFile, StandardCharsets.UTF_8));

                return properties.getProperty(name, def);
            } else {
                InputStream in = getClass().getResourceAsStream("/lang/plugin_messages.properties");
                if (in != null) {
                    PathUtilities.create(propFile);
                    Files.copy(in, propFile, StandardCopyOption.REPLACE_EXISTING);

                    Properties properties = new Properties();
                    properties.load(in);

                    in.close();

                    return properties.getProperty(name, def);
                }
            }
        } catch (Throwable ignored) {
        }

        return "";
    }

    /**
     * Get file properties version and in-file properties
     * version to update if the version does not match
     */
    private void updatePropsVersion() {
        try {
            String comment = "HEY! IF YOU ARE MODIFYING THIS FILE FROM THE INTERNAL\n" +
                    "#LOCKLOGIN JAR, STOP NOW, IT SHOULD BE MODIFIED FROM\n" +
                    "#plugins/LockLogin/plugin_messages.properties.\n" +
                    "#   ---------------------------------------\n" +
                    "#        IMPORTANT ADVICE\n" +
                    "#\n" +
                    "#   PLEASE DO NOT MODIFY THIS FILE\n" +
                    "#   UNLESS YOU KNOW EXACTLY WHAT YOU\n" +
                    "#   ARE DOING. OTHERWISE YOU COULD CAUSE\n" +
                    "#   PLUGIN MALFUNCTIONS OR BAD MESSAGE RESPONSES\n" +
                    "#\n" +
                    "#   PLEASE DO NOT MODIFY properties_lang_version VALUE";

            Path propFile = plugin.getDataPath().resolve("lang").resolve("plugin_messages.properties");
            InputStream in = getClass().getResourceAsStream("/lang/plugin_messages.properties");

            if (Files.exists(propFile) && in != null) {
                Properties local = new Properties();
                Properties jar = new Properties();

                local.load(Files.newBufferedReader(propFile, StandardCharsets.UTF_8));
                jar.load(in);

                String localVersion = local.getProperty("properties_lang_version", "1.0.0");
                String jarVersion = jar.getProperty("properties_lang_version", "1.0.0");
                VersionComparator comparator = StringUtils.compareTo(VersionComparator
                        .createBuilder()
                        .checkVersion(jarVersion)
                        .currentVersion(localVersion));


                if (!comparator.isUpToDate()) {
                    plugin.console().send("Plugin message properties is outdated and will be updated ({0}). Yours: {1}", Level.WARNING, jarVersion, localVersion);

                    ZonedDateTime dt = ZonedDateTime.now();

                    Path tmp = plugin.getDataPath().resolve("lang").resolve("outdated")
                            .resolve(localVersion)
                            .resolve(String.valueOf(dt.getYear()))
                            .resolve(dt.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()))
                            .resolve(String.valueOf(dt.getDayOfMonth()))
                            .resolve("plugin_messages" + dt.getHour() + "-" + dt.getMinute() + "-" + dt.getSecond() + ".properties");
                    PathUtilities.create(tmp);

                    Files.move(propFile, tmp, StandardCopyOption.REPLACE_EXISTING);
                    PathUtilities.create(propFile);

                    jar.store(Files.newBufferedWriter(propFile, StandardCharsets.UTF_8, StandardOpenOption.CREATE), comment);
                    plugin.console().send("Plugin message properties updated successfully", Level.INFO);
                } else {
                    Set<Object> remove = new HashSet<>();
                    Map<Object, String> add = new LinkedHashMap<>();

                    for (Object k : local.keySet()) {
                        if (!jar.containsKey(k)) {
                            remove.add(k);
                        }
                    }
                    for (Object k : jar.keySet()) {
                        String key = (String) k;

                        if (!local.containsKey(key)) {
                            add.put(key, jar.getProperty(key, ""));
                        }
                    }

                    remove.forEach(local::remove);
                    local.putAll(add);

                    local.store(Files.newBufferedWriter(propFile, StandardCharsets.UTF_8, StandardOpenOption.CREATE), comment);
                }
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }
}
