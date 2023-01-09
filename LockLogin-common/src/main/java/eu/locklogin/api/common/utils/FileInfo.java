package eu.locklogin.api.common.utils;

import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.util.enums.UpdateChannel;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.karma.file.yaml.KarmaYamlManager;
import ml.karmaconfigs.api.common.karma.source.APISource;
import ml.karmaconfigs.api.common.karma.source.KarmaSource;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.url.URLUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
@SuppressWarnings("unused")
public class FileInfo {

    /**
     * Get the global internal file
     *
     * @return the global.yml file
     */
    private static InputStream getGlobal(final File target) {
        File file = target;
        if (file == null)
            file = new File(FileInfo.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath().replaceAll("%20", " "));

        ZipFile zip = null;
        InputStream stream = null;
        ByteArrayOutputStream b = null;
        try {
            zip = new ZipFile(file);
            ZipEntry entry = zip.getEntry("global.yml");
            if (entry != null) {
                stream = zip.getInputStream(entry);
                if (stream != null) {
                    b = new ByteArrayOutputStream();

                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = stream.read(buffer)) > -1) {
                        b.write(buffer, 0, len);
                    }

                    b.flush();
                    return new ByteArrayInputStream(b.toByteArray());
                }
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (zip != null) {
                    zip.close();
                }

                if (stream != null) {
                    stream.close();
                }

                if (b != null) {
                    b.close();
                }
            } catch (Throwable ignored) {}
        }

        return null;
    }

    /**
     * Get the specified jar file name
     *
     * @param target the file to read from
     * @return the jar file LockLogin name
     */
    public static String getJarName(final File target) {
        InputStream global = getGlobal(target);
        if (global != null) {
            KarmaYamlManager manager = new KarmaYamlManager(global);
            return manager.getString("project_name", "LockLogin");
        }

        return "LockLogin";
    }

    /**
     * Get the specified jar file version
     *
     * @param target the file to read from
     * @return the jar file LockLogin version
     */
    public static String getJarVersion(final File target) {
        InputStream global = getGlobal(target);
        if (global != null) {
            KarmaYamlManager manager = new KarmaYamlManager(global);
            return manager.getString("project_version", "1.0.0");
        }

        return "1.0.0";
    }

    /**
     * Get the specified manager module file version
     *
     * @param target the file to read from
     * @return the jar file LockLoginManager version
     */
    public static String getManagerVersion(final File target) {
        InputStream global = getGlobal(target);
        if (global != null) {
            KarmaYamlManager manager = new KarmaYamlManager(global);
            return manager.getString("project_manager", "v1");
        }

        return "v1";
    }

    /**
     * Get the specified jar file authors
     *
     * @param target the file to read from
     * @return the jar file LockLogin authors
     */
    public static String getJarAuthors(final File target) {
        InputStream global = getGlobal(target);
        if (global != null) {
            KarmaYamlManager manager = new KarmaYamlManager(global);
            return manager.getString("project_developers", "KarmaDev");
        }

        return "KarmaDev";
    }

    /**
     * Get the specified jar file description
     *
     * @param target the file to read from
     * @return the jar file LockLogin description
     */
    public static String getJarDescription(final File target) {
        InputStream global = getGlobal(target);
        if (global != null) {
            KarmaYamlManager manager = new KarmaYamlManager(global);
            return manager.getString("project_description", "LockLogin plugin");
        }

        return "LockLogin plugin";
    }

    /**
     * Get the specified jar update name
     *
     * @param target the file to read from
     * @return the jar file update name
     */
    public static String getUpdateName(final File target) {
        InputStream global = getGlobal(target);
        if (global != null) {
            KarmaYamlManager manager = new KarmaYamlManager(global);
            return manager.getString("project_update", "NoUpdate");
        }

        return "NoUpdate";
    }

    /**
     * Get if the jar file should show dependencies
     * checksums
     *
     * @param target the file to read from
     * @return the jar checksum displayed configuration
     */
    public static boolean showChecksums(final File target) {
        InputStream global = getGlobal(target);
        if (global != null) {
            KarmaYamlManager manager = new KarmaYamlManager(global);
            return manager.getBoolean("project_checksums", false);
        }

        return false;
    }

    /**
     * Get hosts LockLogin communicates with
     *
     * @param target the file to read from
     * @return the LockLogin trusted hosts
     */
    public static URL checksumHost(final File target) {
        InputStream global = getGlobal(target);
        if (global != null) {
            KarmaYamlManager manager = new KarmaYamlManager(global);
            String checksumVersion = manager.getString("project_checksum", "v1");

            KarmaYamlManager defaults = new KarmaYamlManager(new HashMap<>());
            defaults.set("ml.karmaconfigs.safe", true);
            defaults.set("ml.karmaconfigs.target", "/locklogin/");
            defaults.set("ml.karmaconfigs.checksum", "checksum/{0}/checksum.lldb");
            defaults.set("ml.karmaconfigs.alternative", Collections.singletonList("backup"));

            defaults.set("ml.karmarepo.safe", true);
            defaults.set("ml.karmarepo.target", "/locklogin/");
            defaults.set("ml.karmarepo.checksum", "checksum/{0}/checksum.lldb");
            defaults.set("ml.karmarepo.alternative", Collections.singletonList("backup"));

            defaults.set("es.karmadev.safe", true);
            defaults.set("es.karmadev.target", "/locklogin/");
            defaults.set("es.karmadev.checksum", "checksum/{0}/checksum.lldb");
            defaults.set("es.karmadev.alternative", Collections.singletonList("backup"));

            defaults.set("io.github.safe", true);
            defaults.set("io.github.target", "/karmaconfigs/updates/LockLogin/");
            defaults.set("io.github.checksum", "data/{0}/checksum.lldb");
            defaults.set("io.github.alternative", Collections.emptyList());

            KarmaYamlManager section = manager.getSection("project_urls", defaults);
            Set<String> keys = section.getKeySet();

            Set<String> domains = new LinkedHashSet<>();
            for (String ext : keys) {
                KarmaYamlManager extension = section.getSection(ext);
                if (extension.getKeySet().size() > 0) {
                    extension.getKeySet().forEach((domain) -> {
                        KarmaYamlManager data = extension.getSection(domain);
                        boolean overHttps = data.getBoolean("safe");
                        String domainDirectory = data.getString("target");

                        String url = "http://";
                        if (overHttps) {
                            url = "https://";
                        }

                        url = url + domain + "." + ext + domainDirectory;
                        domains.add(url + data.getString("checksum").replace("{0}", checksumVersion));
                        for (String alt : data.getStringList("alternative")) {
                            url = url.replace(domain + "." + ext, alt + "." + domain + "." + ext);

                            domains.add(url + data.getString("checksum").replace("{0}", checksumVersion));
                        }
                    });
                }
            }

            for (String url : domains) {
                int response_code = URLUtils.getResponseCode(url);
                if (response_code == 200)
                    try {
                        return new URL(url);
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                    }
            }
        }

        return null;
    }

    /**
     * Get hosts LockLogin communicates with
     *
     * @param target the file to read from
     * @return the LockLogin trusted hosts
     */
    public static URL versionHost(final File target) {
        InputStream global = getGlobal(target);
        if (global != null) {
            PluginConfiguration configuration = CurrentPlatform.getConfiguration();
            UpdateChannel config_channel = configuration.getUpdaterOptions().getChannel();

            KarmaYamlManager manager = new KarmaYamlManager(global);
            UpdateChannel tmp_channel = UpdateChannel.valueOf(manager.getString("project_build", "RELEASE").toUpperCase());

            switch (tmp_channel) {
                case RELEASE:
                    tmp_channel = config_channel;
                    break;
                case RC:
                case SNAPSHOT:
                    if (!config_channel.equals(tmp_channel)) {
                        KarmaSource plugin = APISource.loadProvider("LockLogin");
                        plugin.console().send("Cannot pass from {0} to {1} without an official update. To downgrade version channel, please remove the current plugin .jar and download the latest stable version",
                                Level.GRAVE,
                                tmp_channel.webName(),
                                config_channel.webName());
                    }
                    break;
            }

            UpdateChannel channel = tmp_channel;
            KarmaYamlManager defaults = new KarmaYamlManager(new HashMap<>());
            defaults.set("ml.karmaconfigs.safe", true);
            defaults.set("ml.karmaconfigs.target", "/locklogin/");
            defaults.set("ml.karmaconfigs.version", "version/");
            defaults.set("ml.karmaconfigs.alternative", Collections.singletonList("backup"));

            defaults.set("ml.karmarepo.safe", true);
            defaults.set("ml.karmarepo.target", "/locklogin/");
            defaults.set("ml.karmarepo.version", "version/");
            defaults.set("ml.karmarepo.alternative", Collections.singletonList("backup"));

            defaults.set("es.karmadev.safe", true);
            defaults.set("es.karmadev.target", "/locklogin/");
            defaults.set("es.karmadev.version", "version/");
            defaults.set("es.karmadev.alternative", Collections.singletonList("backup"));

            defaults.set("io.github.safe", true);
            defaults.set("io.github.target", "/karmaconfigs/updates/LockLogin/");
            defaults.set("io.github.version", "version/");
            defaults.set("io.github.alternative", Collections.emptyList());

            KarmaYamlManager section = manager.getSection("project_urls", defaults);
            Set<String> keys = section.getKeySet();

            Set<String> domains = new LinkedHashSet<>();
            for (String ext : keys) {
                KarmaYamlManager extension = section.getSection(ext);
                if (extension.getKeySet().size() > 0) {
                    extension.getKeySet().forEach((domain) -> {
                        KarmaYamlManager data = extension.getSection(domain);
                        boolean overHttps = data.getBoolean("safe");
                        String domainDirectory = data.getString("target");

                        String url = "http://";
                        if (overHttps) {
                            url = "https://";
                        }

                        url = url + domain + "." + ext + domainDirectory;
                        domains.add(url + data.getString("version") + channel.webName() + ".kup");
                        for (String alt : data.getStringList("alternative")) {
                            url = url.replace(domain + "." + ext, alt + "." + domain + "." + ext);

                            domains.add(url + data.getString("version") + channel.webName() + ".kup");
                        }
                    });
                }
            }

            for (String url : domains) {
                int response_code = URLUtils.getResponseCode(url);
                if (response_code == 200)
                    try {
                        return new URL(url);
                    } catch (Throwable ignored) {}
            }
        }

        return null;
    }

    /**
     * Get hosts LockLogin communicates with
     *
     * @param target the file to read from
     * @param file the dependency file name
     *
     * @return the LockLogin trusted hosts
     */
    public static URL repositoryHost(final File target, final String file) {
        InputStream global = getGlobal(target);
        if (global != null) {
            KarmaYamlManager manager = new KarmaYamlManager(global);
            String repositoryVersion = manager.getString("project_repository", "v1");

            KarmaYamlManager defaults = new KarmaYamlManager(new HashMap<>());
            defaults.set("ml.karmaconfigs.safe", true);
            defaults.set("ml.karmaconfigs.target", "/locklogin/");
            defaults.set("ml.karmaconfigs.dependency", "repository/{0}/");
            defaults.set("ml.karmaconfigs.alternative", Collections.singletonList("backup"));

            defaults.set("ml.karmarepo.safe", true);
            defaults.set("ml.karmarepo.target", "/locklogin/");
            defaults.set("ml.karmarepo.dependency", "repository/{0}/");
            defaults.set("ml.karmarepo.alternative", Collections.singletonList("backup"));

            defaults.set("es.karmadev.safe", true);
            defaults.set("es.karmadev.target", "/locklogin/");
            defaults.set("es.karmadev.dependency", "repository/{0}/");
            defaults.set("es.karmadev.alternative", Collections.singletonList("backup"));

            defaults.set("io.github.safe", true);
            defaults.set("io.github.target", "/karmaconfigs/updates/LockLogin/");
            defaults.set("io.github.dependency", "repository/{0}/");
            defaults.set("io.github.alternative", Collections.emptyList());

            KarmaYamlManager section = manager.getSection("project_urls", defaults);
            Set<String> keys = section.getKeySet();

            Set<String> domains = new LinkedHashSet<>();
            for (String ext : keys) {
                KarmaYamlManager extension = section.getSection(ext);
                if (extension.getKeySet().size() > 0) {
                    extension.getKeySet().forEach((domain) -> {
                        KarmaYamlManager data = extension.getSection(domain);
                        boolean overHttps = data.getBoolean("safe");
                        String domainDirectory = data.getString("target");

                        String url = "http://";
                        if (overHttps) {
                            url = "https://";
                        }

                        url = url + domain + "." + ext + domainDirectory;
                        domains.add(url + data.getString("dependency").replace("{0}", repositoryVersion) + file);
                        for (String alt : data.getStringList("alternative")) {
                            url = url.replace(domain + "." + ext, alt + "." + domain + "." + ext);

                            domains.add(url + data.getString("dependency").replace("{0}", repositoryVersion) + file);
                        }
                    });
                }
            }

            for (String url : domains) {
                int response_code = URLUtils.getResponseCode(url);
                if (response_code == 200)
                    try {
                        return new URL(url);
                    } catch (Throwable ignored) {}
            }
        }

        return null;
    }

    /**
     * Get hosts LockLogin communicates with
     *
     * @param target the file to read from
     * @return the LockLogin trusted hosts
     */
    public static URL updateHost(final File target) {
        InputStream global = getGlobal(target);
        if (global != null) {
            KarmaYamlManager manager = new KarmaYamlManager(global);
            String currentChannel = getChannel(target).webName();

            KarmaYamlManager defaults = new KarmaYamlManager(new HashMap<>());
            defaults.set("ml.karmaconfigs.safe", true);
            defaults.set("ml.karmaconfigs.target", "/locklogin/");
            defaults.set("ml.karmaconfigs.update", "version/{0}/LockLogin.jar");
            defaults.set("ml.karmaconfigs.alternative", Collections.singletonList("backup"));

            defaults.set("ml.karmarepo.safe", true);
            defaults.set("ml.karmarepo.target", "/locklogin/");
            defaults.set("ml.karmarepo.update", "version/{0}/LockLogin.jar");
            defaults.set("ml.karmarepo.alternative", Collections.singletonList("backup"));

            defaults.set("es.karmadev.safe", true);
            defaults.set("es.karmadev.target", "/locklogin/");
            defaults.set("es.karmadev.update", "version/{0}/LockLogin.jar");
            defaults.set("es.karmadev.alternative", Collections.singletonList("backup"));

            defaults.set("io.github.safe", true);
            defaults.set("io.github.target", "/karmaconfigs/updates/LockLogin/");
            defaults.set("io.github.update", "version/{0}/LockLogin.jar");
            defaults.set("io.github.alternative", Collections.emptyList());

            KarmaYamlManager section = manager.getSection("project_urls", defaults);
            Set<String> keys = section.getKeySet();

            Set<String> domains = new LinkedHashSet<>();
            for (String ext : keys) {
                KarmaYamlManager extension = section.getSection(ext);
                if (extension.getKeySet().size() > 0) {
                    extension.getKeySet().forEach((domain) -> {
                        KarmaYamlManager data = extension.getSection(domain);
                        boolean overHttps = data.getBoolean("safe");
                        String domainDirectory = data.getString("target");

                        String url = "http://";
                        if (overHttps) {
                            url = "https://";
                        }

                        url = url + domain + "." + ext + domainDirectory;
                        domains.add(url + data.getString("update").replace("{0}", currentChannel));
                        for (String alt : data.getStringList("alternative")) {
                            url = url.replace(domain + "." + ext, alt + "." + domain + "." + ext);

                            domains.add(url + data.getString("update").replace("{0}", currentChannel));
                        }
                    });
                }
            }

            for (String url : domains) {
                int response_code = URLUtils.getResponseCode(url);
                if (response_code == 200)
                    try {
                        return new URL(url);
                    } catch (Throwable ignored) {}
            }
        }

        return null;
    }

    /**
     * Get hosts LockLoginManager will be downloaded from
     *
     * @param target the file to read from
     * @return the LockLoginManager download source
     */
    public static URL managerHost(final File target) {
        InputStream global = getGlobal(target);
        if (global != null) {
            KarmaYamlManager manager = new KarmaYamlManager(global);
            String currentChannel = getManagerVersion(target);

            KarmaYamlManager defaults = new KarmaYamlManager(new HashMap<>());
            defaults.set("ml.karmaconfigs.safe", true);
            defaults.set("ml.karmaconfigs.target", "/locklogin/");
            defaults.set("ml.karmaconfigs.manager", "-repository/manager/{0}/LockLoginManager.jar");
            defaults.set("ml.karmaconfigs.alternative", Collections.singletonList("backup"));

            defaults.set("ml.karmarepo.safe", true);
            defaults.set("ml.karmarepo.target", "/locklogin/");
            defaults.set("ml.karmarepo.manager", "-repository/manager/{0}/LockLoginManager.jar");
            defaults.set("ml.karmarepo.alternative", Collections.singletonList("backup"));

            defaults.set("es.karmadev.safe", true);
            defaults.set("es.karmadev.target", "/locklogin/");
            defaults.set("ml.karmadev.manager", "-repository/manager/{0}/LockLoginManager.jar");
            defaults.set("es.karmadev.alternative", Collections.singletonList("backup"));

            defaults.set("io.github.safe", true);
            defaults.set("io.github.target", "/karmaconfigs/updates/LockLogin/");
            defaults.set("io.github.manager", "modules/manager/{0}/LockLoginManager.jar");
            defaults.set("io.github.alternative", Collections.emptyList());

            KarmaYamlManager section = manager.getSection("project_urls", defaults);
            Set<String> keys = section.getKeySet();

            Set<String> domains = new LinkedHashSet<>();
            for (String ext : keys) {
                KarmaYamlManager extension = section.getSection(ext);
                if (extension.getKeySet().size() > 0) {
                    extension.getKeySet().forEach((domain) -> {
                        KarmaYamlManager data = extension.getSection(domain);
                        boolean overHttps = data.getBoolean("safe");
                        String domainDirectory = data.getString("target");

                        String url = "http://";
                        if (overHttps) {
                            url = "https://";
                        }

                        url = url + domain + "." + ext + domainDirectory;
                        domains.add(url + data.getString("manager").replace("{0}", currentChannel));
                        for (String alt : data.getStringList("alternative")) {
                            url = url.replace(domain + "." + ext, alt + "." + domain + "." + ext);

                            domains.add(url + data.getString("manager").replace("{0}", currentChannel));
                        }
                    });
                }
            }

            for (String url : domains) {
                int response_code = URLUtils.getResponseCode(url);
                if (response_code == 200)
                    try {
                        return new URL(url);
                    } catch (Throwable ignored) {}
            }
        }

        return null;
    }

    /**
     * Get the specified jar version channel
     *
     * @param target the file to read from
     * @return the jar version channel
     */
    public static UpdateChannel getChannel(final File target) {
        InputStream global = getGlobal(target);
        if (global != null) {
            KarmaYamlManager manager = new KarmaYamlManager(global);
            return UpdateChannel.valueOf(manager.getString("project_build", "RELEASE").toUpperCase());
        }

        return UpdateChannel.RELEASE;
    }
}
