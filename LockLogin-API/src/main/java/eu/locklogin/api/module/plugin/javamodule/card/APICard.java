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
import eu.locklogin.api.module.plugin.javamodule.card.listener.CardListener;
import eu.locklogin.api.module.plugin.javamodule.card.listener.event.CardConsumedEvent;
import eu.locklogin.api.module.plugin.javamodule.card.listener.event.CardEvent;
import eu.locklogin.api.module.plugin.javamodule.card.listener.event.CardPreConsumeEvent;
import eu.locklogin.api.module.plugin.javamodule.card.listener.event.CardQueueEvent;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * APICard containing what any API card
 * should have
 *
 * @param <A> the card object
 */
public abstract class APICard<A> {

    private final static Set<CardListener> listeners = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final PluginModule module;
    private final String name;

    public APICard(final PluginModule owner, final String identifier) {
        module = owner;
        name = identifier;
    }

    /**
     * Update the card object
     *
     * @param update the update value
     */
    protected abstract void update(final A update);

    /**
     * Get the card value
     *
     * @return the card value
     */
    public abstract A get();

    /**
     * Get the card module
     *
     * @return the card module owner
     */
    public final PluginModule module() {
        return module;
    }

    /**
     * Get the identifier
     *
     * @return the card identifier
     */
    public final String identifier() {
        return module.name() + ":" + name;
    }

    /**
     * Add a card listener
     *
     * @param listener the card listener
     */
    public static void addCardListener(final CardListener listener) {
        listeners.remove(listener);
        listeners.add(listener);
    }

    /**
     * Remove a card listener
     *
     * @param listener the card listener
     */
    public static void removeCardListener(final CardListener listener) {
        listeners.remove(listener);
    }

    /**
     * Invoke an event
     *
     * @param event the event
     */
    public static void invoke(final CardEvent event) {
        if (event instanceof CardQueueEvent) {
            listeners.forEach((listener) -> listener.cardQueued((CardQueueEvent) event));
        }
        if (event instanceof CardPreConsumeEvent) {
            listeners.forEach((listener) -> listener.cardPreConsumeEvent((CardPreConsumeEvent) event));
        }
        if (event instanceof CardConsumedEvent) {
            listeners.forEach((listener) -> listener.cardConsumedEvent((CardConsumedEvent) event));
        }
    }
}
