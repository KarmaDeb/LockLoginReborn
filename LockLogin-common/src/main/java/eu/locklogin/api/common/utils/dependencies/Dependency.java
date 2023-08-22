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
import org.jetbrains.annotations.NotNull;

/**
 * LockLogin dependencies
 */
public enum Dependency {
    /**
     * Jar relocator
     */
    //JAR_RELOCATOR,
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
    GUAVA,
    /**
     * LockLogin 1.7.10 dependency
     */
    APACHE_COMMONS,
    /**
     * LockLogin web services dependency
     */
    //SOCKET_IO,
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
            /*case JAR_RELOCATOR:
                return PluginDependency.of(
                        prettyName(),
                        FileInfo.repositoryHost(null, "JarRelocator.jar"),
                        true, false, true);*/
            case GOOGLE_AUTHENTICATOR:
                return PluginDependency.of(
                        prettyName(),
                        FileInfo.repositoryHost(null, "GoogleAuthenticator.jar"), true)
                        .relocate("eu.locklogin.api.shaded.googleauth", "com", "warrenstrange", "googleauth");
            case LOG4J:
                return PluginDependency.of(
                        prettyName(),
                        FileInfo.repositoryHost(null, "Log4j.jar"), true)
                        .relocate("eu.locklogin.api.shaded.log4j.core", "org", "apache", "logging", "log4j", "core");
            case LOG4J_WEB:
                return PluginDependency.of(
                        prettyName(),
                        FileInfo.repositoryHost(null, "Log4jWeb.jar"), true)
                        .relocate("eu.locklogin.api.shaded.log4j.web", "org", "apache", "logging", "log4j", "web");
            case GUAVA:
                return PluginDependency.of(
                        prettyName(),
                        FileInfo.repositoryHost(null, "Guava.jar"), true)
                        .relocate("eu.locklogin.api.shaded.guava.thirdparty", "com", "google", "thirdparty", "publicsuffix")
                        .relocate("eu.locklogin.api.shaded.guava.common", "com", "google", "common");
            case APACHE_COMMONS:
                return PluginDependency.of(
                        prettyName(),
                        FileInfo.repositoryHost(null, "ApacheCommons.jar"), true);
            /*case SOCKET_IO:
                return PluginDependency.of(
                                prettyName(),
                                FileInfo.repositoryHost(null, "SocketIO.jar"), true)
                        .relocate("eu.locklogin.plugin.shaded.io.socket", "io", "socket")
                        .relocate("eu.locklogin.plugin.shaded.org.json", "org", "json")
                        .relocate("eu.locklogin.plugin.shaded.org.codehaus.mojo", "org", "codehaus", "mojo", "animal_sniffer");*/
            /*case SOCKET_IO:
                return PluginDependency.of(prettyName(), FileInfo.repositoryHost(null, "SocketIO.jar"), true);
            */case MANAGER:
            default:
                return PluginDependency.of(prettyName(), FileInfo.managerHost(null), true, true);
        }
    }

    public final String prettyName() {
        switch (this) {
            /*case JAR_RELOCATOR:
                return "Jar Relocator";*/
            case GOOGLE_AUTHENTICATOR:
                return "Google Authenticator";
            case LOG4J:
                return "Log4j";
            case LOG4J_WEB:
                return "Log4j Web";
            case GUAVA:
                return "Google Guava";
            case APACHE_COMMONS:
                return "Apache Commons";
            /*case SOCKET_IO:
                return "SocketIO";*/
            /*case SOCKET_IO:
                return "Socket IO";
            */case MANAGER:
            default:
                return "LockLoginManager";
        }
    }
}
