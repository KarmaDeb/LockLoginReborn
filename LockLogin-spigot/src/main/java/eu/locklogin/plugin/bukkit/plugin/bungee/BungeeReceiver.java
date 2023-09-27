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
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.module.plugin.client.permission.plugin.PluginPermissions;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import eu.locklogin.api.security.LockLoginRuntime;
import eu.locklogin.api.util.platform.CurrentPlatform;
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
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.utils.enums.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static eu.locklogin.plugin.bukkit.LockLogin.*;

@SuppressWarnings("UnstableApiUsage")
public final class BungeeReceiver implements PluginMessageListener {

    private static final Map<String, AccountManager> accounts = new ConcurrentHashMap<>();

    public final static Map<UUID, UUID> proxies_map = new ConcurrentHashMap<>();

    private static boolean advised = false;

    static {
        LockLoginRuntime.checkSecurity(true);
    }

    public BungeeReceiver() {
        LockLoginRuntime.checkSecurity(true);
        MessagePool.whenValid((ch, pl, b_Input) -> {
            try {
                String raw = b_Input.readUTF();
                Gson gson = new GsonBuilder().create();

                PluginConfiguration config = CurrentPlatform.getConfiguration();

                JsonObject json = gson.fromJson(raw, JsonObject.class);
                boolean canRead = true;
                if (!ch.equalsIgnoreCase("ll:access")) {
                    String token = json.get("key").getAsString();
                    canRead = token.equals(config.comKey());

                    if (config.comKey().isEmpty() && !advised) {
                        console.send("You are using an empty communication key. It's highly recommended to define an unique key in the whole network for security communication (config.yml > BungeeKey)", Level.GRAVE);
                        advised = true;
                    }
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
            } catch (Throwable ex) {
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
    public void onPluginMessageReceived(@NotNull String channel, @Nullable Player player, byte[] bytes) {
        ByteArrayDataInput input = ByteStreams.newDataInput(bytes);
        String json_raw = input.readUTF();

        Gson gson = new GsonBuilder().create();

        JsonObject bungee_data = gson.fromJson(json_raw, JsonObject.class);
        UUID id = (player != null ? player.getUniqueId() : UUID.randomUUID());
        if (bungee_data.has("player")) {
            Player tmp = plugin.getServer().getPlayer(UUID.fromString(bungee_data.get("player").getAsString()));
            if (tmp != null) {
                id = tmp.getUniqueId();
            } else {
                logger.scheduleLog(Level.WARNING, "Received player message for non online player: {0}", bungee_data.get("player").getAsString());
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
            PluginMessages messages = CurrentPlatform.getMessages();

            switch (sub) {
                case JOIN:
                    if (!user.isLockLoginUser()) {
                        user.applyLockLoginUser();
                    }

                    BungeeSender.validatePlayer(player);
                    session.validate();

                    plugin.logger().scheduleLog(Level.INFO, "Validated bungeecord join session of {0}", player.getUniqueId());

                    if (config.enableSpawn()) {
                        trySync(TaskTarget.TELEPORT, () -> player.teleport(player.getWorld().getSpawnLocation()));

                        Spawn spawn = new Spawn(player.getWorld());
                        spawn.load().whenComplete(() -> spawn.teleport(player));
                    }

                    session.setLogged(bungee_data.get("pass_login").getAsBoolean());
                    session.set2FALogged(bungee_data.get("2fa_login").getAsBoolean());
                    session.setPinLogged(bungee_data.get("pin_login").getAsBoolean());
                    user.setRegistered(bungee_data.get("registered").getAsBoolean());

                    if (bungee_data.get("2fa_login").getAsBoolean()) {
                        ModulePlayer module = user.getModule();
                        if (!module.hasPermission(PluginPermissions.join_silent())) {
                            String message = messages.playerJoin(module);
                            if (!StringUtils.isNullOrEmpty(message)) {
                                Bukkit.getServer().broadcastMessage(StringUtils.toColor(message));
                            }
                        }
                    }

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

                        ModulePlayer module = user.getModule();
                        if (!module.hasPermission(PluginPermissions.leave_silent())) {
                            String message = messages.playerLeave(module);
                            if (!StringUtils.isNullOrEmpty(message)) {
                                Bukkit.getServer().broadcastMessage(StringUtils.toColor(message));
                            }
                        }

                        //session.invalidate();
                        session.setLogged(false);
                        session.setPinLogged(false);
                        session.set2FALogged(false);

                        user.removeLockLoginUser();

                        plugin.logger().scheduleLog(Level.INFO, "Invalidating bungeecord session of {0}", player.getUniqueId());
                    }

                    TransientMap.apply(player);

                    user.setTempSpectator(false);
                    proxies_map.remove(player.getUniqueId());
                    break;
                case VALIDATION:
                    BungeeSender.validatePlayer(player);
                    session.validate();

                    plugin.logger().scheduleLog(Level.INFO, "Validated bungeecord session of {0}", player.getUniqueId());
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
                            if (StringUtils.stripColor(view.getTitle()).equals(StringUtils.stripColor(messages.pinTitle()))) {
                                PinInventory inventory = new PinInventory(player);
                                inventory.close();
                            }
                        });
                    }
                    break;
                case GAUTH:
                    TransientMap.apply(player);

                    if (!session.is2FALogged()) {
                        session.set2FALogged(true);

                        ModulePlayer module = user.getModule();
                        if (!module.hasPermission(PluginPermissions.join_silent())) {
                            String message = messages.playerJoin(module);
                            if (!StringUtils.isNullOrEmpty(message)) {
                                Bukkit.getServer().broadcastMessage(StringUtils.toColor(message));
                            }
                        }

                        if (config.takeBack()) {
                            LastLocation location = new LastLocation(player);
                            location.teleport();
                        }
                    }
                    break;
                case CLOSE:
                    boolean closeOperated = false;

                    if (session.isLogged() || session.is2FALogged() || session.isPinLogged()) {
                        closeOperated = true;

                        session.setLogged(false);
                        session.set2FALogged(false);
                        session.setPinLogged(false);
                    }

                    if (closeOperated) {
                        ModulePlayer module = user.getModule();
                        if (!module.hasPermission(PluginPermissions.leave_silent())) {
                            String message = messages.playerLeave(module);
                            if (!StringUtils.isNullOrEmpty(message)) {
                                Bukkit.getServer().broadcastMessage(StringUtils.toColor(message));
                            }
                        }
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
                    BungeeSender.sendPlayerInstance(serialized);
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
                ModulePlugin.receiveMessage(bungee_data);
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
