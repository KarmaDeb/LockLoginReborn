package eu.locklogin.api.module.plugin.javamodule.server;

import com.google.gson.JsonObject;

public interface ModuleMessageListener {

    /**
     * When a message has been received
     *
     * @param object the message
     */
    void onMessageReceived(final JsonObject object);
}
