package ml.karmaconfigs.locklogin.api.modules.api.channel;

public abstract class ModuleMessagingChannel {

    public abstract void onMessageReceived(final String channel, final byte[] data);
}
