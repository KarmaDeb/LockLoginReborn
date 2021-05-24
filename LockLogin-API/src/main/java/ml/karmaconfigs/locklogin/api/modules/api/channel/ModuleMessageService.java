package ml.karmaconfigs.locklogin.api.modules.api.channel;

import ml.karmaconfigs.locklogin.api.modules.PluginModule;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public final class ModuleMessageService {

    private final static Map<PluginModule, Map<String, ModuleMessagingChannel>> channels = new HashMap<>();

    private final PluginModule module;

    private static BiConsumer<String, byte[]> onDataSent;

    /**
     * Initialize the module message service
     *
     * @param owner the module owner
     */
    public ModuleMessageService(final PluginModule owner) {
        module = owner;
    }

    /**
     * Register a new message channel service
     *
     * @param name the channel name
     * @param channel the channel instance
     */
    public final void registerService(String name, final ModuleMessagingChannel channel) {
        name = name.toLowerCase();

        Map<String, ModuleMessagingChannel> registered = channels.getOrDefault(module, new HashMap<>());
        if (registered.getOrDefault(name, null) == null) {
            registered.put(name, channel);
            channels.put(module, registered);
        }
    }

    /**
     * Remove a message channel service
     *
     * @param name the channel name
     */
    public final void unregisterService(String name) {
        name = name.toLowerCase();

        Map<String, ModuleMessagingChannel> registered = channels.getOrDefault(module, new HashMap<>());
        if (registered.containsKey(name)) {
            for (String str : registered.keySet()) {
                if (str.equalsIgnoreCase(name)) {
                    registered.remove(name);
                    channels.put(module, registered);
                }
            }
        }
    }

    /**
     * Send a message to all the registered channels
     *
     * @param name the channel name
     * @param data the message data
     */
    public static void sendMessage(final String name, final byte[] data) {
        if (onDataSent != null)
            onDataSent.accept(name, data);
    }

    /**
     * Listen a message
     *
     * @param name the channel
     * @param data the message data
     */
    public static void listenMessage(final String name, final byte[] data) {
        for (PluginModule instance : channels.keySet()) {
            Map<String, ModuleMessagingChannel> registered = channels.getOrDefault(instance, new HashMap<>());

            ModuleMessagingChannel channel = registered.getOrDefault(name.toLowerCase(), null);
            if (channel != null) {
                channel.onMessageReceived(name, data);
            }
        }
    }
}
