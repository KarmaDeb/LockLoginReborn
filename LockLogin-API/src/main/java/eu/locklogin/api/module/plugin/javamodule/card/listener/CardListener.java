package eu.locklogin.api.module.plugin.javamodule.card.listener;

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

import eu.locklogin.api.module.plugin.javamodule.card.listener.event.CardConsumedEvent;
import eu.locklogin.api.module.plugin.javamodule.card.listener.event.CardPostQueueEvent;
import eu.locklogin.api.module.plugin.javamodule.card.listener.event.CardPreConsumeEvent;
import eu.locklogin.api.module.plugin.javamodule.card.listener.event.CardQueueEvent;

/**
 * Simple card listener
 */
public interface CardListener {

    /**
     * When a card as been queued
     *
     * @param event the card queue event wrapper
     */
    void cardQueued(final CardQueueEvent event);

    /**
     * When a card has been completely queued
     *
     * @param event the card queue event wrapper
     */
    void cardPostQueued(final CardPostQueueEvent event);

    /**
     * When a card is about to be consumed
     *
     * @param event the card pre consume event wrapper
     */
    void cardPreConsumeEvent(final CardPreConsumeEvent event);

    /**
     * When a card has been consumed
     *
     * @param event the card consume event wrapper
     */
    void cardConsumedEvent(final CardConsumedEvent event);
}
