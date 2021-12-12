package eu.locklogin.module.manager;

import eu.locklogin.api.account.AccountID;
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.common.session.SessionDataContainer;
import eu.locklogin.api.module.plugin.api.event.ModuleEventHandler;
import eu.locklogin.api.module.plugin.api.event.user.*;
import eu.locklogin.api.module.plugin.api.event.util.EventListener;
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.utils.string.StringUtils;

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

        ModulePlayer player = e.getPlayer();
        AccountManager created = player.getAccount();

        String name = created.getName();

        if (StringUtils.isNullOrEmpty(name))
            created.setName(StringUtils.stripColor(player.getName()));

        created.saveUUID(AccountID.fromUUID(player.getUUID()));
    }

    @ModuleEventHandler(priority = ModuleEventHandler.Priority.LAST)
    public final void unHook(UserUnHookEvent e) {
        SessionDataContainer.setLogged(SessionDataContainer.getLogged() - 1);
        CurrentPlatform.disconnectPlayer(e.getPlayer());
    }

    @ModuleEventHandler(priority = ModuleEventHandler.Priority.LAST)
    public final void onQuit(UserQuitEvent e) {
        SessionDataContainer.setLogged(SessionDataContainer.getLogged() - 1);
        CurrentPlatform.disconnectPlayer(e.getPlayer());
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
