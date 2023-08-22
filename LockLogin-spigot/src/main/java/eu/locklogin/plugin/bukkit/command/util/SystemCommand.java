package eu.locklogin.plugin.bukkit.command.util;

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

import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.command.*;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.utils.enums.Level;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static eu.locklogin.plugin.bukkit.LockLogin.plugin;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface SystemCommand {

    String command() default "";
    String bungee_command() default "";

    String[] aliases() default {""};

    boolean bungeecord() default false;

    /**
     * Get the plugin command manager
     */
    class manager {

        /**
         * Get a list of recognized system account commands
         * @return an array of system commands
         */
        public static Class<?>[] recognizedClasses() {
            List<Class<?>> classes = new ArrayList<>(Arrays.asList(
                    AccountCommand.class,
                    AliasCommand.class,
                    GoogleAuthCommand.class,
                    LastLocationCommand.class,
                    LockLoginCommand.class,
                    LoginCommand.class,
                    PanicCommand.class,
                    PinCommand.class,
                    PlayerInfoCommand.class,
                    RegisterCommand.class,
                    SetSpawnCommand.class
            ));

            if (CurrentPlatform.getConfiguration().enablePremium() && CurrentPlatform.getConfiguration().enablePremium()) {
                classes.add(PremiumCommand.class);
                plugin.console().send("Registered premium command as a premium database has been registered", Level.INFO);
            }

            return classes.toArray(new Class[0]);
        }

        private static Class<?> toClass(final String pack, final String clazz) {
            try {
                return Class.forName(pack + "." + clazz.substring(0, clazz.lastIndexOf('.')));
            } catch (Throwable ignored) {}

            return null;
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
                    String command = cmd.command();

                    if (cmd.bungeecord() && CurrentPlatform.getConfiguration().isBungeeCord()) {
                        String bungee_cmd = cmd.bungee_command();
                        if (!StringUtils.isNullOrEmpty(bungee_cmd))
                            command = bungee_cmd;
                    }

                    return command;
                } catch (Throwable ignored) {
                }
            }

            return "";
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
         * Get the declared bungee status of the
         * class
         *
         * @param clazz the command class
         * @return the bungee status of the class
         */
        public static boolean getBungeeStatus(final Class<?> clazz) {
            if (clazz.isAnnotationPresent(SystemCommand.class)) {
                SystemCommand cmd = clazz.getAnnotation(SystemCommand.class);

                try {
                    return cmd.bungeecord();
                } catch (Throwable ignored) {
                }
            }

            return false;
        }
    }
}
