package eu.locklogin.module.manager.bungee;

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
import eu.locklogin.module.manager.UsersListener;
import eu.locklogin.module.manager.bungee.command.ModuleHelpCommand;
import eu.locklogin.module.manager.bungee.listener.UpdateRequestListener;

public final class Main extends PluginModule {

    @Override
    public void enable() {
        getConsole().sendMessage("&aEnabling LockLogin manager module, to dynamically update LockLogin and enable helpme command");

        getManager().registerListener(new UpdateRequestListener());
        getManager().registerListener(new UsersListener());
        getManager().registerCommand(new ModuleHelpCommand());
    }

    @Override
    public void disable() {
        getConsole().sendMessage("&cDisabling LockLogin manager module");
    }
}
