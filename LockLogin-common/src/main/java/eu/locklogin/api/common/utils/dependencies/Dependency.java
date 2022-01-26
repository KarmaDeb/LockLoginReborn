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
import ml.karmaconfigs.api.common.utils.url.URLUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.URL;

/**
 * LockLogin dependencies
 */
public enum Dependency {
    /**
     * LockLogin dependency
     */
    //COMMONS_IO,
    /**
     * LockLogin dependency
     */
    //APACHE_COMMONS_CODEC,
    /**
     * LockLogin dependency
     */
    //JNA,
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
    //JAVASSIST,
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
            /*case COMMONS_IO:
                return PluginDependency.of(prettyName(), "https://repo1.maven.org/maven2/commons-io/commons-io/20030203.000550/commons-io-20030203.000550.jar", true);
            case APACHE_COMMONS_CODEC:
                return PluginDependency.of(prettyName(), "https://repo1.maven.org/maven2/commons-codec/commons-codec/1.15/commons-codec-1.15.jar", true);
            case JNA:
                return PluginDependency.of(prettyName(), "https://repo1.maven.org/maven2/net/java/dev/jna/jna/5.8.0/jna-5.8.0.jar", true);*/
            case GOOGLE_AUTHENTICATOR:
                return PluginDependency.of(prettyName(), "https://repo1.maven.org/maven2/com/warrenstrange/googleauth/1.5.0/googleauth-1.5.0.jar", true);
            case LOG4J:
                return PluginDependency.of(prettyName(), "https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-core/2.17.1/log4j-core-2.17.1.jar", true);
            case LOG4J_WEB:
                return PluginDependency.of(prettyName(), "https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-web/2.17.1/log4j-web-2.17.1.jar", true);
            /*case JAVASSIST:
                return PluginDependency.of(prettyName(), "https://repo1.maven.org/maven2/org/javassist/javassist/3.28.0-GA/javassist-3.28.0-GA.jar", true);*/
            case GUAVA:
                return PluginDependency.of(prettyName(), "https://repo1.maven.org/maven2/com/google/guava/guava/30.1.1-jre/guava-30.1.1-jre.jar", true);
            case GSON:
                return PluginDependency.of(prettyName(), "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.8.7/gson-2.8.7.jar", false);
            case MANAGER:
            default:
                String version = FileInfo.getManagerVersion(new File(CurrentPlatform.getMain().getProtectionDomain().getCodeSource().getLocation().getPath().replaceAll("%20", " ")));

                URL url = URLUtils.getOrBackup(
                        "https://karmarepo.000webhostapp.com/locklogin/modules/manager/" + version + "/LockLoginManager.jar",
                        "https://karmaconfigs.github.io/updates/LockLogin/modules/manager/" + version + "/LockLoginManager.jar");

                if (url != null) {
                    return PluginDependency.of(prettyName(), url.toString(), true, true);
                } else {
                    //I just don't know what to do in that case
                    return PluginDependency.of(prettyName(), "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.8.7/gson-2.8.7.jar", false);
                }
        }
    }

    public final String prettyName() {
        switch (this) {
            /*case COMMONS_IO:
                return "Apache Commons IO";
            case APACHE_COMMONS_CODEC:
                return "Apache Commons Codec";
            case JNA:
                return "Java Native Access";*/
            case GOOGLE_AUTHENTICATOR:
                return "Google Authenticator";
            case LOG4J:
                return "Log4j";
            case LOG4J_WEB:
                return "Log4j Web";
            /*case JAVASSIST:
                return "Java Assist";*/
            case GUAVA:
                return "Google Guava";
            case GSON:
                return "Google Gson";
            case MANAGER:
            default:
                return "LockLoginManager";
        }
    }
}
