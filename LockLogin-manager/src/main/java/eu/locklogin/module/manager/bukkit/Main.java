package eu.locklogin.module.manager.bukkit;

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

import eu.locklogin.api.module.PluginModule;
import eu.locklogin.module.manager.LockLoginManager;
import eu.locklogin.module.manager.ModuleHelpCommand;
import eu.locklogin.module.manager.ModuleInfoCommand;
import eu.locklogin.module.manager.UsersListener;
import eu.locklogin.module.manager.bukkit.listener.UpdateRequestListener;
import ml.karmaconfigs.api.common.karmafile.karmayaml.FileCopy;

public final class Main extends PluginModule {

    @Override
    public void enable() {
        getConsole().sendMessage("&aEnabling LockLogin manager module, to dynamically update LockLogin and enable helpme command");

        try {
            FileCopy copy = new FileCopy(LockLoginManager.module, "config.yml");
            copy.copy(LockLoginManager.module.getFile("config.yml"));
        } catch (Throwable ex) {
            ex.printStackTrace();
        }

        getPlugin().registerListener(new UpdateRequestListener());
        getPlugin().registerListener(new UsersListener());
        getPlugin().registerCommand(new ModuleHelpCommand());
        getPlugin().registerCommand(new ModuleInfoCommand());
    }

    @Override
    public void disable() {
        getConsole().sendMessage("&cDisabling LockLogin manager module");
    }
}
