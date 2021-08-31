package eu.locklogin.api.common.utils;

import eu.locklogin.api.util.enums.UpdateChannel;
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
     * Get the specified jar file name
     *
     * @param file the jar file
     * @return the jar file LockLogin name
     */
    static String getJarName(final File file) {
        try {
            JarFile jar = new JarFile(file);
            JarEntry jar_info = jar.getJarEntry("global.yml");

            if (jar_info != null) {
                InputStream yml = jar.getInputStream(jar_info);

                Yaml yaml = new Yaml();
                Map<String, Object> values = yaml.load(yml);
                yml.close();
                return values.getOrDefault("project_name", "LockLogin").toString();
            }
            jar.close();

            return "LockLogin";
        } catch (Throwable ex) {
            return "LockLogin";
        }
    }

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
     * Get the specified manager module file version
     *
     * @param file the jar file
     * @return the jar file LockLoginManager version
     */
    static String getManagerVersion(final File file) {
        try {
            JarFile jar = new JarFile(file);
            JarEntry jar_info = jar.getJarEntry("global.yml");

            if (jar_info != null) {
                InputStream yml = jar.getInputStream(jar_info);

                Yaml yaml = new Yaml();
                Map<String, Object> values = yaml.load(yml);
                yml.close();
                return values.getOrDefault("project_manager", "v1").toString();
            }
            jar.close();

            return "-1";
        } catch (Throwable ex) {
            return "-1";
        }
    }

    /**
     * Get the specified jar file authors
     *
     * @param file the jar file
     * @return the jar file LockLogin authors
     */
    static String getJarAuthors(final File file) {
        try {
            JarFile jar = new JarFile(file);
            JarEntry jar_info = jar.getJarEntry("global.yml");

            if (jar_info != null) {
                InputStream yml = jar.getInputStream(jar_info);

                Yaml yaml = new Yaml();
                Map<String, Object> values = yaml.load(yml);
                yml.close();
                return values.getOrDefault("project_developers", "KarmaDev").toString();
            }
            jar.close();

            return "KarmaDev";
        } catch (Throwable ex) {
            return "KarmaDev";
        }
    }

    /**
     * Get the specified jar file description
     *
     * @param file the jar file
     * @return the jar file LockLogin description
     */
    static String getJarDescription(final File file) {
        try {
            JarFile jar = new JarFile(file);
            JarEntry jar_info = jar.getJarEntry("global.yml");

            if (jar_info != null) {
                InputStream yml = jar.getInputStream(jar_info);

                Yaml yaml = new Yaml();
                Map<String, Object> values = yaml.load(yml);
                yml.close();
                return values.getOrDefault("project_description", "LockLogin plugin").toString();
            }
            jar.close();

            return "LockLogin plugin";
        } catch (Throwable ex) {
            return "LockLogin plugin";
        }
    }

    /**
     * Get the specified jar update name
     *
     * @param file the jar file
     * @return the jar file update name
     */
    static String getUpdateName(final File file) {
        String def = "--";

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
     * Get if the jar file should show dependencies
     * checksums
     *
     * @param file the jar file
     * @return the jar checksum displayed configuration
     */
    static boolean showChecksums(final File file) {
        try {
            JarFile jar = new JarFile(file);
            JarEntry jar_info = jar.getJarEntry("global.yml");

            if (jar_info != null) {
                InputStream yml = jar.getInputStream(jar_info);

                Yaml yaml = new Yaml();
                Map<String, Object> values = yaml.load(yml);
                yml.close();
                return Boolean.parseBoolean(values.getOrDefault("project_checksums", false).toString());
            }
            jar.close();

            return false;
        } catch (Throwable ex) {
            return false;
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
