package eu.locklogin.plugin.bukkit.plugin.bungee;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.common.web.services.socket.SocketClient;
import eu.locklogin.api.encryption.CryptoFactory;
import eu.locklogin.api.encryption.HashType;
import eu.locklogin.api.encryption.Validation;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.TaskTarget;
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
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.utils.enums.Level;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static eu.locklogin.plugin.bukkit.LockLogin.*;

@SuppressWarnings("UnstableApiUsage")
public final class BungeeReceiver implements PluginMessageListener {

    private static final Map<String, AccountManager> accounts = new ConcurrentHashMap<>();
    private static boolean tryingConnect = false;
    private static boolean connected = false;
    private static boolean has_events = false;

    static String proxy_com = "";

    static SocketClient client;
    static boolean usesSocket = false;

    private final static Set<UUID> known_proxies = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public final static Map<UUID, UUID> proxies_map = new ConcurrentHashMap<>();

    /**
     * Initialize the bungee receiver
     */
    public BungeeReceiver(final SocketClient socket) {
        client = socket;

        MessagePool.whenValid((channel, player, b_Input) -> {
            try {
                String json_raw = b_Input.readUTF();
                Gson gson = new GsonBuilder().create();

                JsonObject bungee_data = gson.fromJson(json_raw, JsonObject.class);

                boolean socket_compatible = bungee_data.get("socket").getAsBoolean();
                String token = bungee_data.get("key").getAsString();
                UUID id = UUID.fromString(bungee_data.get("proxy").getAsString());
                String server = bungee_data.get("server").getAsString();

                boolean canRead = true;
                if (!channel.equalsIgnoreCase("ll:access")) {
                    canRead = /*TokenGen.matches(token, id.toString(), storager.getServerName())*/ CryptoFactory
                            .getBuilder().withPassword(token).withToken(proxy_com).build().validate(Validation.ALL);
                }

                if (socket_compatible) {
                    if (socket != null) {
                        Socket client = socket.client();

                        if (client.connected()) {
                            connected = true;

                            if (!known_proxies.contains(id) && canRead) {
                                console.send("Requesting to link with proxy", Level.INFO);

                                JsonObject request = new JsonObject();
                                request.addProperty("proxy", id.toString());
                                request.addProperty("name", server);

                                known_proxies.add(id);
                                tryingConnect = true;
                                client.emit("request", request, (Ack) (r2) -> {
                                    JsonObject r = gson.fromJson(String.valueOf(r2[0]), JsonObject.class);

                                    boolean connected = r.get("success").getAsBoolean();
                                    if (!connected) {
                                        console.send("Failed to connect to BungeeCord bridge server through web communication ({0}). Using alternative communications.", Level.GRAVE, r.get("message").getAsString());
                                    } else {
                                        usesSocket = true;
                                        console.send("Successfully connected to BungeeCord bridge through web communication. (Licensed: {0})", Level.OK, r.get("licensed").getAsBoolean());

                                        if (!has_events) {
                                            has_events = true;

                                            client.on("ll:account", (args) -> {
                                                JsonObject remote_bungee_data = gson.fromJson(String.valueOf(args[0]), JsonObject.class);

                                                Player remote_client = null;
                                                if (remote_bungee_data.has("player")) {
                                                    remote_client = plugin.getServer().getPlayer(UUID.fromString(remote_bungee_data.get("player").getAsString()));
                                                }
                                                if (remote_client != null && remote_client.isOnline()) {
                                                    processAccountCommand(remote_client, remote_bungee_data);

                                                    JsonObject res = new JsonObject();
                                                    res.addProperty("id", remote_bungee_data.get("msg_id").getAsInt());
                                                    client.emit("message", res);
                                                }
                                            });
                                            client.on("ll:plugin", (args) -> {
                                                JsonObject remote_bungee_data = gson.fromJson(String.valueOf(args[0]), JsonObject.class);

                                                Player remote_client = player;
                                                if (remote_bungee_data.has("player")) {
                                                    remote_client = plugin.getServer().getPlayer(UUID.fromString(remote_bungee_data.get("player").getAsString()));
                                                }
                                                if (remote_client != null && remote_client.isOnline()) {
                                                    processPluginCommand(remote_client, remote_bungee_data);

                                                    JsonObject res = new JsonObject();
                                                    res.addProperty("id", remote_bungee_data.get("msg_id").getAsInt());
                                                    client.emit("message", res);
                                                }
                                            });
                                            client.on("ll:access", (args) -> {
                                                JsonObject remote_bungee_data = gson.fromJson(String.valueOf(args[0]), JsonObject.class);

                                                //Do nothing but make the server know the message has been received
                                                JsonObject res = new JsonObject();
                                                res.addProperty("id", remote_bungee_data.get("msg_id").getAsInt());
                                                client.emit("message", res);
                                            });
                                        }
                                    }

                                    tryingConnect = false;
                                });
                            }
                        } else {
                            connected = false;
                        }
                    }
                }

                if (!connected && !tryingConnect) {
                    if (canRead) {
                        switch (channel.toLowerCase()) {
                            case "ll:account":
                                processAccountCommand(player, bungee_data);
                                break;
                            case "ll:plugin":
                                processPluginCommand(player, bungee_data);
                                break;
                            case "ll:access":
                                processAccessCommand(bungee_data);
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
     * Listens for incoming plugin messages
     *
     * @param channel the channel
     * @param player  the player used to send the message
     * @param bytes   the message bytes
     */
    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] bytes) {
        ByteArrayDataInput input = ByteStreams.newDataInput(bytes);
        String json_raw = input.readUTF();
        Gson gson = new GsonBuilder().create();

        JsonObject bungee_data = gson.fromJson(json_raw, JsonObject.class);
        UUID id = player.getUniqueId();
        if (bungee_data.has("player")) {
            Player tmp = plugin.getServer().getPlayer(UUID.fromString(bungee_data.get("player").getAsString()));
            if (tmp != null) {
                id = tmp.getUniqueId();
            }
        }

        ByteArrayDataOutput i_copy = ByteStreams.newDataOutput();
        i_copy.writeUTF(json_raw);
        input = ByteStreams.newDataInput(i_copy.toByteArray());

        MessagePool.addPlayer(channel, id, input);
    }

    /**
     * Process the ll:account message
     *
     * @param player the player that involves this message
     * @param bungee_data the bungeecord data
     */
    private void processAccountCommand(final Player player, final JsonObject bungee_data) {
        DataType sub = DataType.valueOf(bungee_data.get("data_type").getAsString());
        String emitter;
        if (bungee_data.has("from")) {
            emitter = bungee_data.get("from").getAsString();
        } else {
            emitter = bungee_data.get("proxy").getAsString();
        }

        PluginConfiguration config = CurrentPlatform.getConfiguration();
        User user = new User(player);
        ClientSession session = user.getSession();

        switch (sub) {
            case JOIN:
                if (!user.isLockLoginUser()) {
                    user.applyLockLoginUser();
                }

                if (!session.isValid()) {
                    //Validate BungeeCord/Velocity session
                    BungeeSender.validatePlayer(player, emitter);
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

                proxies_map.put(player.getUniqueId(), UUID.fromString(emitter));
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

    private void processPluginCommand(final Player player, final JsonObject bungee_data) {
        DataType sub = DataType.valueOf(bungee_data.get("data_type").getAsString());
        BungeeDataStorager storager = new BungeeDataStorager();
        UUID id;
        if (bungee_data.has("from")) {
            id = UUID.fromString(bungee_data.get("from").getAsString());
        } else {
            id = UUID.fromString(bungee_data.get("proxy").getAsString());
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
                    BungeeSender.sendPlayerInstance(serialized, id.toString());
                } else {
                    console.send("Received null serialized player account from proxy with id {0}", Level.INFO, id);
                }
                break;
            case MESSAGES:
                String messageString = bungee_data.get("messages_yml").getAsString();
                CurrentPlatform.getMessages().loadString(messageString);
                break;
            case CONFIG:
                String configString = bungee_data.get("config.yml").getAsString();
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
                proxy_com = CryptoFactory.getBuilder().withPassword(proxyKey).unsafe().hash(HashType.pickRandom(), true);
                BungeeSender.sendProxyStatus(sub.name().toLowerCase());
            } else {
                BungeeSender.sendProxyStatus(sub.name().toLowerCase());
                logger.scheduleLog(Level.GRAVE, "Proxy with id {0} tried to register a key but the key is already registered and the specified one is incorrect", id.toString());
            }
                /*case KEY: //PREVIOUSLY: REGISTER
                if (storager.isProxyKey(proxyKey)) {
                    BungeeSender.sendProxyStatus(sub.name().toLowerCase());
                } else {
                    BungeeSender.sendProxyStatus(sub.name().toLowerCase());
                    logger.scheduleLog(Level.GRAVE, "Proxy with id {0} to register itself with an invalid access key", id.toString());
                }
                break;*/
            /*case REMOVE:
                //This can be removed as proxy key is always the same
                if (storager.isProxyKey(proxyKey)) {
                    try {
                        JarManager.changeField(BungeeDataStorager.class, "proxyKey", "");
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                    }
                    BungeeSender.sendProxyStatus(sub.name().toLowerCase());
                } else {
                    BungeeSender.sendProxyStatus(sub.name().toLowerCase());
                    logger.scheduleLog(Level.GRAVE, "Tried to remove proxy with id {0} using an invalid key", id.toString());
                }*/
        }
    }
}
