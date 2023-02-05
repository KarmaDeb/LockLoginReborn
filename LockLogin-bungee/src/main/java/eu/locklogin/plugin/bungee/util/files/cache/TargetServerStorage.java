package eu.locklogin.plugin.bungee.util.files.cache;

import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.karma.file.element.types.Element;
import ml.karmaconfigs.api.common.karma.file.element.types.ElementPrimitive;

import java.util.UUID;

import static eu.locklogin.plugin.bungee.LockLogin.plugin;

public final class TargetServerStorage {

    private final static KarmaMain storageFile = new KarmaMain(plugin, "servers.kf", "cache");
    private final String name;

    public TargetServerStorage(final String n) {
        name = n;
    }

    public UUID load() {
        Element<?> element = storageFile.get(name);
        if (element.isPrimitive()) {
            ElementPrimitive primitive = element.getAsPrimitive();
            if (primitive.isString()) {
                return UUID.fromString(primitive.asString());
            }
        }

        return null;
    }

    public void save(final UUID id) {
        storageFile.setRaw(name, id.toString());
    }
}
