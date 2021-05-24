package ml.karmaconfigs.locklogin.plugin.bungee.util.player;

import ml.karmaconfigs.api.bungee.makeiteasy.TitleMessage;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.account.AccountID;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.files.PluginConfiguration;
import ml.karmaconfigs.locklogin.api.files.options.LoginConfig;
import ml.karmaconfigs.locklogin.api.files.options.RegisterConfig;
import ml.karmaconfigs.locklogin.api.modules.api.event.user.UserAuthenticateEvent;
import ml.karmaconfigs.locklogin.api.modules.util.client.ModulePlayer;
import ml.karmaconfigs.locklogin.api.modules.util.javamodule.JavaModuleManager;
import ml.karmaconfigs.locklogin.api.modules.api.event.user.SessionInitializationEvent;
import ml.karmaconfigs.locklogin.api.utils.platform.CurrentPlatform;
import ml.karmaconfigs.locklogin.plugin.bungee.permissibles.Permission;
import ml.karmaconfigs.locklogin.plugin.bungee.plugin.sender.DataSender;
import ml.karmaconfigs.locklogin.plugin.common.session.SessionDataContainer;
import ml.karmaconfigs.locklogin.plugin.common.session.SessionKeeper;
import ml.karmaconfigs.locklogin.plugin.common.utils.DataType;
import ml.karmaconfigs.locklogin.plugin.bungee.util.files.Proxy;
import ml.karmaconfigs.locklogin.plugin.bungee.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.common.security.GoogleAuthFactory;
import net.md_5.bungee.api.ServerConnectRequest;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static ml.karmaconfigs.locklogin.plugin.bungee.LockLogin.*;
import static ml.karmaconfigs.locklogin.plugin.bungee.plugin.sender.DataSender.*;

/**
 * Initialize the user
 */
public final class User {

    @SuppressWarnings("FieldMayBeFinal") //This could be modified by the cache loader, so it can't be final
    private static Map<UUID, ClientSession> sessions = new HashMap<>();
    private final static Map<UUID, AccountManager> managers = new HashMap<>();

    private final AccountManager manager;
    private final ProxiedPlayer player;

    /**
     * Initialize the user
     *
     * @param _player the player
     * @throws IllegalStateException as part of session and account managers setup, if some of
     *                               they are null or can't be initialized, this exception will be thrown and the plugin will be
     *                               instantly disabled
     */
    public User(final ProxiedPlayer _player) throws IllegalStateException {
        player = _player;

        if (!sessions.containsKey(player.getUniqueId())) {
            if (CurrentPlatform.isValidSessionManager()) {
                ClientSession session = CurrentPlatform.getSessionManager(null);

                if (session == null) {
                    throw new IllegalStateException("Cannot initialize user with a null session manager");
                } else {
                    session.initialize();

                    SessionInitializationEvent event = new SessionInitializationEvent(fromPlayer(player), session, null);
                    JavaModuleManager.callEvent(event);

                    sessions.put(player.getUniqueId(), session);
                }
            } else {
                throw new IllegalStateException("Cannot initialize user with a null session manager");
            }
        }

        if (CurrentPlatform.isValidAccountManager()) {
            manager = CurrentPlatform.getAccountManager(new Class[]{ProxiedPlayer.class}, player);

            if (manager == null) {
                throw new IllegalStateException("Cannot initialize user with a null player account manager");
            } else {
                //Try to fix the empty manager values that are
                //required
                if (manager.exists()) {
                    String name = manager.getName();
                    AccountID id = manager.getUUID();

                    if (name.replaceAll("\\s", "").isEmpty())
                        manager.setName(StringUtils.stripColor(player.getDisplayName()));

                    if (id.getId().replaceAll("\\s", "").isEmpty())
                        manager.saveUUID(AccountID.fromUUID(player.getUniqueId()));
                }

                managers.put(player.getUniqueId(), manager);
            }
        } else {
            throw new IllegalStateException("Cannot initialize user with an invalid player account manager");
        }
    }

    /**
     * Get the map of sessions
     *
     * @return the map of sessions
     */
    public static Map<UUID, ClientSession> getSessionMap() {
        return new HashMap<>(sessions);
    }

    /**
     * Get the player session
     *
     * @param player the player
     * @return the player session
     */
    public static ClientSession getSession(final ProxiedPlayer player) {
        return sessions.get(player.getUniqueId());
    }

    /**
     * Get the player account manager
     *
     * @param player the player
     * @return the player account manager
     */
    public static AccountManager getManager(final ProxiedPlayer player) {
        return managers.get(player.getUniqueId());
    }

    /**
     * Send a message to the player
     *
     * @param message the message to send
     */
    public final void send(final String message) {
        String[] parsed = parseMessage(message);

        Message messages = new Message();

        if (parsed.length > 1) {
            for (String str : parsed)
                player.sendMessage(TextComponent.fromLegacyText(StringUtils.toColor(str)));
        } else {
            //Make sure to avoid null messages
            if (parsed.length == 1) {
                if (!parsed[0].replace(messages.prefix(), "").replaceAll("\\s", "").isEmpty())
                    player.sendMessage(TextComponent.fromLegacyText(StringUtils.toColor(parsed[0])));
            }
        }
    }

    /**
     * Send a message to the player
     *
     * @param message the message to send
     */
    public final void send(final TextComponent message) {
        player.sendMessage(message);
    }

    /**
     * Send a title and subtitle to the player
     *
     * @param title    the title to send
     * @param subtitle the subtitle to send
     */
    public final void send(final String title, final String subtitle) {
        TitleMessage titleMessage = new TitleMessage(player, title, subtitle);
        titleMessage.send(0, 5, 0);
    }

    /**
     * Check the player server
     *
     * @param failed the failed servers
     */
    public final void checkServer(final String... failed) {
        Set<String> failSet = new LinkedHashSet<>(Arrays.asList(failed));

        Proxy proxy = new Proxy();
        if (proxy.sendToServers()) {
            ClientSession session = getSession();

            if (session.isValid()) {
                if (session.isLogged() && session.isCaptchaLogged()) {
                    if (Proxy.inAuth(player) && Proxy.lobbiesValid()) {
                        Iterator<ServerInfo> lobbies = proxy.lobbyServers(ServerInfo.class);
                        if (lobbies.hasNext()) {
                            do {
                                ServerInfo lInfo = lobbies.next();
                                if (!failSet.contains(lInfo.getName())) {
                                    player.connect(ServerConnectRequest.builder().target(lInfo).connectTimeout(10).reason(ServerConnectEvent.Reason.PLUGIN).callback((result, error) -> {
                                        if (error != null || result == ServerConnectRequest.Result.FAIL) {
                                            failSet.add(lInfo.getName());

                                            checkServer(failSet.toArray(new String[]{}));
                                        }
                                    }).build());
                                    break;
                                }
                            } while (lobbies.hasNext());
                        }
                    }
                } else {
                    if (!Proxy.inAuth(player) && Proxy.authsValid()) {
                        Iterator<ServerInfo> auths = proxy.authServer(ServerInfo.class);
                        if (auths.hasNext()) {
                            do {
                                ServerInfo aInfo = auths.next();
                                if (!failSet.contains(aInfo.getName())) {
                                    player.connect(ServerConnectRequest.builder().target(aInfo).connectTimeout(10).reason(ServerConnectEvent.Reason.PLUGIN).callback((result, error) -> {
                                        if (error != null || result == ServerConnectRequest.Result.FAIL) {
                                            failSet.add(aInfo.getName());

                                            checkServer(failSet.toArray(new String[]{}));
                                        }
                                    }).build());
                                    break;
                                }
                            } while (auths.hasNext());
                        }
                    }
                }
            }
        }
    }

    /**
     * Perform the command as the player
     *
     * @param command the command to perform
     */
    public final void performCommand(final String command) {
        if (plugin.getProxy().getDisabledCommands().stream().noneMatch(command::contains))
            plugin.getProxy().getPluginManager().dispatchCommand(player, command);
    }

    /**
     * Kick the player with the specified reason
     *
     * @param reason the reason of the kick
     */
    public synchronized final void kick(final String reason) {
        plugin.getProxy().getScheduler().schedule(plugin, () -> {
            String[] parsed = parseMessage(reason);

            if (parsed.length > 1) {
                StringBuilder kickBuilder = new StringBuilder();
                for (String string : parsed)
                    kickBuilder.append(string).append("\n");

                player.disconnect(TextComponent.fromLegacyText(StringUtils.toColor(StringUtils.replaceLast(kickBuilder.toString(), "\n", ""))));
            } else {
                player.disconnect(TextComponent.fromLegacyText(StringUtils.toColor(parsed[0])));
            }
        }, 0, TimeUnit.SECONDS);
    }

    /**
     * Apply the session potion effect
     * types
     */
    public synchronized final void applySessionEffects() {
        PluginConfiguration config = CurrentPlatform.getConfiguration();
        MessageDataBuilder builder = getBuilder(DataType.EFFECTS, CHANNEL_PLAYER);
        if (isRegistered()) {
            LoginConfig login = config.loginOptions();

            builder.addBoolData(login.blindEffect());
        } else {
            RegisterConfig register = config.registerOptions();

            builder.addBoolData(register.blindEffect());
        }

        DataSender.send(player, builder.build());
    }

    /**
     * Restore the player potion effects
     */
    public synchronized final void restorePotionEffects() {
        MessageData data = getBuilder(DataType.EFFECTS, CHANNEL_PLAYER)
                .addBoolData(false).build();
        DataSender.send(player, data);
    }

    /**
     * Get the current player account manager
     *
     * @return the player account manager
     */
    @NotNull
    public final AccountManager getManager() {
        return manager;
    }

    /**
     * Get the current player session
     *
     * @return the player session
     */
    @NotNull
    public final ClientSession getSession() {
        return sessions.get(player.getUniqueId());
    }

    /**
     * Get the user google auth token factory
     *
     * @return the user google auth token
     * factory
     */
    public final GoogleAuthFactory getTokenFactory() {
        return new GoogleAuthFactory(player.getUniqueId(), StringUtils.toColor(player.getDisplayName()));
    }

    /**
     * Check if the user is registered or not
     *
     * @return if the user is registered
     */
    public final boolean isRegistered() {
        AccountManager manager = getManager();
        String password = manager.getPassword();

        return !password.replaceAll("\\s", "").isEmpty();
    }

    /**
     * Check if the user has the specified permission
     *
     * @param permission the permission
     * @return if the player has the permission
     */
    public final boolean hasPermission(final Permission permission) {
        return permission.isPermissible(player, permission);
    }

    /**
     * Parse the message, replacing "{newline}"
     * with "\n"
     *
     * @return the parsed message
     */
    private String[] parseMessage(final String official) {
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        if (official.contains("{newline}")) {
            String messageData = official.replace("{newline}", "\n");
            String[] messages = messageData.split("\n");

            for (int i = 0; i < messages.length; i++) {
                String message = messages[i];

                ClientSession session = getSession();
                if (session.isCaptchaLogged()) {
                    message = message.replace("{captcha}", "")
                            .replace("<captcha>", "");
                } else {
                    message = message.replace("{captcha}", "<captcha>");
                }
                message = message
                        .replace("{player}", StringUtils.stripColor(player.getDisplayName()))
                        .replace("{ServerName}", config.serverName());

                messages[i] = message;
            }

            return messages;
        } else {
            ClientSession session = getSession();

            return new String[]{official
                    .replace("{captcha}", (session.isCaptchaLogged() ? "" : "<captcha>"))
                    .replace("<captcha>", (session.isCaptchaLogged() ? "" : "<captcha>"))
                    .replace("{player}", StringUtils.stripColor(player.getDisplayName()))
                    .replace("{ServerName}", config.serverName())};
        }
    }
}
