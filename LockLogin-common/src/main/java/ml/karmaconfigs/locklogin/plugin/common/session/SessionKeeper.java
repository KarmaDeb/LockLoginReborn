package ml.karmaconfigs.locklogin.plugin.common.session;

import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.files.PluginConfiguration;
import ml.karmaconfigs.locklogin.api.modules.util.client.ModulePlayer;
import ml.karmaconfigs.locklogin.api.utils.platform.CurrentPlatform;

import java.util.*;
import java.util.concurrent.TimeUnit;

public final class SessionKeeper {

    private final static Set<UUID> sessions = new HashSet<>();

    private final ModulePlayer player;

    /**
     * Initialize the session keeper
     *
     * @param owner the session owner
     */
    public SessionKeeper(final ModulePlayer owner) {
        player = owner;
    }

    /**
     * Store the client session
     */
    public final void store() {
        AccountManager manager = player.getAccount();
        ClientSession session = player.getSession();

        PersistentSessionData persistent = new PersistentSessionData(manager.getUUID());

        if (session.isValid() && session.isCaptchaLogged() && session.isLogged() && session.isTempLogged()) {
            if (persistent.isPersistent()) {
                PluginConfiguration config = CurrentPlatform.getConfiguration();

                if (config.enableSessions()) {
                    sessions.add(player.getUUID());

                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        int back = (int) TimeUnit.MINUTES.toSeconds(config.sessionTime());
                        final UUID id = player.getUUID();

                        @Override
                        public void run() {
                            if (back == 0 || !sessions.contains(id)) {
                                sessions.remove(id);
                                timer.cancel();
                            }

                            back--;
                        }
                    }, 0, 1000);
                }
            }
        }
    }

    /**
     * Get if the player has an old session
     *
     * @return if the player has old session
     */
    public final boolean hasSession() {
        return sessions.contains(player.getUUID());
    }

    /**
     * Destroy the session keeper for the player
     */
    public final void destroy() {
        sessions.remove(player.getUUID());
    }
}
