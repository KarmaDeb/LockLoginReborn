package eu.locklogin.api.module.plugin.javamodule.sender;

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
import eu.locklogin.api.account.param.Parameter;
import eu.locklogin.api.account.param.SimpleParameter;
import eu.locklogin.api.module.plugin.client.ActionBarSender;
import eu.locklogin.api.module.plugin.client.MessageSender;
import eu.locklogin.api.module.plugin.client.TitleSender;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.utils.string.StringUtils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * ModulePlayer class
 */
public final class ModulePlayer extends ModuleSender implements Serializable {

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
    private final String name;
    private final UUID uniqueId;
    private final ClientSession session;
    private final AccountManager manager;
    private final InetAddress address;

    /**
     * Initialize the player object
     *
     * @param client the client name
     * @param id     the client id
     * @param ses    the client session
     * @param acc    the player account manager
     * @param ip     the player ip address
     */
    public ModulePlayer(final String client, final UUID id, final ClientSession ses, final AccountManager acc, final InetAddress ip) {
        super();
        name = client;
        uniqueId = id;
        session = ses;
        manager = acc;
        address = ip;
    }

    /**
     * Get the sender name
     *
     * @return the sender name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Get the player unique id
     *
     * @return the player unique id
     */
    public UUID getUUID() {
        return uniqueId;
    }

    /**
     * Get the player session
     *
     * @return the player session
     */
    public ClientSession getSession() {
        return session;
    }

    /**
     * Get the player account manager
     *
     * @return the player account manager
     */
    public AccountManager getAccount() {
        return manager;
    }

    /**
     * Get the player address
     *
     * @return the player address
     */
    public InetAddress getAddress() {
        return address;
    }

    /**
     * Get the player object
     *
     * @param <T> the player type
     * @return the player object
     */
    @SuppressWarnings("unchecked")
    public <T> T getPlayer() {
        Map<UUID, Object> connected = CurrentPlatform.getConnectedPlayers();
        for (UUID uuid : connected.keySet()) {
            if (uuid.equals(uniqueId)) {
                Object player = connected.getOrDefault(uuid, null);
                return (T) player;
            }
        }

        return null;
    }

    /**
     * Get the player object
     *
     * @return the player object
     */
    public Object getPlayerObject() {
        try {
            switch (CurrentPlatform.getPlatform()) {
                case BUKKIT:
                    Class<?> bukkit = Class.forName("org.bukkit.Bukkit");

                    Object bukkitServer = bukkit.getMethod("getServer").invoke(bukkit);
                    Method getPlayer = bukkitServer.getClass().getMethod("getPlayer", UUID.class);

                    return getPlayer.invoke(bukkitServer, uniqueId);
                case BUNGEE:
                    Class<?> proxy = Class.forName("net.md_5.bungee.api.ProxyServer");

                    Object bungeeServer = proxy.getMethod("getInstance").invoke(proxy);
                    getPlayer = bungeeServer.getClass().getMethod("getPlayer", UUID.class);

                    return getPlayer.invoke(bungeeServer, uniqueId);
                case VELOCITY:
                    Class<?> locklogin = Class.forName("eu.locklogin.plugin.velocity.LockLogin");
                    Object velocityServer = locklogin.getField("server").get(null);

                    getPlayer = velocityServer.getClass().getMethod("getPlayer", UUID.class);
                    Object result = getPlayer.invoke(velocityServer, uniqueId);

                    if (result instanceof Optional) {
                        Optional<?> optional = (Optional<?>) result;
                        return optional.orElse(null);
                    } else {
                        APISource.loadProvider("LockLogin").console().send("&cTried to get player object from LockLoginAPI with server platform velocity, but player result does not match velocity method");
                        return null;
                    }
                default:
                    APISource.loadProvider("LockLogin").console().send("&cTried to get player object from LockLoginAPI with an unknown server platform");
                    return null;
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Get if the module player is connected to server
     *
     * @return if the module player is connected to server
     */
    public boolean isPlaying() {
        Object player = getPlayerObject();
        if (player != null) {
            try {
                switch (CurrentPlatform.getPlatform()) {
                    case BUKKIT:
                        Method isOnline = player.getClass().getMethod("isOnline");
                        return (boolean) isOnline.invoke(player);
                    case BUNGEE:
                        Method isConnected = player.getClass().getMethod("isConnected");
                        return (boolean) isConnected.invoke(player);
                    case VELOCITY:
                        Method isActive = player.getClass().getMethod("isActive");
                        return (boolean) isActive.invoke(player);
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }

        return false;
    }

    /**
     * Send a message to the player
     *
     * @param message  the message to send
     * @param replaces the message replaces
     */
    @Override
    public void sendMessage(final String message, final Object... replaces) {
        if (onChat != null && isPlaying()) {
            onChat.accept(new MessageSender(this, StringUtils.formatString(message, replaces)));
        }
    }

    /**
     * Send an action bar to the player
     *
     * @param message the message to send
     */
    public void sendActionBar(final String message) {
        if (onBar != null && isPlaying()) {
            onBar.accept(new ActionBarSender(this, message));
        }
    }

    /**
     * Send a title to the player
     *
     * @param title    the title
     * @param subtitle the subtitle
     */
    public void sendTitle(final String title, final String subtitle) {
        if (onTitle != null && isPlaying()) {
            onTitle.accept(new TitleSender(this, title, subtitle, 2, 5, 2));
        }
    }

    /**
     * Send a title to the player
     *
     * @param title    the title
     * @param subtitle the subtitle
     * @param fadeOut  the time before showing the title
     * @param keepIn   the time to show the title
     * @param hideIn   the time that will take to hide the title
     */
    public void sendTitle(final String title, final String subtitle, final int fadeOut, final int keepIn, final int hideIn) {
        if (onTitle != null && isPlaying()) {
            onTitle.accept(new TitleSender(this, title, subtitle, fadeOut, keepIn, fadeOut));
        }
    }

    /**
     * Request the player to be kicked
     *
     * @param reason the kick reason
     */
    public void requestKick(final String reason) {
        if (onKick != null && isPlaying()) {
            onKick.accept(new MessageSender(this, reason));
        }
    }

    /**
     * Request client force-login
     */
    public void requestLogin() {
        if (onLogin != null && isPlaying()) {
            onLogin.accept(this);
        }
    }

    /**
     * Request client close session
     */
    public void requestUnlogin() {
        if (onClose != null && isPlaying()) {
            onClose.accept(this);
        }
    }

    /**
     * Get the parameter of the account parameter
     *
     * @return the account constructor parameter
     */
    @Override
    public Parameter<ModuleSender> getParameter() {
        return new SimpleParameter<>("player", this);
    }

    /**
     * Get a class instance of the account constructor
     * type
     *
     * @return the account constructor type
     */
    @Override
    public Class<? extends ModuleSender> getType() {
        return ModulePlayer.class;
    }
}
