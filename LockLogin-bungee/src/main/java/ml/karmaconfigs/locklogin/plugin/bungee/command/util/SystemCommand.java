package ml.karmaconfigs.locklogin.plugin.bungee.command.util;

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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface SystemCommand {

    String command() default "";

    /**
     * Get the plugin command manager
     */
    class manager {

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
