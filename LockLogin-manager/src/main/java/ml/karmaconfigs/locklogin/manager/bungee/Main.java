package ml.karmaconfigs.locklogin.manager.bungee;

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

import ml.karmaconfigs.locklogin.api.modules.bungee.JavaModule;
import ml.karmaconfigs.locklogin.api.modules.util.javamodule.JavaModuleManager;
import ml.karmaconfigs.locklogin.manager.bungee.command.ModuleHelpCommand;
import ml.karmaconfigs.locklogin.manager.bungee.listener.UpdateRequestListener;

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
