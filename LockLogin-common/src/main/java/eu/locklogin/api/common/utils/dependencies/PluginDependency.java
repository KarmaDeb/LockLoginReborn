package eu.locklogin.api.common.utils.dependencies;

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

import eu.locklogin.api.common.web.ChecksumTables;
import eu.locklogin.api.module.PluginModule;
import eu.locklogin.api.util.platform.CurrentPlatform;
import me.lucko.jarrelocator.Relocation;
import ml.karmaconfigs.api.common.string.StringUtils;

import java.io.File;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Adler32;
import java.util.zip.CRC32;

/**
 * LockLogin dependency, this can be used by
 * modules to inject their dependencies into LockLogin so all the dependencies
 * including the one who requested the dependency can
 * use it
 */
public abstract class PluginDependency {

    private final static File pluginsFolder = new File(CurrentPlatform.getMain().getProtectionDomain().getCodeSource().getLocation().getPath().replaceAll("%20", " ")).getParentFile();

    private final String dependencyName;
    private final URL dependencyDownload;
    private File location;

    private boolean openChecksum = true;
    private boolean module = false;
    private boolean high_priority = false;

    private final Map<String, String> relocation = new HashMap<>();

    /**
     * Initialize the dependency object
     * <p>
     * Why private?
     * - It's easier to use {@link PluginDependency#of(String, URL, boolean, String...)} to create a new instance
     * - It's a non-sense to have a public constructor of an abstract class which is not expected to be extended
     *
     * @param name        the dependency name
     * @param downloadURL the dependency download url
     */
    private PluginDependency(final String name, final URL downloadURL, final String... sub) {
        dependencyName = name;
        dependencyDownload = downloadURL;

        if (sub.length == 0) {
            location = new File(pluginsFolder + File.separator + "LockLogin" + File.separator + "plugin" + File.separator + "libraries", name + ".jar");
        } else {
            StringBuilder locBuilder = new StringBuilder();
            for (String location : sub)
                locBuilder.append(File.separator).append(location);

            location = new File(pluginsFolder + File.separator + "LockLogin" + File.separator + "plugin" + File.separator + "libraries" + locBuilder, name + ".jar");
        }
    }

    /**
     * Relocate a dependency
     *
     * @param packPaths the package name
     * @param target the target
     * @return this instance
     */
    public PluginDependency relocate(final String target, final String... packPaths) {
        StringBuilder packBuilder = new StringBuilder();
        for (String names : packPaths) {
            packBuilder.append(names).append('.');
        }
        String pack = packBuilder.substring(0, packBuilder.length() - 1);

        relocation.put(pack, target);
        return this;
    }

    /**
     * Get if the dependency has relocations
     *
     * @return if the dependency has relocations
     */
    public boolean hasRelocations() {
        return !relocation.isEmpty();
    }

    /**
     * Get if the dependency is high priority
     *
     * @return if dependency must be downloaded and
     * loaded before any other
     */
    public boolean isHighPriority() {
        return high_priority;
    }

    /**
     * Get the relocations
     *
     * @return the relocations
     */
    public List<Relocation> relocations() {
        List<Relocation> r = new ArrayList<>();
        /*for (String key : relocation.keySet()) {
            String value = relocation.getOrDefault(key, null);
            if (!StringUtils.isNullOrEmpty(value)) {
                r.add(new Relocation(key, value));
            }
        }*/

        return r;
    }

    /**
     * Get a temp plugin dependency from the provided info
     *
     * @param name         the dependency name
     * @param downloadURL  the download url
     * @param openChecksum if the dependency wants to enable
     *                     checksum
     * @param sub          the dependency custom sub directory
     * @return the temporal plugin dependency
     */
    public static PluginDependency of(final String name, final URL downloadURL, final boolean openChecksum, final String... sub) {
        PluginDependency temp = new PluginDependency(name, downloadURL, sub) {
            @Override
            public String toString() {
                return "PluginDependency@" + this.hashCode() + "{" +
                        "name:" + name +
                        "url:" + downloadURL +
                        "openChecksum:" + openChecksum + "}";
            }
        };
        temp.openChecksum = openChecksum;

        return temp;
    }

    /**
     * Get a temp plugin dependency from the provided info
     *
     * @param name         the dependency name
     * @param downloadURL  the download url
     * @param openChecksum if checksum is enabled
     * @param module       if the dependency is a module
     * @return the temporal plugin dependency
     */
    public static PluginDependency of(final String name, final URL downloadURL, final boolean openChecksum, final boolean module) {
        PluginDependency temp = new PluginDependency(name, downloadURL) {
            @Override
            public String toString() {
                return "PluginDependency@" + this.hashCode() + "{" +
                        "name:" + name +
                        "url:" + downloadURL +
                        "openChecksum:" + openChecksum + "}";
            }
        };

        if (module) {
            temp.location = new File(pluginsFolder + File.separator + "LockLogin" + File.separator + "plugin" + File.separator + "modules", name + ".jar");
            temp.module = true;
        }

        temp.openChecksum = openChecksum;

        return temp;
    }

    /**
     * Get a temp plugin dependency from the provided info
     *
     * @param name         the dependency name
     * @param downloadURL  the download url
     * @param openChecksum if checksum is enabled
     * @param module       if the dependency is a module
     * @return the temporal plugin dependency
     */
    public static PluginDependency of(final String name, final URL downloadURL, final boolean openChecksum, final boolean module, final boolean high_priority) {
        PluginDependency temp = new PluginDependency(name, downloadURL) {
            @Override
            public String toString() {
                return "PluginDependency@" + this.hashCode() + "{" +
                        "name:" + name +
                        "url:" + downloadURL +
                        "openChecksum:" + openChecksum + "}";
            }
        };

        if (module) {
            temp.location = new File(pluginsFolder + File.separator + "LockLogin" + File.separator + "plugin" + File.separator + "modules", name + ".jar");
            temp.module = true;
        }

        temp.high_priority = high_priority;
        temp.openChecksum = openChecksum;

        return temp;
    }

    /**
     * Set the dependency module owner
     *
     * @param module the module owner
     */
    public final void setOwner(final PluginModule module) {
        location = new File(pluginsFolder + File.separator + "LockLogin" + File.separator + "plugin" + File.separator + "api" + File.separator + module.name() + File.separator + "lib", dependencyName + ".jar");
    }

    /**
     * Get the dependency name
     *
     * @return the dependency name
     */
    public final String getName() {
        return dependencyName;
    }

    /**
     * Get the dependency download url
     *
     * @return the dependency download url
     */
    public final URL getDownloadURL() {
        return dependencyDownload;
    }

    /**
     * Get the dependency file location
     *
     * @return the dependency file location
     */
    public final File getLocation() {
        return location;
    }

    /**
     * Check if the file is valid by verifying two
     * checksums
     *
     * @return if the file is valid
     */
    public final boolean isValid() {
        if (openChecksum) {
            try {
                byte[] bytes = Files.readAllBytes(getLocation().toPath());

                Adler32 adler = new Adler32();
                adler.update(ByteBuffer.wrap(bytes));

                long adlerChecksum = adler.getValue();
                if (adlerChecksum == ChecksumTables.getAdler(this)) {
                    CRC32 crc = new CRC32();
                    crc.update(ByteBuffer.wrap(bytes));

                    long crcChecksum = crc.getValue();
                    return crcChecksum == ChecksumTables.getCRC(this);
                }

                return false;
            } catch (Throwable ex) {
                return false;
            }
        } else {
            return true;
        }
    }

    /**
     * Get if the dependency is not a module
     *
     * @return if the dependency is not a module
     */
    public final boolean isDependency() {
        return !module;
    }

    /**
     * Get adler checksum value
     *
     * @return the adler checksum value
     */
    public final long getAdlerCheck() {
        try {
            byte[] bytes = Files.readAllBytes(getLocation().toPath());

            Adler32 adler = new Adler32();
            adler.update(ByteBuffer.wrap(bytes));

            return adler.getValue();
        } catch (Throwable ex) {
            return 0L;
        }
    }

    /**
     * Get the CRC32 checksum value
     *
     * @return the CRC32C checksum value
     */
    public final long getCRCCheck() {
        try {
            byte[] bytes = Files.readAllBytes(getLocation().toPath());

            CRC32 adler = new CRC32();
            adler.update(ByteBuffer.wrap(bytes));

            return adler.getValue();
        } catch (Throwable ex) {
            return 0L;
        }
    }
}
