package ml.karmaconfigs.locklogin.api;

import ml.karmaconfigs.locklogin.api.event.util.Event;
import ml.karmaconfigs.locklogin.api.modules.bukkit.JavaModule;
import ml.karmaconfigs.locklogin.api.event.util.EventListener;

import java.lang.reflect.Method;
import java.util.*;

/**
 * LockLogin listener
 */
public final class LockLoginListener {

    private final static Map<JavaModule, Set<EventListener>> listeners = new HashMap<>();
    private final static Map<JavaModule, Set<Method>> unregistered = new HashMap<>();

    /**
     * Register a listener and link it to
     * the specified module
     *
     * @param owner the module owner
     * @param handler the event handler class
     */
    public static void registerListener(final JavaModule owner, final EventListener handler) {
        Set<EventListener> handlers = listeners.getOrDefault(owner, new LinkedHashSet<>());
        handlers.add(handler);

        listeners.put(owner, handlers);
    }

    /**
     * Unregister a listener from the specified owner
     *
     * @param owner the module owner
     * @param event the event to ignore
     */
    public static void unregisterListener(final JavaModule owner, final Event event) {
        Set<EventListener> handlers = listeners.getOrDefault(owner, new LinkedHashSet<>());
        Set<Method> disabled = unregistered.getOrDefault(owner, new LinkedHashSet<>());
        for (EventListener handler : handlers) {

            Method[] methods = handler.getClass().getMethods();
            for (Method method : methods) {
                if (method.getParameterTypes()[0].isAssignableFrom(event.getClass())) {
                    disabled.add(method);
                }
            }
        }

        unregistered.put(owner, disabled);
    }

    /**
     * Unregister all the listeners of the specified
     * module
     *
     * @param module the module
     */
    public static void unregisterListeners(final JavaModule module) {
        listeners.put(module, new HashSet<>());
    }

    /**
     * Call an event, so each module can handle it
     *
     * @param event the event to call
     */
    public static void callEvent(final Event event) {
        for (JavaModule module : listeners.keySet()) {
            Set<EventListener> handlers = listeners.getOrDefault(module, new LinkedHashSet<>());

            for (EventListener handler : handlers) {
                //Only call the event if the event class is instance of the
                //listener class
                Method[] methods = handler.getClass().getMethods();
                for (Method method : methods) {
                    if (method.isAnnotationPresent(ModuleEventHandler.class)) {
                        if (method.getParameterTypes()[0].isAssignableFrom(event.getClass())) {
                            try {
                                method.invoke(handler, event);
                            } catch (Throwable ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }
}
