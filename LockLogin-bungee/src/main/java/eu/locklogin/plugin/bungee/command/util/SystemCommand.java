package eu.locklogin.plugin.bungee.command.util;

/*
 * Private GSA code
 *
 * The use of this code
 * without GSA team authorization
 * will be a violation of
 * terms of use determined
 * in <a href="http://karmaconfigs.cf/license/"> here </a>
 * or (fallback domain) <a href="https://karmaconfigs.github.io/page/license"> here </a>
 */

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface SystemCommand {

    String command() default "";

    String[] aliases() default {""};

    /**
     * Get the plugin command manager
     */
    class manager {

        private final static Package pack = SystemCommand.class.getPackage();

        /**
         * Get a list of recognized system account commands
         * @return an array of system commands
         */
        public static Class<?>[] recognizedClasses() {
            String name = pack.getName().substring(0, pack.getName().lastIndexOf('.'));

            InputStream stream = ClassLoader.getSystemClassLoader()
                    .getResourceAsStream(name.replaceAll("[.]", "/"));
            if (stream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                return reader.lines().filter(line -> line.endsWith(".class"))
                        .map(line -> toClass(name, line)).distinct().toArray(Class<?>[]::new);
            }

            return new Class<?>[0];
        }

        private static Class<?> toClass(final String pack, final String clazz) {
            try {
                return Class.forName(pack + "." + clazz.substring(0, clazz.lastIndexOf('.')));
            } catch (Throwable ignored) {}

            return null;
        }

        /**
         * Get the declared aliases of the class
         *
         * @param clazz the command class
         * @return the aliases of the class
         */
        public static List<String> getDeclaredAliases(final Class<?> clazz) {
            if (clazz.isAnnotationPresent(SystemCommand.class)) {
                SystemCommand cmd = clazz.getAnnotation(SystemCommand.class);

                try {
                    return Arrays.asList(cmd.aliases());
                } catch (Throwable ignored) {
                }
            }

            return Collections.emptyList();
        }

        /**
         * Get the declared command of the
         * class
         *
         * @param clazz the command class
         * @return the command of the class
         */
        public static String getDeclaredCommand(final Class<?> clazz) {
            if (clazz.isAnnotationPresent(SystemCommand.class)) {
                SystemCommand cmd = clazz.getAnnotation(SystemCommand.class);

                try {
                    return cmd.command();
                } catch (Throwable ignored) {
                }
            }

            return "";
        }
    }
}
