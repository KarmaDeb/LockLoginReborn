package ml.karmaconfigs.locklogin.api.modules.util.client;

import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.account.ClientSession;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.UUID;
import java.util.function.Consumer;

public final class ModulePlayer implements Serializable {

    private final String name;
    private final UUID uniqueId;
    private final ClientSession session;
    private final AccountManager manager;
    private final InetAddress address;

    @SuppressWarnings("FieldMayBeFinal")
    private static Consumer<MessageSender> onChat = null;

    /**
     * Initialize the player object
     *
     * @param client the client name
     * @param id the client id
     * @param ses the client session
     */
    public ModulePlayer(final String client, final UUID id, final ClientSession ses, final AccountManager acc, final InetAddress ip) {
        name = client;
        uniqueId = id;
        session = ses;
        manager = acc;
        address = ip;
    }

    /**
     * Get the player name
     *
     * @return the player name
     */
    public final String getName() {
        return name;
    }

    /**
     * Get the player unique id
     *
     * @return the player unique id
     */
    public final UUID getUUID() {
        return uniqueId;
    }

    /**
     * Get the player session
     *
     * @return the player session
     */
    public final ClientSession getSession() {
        return session;
    }

    /**
     * Get the player account manager
     *
     * @return the player account manager
     */
    public final AccountManager getAccount() {
        return manager;
    }

    /**
     * Get the player address
     *
     * @return the player address
     */
    public final InetAddress getAddress() {
        return address;
    }

    /**
     * Send a message to the player
     *
     * @param message the message to send
     */
    public final void sendMessage(final String message) {
        if (onChat != null) {
            onChat.accept(new MessageSender(this, message));
        }
    }
}
