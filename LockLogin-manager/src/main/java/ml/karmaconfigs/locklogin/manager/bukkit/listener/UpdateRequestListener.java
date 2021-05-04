package ml.karmaconfigs.locklogin.manager.bukkit.listener;

import ml.karmaconfigs.locklogin.api.modules.event.ModuleEventHandler;
import ml.karmaconfigs.locklogin.api.modules.event.plugin.UpdateRequestEvent;
import ml.karmaconfigs.locklogin.api.modules.event.util.EventListener;
import ml.karmaconfigs.locklogin.manager.bukkit.BukkitManager;
import org.bukkit.command.CommandSender;

public class UpdateRequestListener implements EventListener {

    @ModuleEventHandler
    public final void onRequest(final UpdateRequestEvent e) {
        CommandSender issuer = (CommandSender) e.getSender();
        BukkitManager.update(issuer, e.canPerformUnsafeUpdate());
    }
}
