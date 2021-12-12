package eu.locklogin.api.module.plugin.javamodule.sender;

import eu.locklogin.api.account.param.AccountConstructor;
import eu.locklogin.api.module.plugin.api.event.server.ServerSendMessageEvent;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.karma.APISource;

public abstract class ModuleSender extends AccountConstructor<ModuleSender> {

    /**
     * Make it non-buildable
     */
    ModuleSender() {
    }

    /**
     * Get the sender name
     *
     * @return the sender name
     */
    public String getName() {
        return CurrentPlatform.getConfiguration().serverName();
    }

    /**
     * Send a message to the player
     *
     * @param message the message to send
     */
    public void sendMessage(final String message, final Object... replaces) {
        ServerSendMessageEvent event = new ServerSendMessageEvent(message);
        ModulePlugin.callEvent(event);

        if (!event.isHandled()) {
            APISource.loadProvider("LockLogin").console().send(event.getMessage());
        }
    }
}
