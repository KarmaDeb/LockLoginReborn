/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
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

import eu.locklogin.api.common.utils.dependencies.Dependency;
import eu.locklogin.api.common.utils.dependencies.PluginDependency;
import ml.karmaconfigs.api.common.karma.loader.KarmaBootstrap;

import java.nio.file.Path;
import java.util.EnumMap;

/**
 * Loads and manages runtime dependencies for the plugin.
 */
public class DependencyManager {

    /** The plugin instance */
    private final KarmaBootstrap plugin;

    /** A map of dependencies which have already been loaded. */
    private final EnumMap<Dependency, Path> loaded = new EnumMap<>(Dependency.class);

    /**
     * LockLogin dependency manager
     *
     * @param plugin the plugin
     */
    public DependencyManager(KarmaBootstrap plugin) {
        this.plugin = plugin;
    }

    /**
     * Load all LockLogin dependencies
     */
    public void loadDependencies() {
        for (Dependency dependency : Dependency.values()) {
            PluginDependency pd = dependency.getAsDependency();

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

    /**
     * Load the dependency
     *
     * @param dependency the dependency
     */
    private void loadDependency(Dependency dependency) {
        if (loaded.containsKey(dependency)) {
            return;
        }

        Path file = dependencyFile(dependency.getAsDependency());

        loaded.put(dependency, file);
        plugin.getAppender().addJarToClasspath(file);
    }

    private Path dependencyFile(PluginDependency pluginDependency) {
        return pluginDependency.getLocation().toPath();
    }
}
