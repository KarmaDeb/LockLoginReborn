package ml.karmaconfigs.locklogin.api.modules.bungee;

import ml.karmaconfigs.api.bungee.timer.AdvancedPluginTimer;
import ml.karmaconfigs.locklogin.api.modules.PluginModule;
import net.md_5.bungee.api.ProxyServer;

import java.lang.reflect.InvocationTargetException;

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
        return new AdvancedPluginTimer(ProxyServer.getInstance().getPluginManager().getPlugin("LockLogin"), period, repeat);
    }

    /**
     * Get the plugin scheduler
     *
     * @param period the timer period
     * @return the custom plugin scheduler
     */
    public final AdvancedPluginTimer getScheduler(final int period) {
        return new AdvancedPluginTimer(ProxyServer.getInstance().getPluginManager().getPlugin("LockLogin"), period);
    }
}
