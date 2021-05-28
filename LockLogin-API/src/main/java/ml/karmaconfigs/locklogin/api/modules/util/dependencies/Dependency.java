package ml.karmaconfigs.locklogin.api.modules.util.dependencies;

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

import ml.karmaconfigs.api.common.utils.FileUtilities;
import ml.karmaconfigs.locklogin.api.modules.PluginModule;

import java.io.File;

/**
 * LockLogin dependency, this can be used by
 * modules to inject their dependencies into LockLogin so all the dependencies
 * including the one who requested the dependency can
 * use it
 */
public abstract class Dependency {

    private final String dependencyName;
    private final String dependencyDownload;
    private File location;

    /**
     * Initialize the dependency object
     *
     * @param name        the dependency name
     * @param downloadURL the dependency download url
     */
    public Dependency(final String name, final String downloadURL) {
        dependencyName = name;
        dependencyDownload = downloadURL;

        location = new File(FileUtilities.getPluginsFolder() + File.separator + "LockLogin" + File.separator + File.separator + "plugin" + File.separator + "api" + File.separator + "lib", name + ".jar");
    }

    /**
     * Set the dependency module owner
     *
     * @param module the module owner
     */
    public final void setOwner(final PluginModule module) {
        location = new File(FileUtilities.getPluginsFolder() + File.separator + "LockLogin" + File.separator + "plugin" + File.separator + "api" + File.separator + module.name() + File.separator + "lib", dependencyName + ".jar");
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
    public final String getDownloadURL() {
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
     * On dependency inject
     */
    public abstract void inject();
}
