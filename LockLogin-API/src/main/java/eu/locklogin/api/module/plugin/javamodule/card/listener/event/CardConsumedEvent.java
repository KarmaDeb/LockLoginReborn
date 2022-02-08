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
 * Card queue event wrapper
 */
public final class CardConsumedEvent extends CardEvent {

    private final PluginModule module;
    private final PluginModule container;
    private final String identifier;

    /**
     * Initialize the event
     *
     * @param owner the card owner
     * @param c the module where the card is contained. It is usally the same as owner
     * @param name the card name
     */
    public CardConsumedEvent(final PluginModule owner, final PluginModule c, final String name) {
        module = owner;
        container = c;
        identifier = name;
    }

    /**
     * Get the module owning the card
     *
     * @return the card owner
     */
    @Override
    public PluginModule getModule() {
        return module;
    }

    /**
     * Get the module containing the card. It's usuall the same
     * {@link CardEvent#getModule()}
     *
     * @return the card container
     */
    @Override
    public PluginModule getContainer() {
        return container;
    }

    /**
     * Get the card identifier
     *
     * @return the card identifier
     */
    @Override
    public String getIdentifier() {
        return identifier;
    }
}
