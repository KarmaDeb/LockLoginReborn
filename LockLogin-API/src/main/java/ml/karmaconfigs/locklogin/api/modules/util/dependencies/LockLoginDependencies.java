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

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * LockLogin dependencies
 */
public enum LockLoginDependencies {
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
    REFLECTIONS,
    /**
     * LockLogin dependency
     */
    GUAVA;

    /**
     * Get the dependency as a dependency object
     *
     * @param onInject on dependency injection request action
     * @return the dependency as a dependency object
     */
    public Dependency getAsDependency(final @NotNull Consumer<Dependency> onInject) {
        switch (this) {
            case APACHE_COMMONS_CODEC:
                return new Dependency("Apache Commons Codec", "https://repo1.maven.org/maven2/commons-codec/commons-codec/1.15/commons-codec-1.15.jar") {
                    @Override
                    public void inject() {
                        onInject.accept(this);
                    }
                };
            case JNA:
                return new Dependency("Java Native Access", "https://repo1.maven.org/maven2/net/java/dev/jna/jna/5.8.0/jna-5.8.0.jar") {
                    @Override
                    public void inject() {
                        onInject.accept(this);
                    }
                };
            case GOOGLE_AUTHENTICATOR:
                return new Dependency("Google Authenticator", "https://repo1.maven.org/maven2/com/warrenstrange/googleauth/1.5.0/googleauth-1.5.0.jar") {
                    @Override
                    public void inject() {
                        onInject.accept(this);
                    }
                };
            case LOG4J:
                return new Dependency("Log4j", "https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-core/2.14.1/log4j-core-2.14.1.jar") {
                    @Override
                    public void inject() {
                        onInject.accept(this);
                    }
                };
            case LOG4J_WEB:
                return new Dependency("Log4j Web", "https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-web/2.14.1/log4j-web-2.14.1.jar") {
                    @Override
                    public void inject() {
                        onInject.accept(this);
                    }
                };
            case JAVASSIST:
                return new Dependency("Java Assist", "https://repo1.maven.org/maven2/org/javassist/javassist/3.27.0-GA/javassist-3.27.0-GA.jar") {
                    @Override
                    public void inject() {
                        onInject.accept(this);
                    }
                };
            case REFLECTIONS:
                return new Dependency("Reflections", "https://repo1.maven.org/maven2/org/reflections/reflections/0.9.12/reflections-0.9.12.jar") {
                    @Override
                    public void inject() {
                        onInject.accept(this);
                    }
                };
            case GUAVA:
                return new Dependency("Google Guava", "https://repo1.maven.org/maven2/com/google/guava/guava/30.1.1-jre/guava-30.1.1-jre.jar") {
                    @Override
                    public void inject() {
                        onInject.accept(this);
                    }
                };
            default:
                return null;
        }
    }
}
