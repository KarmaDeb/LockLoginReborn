package eu.locklogin.api.module.plugin.javamodule.card.listener.event;

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

import eu.locklogin.api.module.PluginModule;

/**
 * Card event
 */
public abstract class CardEvent {

    /**
     * Get the module owning the card
     *
     * @return the card owner
     */
    public abstract PluginModule getModule();

    /**
     * Get the module containing the card. It's usually the same
     * {@link CardEvent#getModule()}
     *
     * @return the card container
     */
    public abstract PluginModule getContainer();

    /**
     * Get the card identifier
     *
     * @return the card identifier
     */
    public abstract String getIdentifier();
}
