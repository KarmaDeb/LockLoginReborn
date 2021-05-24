package ml.karmaconfigs.locklogin.manager.bungee;

import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.locklogin.api.modules.bungee.JavaModule;
import ml.karmaconfigs.locklogin.manager.bungee.command.ModuleHelpCommand;
import ml.karmaconfigs.locklogin.manager.bungee.listener.UpdateRequestListener;

public final class Main extends JavaModule {

    /**
     * On module enable logic
     */
    @Override
    public void onEnable() {
        Console.send("&aEnabling LockLogin manager module, to dynamically update LockLogin and enable helpme command");

        getManager().registerListener(new UpdateRequestListener());
        getManager().registerCommand(new ModuleHelpCommand());
    }

    /**
     * On module disable logic
     */
    @Override
    public void onDisable() {
        Console.send("&cDisabling LockLogin manager module");
    }
}
