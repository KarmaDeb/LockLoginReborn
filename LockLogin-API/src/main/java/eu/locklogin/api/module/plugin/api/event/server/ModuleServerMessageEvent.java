package eu.locklogin.api.module.plugin.api.event.server;

import eu.locklogin.api.module.PluginModule;

/**
 * This event is fired when a module sends a message to the server
 */
public final class ModuleServerMessageEvent extends ServerSendMessageEvent {

    private final PluginModule module;
    private Object[] replaces;

    /**
     * Initialize the server receive message event
     *
     * @param msg the message
     */
    public ModuleServerMessageEvent(PluginModule mod, final String msg, final Object... rep) {
        super(msg);

        module = mod;
        replaces = rep;
    }

    /**
     * Update a replace
     *
     * @param placeholder the placeholder
     * @param newValue    the new replacement value
     */
    public void setReplace(final int placeholder, final Object newValue) {
        try {
            replaces[placeholder] = newValue;
        } catch (Throwable ignored) {
        }
    }

    /**
     * Add a new replacement to the replaces
     *
     * @param newValue the new replacement
     */
    public void addReplacement(final Object newValue) {
        Object[] replacements = new Object[replaces.length + 1];
        System.arraycopy(replaces, 0, replacements, 0, replaces.length);

        replacements[replaces.length + 1] = newValue;

        replaces = replacements;
    }

    /**
     * Get the module who sent the message
     *
     * @return the module who sent the message
     */
    public PluginModule getModule() {
        return module;
    }

    /**
     * Get the amount of replacements
     *
     * @return the amount of replacements
     */
    public int getReplacesAmount() {
        return replaces.length;
    }

    /**
     * Get the message replaces
     *
     * @return the message replaces
     */
    public Object[] getReplaces() {
        return replaces;
    }

    /**
     * Get the replacement
     *
     * @param placeholder the placeholder index
     * @return the placeholder value
     */
    public Object getReplace(final int placeholder) {
        try {
            return replaces[placeholder];
        } catch (Throwable ex) {
            return null;
        }
    }
}
