package ml.karmaconfigs.locklogin.manager.bukkit;

import ml.karmaconfigs.locklogin.api.modules.bukkit.JavaModule;
import ml.karmaconfigs.locklogin.api.modules.util.javamodule.JavaModuleManager;
import ml.karmaconfigs.locklogin.manager.bukkit.command.ModuleHelpCommand;
import ml.karmaconfigs.locklogin.manager.bukkit.listener.UpdateRequestListener;

public final class Main extends JavaModule {

    public static JavaModuleManager manager;

    /**
     * On module enable logic
     */
    @Override
    public void onEnable() {
        manager = getManager();

        getManager().getConsoleSender().sendMessage("&aEnabling LockLogin manager module, to dynamically update LockLogin and enable helpme command");

        getManager().registerListener(new UpdateRequestListener());
        getManager().registerCommand(new ModuleHelpCommand());
    }

    /**
     * On module disable logic
     */
    @Override
    public void onDisable() {
        getManager().getConsoleSender().sendMessage("&cDisabling LockLogin manager module");
    }
}
