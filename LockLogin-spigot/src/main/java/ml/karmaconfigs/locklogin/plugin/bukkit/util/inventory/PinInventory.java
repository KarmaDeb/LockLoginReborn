package ml.karmaconfigs.locklogin.plugin.bukkit.util.inventory;

import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.LockLoginListener;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.encryption.CryptoUtil;
import ml.karmaconfigs.locklogin.api.event.user.UserAuthenticateEvent;
import ml.karmaconfigs.locklogin.plugin.bukkit.plugin.bungee.BungeeSender;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.configuration.Config;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.inventory.object.Number;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.player.User;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.*;

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

    private final static HashMap<Player, String> input = new HashMap<>();

    private final Player player;
    private final Inventory inventory;

    private final static Map<UUID, PinInventory> inventories = new HashMap<>();

    /**
     * Initialize the pin inventory for
     * the player
     *
     * @param _player the player
     */
    public PinInventory(Player _player) {
        if (!inventories.containsKey(_player.getUniqueId())) {
            player = _player;

            Message messages = new Message();

            inventory = plugin.getServer().createInventory(this, 45, StringUtils.toColor(messages.pinTitle()));
            makeInventory();

            inventories.put(_player.getUniqueId(), this);
        } else {
            player = inventories.get(_player.getUniqueId()).player;
            inventory = inventories.get(_player.getUniqueId()).inventory;
        }
    }

    /**
     * Initialize the inventory items
     */
    private void makeInventory() {
        inventory.setItem(12, Number.seven());
        inventory.setItem(13, Number.eight());
        inventory.setItem(14, Number.nine());
        inventory.setItem(21, Number.four());
        inventory.setItem(22, Number.five());
        inventory.setItem(23, Number.six());
        inventory.setItem(25, getInput());
        inventory.setItem(30, Number.one());
        inventory.setItem(31, Number.two());
        inventory.setItem(32, Number.three());
        inventory.setItem(36, Number.erase());
        inventory.setItem(40, Number.zero());
        inventory.setItem(44, Number.confirm());

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
    public final Inventory getInventory() {
        return inventory;
    }

    /**
     * Open the inventory to the player
     */
    public final void open() {
        try {
            player.openInventory(inventory);
        } catch (Throwable e) {
            logger.scheduleLog(Level.GRAVE, e);
            logger.scheduleLog(Level.INFO, "Couldn't open pin GUI to player " + player.getName());
        }
    }

    /**
     * Close the inventory to the player
     */
    public final void close() {
        if (player != null && player.isOnline()) {
            player.getInventory();
            player.closeInventory();

            inventories.remove(player.getUniqueId());
        }
    }

    /**
     * Confirm the pin input
     */
    public final void confirm() {
        User user = new User(player);
        ClientSession session = user.getSession();
        AccountManager manager = user.getManager();

        Config config = new Config();
        Message messages = new Message();

        if (session.isValid()) {
            if (!input.getOrDefault(player, "/-/-/-/").contains("/")) {
                if (!config.isBungeeCord()) {
                    String pin = input.get(player).replaceAll("-", "");

                    CryptoUtil utils = new CryptoUtil(pin, user.getManager().getPin());
                    if (utils.validate()) {
                        if (manager.has2FA()) {
                            UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PIN, UserAuthenticateEvent.Result.SUCCESS_TEMP, player, messages.gAuthInstructions(), null);
                            LockLoginListener.callEvent(event);

                            user.send(messages.prefix() + event.getAuthMessage());
                        } else {
                            UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PIN, UserAuthenticateEvent.Result.SUCCESS, player, messages.logged(), null);
                            LockLoginListener.callEvent(event);

                            user.send(messages.prefix() + event.getAuthMessage());
                            session.setPinLogged(true);
                        }

                        close();
                    } else {
                        UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PIN, UserAuthenticateEvent.Result.FAILED, player, messages.incorrectPin(), null);
                        LockLoginListener.callEvent(event);

                        user.send(messages.prefix() + event.getAuthMessage());
                    }

                    input.put(player, "/-/-/-/");
                    updateInput();
                } else {
                    String pinText = input.get(player).replaceAll("-", "");
                    BungeeSender.sendPinInput(player, pinText);

                    input.put(player, "/-/-/-/");
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
    public final void eraseInput() {
        String finalNew = "/-/-/-/";

        String[] current = input.getOrDefault(player, "/-/-/-/").split("-");
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

        input.put(player, finalNew);

        updateInput();
    }

    /**
     * Update the player inventory input
     */
    public final void updateInput() {
        player.getOpenInventory().setItem(25, getInput());
    }

    /**
     * Get the player pin input as
     * item
     *
     * @return the input item stack
     */
    public final ItemStack getInput() {
        ItemStack paper = new ItemStack(Material.PAPER, 1);
        ItemMeta paperMeta = paper.getItemMeta();
        assert paperMeta != null;

        paperMeta.setDisplayName(StringUtils.toColor("&c" + input.getOrDefault(player, "/-/-/-/")));

        paper.setItemMeta(paperMeta);

        return paper;
    }

    /**
     * Set the player pin input
     *
     * @param newInput the input
     */
    public final void addInput(String newInput) {
        String finalNew;

        if (input.getOrDefault(player, "/-/-/-/").contains("/")) {
            String[] current = input.getOrDefault(player, "/-/-/-/").split("-");
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

            input.put(player, finalNew);
        }
    }

    /**
     * Fill the empty inventory slots
     * with the specified item
     *
     * @param item the item
     */
    private void fillEmptySlots(ItemStack item) {
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
