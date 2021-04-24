package ml.karmaconfigs.locklogin.plugin.common.dependencies;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

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
    LOG4J_WEB;

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
            default:
                return null;
        }
    }
}