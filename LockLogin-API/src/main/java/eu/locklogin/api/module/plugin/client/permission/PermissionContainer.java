package eu.locklogin.api.module.plugin.client.permission;

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

import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;

/**
 * ModulePlayer permission check container, this
 * contains the module player who is being permission
 * checked, the permission and the result
 */
public final class PermissionContainer {

    private final ModulePlayer player;
    private final PermissionObject permission;
    private boolean result;

    /**
     * Initialize the permission container
     *
     * @param attachment the player who is being permission-checked
     * @param perm the permission
     */
    public PermissionContainer(final ModulePlayer attachment, final PermissionObject perm) {
        player = attachment;
        permission = perm;
    }

    /**
     * Get the player that is being permission-checked
     *
     * @return the player
     */
    public ModulePlayer getAttachment() {
        return player;
    }

    /**
     * Get the permission that is being checked
     *
     * @return the permission
     */
    public PermissionObject getPermission() {
        return permission;
    }

    /**
     * If the player has the permission or not
     *
     * @return the result
     */
    public boolean getResult() {
        return result;
    }

    /**
     * Set if the player has the permission or not
     *
     * @param status if the player has the permission
     */
    public void setResult(final boolean status) {
        result = status;
    }
}
