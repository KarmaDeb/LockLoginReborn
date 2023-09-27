package eu.locklogin.plugin.bungee.util.player;

/*
 * Private GSA code
 *
 * The use of this code
 * without GSA team authorization
 * will be a violation of
 * terms of use determined
 * in <a href="http://karmaconfigs.cf/license/"> here </a>
 * or (fallback domain) <a href="https://karmaconfigs.github.io/page/license"> here </a>
 */

import eu.locklogin.api.account.AccountID;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.common.security.google.GoogleAuthFactory;
import eu.locklogin.api.common.session.SessionCheck;
import eu.locklogin.api.common.utils.Channel;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.common.utils.other.name.AccountNameDatabase;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.file.ProxyConfiguration;
import eu.locklogin.api.module.plugin.api.event.user.SessionInitializationEvent;
import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.client.permission.PermissionObject;
import eu.locklogin.api.module.plugin.client.permission.plugin.PluginPermissions;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import eu.locklogin.api.util.enums.ManagerType;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bungee.BungeeSender;
import eu.locklogin.plugin.bungee.com.message.DataMessage;
import eu.locklogin.plugin.bungee.plugin.Manager;
import eu.locklogin.plugin.bungee.util.files.Proxy;
import ml.karmaconfigs.api.bungee.makeiteasy.BossMessage;
import ml.karmaconfigs.api.bungee.makeiteasy.TitleMessage;
import ml.karmaconfigs.api.common.karma.source.APISource;
import ml.karmaconfigs.api.common.karma.source.KarmaSource;
import ml.karmaconfigs.api.common.minecraft.boss.BossColor;
import ml.karmaconfigs.api.common.minecraft.boss.BossProvider;
import ml.karmaconfigs.api.common.minecraft.boss.ProgressiveBar;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.utils.enums.Level;
import net.md_5.bungee.api.ServerConnectRequest;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static eu.locklogin.plugin.bungee.LockLogin.*;

/**
 * Initialize the user
 */
public final class User {

    private final static KarmaSource lockLogin = APISource.loadProvider("LockLogin");
    private final static Map<UUID, eu.locklogin.api.account.AccountManager> managers = new ConcurrentHashMap<>();
    private final static Map<UUID, SessionCheck<ProxiedPlayer>> sessionChecks = new ConcurrentHashMap<>();
    @SuppressWarnings("FieldMayBeFinal") //This could be modified by the cache loader, so it can't be final
    private static Map<UUID, ClientSession> sessions = new ConcurrentHashMap<>();
    private final static Set<UUID> premium_users = Collections.newSetFromMap(new ConcurrentHashMap<>());

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

        User loaded = UserDatabase.loadUser(player);
        if (loaded == null) {
            if (CurrentPlatform.isValidAccountManager()) {
                eu.locklogin.api.account.AccountManager manager = CurrentPlatform.getAccountManager(ManagerType.CUSTOM, AccountID.fromUUID(player.getUniqueId()));

                if (manager == null) {
                    throw new IllegalStateException("Cannot initialize user with a null player account manager");
                } else {
                    AccountNameDatabase database = new AccountNameDatabase(player.getUniqueId());
                    lockLogin.async().queue("user_fetch", () -> {
                        database.assign(StringUtils.stripColor(player.getName()));
                        database.assign(StringUtils.stripColor(player.getDisplayName()));
                    });

                    //Try to fix the empty manager values that are
                    //required
                    if (manager.exists()) {
                        String name = manager.getName();
                        AccountID id = manager.getUUID();

                        if (StringUtils.isNullOrEmpty(name))
                            manager.setName(StringUtils.stripColor(player.getDisplayName()));

                        if (StringUtils.isNullOrEmpty(id.getId()))
                            manager.saveUUID(AccountID.fromUUID(player.getUniqueId()));
                    }

                    managers.put(player.getUniqueId(), manager);
                }
            } else {
                throw new IllegalStateException("Cannot initialize user with an invalid player account manager");
            }

            if (!sessions.containsKey(player.getUniqueId())) {
                if (CurrentPlatform.isValidSessionManager()) {
                    ClientSession session = CurrentPlatform.getSessionManager(new Class[]{AccountID.class}, AccountID.fromUUID(player.getUniqueId()));

                    if (session == null) {
                        throw new IllegalStateException("Cannot initialize user with a null session manager");
                    } else {
                        session.initialize();

                        ModulePlayer modulePlayer = new ModulePlayer(
                                player.getName(),
                                player.getUniqueId(),
                                session,
                                managers.get(player.getUniqueId()),
                                getIp(player.getSocketAddress()));
                        CurrentPlatform.connectPlayer(modulePlayer, player);

                        Event event = new SessionInitializationEvent(modulePlayer, session, null);
                        ModulePlugin.callEvent(event);

                        sessions.put(player.getUniqueId(), session);
                    }
                } else {
                    throw new IllegalStateException("Cannot initialize user with a null session manager");
                }
            }

            UserDatabase.insert(player, this);
        } else {
            ModulePlayer modulePlayer = CurrentPlatform.getServer().getPlayer(player.getUniqueId());
            if (modulePlayer == null || modulePlayer.getAddress() == null) {
                InetAddress ip = getIp(player.getSocketAddress());

                modulePlayer = new ModulePlayer(
                        player.getName(),
                        player.getUniqueId(),
                        sessions.get(player.getUniqueId()),
                        managers.get(player.getUniqueId()),
                        ip);
                CurrentPlatform.connectPlayer(modulePlayer, player);
            }
        }
    }

    /**
     * Set the user premium status
     *
     * @param status the user premium status
     */
    public void setPremium(final boolean status) {
        if (status) {
            premium_users.add(player.getUniqueId());
        } else {
            premium_users.remove(player.getUniqueId());
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
    public static eu.locklogin.api.account.AccountManager getManager(final ProxiedPlayer player) {
        return managers.get(player.getUniqueId());
    }

    /**
     * Send a message to the player
     *
     * @param message the message to send
     */
    public void send(final String message) {
        String[] parsed = parseMessage(message);

        PluginMessages messages = CurrentPlatform.getMessages();

        if (parsed.length > 1) {
            for (String str : parsed)
                player.sendMessage(TextComponent.fromLegacyText(StringUtils.toColor(str)));
        } else {
            //Make sure to avoid null messages
            if (parsed.length == 1) {
                if (!StringUtils.isNullOrEmpty(parsed[0].replace(messages.prefix(), ""))) {
                    player.sendMessage(TextComponent.fromLegacyText(StringUtils.toColor(parsed[0])));
                }
            }
        }
    }

    /**
     * Send a message to the player
     *
     * @param message the message to send
     */
    public void send(final TextComponent message) {
        String[] text = parseMessage(message.getText());
        StringBuilder builder = new StringBuilder();
        for (String str : text) builder.append(str);

        message.setText(builder.toString());

        player.sendMessage(message);
    }

    /**
     * Send a title and subtitle to the player
     *
     * @param title    the title to send
     * @param subtitle the subtitle to send
     * @param si the time to show in
     * @param ki the time keep in
     * @param hi the time to hide in
     */
    public void send(final String title, final String subtitle, final int si, final int ki, final int hi) {
        String[] tmpTitle = parseMessage(title);
        String[] tmpSub = parseMessage(subtitle);

        StringBuilder titleBuilder = new StringBuilder();
        StringBuilder subtitleBuilder = new StringBuilder();

        for (String str : tmpTitle) titleBuilder.append(str).append(" ");
        for (String str : tmpSub) subtitleBuilder.append(str).append(" ");

        TitleMessage titleMessage = new TitleMessage(player,
                StringUtils.replaceLast(titleBuilder.toString(), " ", ""),
                StringUtils.replaceLast(subtitleBuilder.toString(), " ", ""));
        titleMessage.send(si, ki, hi);
    }

    /**
     * Check the player server
     *
     * @param index the server try index
     */
    public void sendToPremium(final int index) {
        ProxyConfiguration proxy = CurrentPlatform.getProxyConfiguration();
        if (proxy.sendToServers()) {
            ClientSession session = getSession();

            if (session.isValid() && !Proxy.inPremium(player)) {
                List<ServerInfo> premium = proxy.premiumServer(ServerInfo.class);
                if (premium.size() > index) {
                    ServerInfo lInfo = premium.get(index);
                    player.connect(ServerConnectRequest.builder().target(lInfo).connectTimeout(10).reason(ServerConnectEvent.Reason.PLUGIN).callback((result, error) -> {
                        if (error != null || result == ServerConnectRequest.Result.FAIL) {
                            sendToPremium(index + 1);

                            if (error != null) {
                                logger.scheduleLog(Level.GRAVE, error);
                            }
                            logger.scheduleLog(Level.INFO, "Failed to connect client {0} to server {1}", player.getUniqueId(), lInfo.getName());
                        }
                    }).build());
                }
            }
        }
    }

    /**
     * Check the player server
     *
     * @param index the server try index
     * @param auth was this performed after auth?
     */
    public void checkServer(final int index, final boolean auth) {
        ProxyConfiguration proxy = CurrentPlatform.getProxyConfiguration();
        if (proxy.sendToServers()) {
            ClientSession session = getSession();
            if (session.isLogged() && session.isTempLogged()) {
                PluginConfiguration config = CurrentPlatform.getConfiguration();
                boolean premium = config.enablePremium() && premium_users.contains(player.getUniqueId());

                if (Proxy.inAuth(player)) {
                    if (!hasPermission(PluginPermissions.join_limbo()) || auth) {
                        List<ServerInfo> lobbies;
                        if (premium) {
                            lobbies = proxy.premiumServer(ServerInfo.class);
                        } else {
                            lobbies = proxy.lobbyServers(ServerInfo.class);
                        }
                        if (lobbies.size() > index) {
                            ServerInfo tmpInfo = lobbies.get(index);
                            player.connect(ServerConnectRequest.builder().target(tmpInfo).reason(ServerConnectEvent.Reason.PLUGIN).callback((result, error) -> {
                                if (error != null || !result.equals(ServerConnectRequest.Result.SUCCESS)) {
                                    checkServer(index + 1, auth);

                                    if (error != null) {
                                        logger.scheduleLog(Level.GRAVE, error);
                                    }
                                    logger.scheduleLog(Level.INFO, "Failed to connect client {0} to server {1}", player.getUniqueId(), tmpInfo.getName());
                                }
                            }).build());
                        }
                    }
                }
            }

            /*if (session.isValid()) {
                if (session.isLogged() && session.isTempLogged()) {
                    boolean redirect = false;
                    boolean premium = false;
                    if (Proxy.inAuth(player)) {
                        if (Proxy.lobbiesValid() && !hasPermission(PluginPermissions.join_limbo()))
                    }

                    if (Proxy.inAuth(player)) {
                        redirect = Proxy.lobbiesValid() && !hasPermission(PluginPermissions.join_limbo());
                    } else {
                        if (Proxy.inPremium(player) && !Proxy.inLobby(player)) {
                            PluginConfiguration config = CurrentPlatform.getConfiguration();
                            redirect = config.enablePremium() && !premium_users.contains(player.getUniqueId());
                        }
                    }

                    if (redirect) {
                        premium = premium_users.contains(player.getUniqueId());

                        if (!premium && Proxy.inLobby(player)) {
                            redirect = false;
                        }
                    }

                    if (redirect) {
                        List<ServerInfo> lobbies = (premium ? proxy.premiumServer(ServerInfo.class) : proxy.lobbyServers(ServerInfo.class));
                        if (lobbies.size() > index) {
                            ServerInfo lInfo = lobbies.get(index);
                            player.connect(ServerConnectRequest.builder().target(lInfo).connectTimeout(10).reason(ServerConnectEvent.Reason.PLUGIN).callback((result, error) -> {
                                if (error != null || result == ServerConnectRequest.Result.FAIL) {
                                    checkServer(index + 1);

                                    if (error != null) {
                                        logger.scheduleLog(Level.GRAVE, error);
                                    }
                                    logger.scheduleLog(Level.INFO, "Failed to connect client {0} to server {1}", player.getUniqueId(), lInfo.getName());
                                }
                            }).build());
                        }
                    }
                } else {
                    if (!Proxy.inAuth(player) && Proxy.authsValid()) {
                        List<ServerInfo> auths = proxy.authServer(ServerInfo.class);
                        if (auths.size() > index) {
                            ServerInfo aInfo = auths.get(index);
                            player.connect(ServerConnectRequest.builder().target(aInfo).connectTimeout(10).reason(ServerConnectEvent.Reason.PLUGIN).callback((result, error) -> {
                                if (error != null || result == ServerConnectRequest.Result.FAIL) {
                                    checkServer(index + 1);

                                    if (error != null) {
                                        logger.scheduleLog(Level.GRAVE, error);
                                    }
                                    logger.scheduleLog(Level.INFO, "Failed to connect client {0} to server {1}", player.getUniqueId(), aInfo.getName());
                                }
                            }).build());
                        }
                    }
                }
            }*/
        }
    }

    /**
     * Perform the command as the player
     *
     * @param command the command to perform
     */
    public void performCommand(final String command) {
        if (plugin.getProxy().getDisabledCommands().stream().noneMatch(command::contains))
            plugin.getProxy().getPluginManager().dispatchCommand(player, command);
    }

    /**
     * Kick the player with the specified reason
     *
     * @param reason the reason of the kick
     */
    public synchronized void kick(final String reason) {
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
    public synchronized void applySessionEffects() {
        Manager.sender.queue(BungeeSender.serverFromPlayer(player)).insert(DataMessage.newInstance(DataType.EFFECTS, Channel.ACCOUNT, player)
                .addProperty("effects", true).getInstance().build());
    }

    /**
     * Restore the player potion effects
     */
    public synchronized void restorePotionEffects() {
        Manager.sender.queue(BungeeSender.serverFromPlayer(player)).insert(DataMessage.newInstance(DataType.EFFECTS, Channel.ACCOUNT, player)
                .addProperty("effects", false).getInstance().build());
    }

    /**
     * Remove the user session check
     */
    public void removeSessionCheck() {
        SessionCheck<ProxiedPlayer> check = sessionChecks.remove(player.getUniqueId());
        check.cancelCheck("Check cancelled");
    }

    /**
     * Get the client session checker
     *
     * @return the client session checker
     */
    public SessionCheck<ProxiedPlayer> getChecker() {
        SessionCheck<ProxiedPlayer> checker = sessionChecks.getOrDefault(player.getUniqueId(), null);
        if (checker == null) {
            ModulePlayer sender = getModule();
            if (sender == null) {
                sender = new ModulePlayer(
                        player.getName(),
                        player.getUniqueId(),
                        getSession(),
                        managers.get(player.getUniqueId()),
                        getIp(player.getSocketAddress()));

                CurrentPlatform.connectPlayer(sender, player);
            }

            /*
            So... there was a bug in where sometimes the boss bar would be visible
            even while disabled, that's because I was literally creating the boss
            bar ignoring that option, and then hiding it if disabled.

            The best solution is to just not create it if specified
             */
            BossProvider<ProxiedPlayer> message = null;
            int time = CurrentPlatform.getConfiguration().registerOptions().timeOut();
            if (getManager().isRegistered()) {
                time = CurrentPlatform.getConfiguration().loginOptions().timeOut();

                if (CurrentPlatform.getConfiguration().loginOptions().hasBossBar()) {
                    message = new BossMessage(plugin, CurrentPlatform.getMessages().loginBar("&a", time), time)
                            .color(BossColor.GREEN).progress(ProgressiveBar.DOWN);
                }
            } else {
                if (CurrentPlatform.getConfiguration().registerOptions().hasBossBar()) {
                    message = new BossMessage(plugin, CurrentPlatform.getMessages().registerBar("&a", time), time)
                            .color(BossColor.GREEN).progress(ProgressiveBar.DOWN);
                }
            }

            checker = new SessionCheck<>(plugin, sender, message);
            sessionChecks.put(player.getUniqueId(), checker);
        }

        return checker;
    }

    /**
     * Get the module player of this player
     *
     * @return this player module player
     */
    public ModulePlayer getModule() {
        return CurrentPlatform.getServer().getPlayer(player.getUniqueId());
    }

    /**
     * Get the current player account manager
     *
     * @return the player account manager
     */
    @NotNull
    public eu.locklogin.api.account.AccountManager getManager() {
        return managers.get(player.getUniqueId());
    }

    /**
     * Get the current player session
     *
     * @return the player session
     */
    @NotNull
    public ClientSession getSession() {
        return sessions.get(player.getUniqueId());
    }

    /**
     * Get the user google auth token factory
     *
     * @return the user google auth token
     * factory
     */
    public GoogleAuthFactory getTokenFactory() {
        return new GoogleAuthFactory(player.getUniqueId());
    }

    /**
     * Check if the user has the specified permission
     *
     * @param permission the permission
     * @return if the player has the permission
     */
    public boolean hasPermission(final PermissionObject permission) {
        ModulePlayer player = getModule();
        return permission.isPermissible(player);
    }

    /**
     * Parse the message, replacing "{newline}"
     * with "\n"
     *
     * @return the parsed message
     */
    private String[] parseMessage(final String official) {
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        if (official.contains("{newline}") || official.contains("\\n") || official.contains("\n")) {
            String messageData = official
                    .replace("{newline}", "\n")
                    .replace("\\n", "\n");
            String[] messages = messageData.split("\\r?\\n");

            for (int i = 0; i < messages.length; i++) {
                String previous = (i - 1 >= 0 ? messages[i - 1] : "");
                String lastColor = StringUtils.getLastColor(previous);
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

                messages[i] = lastColor + message;
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

    public boolean isPremium() {
        return premium_users.contains(player.getUniqueId());
    }
}
