package eu.locklogin.plugin.velocity;

import ml.karmaconfigs.api.common.karma.loader.JarAppender;
import ml.karmaconfigs.api.common.karma.loader.KarmaBootstrap;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

public final class VelocitySubJarAppender implements JarAppender {

    private final KarmaBootstrap plugin;

    public VelocitySubJarAppender(final KarmaBootstrap owner) {
        plugin = owner;
    }

    @Override
    public void addJarToClasspath(URL url) {
        File file = new File(url.getPath().replaceAll("%20", " "));
        Main.server.getPluginManager().addToClasspath(plugin, file.toPath());
    }

    @Override
    public void addJarToClasspath(URI uri) {
        try {
            File file = new File(uri.toURL().getPath().replaceAll("%20", " "));
            Main.server.getPluginManager().addToClasspath(plugin, file.toPath());
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void addJarToClasspath(File file) {
        Main.server.getPluginManager().addToClasspath(plugin, file.toPath());
    }

    @Override
    public void addJarToClasspath(Path path) {
        Main.server.getPluginManager().addToClasspath(plugin, path);
    }

    @Override
    public void close() {}

    @Override
    public URLClassLoader getLoader() {
        return null;
    }
}
