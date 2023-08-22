package eu.locklogin.plugin.bukkit.plugin.bungee.que;

import com.google.gson.JsonObject;

public class QueueData {

    private final JsonObject object;
    private final long since = System.currentTimeMillis();

    public QueueData(final JsonObject object) {
        this.object = object;
    }

    public JsonObject getObject() {
        return object;
    }

    public boolean verify() {
        long now = System.currentTimeMillis();
        return now - since <= 5000;
    }
}
