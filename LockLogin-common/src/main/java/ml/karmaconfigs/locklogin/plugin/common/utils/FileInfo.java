package ml.karmaconfigs.locklogin.plugin.common.utils;

import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.plugin.common.utils.enums.UpdateChannel;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
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
public interface FileInfo {

    /**
     * Get the specified jar file version
     *
     * @param file the jar file
     * @return the jar file LockLogin version
     */
    static String getJarVersion(final File file) {
        try {
            JarFile jar = new JarFile(file);
            JarEntry jar_info = jar.getJarEntry("global.yml");

            if (jar_info != null) {
                InputStream yml = jar.getInputStream(jar_info);

                Yaml yaml = new Yaml();
                Map<String, Object> values = yaml.load(yml);
                yml.close();
                return values.getOrDefault("project_version", "1.0.0").toString();
            }
            jar.close();

            return "-1";
        } catch (Throwable ex) {
            return "-1";
        }
    }

    /**
     * Get the specified jar api version
     *
     * @param file the jar file
     * @return the jar file KarmaAPI version
     */
    static String getKarmaVersion(final File file) {
        try {
            JarFile jar = new JarFile(file);
            JarEntry jar_info = jar.getJarEntry("global.yml");

            if (jar_info != null) {
                InputStream yml = jar.getInputStream(jar_info);

                Yaml yaml = new Yaml();
                Map<String, Object> values = yaml.load(yml);
                yml.close();
                return values.getOrDefault("project_karmaapi", "1.1.4").toString();
            }
            jar.close();

            return "1.1.4";
        } catch (Throwable ex) {
            return "1.1.4";
        }
    }

    /**
     * Get the specified jar update name
     *
     * @param file the jar file
     * @return the jar file update name
     */
    static String getUpdateName(final File file) {
        String def = StringUtils.randomString(4, StringUtils.StringGen.ONLY_LETTERS, StringUtils.StringType.ALL_LOWER);

        try {
            JarFile jar = new JarFile(file);
            JarEntry jar_info = jar.getJarEntry("global.yml");

            if (jar_info != null) {
                InputStream yml = jar.getInputStream(jar_info);

                Yaml yaml = new Yaml();
                Map<String, Object> values = yaml.load(yml);
                yml.close();
                return values.getOrDefault("project_update", def).toString();
            }
            jar.close();

            return def;
        } catch (Throwable ex) {
            return def;
        }
    }

    /**
     * Check if the file has advanced debug enabled
     *
     * @param file the jar file
     * @return if the jar file has advanced debug enabled
     */
    static boolean apiDebug(final File file) {
        try {
            JarFile jar = new JarFile(file);
            JarEntry jar_info = jar.getJarEntry("global.yml");

            if (jar_info != null) {
                InputStream yml = jar.getInputStream(jar_info);

                Yaml yaml = new Yaml();
                Map<String, Object> values = yaml.load(yml);
                yml.close();
                return Boolean.parseBoolean(values.getOrDefault("project_debug", false).toString());
            }
            jar.close();

            return false;
        } catch (Throwable ex) {
            return false;
        }
    }

    /**
     * Check if the specified jar ignores if the update
     * version is lower or on another update channel
     *
     * @param file the jar file
     * @return if the jar can update unsafely
     */
    static boolean unsafeUpdates(final File file) {
        try {
            JarFile jar = new JarFile(file);
            JarEntry jar_info = jar.getJarEntry("global.yml");

            if (jar_info != null) {
                InputStream yml = jar.getInputStream(jar_info);

                Yaml yaml = new Yaml();
                Map<String, Object> values = yaml.load(yml);
                yml.close();
                return Boolean.parseBoolean(values.getOrDefault("project_unsafeUpdate", false).toString());
            }
            jar.close();

            return false;
        } catch (Throwable ex) {
            return false;
        }
    }

    /**
     * Get the jar KarmaAPI log max size
     *
     * @param file the jar file
     * @return the jar logs configuration
     */
    static int logFileSize(final File file) {
        try {
            JarFile jar = new JarFile(file);
            JarEntry jar_info = jar.getJarEntry("global.yml");

            if (jar_info != null) {
                InputStream yml = jar.getInputStream(jar_info);

                Yaml yaml = new Yaml();
                Map<String, Object> values = yaml.load(yml);
                yml.close();
                return Integer.parseInt(values.getOrDefault("project_logSizeLimit", 100).toString());
            }
            jar.close();

            return 100;
        } catch (Throwable ex) {
            return 100;
        }
    }

    /**
     * Get the specified jar version channel
     *
     * @param file the jar file
     * @return the jar version channel
     */
    static UpdateChannel getChannel(final File file) {
        try {
            JarFile jar = new JarFile(file);
            JarEntry jar_info = jar.getJarEntry("global.yml");

            if (jar_info != null) {
                InputStream yml = jar.getInputStream(jar_info);

                Yaml yaml = new Yaml();
                Map<String, Object> values = yaml.load(yml);
                yml.close();

                String value = values.getOrDefault("project_build", "RELEASE").toString();
                return UpdateChannel.valueOf(value.toUpperCase());
            }
            jar.close();

            return UpdateChannel.RELEASE;
        } catch (Throwable ex) {
            return UpdateChannel.RELEASE;
        }
    }
}
