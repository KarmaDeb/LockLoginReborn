package eu.locklogin.api.module.plugin.client;

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
 * ModulePlayer operator checker, this contains
 * the player that is being op-checked and the result
 */
public final class OpContainer {

    private final ModulePlayer player;
    private boolean result = false;

    /**
     * Initialize the op container
     *
     * @param attachment the player who is being op-checked
     */
    public OpContainer(final ModulePlayer attachment) {
        player = attachment;
    }

    /**
     * Get the player that is being op-checked
     *
     * @return the player
     */
    public ModulePlayer getAttachment() {
        return player;
    }

    /**
     * If the player is op or not
     *
     * @return the result
     */
    public boolean getResult() {
        return result;
    }

    /**
     * Set if the player has op or not
     *
     * @param status if the player has op
     */
    public void setResult(final boolean status) {
        result = status;
    }
}
