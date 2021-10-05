package eu.locklogin.module.manager;

import eu.locklogin.api.module.PluginModule;
import eu.locklogin.api.module.plugin.javamodule.ModuleLoader;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public interface LockLoginManager {

    @NotNull
    PluginModule module = Objects.requireNonNull(ModuleLoader.getByFile(Objects.requireNonNull(ModuleLoader.getModuleFile("LockLoginManager"))));
}
