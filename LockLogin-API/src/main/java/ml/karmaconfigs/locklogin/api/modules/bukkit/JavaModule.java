package ml.karmaconfigs.locklogin.api.modules.bukkit;

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

import ml.karmaconfigs.api.bukkit.timer.AdvancedPluginTimer;
import ml.karmaconfigs.locklogin.api.modules.PluginModule;
import org.bukkit.Bukkit;

import java.lang.reflect.InvocationTargetException;

/**
 * LockLogin bukkit java module
 */
public abstract class JavaModule extends PluginModule {

    /**
     * Get a module instance based on the
     * main class
     *
     * @param clazz the main class
     * @return a java module instance
     */
    public static JavaModule getModuleInstance(final Class<? extends JavaModule> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Get the plugin scheduler
     *
     * @param period the timer period
     * @param repeat repeat the timer on end
     * @return the custom plugin scheduler
     */
    public final AdvancedPluginTimer getScheduler(final int period, final boolean repeat) {
        return new AdvancedPluginTimer(Bukkit.getServer().getPluginManager().getPlugin("LockLogin"), period, repeat);
    }

    /**
     * Get the plugin scheduler
     *
     * @param period the timer period
     * @return the custom plugin scheduler
     */
    public final AdvancedPluginTimer getScheduler(final int period) {
        return new AdvancedPluginTimer(Bukkit.getServer().getPluginManager().getPlugin("LockLogin"), period);
    }
}
