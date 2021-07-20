package eu.locklogin.api.module.plugin.client;

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

import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.util.platform.CurrentPlatform;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * ModulePlayer class
 */
public final class ModulePlayer implements Serializable {

    private final String name;
    private final UUID uniqueId;
    private final ClientSession session;
    private final AccountManager manager;
    private final InetAddress address;

    @SuppressWarnings("FieldMayBeFinal")
    private static Consumer<MessageSender> onChat = null;
    @SuppressWarnings("FieldMayBeFinal")
    private static Consumer<ActionBarSender> onBar = null;
    @SuppressWarnings("FieldMayBeFinal")
    private static Consumer<TitleSender> onTitle = null;
    @SuppressWarnings("FieldMayBeFinal")
    private static Consumer<MessageSender> onKick = null;
    @SuppressWarnings("FieldMayBeFinal")
    private static Consumer<ModulePlayer> onLogin = null;
    @SuppressWarnings("FieldMayBeFinal")
    private static Consumer<ModulePlayer> onClose = null;

    /**
     * Initialize the player object
     *
     * @param client the client name
     * @param id the client id
     * @param ses the client session
     * @param acc the player account manager
     * @param ip the player ip address
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
     * Get the player object
     *
     * @param <T> the player type
     * @return the player object
     */
    public final <T> T getPlayer() {
        try {
            Class<?> serverClass;
            Field utilField;
            Object server;
            Method getPlayer;

            switch (CurrentPlatform.getPlatform()) {
                case BUKKIT:
                    serverClass = Class.forName("org.bukkit.Bukkit");
                    server = serverClass.getMethod("getServer").invoke(serverClass);
                    getPlayer = server.getClass().getMethod("getPlayer", UUID.class);
                    return (T) getPlayer.invoke(server, uniqueId);
                case BUNGEE:
                    serverClass = Class.forName("net.md_5.bungee.api.ProxyServer");
                    server = serverClass.getMethod("getInstance").invoke(serverClass);
                    getPlayer = server.getClass().getMethod("getPlayer", UUID.class);
                    return (T) getPlayer.invoke(server, uniqueId);
                case VELOCITY:
                    serverClass = Class.forName("eu.locklogin.plugin.velocity.LockLogin");
                    utilField = serverClass.getField("server");
                    server = utilField.get(null);
                    getPlayer = server.getClass().getMethod("getPlayer", UUID.class);
                    Optional<T> player = (Optional<T>) getPlayer.invoke(server, uniqueId);
                    return player.get();
                default:
                    return null;
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            return null;
        }
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

    /**
     * Send an action bar to the player
     *
     * @param message the message to send
     */
    public final void sendActionBar(final String message) {
        if (onBar != null) {
            onBar.accept(new ActionBarSender(this, message));
        }
    }

    /**
     * Send a title to the player
     *
     * @param title the title
     * @param subtitle the subtitle
     */
    public final void sendTitle(final String title, final String subtitle) {
        if (onTitle != null) {
            onTitle.accept(new TitleSender(this, title, subtitle, 2, 5, 2));
        }
    }

    /**
     * Send a title to the player
     *
     * @param title the title
     * @param subtitle the subtitle
     * @param fadeOut the time before showing the title
     * @param keepIn the time to show the title
     * @param hideIn the time that will take to hide the title
     */
    public final void sendTitle(final String title, final String subtitle, final int fadeOut, final int keepIn, final int hideIn) {
        if (onTitle != null) {
            onTitle.accept(new TitleSender(this, title, subtitle, fadeOut, keepIn, fadeOut));
        }
    }

    /**
     * Request the player to be kicked
     *
     * @param reason the kick reason
     */
    public final void requestKick(final String reason) {
        if (onKick != null) {
            onKick.accept(new MessageSender(this, reason));
        }
    }

    /**
     * Request client force-login
     */
    public final void requestLogin() {
        if (onLogin != null) {
            onLogin.accept(this);
        }
    }

    /**
     * Request client close session
     */
    public final void requestUnlogin() {
        if (onClose != null) {
            onClose.accept(this);
        }
    }
}
