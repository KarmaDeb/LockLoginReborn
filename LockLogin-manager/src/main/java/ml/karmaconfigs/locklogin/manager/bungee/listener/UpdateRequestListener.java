package ml.karmaconfigs.locklogin.manager.bungee.listener;

import ml.karmaconfigs.locklogin.api.modules.api.event.ModuleEventHandler;
import ml.karmaconfigs.locklogin.api.modules.api.event.plugin.UpdateRequestEvent;
import ml.karmaconfigs.locklogin.api.modules.api.event.util.EventListener;
import ml.karmaconfigs.locklogin.manager.bungee.manager.BungeeManager;
import net.md_5.bungee.api.CommandSender;

public class UpdateRequestListener implements EventListener {

    @ModuleEventHandler
    public final void onRequest(final UpdateRequestEvent e) {
        CommandSender issuer = (CommandSender) e.getSender();
        BungeeManager.update(issuer, e.canPerformUnsafeUpdate());
    }
}
