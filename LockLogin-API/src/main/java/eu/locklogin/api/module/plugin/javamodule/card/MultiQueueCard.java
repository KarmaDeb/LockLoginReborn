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

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Multi queue card
 */
public class MultiQueueCard extends APICard<Set<APICard<?>>> {

    private final Set<APICard<?>> objects = new LinkedHashSet<>();

    /**
     * Initialize the multi queued card
     *
     * @param module the module owning this card
     * @param cards the cards inside this card
     */
    public MultiQueueCard(final PluginModule module, final APICard<?>... cards) {
        super(module, "multi_card");

        for (APICard<?> card : cards) {
            if (card.get() != null) {
                objects.add(card);
            }
        }
    }

    /**
     * Update the card object
     *
     * @param update the update value
     */
    @Override
    @SuppressWarnings("unchecked")
    public final void update(final Set<APICard<?>> update) {
        Set<APICard<Object>> removes = new HashSet<>();
        Set<String> names = new HashSet<>();
        update.forEach((card) -> names.add(card.identifier()));

        for (APICard<?> obj : objects) {
            if (names.contains(obj.identifier())) {
                removes.add((APICard<Object>) obj);
            }
        }

        objects.removeAll(removes);
        objects.addAll(update);
    }

    /**
     * Get the card value
     *
     * @return the card value
     */
    @Override
    public final Set<APICard<?>> get() {
        return objects;
    }

    /**
     * Perform an action for each card
     *
     * @param consumer the card consumer
     */
    public void forEach(final Consumer<APICard<?>> consumer) {
        objects.forEach(consumer);
    }

    /**
     * Perform an action for each card
     *
     * @param finder the card identifier name
     * @param consumer the card consumer
     */
    public void forEach(final String finder, final Consumer<APICard<?>> consumer) {
        objects.forEach((card) -> {
            String identifier = card.identifier();
            String[] data = identifier.split(":");

            identifier = identifier.replaceFirst(data[0] + ":", "");
            if (identifier.equals(finder))
                consumer.accept(card);
        });
    }

    /**
     * Perform an action for each card
     *
     * @param module the card identifier module
     * @param consumer the card consumer
     */
    public void forEach(final PluginModule module, final Consumer<APICard<?>> consumer) {
        objects.forEach((card) -> {
            String identifier = card.identifier();
            String[] data = identifier.split(":");

            identifier = data[0];
            if (identifier.equals(module.name()))
                consumer.accept(card);
        });
    }

    /**
     * Perform an action for each card
     *
     * @param module the card identifier module
     * @param finder the card identifier name
     * @param consumer the card consumer
     */
    public void forEach(final PluginModule module, final String finder, final Consumer<APICard<?>> consumer) {
        objects.forEach((card) -> {
            String identifier = card.identifier();

            if (identifier.equals(module.name() + ":" + finder))
                consumer.accept(card);
        });
    }
}
