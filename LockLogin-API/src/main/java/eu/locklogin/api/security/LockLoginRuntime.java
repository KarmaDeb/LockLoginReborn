package eu.locklogin.api.security;

import eu.locklogin.api.module.PluginModule;
import eu.locklogin.api.module.plugin.javamodule.ModuleLoader;
import ml.karmaconfigs.api.common.data.path.PathUtilities;
import ml.karmaconfigs.api.common.karma.source.APISource;
import ml.karmaconfigs.api.common.string.StringUtils;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * LockLogin runtime environment
 */
public final class LockLoginRuntime {

    /**
     * Get the caller of a method
     *
     * @return the path to the jar that loaded the
     * class
     */
    public static PluginModule getMethodCaller() {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : elements) {
            String name = element.getClassName();
            try {
                Class<?> clazz = Class.forName(name);
                URL url = clazz.getResource('/' + name.replace('.', '/') + ".class");
                if (url != null) {
                    String urlPath = url.getPath();
                    if (urlPath.startsWith("file:") && urlPath.contains("!")) {
                        String jarPath = urlPath.substring("file:".length(), urlPath.indexOf('!'));
                        File file = new File(jarPath.replaceAll("%20", " "));

                        PluginModule module = ModuleLoader.getByFile(file);
                        if (module != null) return module;
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        return null;
    }

    /**
     * Verify that the current method is being called safely
     *
     * @param pluginOnly should be the plugin the only one that should
     *                   have access to this method?
     */
    public static void checkSecurity(final boolean pluginOnly) throws SecurityException {
        String path = LockLoginRuntime.class.getProtectionDomain().getCodeSource().getLocation().getFile().replaceAll("%20", " ");
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        Path pluginsFolder = APISource.loadProvider("LockLogin").getDataPath().getParent();
        String loader = APISource.class.getProtectionDomain().getCodeSource().getLocation().getFile().replaceAll("%20", " ");
        if (loader.startsWith("/")) {
            loader = loader.substring(1);
        }

        String server = LockLoginRuntime.class.getClassLoader().getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
        if (server.startsWith("/")) {
            server = server.substring(1);
        }

        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        String method = "";
        for (StackTraceElement element : elements) {
            String name = element.getClassName();
            try {
                Class<?> clazz = Class.forName(name);
                URL url = clazz.getResource('/' + name.replace('.', '/') + ".class");
                if (url != null) {
                    String urlPath = url.getPath();
                    if (urlPath.startsWith("file:") && urlPath.contains("!")) {
                        String jarPath = urlPath.substring((urlPath.startsWith("file:/") ? 6 : 5), urlPath.indexOf('!')).replaceAll("%20", " ");
                        if (jarPath.equals(path) && StringUtils.isNullOrEmpty(method)) {
                            method = element.getClassName() + "#" + element.getMethodName();
                        }

                        if (!jarPath.equals(path) && !jarPath.equals(loader) && !jarPath.equals(server)) {
                            if (jarPath.startsWith(pluginsFolder.toString())) {
                                Path caller = Paths.get(jarPath);

                                PluginModule mod = ModuleLoader.getByFile(caller.toFile());
                                String pathName = PathUtilities.getPrettyPath(caller);
                                if (mod != null) {
                                    pathName = "(Module) " + mod.name();
                                    if (!pluginOnly) {
                                        break;
                                    }
                                }

                                throw new SecurityException("Cannot access API method " + method + " from an unsafe source. " + pathName);
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }

    public static Path getCaller() {
        PluginModule module = getMethodCaller();
        if (module != null) return module.getModule().toPath();

        String path = LockLoginRuntime.class.getProtectionDomain().getCodeSource().getLocation().getFile().replaceAll("%20", " ");
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        Path pluginsFolder = APISource.loadProvider("LockLogin").getDataPath().getParent();
        String loader = APISource.class.getProtectionDomain().getCodeSource().getLocation().getFile().replaceAll("%20", " ");
        if (loader.startsWith("/")) {
            loader = loader.substring(1);
        }

        String server = LockLoginRuntime.class.getClassLoader().getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
        if (server.startsWith("/")) {
            server = server.substring(1);
        }

        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : elements) {
            String name = element.getClassName();
            try {
                Class<?> clazz = Class.forName(name);
                URL url = clazz.getResource('/' + name.replace('.', '/') + ".class");
                if (url != null) {
                    String urlPath = url.getPath();
                    if (urlPath.startsWith("file:") && urlPath.contains("!")) {
                        String jarPath = urlPath.substring((urlPath.startsWith("file:/") ? 6 : 5), urlPath.indexOf('!')).replaceAll("%20", " ");

                        if (!jarPath.equals(path) && !loader.equals(jarPath) && !jarPath.equals(server)) {
                            if (jarPath.startsWith(pluginsFolder.toString())) {
                                return Paths.get(jarPath);
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        return Paths.get(path);
    }
}
