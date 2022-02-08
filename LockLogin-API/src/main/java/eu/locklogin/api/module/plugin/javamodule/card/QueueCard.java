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

/**
 * Queue card
 *
 * @param <A> card object
 */
public class QueueCard<A> extends APICard<A> {

    private A object;

    /**
     * Initialize the card
     *
     * @param module the module owning this card
     * @param instance the object managed in this card
     */
    public QueueCard(final PluginModule module, final A instance) {
        super(module, instance.getClass().getSimpleName().toLowerCase());

        object = instance;
    }

    /**
     * Update the card object
     *
     * @param update the update value
     */
    @Override
    protected void update(final A update) {
        if (update != null)
            object = update;
    }

    /**
     * Get the card value
     *
     * @return the card value
     */
    @Override
    public A get() {
        return object;
    }
}
