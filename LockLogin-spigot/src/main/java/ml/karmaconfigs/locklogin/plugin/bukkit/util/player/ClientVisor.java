package ml.karmaconfigs.locklogin.plugin.bukkit.util.player;

import ml.karmaconfigs.locklogin.api.account.ClientSession;
import org.bukkit.entity.Player;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.plugin;

public final class ClientVisor {

    private final Player player;

    /**
     * Initialize the client visor
     *
     * @param _player the player to hide/show players
     */
    public ClientVisor(final Player _player) {
        player = _player;
    }

    /**
     * Hide the player to/from everyone
     */
    @SuppressWarnings("deprecation")
    public final void hide() {
        try {
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                if (!online.getUniqueId().equals(player.getUniqueId())) {
                    online.hidePlayer(plugin, player);
                    player.hidePlayer(plugin, online);
                }
            }
        } catch (Throwable ex) {
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                if (!online.getUniqueId().equals(player.getUniqueId())) {
                    online.hidePlayer(player);
                    player.hidePlayer(online);
                }
            }
        }
    }

    /**
     * Re-show the player to authenticated
     * players
     */
    @SuppressWarnings("deprecation")
    public final void authenticate() {
        try {
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                User user = new User(online);
                ClientSession session = user.getSession();
                if (session.isCaptchaLogged() && session.isLogged() && session.isTempLogged()) {
                    online.showPlayer(plugin, player);
                    player.showPlayer(plugin, online);
                }
            }
        } catch (Throwable ex) {
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                User user = new User(online);
                ClientSession session = user.getSession();
                if (session.isCaptchaLogged() && session.isLogged() && session.isTempLogged()) {
                    online.showPlayer(player);
                    player.showPlayer(online);
                }
            }
        }
    }
}
