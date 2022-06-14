package eu.locklogin.api.module;

public class InvalidKeyError extends Error {

    public InvalidKeyError(final PluginModule module) {
        super("Provided key does not match " + module.name() + " key");
    }
}
