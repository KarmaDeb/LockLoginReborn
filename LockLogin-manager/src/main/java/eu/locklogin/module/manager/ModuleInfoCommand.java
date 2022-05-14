package eu.locklogin.module.manager;

import eu.locklogin.api.common.session.SessionDataContainer;
import eu.locklogin.api.common.web.alert.Notification;
import eu.locklogin.api.common.web.alert.RemoteNotification;
import eu.locklogin.api.module.plugin.api.command.Command;
import eu.locklogin.api.module.plugin.javamodule.sender.ModuleConsole;
import eu.locklogin.api.module.plugin.javamodule.sender.ModuleSender;
import eu.locklogin.api.util.platform.CurrentPlatform;

public final class ModuleInfoCommand extends Command {

    /**
     * Initialize the module info command
     */
    public ModuleInfoCommand() {
        super("Shows information about the plugin", "info");
    }

    /**
     * Process the command when
     * its fired
     *
     * @param arg        the used argument
     * @param sender     the command sender
     * @param parameters the command parameters
     */
    @Override
    public void processCommand(final String arg, final ModuleSender sender, final String... parameters) {
        if (sender instanceof ModuleConsole) {
            sender.sendMessage("&d------------------------------");
            sender.sendMessage("");
            sender.sendMessage("&7LockLogin plugin information");
            sender.sendMessage("");
            sender.sendMessage("&bServer hash: &c" + CurrentPlatform.getServerHash());
            sender.sendMessage("&bPanel status: &cNot terminated (https://panel.karmaconfigs.ml)");
            sender.sendMessage("&bRegistered users: &c" + SessionDataContainer.getRegistered());
            sender.sendMessage("&bLogged users: &c" + SessionDataContainer.getLogged());
            RemoteNotification rm = new RemoteNotification();
            if (parameters.length == 1) {
                String param = parameters[0];
                if (param.equalsIgnoreCase("--force-alert")) {
                    rm.checkAlerts();
                }
            }
            Notification notification = rm.getNotification();

            sender.sendMessage("&bLast notification level: &c" + notification.getLevel());
            sender.sendMessage("&bLast notification: &c" + notification.getNotification());
            sender.sendMessage("&bForce configuration: &c" + notification.forceConfig());
            sender.sendMessage("&bForce proxy configuration: &c" + notification.forceProxy());
            sender.sendMessage("");
            sender.sendMessage("&d------------------------------");
        } else {
            sender.sendMessage(CurrentPlatform.getMessages().prefix() + "&cThis command can be only run from console!");
        }
    }
}

