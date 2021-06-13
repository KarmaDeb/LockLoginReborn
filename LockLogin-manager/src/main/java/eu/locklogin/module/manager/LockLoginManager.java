package eu.locklogin.module.manager;

import eu.locklogin.api.module.PluginModule;
import eu.locklogin.api.module.plugin.javamodule.JavaModuleLoader;

public interface LockLoginManager {

    PluginModule module = JavaModuleLoader.getByName("LockLoginManager");
}
