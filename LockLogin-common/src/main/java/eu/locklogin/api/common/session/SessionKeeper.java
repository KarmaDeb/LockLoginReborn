package eu.locklogin.api.common.session;

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

import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import eu.locklogin.api.util.platform.CurrentPlatform;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * LockLogin persistent session manager
 */
public final class SessionKeeper {

    private final static Map<UUID, InetAddress> sessions = new ConcurrentHashMap<>();

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
    public void store() {
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        if (config.enableSessions()) {
            AccountManager manager = player.getAccount();
            ClientSession session = player.getSession();

            PersistentSessionData persistent = new PersistentSessionData(manager.getUUID());
            if (persistent.isPersistent()) {
                if (session.isValid() && session.isCaptchaLogged() && session.isLogged() && session.isTempLogged()) {
                    sessions.put(player.getUUID(), player.getAddress());

                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        final UUID id = player.getUUID();
                        int back = (int) TimeUnit.MINUTES.toSeconds(config.sessionTime());

                        @Override
                        public void run() {
                            if (back == 0 || !sessions.containsKey(id)) {
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
    public boolean hasSession() {
        return sessions.containsKey(player.getUUID());
    }

    /**
     * Get if the player is the owner of the session
     *
     * @param address the session address
     * @return if the player is owner of the session
     */
    public boolean isOwner(final @NotNull InetAddress address) {
        InetAddress ip = sessions.getOrDefault(player.getUUID(), null);
        return ip != null && Arrays.equals(ip.getAddress(), address.getAddress());
    }

    /**
     * Destroy the session keeper for the player
     */
    public void destroy() {
        sessions.remove(player.getUUID());
    }
}
