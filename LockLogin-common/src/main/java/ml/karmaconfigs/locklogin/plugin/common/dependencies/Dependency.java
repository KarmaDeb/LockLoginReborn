package ml.karmaconfigs.locklogin.plugin.common.dependencies;

import ml.karmaconfigs.api.common.utils.FileUtilities;

import java.io.File;

public abstract class Dependency {

    private final String dependencyName;
    private final String dependencyDownload;
    private final File location;

    public Dependency(final String name, final String downloadURL) {
        dependencyName = name;
        dependencyDownload = downloadURL;

        location = new File(FileUtilities.getPluginsFolder() + File.separator + "LockLogin" + File.separator + "libraries", name + ".jar");
    }

    public final String getName() {
        return dependencyName;
    }

    public final String getDownloadURL() {
        return dependencyDownload;
    }

    public final File getLocation() {
        return location;
    }

    public abstract void inject();
}
