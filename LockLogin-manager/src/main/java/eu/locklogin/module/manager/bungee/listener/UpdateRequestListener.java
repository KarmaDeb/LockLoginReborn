package eu.locklogin.module.manager.bungee.listener;

/*
 * GNU LESSER GENERAL PUBLIC LICENSE
 * Version 2.1, February 1999
 * <p>
 * Copyright (C) 1991, 1999 Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * Everyone is permitted to copy and distribute verbatim copies
 * of this license document, but changing it is not allowed.
 * <p>
 * [This is the first released version of the Lesser GPL.  It also counts
 * as the successor of the GNU Library Public License, version 2, hence
 * the version number 2.1.]
 */

import eu.locklogin.module.manager.bungee.manager.BungeeManager;
import eu.locklogin.api.module.plugin.api.event.ModuleEventHandler;
import eu.locklogin.api.module.plugin.api.event.plugin.UpdateRequestEvent;
import eu.locklogin.api.module.plugin.api.event.util.EventListener;
import net.md_5.bungee.api.CommandSender;

public class UpdateRequestListener implements EventListener {

    @ModuleEventHandler
    public final void onRequest(final UpdateRequestEvent e) {
        CommandSender issuer = (CommandSender) e.getSender();
        BungeeManager.update(issuer);
    }
}
