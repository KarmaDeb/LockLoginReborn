package eu.locklogin.module.manager;

import eu.locklogin.api.common.session.SessionDataContainer;
import eu.locklogin.api.module.plugin.api.event.ModuleEventHandler;
import eu.locklogin.api.module.plugin.api.event.user.*;
import eu.locklogin.api.module.plugin.api.event.util.EventListener;
import eu.locklogin.api.module.plugin.client.ModulePlayer;
import eu.locklogin.api.util.platform.CurrentPlatform;

public class UsersListener implements EventListener {

    @ModuleEventHandler(priority = ModuleEventHandler.Priority.FIRST)
    public final void onLogin(UserAuthenticateEvent e) {
        if (e.getAuthResult() == UserAuthenticateEvent.Result.SUCCESS) {
            SessionDataContainer.setLogged(SessionDataContainer.getLogged() + 1);
        }
    }

    @ModuleEventHandler(priority = ModuleEventHandler.Priority.FIRST)
    public final void onRegister(AccountCreatedEvent e) {
        SessionDataContainer.setRegistered(SessionDataContainer.getRegistered() + 1);
    }

    @ModuleEventHandler(priority = ModuleEventHandler.Priority.FIRST)
    public final void unHook(UserUnHookEvent e) {
        SessionDataContainer.setLogged(SessionDataContainer.getLogged() - 1);
        CurrentPlatform.disconnectPlayer(e.getPlayer().getUUID());
    }

    @ModuleEventHandler(priority = ModuleEventHandler.Priority.FIRST)
    public final void onQuit(UserQuitEvent e) {
        SessionDataContainer.setLogged(SessionDataContainer.getLogged() - 1);
        CurrentPlatform.disconnectPlayer(e.getPlayer().getUUID());
    }

    @ModuleEventHandler(priority = ModuleEventHandler.Priority.FIRST)
    public final void onUnRegister(AccountRemovedEvent e) {
        SessionDataContainer.setRegistered(SessionDataContainer.getRegistered() - 1);
    }

    @ModuleEventHandler(priority = ModuleEventHandler.Priority.FIRST)
    public final void onHook(UserHookEvent e) {
        ModulePlayer player = e.getPlayer();
        if (player.getSession().isLogged()) {
            SessionDataContainer.setLogged(SessionDataContainer.getLogged() + 1);
        }
    }
}
