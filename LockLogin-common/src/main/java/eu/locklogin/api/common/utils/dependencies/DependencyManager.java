/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (eu.c) lucko (Luck) <luck@lucko.me>
 *  Copyright (eu.c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package eu.locklogin.api.common.utils.dependencies;

import eu.locklogin.api.common.JarManager;
import eu.locklogin.api.util.platform.CurrentPlatform;
import me.lucko.jarrelocator.JarRelocator;
import ml.karmaconfigs.api.common.data.path.PathUtilities;
import ml.karmaconfigs.api.common.karma.source.APISource;
import ml.karmaconfigs.api.common.karma.source.KarmaSource;
import ml.karmaconfigs.api.common.utils.enums.Level;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;

/**
 * Loads and manages runtime dependencies for the plugin.
 */
public class DependencyManager {

    private final static KarmaSource source = APISource.loadProvider("LockLogin");

    /**
     * A map of dependencies which have already been loaded.
     */
    private final static EnumMap<Dependency, Path> loaded = new EnumMap<>(Dependency.class);

    /**
     * Load all LockLogin dependencies
     */
    public static void loadDependencies() {
        for (Dependency dependency : Dependency.values()) {
            PluginDependency pd = dependency.getAsDependency();
            if (!pd.isHighPriority() && JarManager.hasBeenProcessed(pd)) {
                //Ignore modules, they must be loaded later...
                if (pd.isDependency()) {
                    try {
                        loadDependency(dependency);
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Load the dependency
     *
     * @param dependency the dependency
     */
    private static void loadDependency(final Dependency dependency) {
        if (loaded.containsKey(dependency)) {
            return;
        }

        Path file = dependencyFile(dependency.getAsDependency());

        loaded.put(dependency, file);
        source.console().send("Loading dependency {0}", Level.INFO, dependency.prettyName());
        CurrentPlatform.getPluginAppender().add(file);
    }

    private static Path dependencyFile(PluginDependency pluginDependency) {
        if (pluginDependency.relocations().isEmpty()) {
            return pluginDependency.getLocation().toPath();
        }

        File location = pluginDependency.getLocation();
        Path expected = source.getDataPath()
                .resolve("plugin")
                .resolve("libraries")
                .resolve("relocate")
                .resolve(location.getName());

        if (!Files.exists(expected)) {
            PathUtilities.create(expected);
            JarRelocator relocator = new JarRelocator(pluginDependency.getLocation(), expected.toFile(), pluginDependency.relocations());
            try {
                relocator.run();
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }

        return expected;
    }
}
