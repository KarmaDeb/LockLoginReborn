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

import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.api.common.utils.file.FileUtilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/**
 * LockLogin plugin messages
 */
public final class PluginProperties {

    /**
     * Get a plugin message property
     *
     * @param name the property name
     * @param def the default value
     * @return the messages properties
     */
    public String getProperty(final String name, final String def) {
        File propFile = new File(FileUtilities.getProjectFolder() + File.separator + "LockLogin", "lang/plugin_messages.properties");
        propFile = FileUtilities.getFixedFile(propFile);
        try {
            if (propFile.exists()) {
                updatePropsVersion();
                FileInputStream in = new FileInputStream(propFile);

                Properties properties = new Properties();
                properties.load(in);

                in.close();

                return properties.getProperty(name, def);
            } else {
                InputStream in = getClass().getResourceAsStream("/lang/plugin_messages.properties");
                if (in != null) {
                    Files.copy(in, propFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

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

            File propFile = new File(FileUtilities.getProjectFolder() + File.separator + "LockLogin", "lang/plugin_messages.properties");
            propFile = FileUtilities.getFixedFile(propFile);

            Properties inProperties = new Properties();
            Properties outProperties = new Properties();

            inProperties.load(CurrentPlatform.getMain().getResourceAsStream("/lang/plugin_messages.properties"));
            outProperties.load(new FileInputStream(propFile));

            String outVersion = outProperties.getProperty("properties_lang_version", "1.0.0");
            String inVersion = inProperties.getProperty("properties_lang_version", "1.0.1");

            if (StringUtils.compareTo(inVersion, outVersion) > 0) {
                for (Object inKey : inProperties.keySet()) {
                    if (outProperties.getOrDefault(inKey, null) == null) {
                        outProperties.put(inKey, inProperties.get(inKey));
                    }
                }

                for (Object outKey : outProperties.keySet()) {
                    if (inProperties.getOrDefault(outKey, null) == null) {
                        outProperties.remove(outKey);
                    }
                }
            }

            outProperties.store(Files.newBufferedWriter(propFile.toPath(), StandardCharsets.UTF_8), comment);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }
}
