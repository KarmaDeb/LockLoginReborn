package eu.locklogin.api.module.plugin.javamodule.card;

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
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import eu.locklogin.api.util.platform.CurrentPlatform;

/**
 * Client card
 */
public class ClientCard extends QueueCard<ModulePlayer> {

    private ModulePlayer player;

    /**
     * Initialize the client card
     *
     * @param module the module owning this card
     * @param client the client managed in this card
     */
    public ClientCard(final PluginModule module, final ModulePlayer client) {
        super(module, client);
        player = client;
    }

    /**
     * Update the card object
     *
     * @param update the update value
     */
    @Override
    protected final void update(final ModulePlayer update) {
        if (CurrentPlatform.getServer().isValid(update)) {
            player = update;
        }
    }

    /**
     * Get the card value
     *
     * @return the card value
     */
    @Override
    public final ModulePlayer get() {
        return player;
    }

    /**
     * Get the ClientCard as a QueuedCard
     *
     * @return the queued card instance of this
     */
    public QueueCard<ModulePlayer> getQueued() {
        return this;
    }
}
