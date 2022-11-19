package eu.locklogin.plugin.bukkit.listener;

import eu.locklogin.api.account.ClientSession;
import eu.locklogin.plugin.bukkit.util.player.User;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

public final class InteractListenerNew implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void atEntityInteract(PlayerInteractAtEntityEvent e) {
        if (!e.isCancelled()) {
            Player player = e.getPlayer();
            User user = new User(player);
            ClientSession session = user.getSession();

            if (user.isLockLoginUser()) {
                if (session.isValid()) {
                    e.setCancelled(!session.isCaptchaLogged() || !session.isLogged() || !session.isTempLogged());
                } else {
                    e.setCancelled(true);
                }
            }
        }
    }
}
