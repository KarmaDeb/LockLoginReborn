package ml.karmaconfigs.locklogin.manager.bukkit.listener;

import ml.karmaconfigs.locklogin.api.ModuleEventHandler;
import ml.karmaconfigs.locklogin.api.event.plugin.UpdateRequestEvent;
import ml.karmaconfigs.locklogin.api.event.util.EventListener;
import ml.karmaconfigs.locklogin.manager.bukkit.BukkitManager;
import org.bukkit.command.CommandSender;

public class UpdateRequestListener implements EventListener {

    @ModuleEventHandler
    public final void onRequest(final UpdateRequestEvent e) {
        CommandSender issuer = (CommandSender) e.getSender();
        BukkitManager.update(issuer, e.canPerformUnsafeUpdate());
    }
}
