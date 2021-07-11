package eu.locklogin.module.manager;

import eu.locklogin.api.module.PluginModule;
import eu.locklogin.api.module.plugin.javamodule.ModuleLoader;

public interface LockLoginManager {

    PluginModule module = ModuleLoader.getByName("LockLoginManager");
}
