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

import eu.locklogin.api.common.utils.FileInfo;
import eu.locklogin.api.util.platform.CurrentPlatform;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * LockLogin dependencies
 */
public enum Dependency {
    /**
     * LockLogin dependency
     */
    APACHE_COMMONS_CODEC,
    /**
     * LockLogin dependency
     */
    JNA,
    /**
     * LockLogin dependency
     */
    GOOGLE_AUTHENTICATOR,
    /**
     * LockLogin dependency
     */
    LOG4J,
    /**
     * LockLogin dependency
     */
    LOG4J_WEB,
    /**
     * LockLogin dependency
     */
    JAVASSIST,
    /**
     * LockLogin dependency
     */
    GUAVA,
    /**
     * LockLogin dependency
     */
    GSON,
    /**
     * LockLogin dependency
     */
    MANAGER;


    /**
     * Get the dependency as a dependency object
     *
     * @return the dependency as a dependency object
     */
    @NotNull
    public PluginDependency getAsDependency() {
        switch (this) {
            case APACHE_COMMONS_CODEC:
                return PluginDependency.of("Apache Commons Codec", "https://repo1.maven.org/maven2/commons-codec/commons-codec/1.15/commons-codec-1.15.jar", true);
            case JNA:
                return PluginDependency.of("Java Native Access", "https://repo1.maven.org/maven2/net/java/dev/jna/jna/5.8.0/jna-5.8.0.jar", true);
            case GOOGLE_AUTHENTICATOR:
                return PluginDependency.of("Google Authenticator", "https://repo1.maven.org/maven2/com/warrenstrange/googleauth/1.5.0/googleauth-1.5.0.jar", true);
            case LOG4J:
                return PluginDependency.of("Log4j", "https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-core/2.14.1/log4j-core-2.14.1.jar", true);
            case LOG4J_WEB:
                return PluginDependency.of("Log4j Web", "https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-web/2.14.1/log4j-web-2.14.1.jar", true);
            case JAVASSIST:
                return PluginDependency.of("Java Assist", "https://repo1.maven.org/maven2/org/javassist/javassist/3.27.0-GA/javassist-3.27.0-GA.jar", true);
            case GUAVA:
                return PluginDependency.of("Google Guava", "https://repo1.maven.org/maven2/com/google/guava/guava/30.1.1-jre/guava-30.1.1-jre.jar", true);
            case GSON:
                return PluginDependency.of("Google Gson", "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.8.7/gson-2.8.7.jar", true);
            case MANAGER:
            default:
                String version = FileInfo.getJarVersion(new File(CurrentPlatform.getMain().getProtectionDomain().getCodeSource().getLocation().getPath().replaceAll("%20", " ")));

                return PluginDependency.of("LockLoginManager",
                        "https://karmaconfigs.github.io/updates/LockLogin/modules/manager/" + version + "/LockLoginManager.jar",
                        true,
                        true);
        }
    }
}
