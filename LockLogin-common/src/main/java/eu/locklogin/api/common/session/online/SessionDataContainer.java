package eu.locklogin.api.common.session.online;

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
import eu.locklogin.api.common.utils.other.LockedAccount;
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import eu.locklogin.api.premium.PremiumDatabase;
import eu.locklogin.api.util.enums.ManagerType;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.api.util.platform.ModuleServer;
import eu.locklogin.api.util.platform.Platform;
import ml.karmaconfigs.api.common.string.StringUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * LockLogin logged/registered data container
 */
public final class SessionDataContainer {

    private static int remoteLogged;
    private static int remoteRegistered;

    /**
     * Get the logged users amount
     *
     * @return the logged users amount
     */
    public static int getLogged() {
        if (!CurrentPlatform.getPlatform().equals(Platform.BUNGEE) &&
                CurrentPlatform.getConfiguration().isBungeeCord()) {
            return remoteLogged;
        }

        ModuleServer server = CurrentPlatform.getServer();

        int amount = 0;
        for (ModulePlayer player : server.getOnlinePlayers()) {
            if (player != null && player.isPlaying()) {
                ClientSession session = player.getSession();
                if (session == null) continue;

                if (session.isLogged() && session.isTempLogged()) {
                    amount++;
                }
            }
        }

        return amount;
    }

    /**
     * Set the logged users amount
     *
     * @param amount the logged users amount
     */
    public static void setLogged(final int amount) {
        remoteLogged = amount;
    }

    /**
     * Get registered users amount
     *
     * @return the registered users amount
     */
    public static int getRegistered() {
        if (!CurrentPlatform.getPlatform().equals(Platform.BUNGEE) &&
                CurrentPlatform.getConfiguration().isBungeeCord()) {
            return remoteRegistered;
        }

        AccountManager manager = CurrentPlatform.getAccountManager(ManagerType.CUSTOM, null);
        if (manager != null) {
            int amount = 0;

            PremiumDatabase pdb = CurrentPlatform.getPremiumDatabase();

            Set<UUID> processed = new HashSet<>();
            for (AccountManager m : manager.getAccounts()) {
                LockedAccount locked = new LockedAccount(m.getUUID());
                String name = m.getName();

                if (!StringUtils.isNullOrEmpty(name)) {
                    UUID offline = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes());

                    if (processed.add(offline) && !locked.isLocked() && (m.isRegistered() || pdb.isPremium(offline))) {
                        amount++;
                    }
                }
            }

            return amount;
        }

        return -1;
    }

    /**
     * Set the registered users amount
     *
     * @param amount the registered users amount
     */
    public static void setRegistered(final int amount) {
        remoteRegistered = amount;
    }
}
