package eu.locklogin.plugin.bungee.util.files.cache;

import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.karma.file.element.KarmaElement;
import ml.karmaconfigs.api.common.karma.file.element.KarmaObject;

import java.util.UUID;

import static eu.locklogin.plugin.bungee.LockLogin.plugin;

public final class TargetServerStorage {

    private final static KarmaMain storageFile = new KarmaMain(plugin, "servers.kf", "cache");
    private final String name;

    public TargetServerStorage(final String n) {
        name = n;
    }

    public UUID load() {
        KarmaElement element = storageFile.get(name);
        if (element != null && element.isString()) {
            return UUID.fromString(element.getObjet().getString());
        }

        return null;
    }

    public void save(final UUID id) {
        storageFile.set(name, new KarmaObject(id.toString()));
    }
}
