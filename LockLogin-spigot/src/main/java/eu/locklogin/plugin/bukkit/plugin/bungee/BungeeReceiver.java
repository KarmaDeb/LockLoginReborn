package eu.locklogin.plugin.bukkit.plugin.bungee;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.common.utils.Channel;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.common.web.services.socket.SocketClient;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.plugin.license.License;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.Main;
import eu.locklogin.plugin.bukkit.TaskTarget;
import eu.locklogin.plugin.bukkit.listener.data.TransientMap;
import eu.locklogin.plugin.bukkit.plugin.bungee.data.BungeeDataStorager;
import eu.locklogin.plugin.bukkit.plugin.bungee.data.MessagePool;
import eu.locklogin.plugin.bukkit.util.files.Config;
import eu.locklogin.plugin.bukkit.util.files.data.LastLocation;
import eu.locklogin.plugin.bukkit.util.files.data.Spawn;
import eu.locklogin.plugin.bukkit.util.inventory.AltAccountsInventory;
import eu.locklogin.plugin.bukkit.util.inventory.PinInventory;
import eu.locklogin.plugin.bukkit.util.inventory.PlayersInfoInventory;
import eu.locklogin.plugin.bukkit.util.player.User;
import io.socket.client.Ack;
import io.socket.client.Socket;
import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.utils.enums.Level;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static eu.locklogin.plugin.bukkit.LockLogin.*;

@SuppressWarnings("UnstableApiUsage")
public final class BungeeReceiver implements PluginMessageListener {

    private static final Map<String, AccountManager> accounts = new ConcurrentHashMap<>();

    static String proxy_com = "";
    static SocketClient client;
    static boolean usesSocket = false;

    public final static Map<UUID, UUID> proxies_map = new ConcurrentHashMap<>();

    private static boolean use_socket = false;

    /**
     * Initialize the bungee receiver
     */
    public BungeeReceiver(final SocketClient socket) {
        client = socket;

        try {
            License license = CurrentPlatform.getLicense();
            if (license != null) {
                proxy_com = license.comKey();
            } else {
                plugin.console().send("IMPORTANT! Please synchronize this server with your proxy license (/locklogin sync) or install a license (/locklogin install) and synchronize the installed license with your network to enable LockLogin BungeeCord", Level.GRAVE);
            }

            BungeeDataStorager storager = new BungeeDataStorager();
            storager.setProxyKey(proxy_com);
        } catch (Throwable ex) {
            logger.scheduleLog(Level.GRAVE, ex);
            plugin.console().send("Failed to initialize bungeecord data receiver", Level.GRAVE);
        }

        MessagePool.whenValid((ch, pl, b_Input) -> {
            try {
                String raw = b_Input.readUTF();
                Gson gson = new GsonBuilder().create();

                JsonObject json = gson.fromJson(raw, JsonObject.class);
                Socket client = socket.client();

                if (client.connected() && json.has("server")) {
                    registerUnder(json.get("server").getAsString(), socket);
                } else {
                    boolean canRead = true;
                    if (!ch.equalsIgnoreCase("ll:access")) {
                        String token = json.get("key").getAsString();
                        canRead = token.equals(proxy_com);
                    }

                    if (canRead) {
                        switch (ch.toLowerCase()) {
                            case "ll:account":
                                processAccountCommand(pl, json);
                                break;
                            case "ll:plugin":
                                processPluginCommand(pl, json);
                                break;
                            case "ll:access":
                                processAccessCommand(json);
                                break;
                            default:
                                //Do nothing
                                break;
                        }
                    }
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
                logger.scheduleLog(Level.GRAVE, ex);
                logger.scheduleLog(Level.INFO, "Failed to read bungeecord message");
            }
        });
    }

    /**
     * Register the received under the specified server name
     *
     * @param server the server name
     * @param socket the socket instance
     */
    public void registerUnder(final String server, final SocketClient socket) {
        Gson gson = new GsonBuilder().create();
        Socket client = socket.client();

        License license = CurrentPlatform.getLicense();
        if (license != null) {
            JsonObject message = new JsonObject();
            message.addProperty("name", server);

            try {
                byte[] license_data = Files.readAllBytes(license.getLocation().resolve("license.dat"));
                client.emit("init", message, license_data, (Ack) (ackData) -> {
                    //for (Object o : data) console.send("{0}", Level.INFO, o);
                    try {
                        JsonObject ackResponse = gson.fromJson(String.valueOf(ackData[0]), JsonObject.class);
                        boolean success = ackResponse.get("success").getAsBoolean();
                        String response_message = ackResponse.get("message").getAsString();

                        if (success) {
                            plugin.console().send("Successfully registered this server", Level.INFO);
                            use_socket = true;

                            client.on("proxy_leave", (data) -> {
                                try {
                                    JsonObject j = gson.fromJson(data[0].toString(), JsonObject.class);

                                    plugin.console().send("Disconnected from proxy {0} ({1})", Level.GRAVE, j.get("server").getAsString(), j.get("cause").getAsString());
                                } catch (Throwable ex) {
                                    plugin.console().send("Some of the proxies you were connected with went offline", Level.WARNING);
                                }
                            });

                            client.on("proxy_alive", (name) -> plugin.console().send("Some of the proxies that disconnected ({0}) is online again", Level.INFO, name[0]));

                            client.on("in", (data) -> {
                                JsonObject response = gson.fromJson(String.valueOf(data[0]), JsonObject.class);
                                String ch_name = response.get("channel").getAsString();

                                Channel channel = Channel.fromName(ch_name);
                                if (channel != null) {
                                    JsonObject message_data = response.get("data").getAsJsonObject();
                                    message_data.addProperty("data_type", response.get("type").getAsString());
                                    int id = response.get("id").getAsInt();

                                    client.emit("confirm", id);

                                    Player player = null;
                                    if (response.has("player")) {
                                        player = plugin.getServer().getPlayer(UUID.fromString(response.get("player").getAsString()));
                                    } else {
                                        if (message_data.has("player")) {
                                            player = plugin.getServer().getPlayer(UUID.fromString(message_data.get("player").getAsString()));
                                        }
                                    }

                                    switch (channel) {
                                        case ACCOUNT:
                                            processAccountCommand(player, message_data);
                                            break;
                                        case PLUGIN:
                                            processPluginCommand(player, message_data);
                                            break;
                                        case ACCESS:
                                            break;
                                    }
                                }
                            });
                        } else {
                            console.send("LockLogin web services denied our connection ({0})", Level.GRAVE, response_message);
                        }
                    } catch (Throwable ex) {
                        logger.scheduleLog(Level.GRAVE, ex);
                    }
                });
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        } else {
            console.send("Cannot register this server because its license is invalid", Level.GRAVE);
        }
    }

    /**
     * Listens for incoming plugin messages
     *
     * @param channel the channel
     * @param player  the player used to send the message
     * @param bytes   the message bytes
     */
    @Override
    public void onPluginMessageReceived(@NotNull String channel, @Nullable Player player, byte[] bytes) {
        if (!use_socket) {
            ByteArrayDataInput input = ByteStreams.newDataInput(bytes);
            String json_raw = input.readUTF();

            Gson gson = new GsonBuilder().create();

            JsonObject bungee_data = gson.fromJson(json_raw, JsonObject.class);
            UUID id = (player != null ? player.getUniqueId() : UUID.randomUUID());
            if (bungee_data.has("player")) {
                Player tmp = plugin.getServer().getPlayer(UUID.fromString(bungee_data.get("player").getAsString()));
                if (tmp != null) {
                    id = tmp.getUniqueId();
                }
            }

            ByteArrayDataOutput i_copy = ByteStreams.newDataOutput();
            i_copy.writeUTF(json_raw);
            input = ByteStreams.newDataInput(i_copy.toByteArray());

            if (channel.equals("ll:access")) {
                MessagePool.trigger(channel, plugin.getServer().getPlayer(id), input); //We want an instant read for these
            } else {
                MessagePool.addPlayer(channel, id, input);
            }
        }
    }

    /**
     * Process the ll:account message
     *
     * @param player the player that involves this message
     * @param bungee_data the bungeecord data
     */
    private void processAccountCommand(final Player player, final JsonObject bungee_data) {
        DataType sub = DataType.valueOf(bungee_data.get("data_type").getAsString());

        PluginConfiguration config = CurrentPlatform.getConfiguration();

        if (player != null && player.isOnline()) {
            User user = new User(player);
            ClientSession session = user.getSession();

            switch (sub) {
                case JOIN:
                    if (!user.isLockLoginUser()) {
                        user.applyLockLoginUser();
                    }

                    if (!session.isValid()) {
                        //Validate BungeeCord/Velocity session
                        BungeeSender.validatePlayer(player);
                        session.validate();
                    }

                    if (config.enableSpawn()) {
                        trySync(TaskTarget.TELEPORT, () -> player.teleport(player.getWorld().getSpawnLocation()));

                        Spawn spawn = new Spawn(player.getWorld());
                        spawn.load().whenComplete(() -> spawn.teleport(player));
                    }

                    session.setLogged(bungee_data.get("pass_login").getAsBoolean());
                    session.set2FALogged(bungee_data.get("2fa_login").getAsBoolean());
                    session.setPinLogged(bungee_data.get("pin_login").getAsBoolean());
                    user.setRegistered(bungee_data.get("registered").getAsBoolean());

                    //proxies_map.put(player.getUniqueId(), UUID.fromString(emitter));
                    break;
                case QUIT:
                    if (user.isLockLoginUser()) {
                        //Last location will be always saved since if the server
                        //owner wants to enable it, it would be good to see
                        //the player last location has been stored to avoid
                        //location problems
                        LastLocation last_loc = new LastLocation(player);
                        last_loc.save();

                        session.invalidate();
                        session.setLogged(false);
                        session.setPinLogged(false);
                        session.set2FALogged(false);

                        user.removeLockLoginUser();
                    }

                    user.setTempSpectator(false);
                    proxies_map.remove(player.getUniqueId());
                    break;
                case VALIDATION:
                    session.validate();
                    break;
                case CAPTCHA:
                    if (!session.isCaptchaLogged()) {
                        session.setCaptchaLogged(true);
                    }
                    break;
                case SESSION:
                    if (!session.isLogged()) {
                        session.setLogged(true);
                    }

                    if (config.takeBack()) {
                        LastLocation location = new LastLocation(player);
                        location.teleport();
                    }
                    break;
                case PIN:
                    BungeeDataStorager storager = new BungeeDataStorager();

                    if (bungee_data.get("pin").getAsBoolean()) {
                        if (!session.isPinLogged()) {
                            plugin.sync().queue("open_pin", () -> {
                                PinInventory pin = new PinInventory(player);
                                pin.open();
                            });
                        }

                        storager.setPinConfirmation(player, true);
                    } else {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            session.setPinLogged(true);
                            storager.setPinConfirmation(player, false);

                            InventoryView view = player.getOpenInventory();
                            PluginMessages messages = CurrentPlatform.getMessages();

                            if (StringUtils.stripColor(view.getTitle()).equals(StringUtils.stripColor(messages.pinTitle()))) {
                                PinInventory inventory = new PinInventory(player);
                                inventory.close();
                            }
                        });
                    }
                    break;
                case GAUTH:
                    session.set2FALogged(true);
                    TransientMap.apply(player);
                    break;
                case CLOSE:
                    if (session.isLogged()) {
                        session.setLogged(false);
                        session.set2FALogged(false);
                        session.setPinLogged(false);
                    }

                    break;
                case EFFECTS:
                    boolean status = bungee_data.get("effects").getAsBoolean();

                    if (status) {
                        user.savePotionEffects();
                        user.applySessionEffects();
                    } else {
                        user.restorePotionEffects();
                    }

                    break;
                case INVALIDATION:
                    session.invalidate();
                    break;
                default:
                    logger.scheduleLog(Level.GRAVE, "Unknown account sub channel message: " + sub);
                    break;
            }
        }
    }

    private void processPluginCommand(final Player player, final JsonObject bungee_data) {
        DataType sub = DataType.valueOf(bungee_data.get("data_type").getAsString());
        BungeeDataStorager storager = new BungeeDataStorager();
        String id;
        if (bungee_data.has("from")) {
            id = bungee_data.get("from").getAsString();
        } else {
            id = bungee_data.get("proxy").getAsString();
        }

        switch (sub) {
            case LOGGED:
                storager.setLoggedAccounts(bungee_data.get("login_count").getAsInt());
                break;
            case REGISTERED:
                storager.setRegisteredAccounts(bungee_data.get("register_count").getAsInt());
                break;
            case INFOGUI:
                String infoMessage = bungee_data.get("player_info").getAsString();

                String[] infoData = infoMessage.split(";");
                Set<AccountManager> infoAccounts = new HashSet<>();
                for (String str : infoData) {
                    AccountManager manager = accounts.getOrDefault(str.replace("-", "").toLowerCase(), null);

                    if (manager != null) {
                        infoAccounts.add(manager);
                    }
                }

                new PlayersInfoInventory(infoAccounts, player);
                break;
            case LOOKUPGUI:
                String lookupMessage = bungee_data.get("player_info").getAsString();

                String[] lookupData = lookupMessage.split(";");
                Set<AccountManager> lookupAccounts = new HashSet<>();
                for (String str : lookupData) {
                    AccountManager manager = accounts.getOrDefault(str.replace("-", "").toLowerCase(), null);

                    if (manager != null) {
                        lookupAccounts.add(manager);
                    }
                }

                new AltAccountsInventory(lookupAccounts, player);
                break;
            case PLAYER:
                String serialized = bungee_data.get("account").getAsString();

                AccountManager manager = StringUtils.loadUnsafe(serialized);
                if (manager != null) {
                    if (!accounts.containsKey(manager.getUUID().getId().replace("-", "").toLowerCase())) {
                        console.send("Stored temp account of client {0}", Level.INFO, manager.getName());
                    } else {
                        console.send("Updated temp account of client {0}", Level.INFO, manager.getName());
                    }

                    accounts.put(manager.getUUID().getId().replace("-", "").toLowerCase(), manager);
                    BungeeSender.sendPlayerInstance(serialized, id);
                } else {
                    console.send("Received null serialized player account from proxy with id {0}", Level.INFO, id);
                }
                break;
            case MESSAGES:
                String messageString = bungee_data.get("raw").getAsString();
                CurrentPlatform.getMessages().loadString(messageString);
                break;
            case CONFIG:
                String configString = bungee_data.get("raw").getAsString();
                Config.manager.loadBungee(configString);
                break;
            case LISTENER:
                break;
            default:
                logger.scheduleLog(Level.GRAVE, "Unknown plugin sub channel message: " + sub);
                break;
        }
    }

    private void processAccessCommand(final JsonObject bungee_data) {
        DataType sub = DataType.valueOf(bungee_data.get("data_type").getAsString());
        BungeeDataStorager storager = new BungeeDataStorager();
        UUID id = UUID.fromString(bungee_data.get("proxy").getAsString());
        String proxyKey = bungee_data.get("key").getAsString();
        String serverName = bungee_data.get("server").getAsString();

        if (sub.equals(DataType.REGISTER)) { //PREVIOUSLY: KEY
            //This also checks if the proxy key is empty, retuning true if it is
            //so we will just use it as a "emptyProxyOwner" check
            if (storager.isProxyKey(proxyKey)) {
                storager.setServerName(serverName);
                storager.setProxyKey(proxyKey);
                BungeeSender.sendProxyStatus(sub.name().toLowerCase());
            } else {
                //BungeeSender.sendProxyStatus(sub.name().toLowerCase());
                logger.scheduleLog(Level.GRAVE, "Proxy with id {0} tried to register a key but the key is already registered and the specified one is incorrect", id.toString());
            }
        }
    }
}
