package eu.locklogin.api.module.plugin.javamodule.server;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import eu.locklogin.api.module.PluginModule;
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class TargetServer implements Iterable<ModulePlayer> {

    private final String name;
    private final UUID token;
    private final InetAddress address;
    private final int port;
    private final boolean online;

    @SuppressWarnings("FieldMayBeFinal")
    private static BiConsumer<String, Set<ModulePlayer>> onPlayers = null;
    @SuppressWarnings("FieldMayBeFinal")
    private static MessageQue que = null;

    /**
     * LockLogin module server
     *
     * @param n the server name
     * @param id the server token id
     * @param ip the server address
     * @param door the server port
     * @param active if the server is active
     */
    public TargetServer(final String n, final UUID id, final InetAddress ip, final int door, final boolean active) {
        name = n;
        token = id;
        address = ip;
        port = door;
        online = active;
    }

    /**
     * Get the server name
     *
     * @return the server name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the server token id
     *
     * @return the server token id
     */
    public UUID getToken() {
        return token;
    }

    /**
     * Get the server address
     *
     * @return the server address
     */
    public InetAddress getAddress() {
        return address;
    }

    /**
     * Get the server port
     *
     * @return the server port
     */
    public int getPort() {
        return port;
    }

    /**
     * Get the server online players
     *
     * @return the server online players
     */
    public Set<ModulePlayer> getOnlinePlayers() {
        Set<ModulePlayer> connected = new HashSet<>();

        if (onPlayers != null) {
            onPlayers.accept(name, connected);
        }

        return connected;
    }

    /**
     * Get if the server is active
     *
     * @return if the server is active
     */
    public boolean isActive() {
        return online;
    }

    /**
     * Send data to the server
     *
     * @param sender the module that is sending the message
     * @param data the data
     */
    @SuppressWarnings("UnstableApiUsage")
    public void sendMessage(final PluginModule sender, final byte[] data) {
        if (que != null) {
            ByteArrayDataOutput modified_out = ByteStreams.newDataOutput();
            modified_out.writeUTF(sender.getID().toString());
            modified_out.write(data); //Not sure if this would work...

            que.add(modified_out.toByteArray());
        }
    }

    /**
     * Returns an iterator over elements of type {@code T}.
     *
     * @return an Iterator.
     */
    @NotNull
    @Override
    public Iterator<ModulePlayer> iterator() {
        Set<ModulePlayer> players = new HashSet<>();

        if (onPlayers != null) {
            onPlayers.accept(name, players);
        }

        return players.iterator();
    }

    /**
     * Performs the given action for each element of the {@code Iterable}
     * until all elements have been processed or the action throws an
     * exception.  Actions are performed in the order of iteration, if that
     * order is specified.  Exceptions thrown by the action are relayed to the
     * caller.
     * <p>
     * The behavior of this method is unspecified if the action performs
     * side effects that modify the underlying source of elements, unless an
     * overriding class has specified a concurrent modification policy.
     *
     * @param action The action to be performed for each element
     * @throws NullPointerException if the specified action is null
     * @since 1.8
     */
    @Override
    public void forEach(Consumer<? super ModulePlayer> action) {
        Set<ModulePlayer> players = new HashSet<>();

        if (onPlayers != null) {
            onPlayers.accept(name, players);
        }

        if (action != null) {
            for (ModulePlayer player : players)
                action.accept(player);
        }
    }
}
