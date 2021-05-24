package ml.karmaconfigs.locklogin.manager.bungee.manager;

import net.md_5.bungee.api.event.AsyncEvent;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventBus;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
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
public class EventBusManager extends EventBus {

    private static final Set<AsyncEvent<?>> uncompletedEvents = Collections.newSetFromMap(new WeakHashMap<AsyncEvent<?>, Boolean>());

    public static void completeIntents(Plugin plugin) {
        synchronized (uncompletedEvents) {
            for (AsyncEvent<?> event : uncompletedEvents) {
                try {
                    event.completeIntent(plugin);
                } catch (Throwable t) {
                }
            }
        }
    }

    @Override
    public void post(Object event) {
        if (event instanceof AsyncEvent) {
            synchronized (uncompletedEvents) {
                uncompletedEvents.add((AsyncEvent<?>) event);
            }
        }
        super.post(event);
    }

}

