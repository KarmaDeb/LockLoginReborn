package eu.locklogin.plugin.bukkit.util.inventory;

import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.common.session.online.SessionDataContainer;
import eu.locklogin.api.encryption.CryptoFactory;
import eu.locklogin.api.encryption.Validation;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.module.plugin.api.event.user.UserAuthenticateEvent;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.TaskTarget;
import eu.locklogin.plugin.bukkit.listener.data.TransientMap;
import eu.locklogin.plugin.bukkit.plugin.bungee.BungeeReceiver;
import eu.locklogin.plugin.bukkit.plugin.bungee.BungeeSender;
import eu.locklogin.plugin.bukkit.util.files.data.LastLocation;
import eu.locklogin.plugin.bukkit.util.inventory.object.Button;
import eu.locklogin.plugin.bukkit.util.player.User;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.utils.enums.Level;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static eu.locklogin.plugin.bukkit.LockLogin.*;

/**
 * Private GSA code
 * <p>
 * The use of this code
 * without GSA team authorization
 * will be a violation of
 * terms of use determined
 * in <a href="https://karmaconfigs.ml/license/"> here </a>
 */
public final class PinInventory implements InventoryHolder {

    private final static Map<UUID, String> input = new ConcurrentHashMap<>();
    private final static Map<UUID, PinInventory> inventories = new ConcurrentHashMap<>();
    private final Inventory inventory;

    private Player player;

    /**
     * Initialize the pin inventory for
     * the player
     *
     * @param _player the player
     */
    public PinInventory(final Player _player) {
        player = _player;
        PinInventory stored = inventories.getOrDefault(_player.getUniqueId(), null);

        if (stored == null) {
            PluginMessages messages = CurrentPlatform.getMessages();

            String title = StringUtils.toColor(messages.pinTitle());
            if (title.length() > 32)
                title = title.substring(0, 32);

            inventory = plugin.getServer().createInventory(this, 45, title);
            inventories.put(_player.getUniqueId(), this);
        } else {
            if (!stored.player.equals(player)) {
                stored.player = player;
                inventories.put(_player.getUniqueId(), stored); //No really need for that, but we will do it anyway
            }

            inventory = stored.inventory;
        }
    }

    /**
     * Initialize the inventory items
     */
    void makeInventory() {
        inventory.setItem(12, Button.getNumber(7));
        inventory.setItem(13, Button.getNumber(8));
        inventory.setItem(14, Button.getNumber(9));
        inventory.setItem(21, Button.getNumber(4));
        inventory.setItem(22, Button.getNumber(5));
        inventory.setItem(23, Button.getNumber(6));
        inventory.setItem(25, getInput());
        inventory.setItem(30, Button.getNumber(1));
        inventory.setItem(31, Button.getNumber(2));
        inventory.setItem(32, Button.getNumber(3));
        inventory.setItem(36, Button.erase());
        inventory.setItem(40, Button.getNumber(0));
        inventory.setItem(44, Button.confirm());

        try {
            fillEmptySlots(new ItemStack(Objects.requireNonNull(Material.matchMaterial("STAINED_GLASS_PANE", true)), 1));
        } catch (Throwable e) {
            fillEmptySlots(new ItemStack(Objects.requireNonNull(Material.matchMaterial("STAINED_GLASS_PANE")), 1));
        }
    }

    /**
     * Get the object's inventory.
     *
     * @return The inventory.
     */
    @NotNull
    @Override
    public Inventory getInventory() {
        return inventories.getOrDefault(player.getUniqueId(), this).inventory;
    }

    /**
     * Open the inventory to the player
     */
    public synchronized void open() {
        trySync(TaskTarget.INVENTORY, () -> {
            InventoryView view = player.getOpenInventory();
            PluginMessages messages = CurrentPlatform.getMessages();

            if (!StringUtils.toColor(view.getTitle()).equals(StringUtils.toColor(messages.pinTitle()))) {
                makeInventory();
                player.openInventory(inventory);
            }
        });
    }

    /**
     * Close the inventory to the player
     */
    public void close() {
        inventories.remove(player.getUniqueId());
        List<HumanEntity> cloned = new ArrayList<>(inventory.getViewers()); //Prevent concurrent modifications
        cloned.forEach(HumanEntity::closeInventory);
    }

    /**
     * Confirm the pin input
     */
    public void confirm() {
        User user = new User(player);
        ClientSession session = user.getSession();
        AccountManager manager = user.getManager();

        PluginConfiguration config = CurrentPlatform.getConfiguration();
        PluginMessages messages = CurrentPlatform.getMessages();

        if (session.isValid()) {
            if (!input.getOrDefault(player.getUniqueId(), "/-/-/-/").contains("/")) {
                if (!config.isBungeeCord()) {
                    String pin = input.get(player.getUniqueId()).replaceAll("-", "");

                    CryptoFactory utils = CryptoFactory.getBuilder().withPassword(pin).withToken(manager.getPin()).build();
                    if (utils.validate(Validation.ALL)) {
                        if (utils.needsRehash(config.passwordEncryption())) {
                            //Set the player password again to update his hash
                            manager.setPin(pin);
                            logger.scheduleLog(Level.INFO, "Updated pin hash of {0} from {1} to {2}",
                                    StringUtils.stripColor(player.getDisplayName()),
                                    utils.getTokenHash().name(),
                                    config.passwordEncryption().name());
                        }

                        if (manager.has2FA()) {
                            UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PIN,
                                    UserAuthenticateEvent.Result.SUCCESS_TEMP,
                                    user.getModule(),
                                    messages.gAuthInstructions(),
                                    null);
                            ModulePlugin.callEvent(event);

                            user.send(messages.prefix() + event.getAuthMessage());
                            session.setPinLogged(true);
                        } else {
                            user.setTempSpectator(false);

                            UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PIN,
                                    UserAuthenticateEvent.Result.SUCCESS,
                                    user.getModule(),
                                    messages.logged(),
                                    null);
                            ModulePlugin.callEvent(event);

                            if (config.takeBack()) {
                                LastLocation location = new LastLocation(player);
                                location.teleport();
                            }

                            user.send(messages.prefix() + event.getAuthMessage());
                            session.setPinLogged(true);
                            session.set2FALogged(true);

                            SessionDataContainer.setLogged(SessionDataContainer.getLogged() + 1);
                            TransientMap.apply(player);
                        }

                        close();
                    } else {
                        UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PIN,
                                UserAuthenticateEvent.Result.FAILED,
                                user.getModule(),
                                messages.incorrectPin(),
                                null);
                        ModulePlugin.callEvent(event);

                        user.send(messages.prefix() + event.getAuthMessage());
                    }

                    input.put(player.getUniqueId(), "/-/-/-/");
                    updateInput();
                } else {
                    String pinText = input.get(player.getUniqueId()).replaceAll("-", "");
                    BungeeSender.sendPinInput(player, pinText, BungeeReceiver.proxies_map.get(player.getUniqueId()).toString());

                    input.put(player.getUniqueId(), "/-/-/-/");
                    updateInput();
                }
            } else {
                user.send(messages.prefix() + messages.pinLength());
            }
        } else {
            user.send(messages.prefix() + properties.getProperty("session_not_valid", "&5&oYour session is invalid, try leaving and joining the server again"));
        }
    }

    /**
     * Erase the last digit from
     * the input
     */
    public void eraseInput() {
        String finalNew = "/-/-/-/";

        String[] current = input.getOrDefault(player.getUniqueId(), "/-/-/-/").split("-");
        String first = current[0];
        String second = current[1];
        String third = current[2];
        String fourth = current[3];

        if (!fourth.equals("/")) {
            finalNew = first + "-" + second + "-" + third + "-/";
        } else {
            if (!third.equals("/")) {
                finalNew = first + "-" + second + "-/-/";
            } else {
                if (!second.equals("/")) {
                    finalNew = first + "-/-/-/";
                } else {
                    if (!first.equals("/")) {
                        finalNew = "/-/-/-/";
                    }
                }
            }
        }

        input.put(player.getUniqueId(), finalNew);
        updateInput();
    }

    /**
     * Update the player inventory input
     */
    public void updateInput() {
        inventory.setItem(25, getInput());
    }

    /**
     * Get the player pin input as
     * item
     *
     * @return the input item stack
     */
    public ItemStack getInput() {
        ItemStack paper = new ItemStack(Material.PAPER, 1);
        ItemMeta paperMeta = paper.getItemMeta();
        assert paperMeta != null;

        paperMeta.setDisplayName(StringUtils.toColor("&c" + input.getOrDefault(player.getUniqueId(), "/-/-/-/")));

        paper.setItemMeta(paperMeta);

        return paper;
    }

    /**
     * Set the player pin input
     *
     * @param newInput the input
     */
    public void addInput(final String newInput) {
        String finalNew;

        if (input.getOrDefault(player.getUniqueId(), "/-/-/-/").contains("/")) {
            String[] current = input.getOrDefault(player.getUniqueId(), "/-/-/-/").split("-");
            String first = current[0];
            String second = current[1];
            String third = current[2];

            if (first.equals("/")) {
                finalNew = newInput + "-/-/-/";
            } else {
                if (second.equals("/")) {
                    finalNew = first + "-" + newInput + "-/-/";
                } else {
                    if (third.equals("/")) {
                        finalNew = first + "-" + second + "-" + newInput + "-/";
                    } else {
                        //Allow the player to modify the latest input even if already set
                        finalNew = first + "-" + second + "-" + third + "-" + newInput;
                    }
                }
            }

            input.put(player.getUniqueId(), finalNew);
        }
    }

    /**
     * Fill the empty inventory slots
     * with the specified item
     *
     * @param item the item
     */
    private void fillEmptySlots(final ItemStack item) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack != null) {
                if (stack.getType().equals(Material.AIR)) {
                    inventory.setItem(i, item);
                }
            } else {
                inventory.setItem(i, item);
            }
        }
    }
}
