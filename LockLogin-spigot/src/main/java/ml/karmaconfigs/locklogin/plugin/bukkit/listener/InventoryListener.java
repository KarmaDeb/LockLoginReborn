package ml.karmaconfigs.locklogin.plugin.bukkit.listener;

import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.inventory.PinInventory;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.inventory.object.Number;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.player.User;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.*;

public final class InventoryListener implements Listener {

    /**
     * Check if the clicked item is similar
     * to the one to check with
     *
     * @param clicked the clicked item
     * @param check   the one to check with
     * @return if the clicked stack is similar to the "check" one
     */
    private boolean isSimilar(ItemStack clicked, ItemStack check) {
        boolean isSimilar = false;
        if (clicked.hasItemMeta()) {
            assert clicked.getItemMeta() != null;
            if (clicked.getItemMeta().hasDisplayName()) {
                if (check.hasItemMeta()) {
                    assert check.getItemMeta() != null;
                    if (check.getItemMeta().hasDisplayName()) {
                        if (StringUtils.stripColor(clicked.getItemMeta().getDisplayName()).equals(StringUtils.stripColor(check.getItemMeta().getDisplayName()))) {
                            isSimilar = true;
                        }
                    }
                }
            }
        }

        return isSimilar;
    }

    /**
     * Get the number of the item stack
     *
     * @param stack the item stack
     * @return the stack number or null
     * if its not a number
     */
    private Action getAction(final ItemStack stack) {
        if (isSimilar(stack, Number.one()))
            return Action.ONE;
        if (isSimilar(stack, Number.two()))
            return Action.TWO;
        if (isSimilar(stack, Number.three()))
            return Action.THREE;
        if (isSimilar(stack, Number.four()))
            return Action.FOUR;
        if (isSimilar(stack, Number.five()))
            return Action.FIVE;
        if (isSimilar(stack, Number.six()))
            return Action.SIX;
        if (isSimilar(stack, Number.seven()))
            return Action.SEVEN;
        if (isSimilar(stack, Number.eight()))
            return Action.EIGHT;
        if (isSimilar(stack, Number.nine()))
            return Action.NINE;
        if (isSimilar(stack, Number.zero()))
            return Action.ZERO;
        if (isSimilar(stack, Number.erase()))
            return Action.ERASE;
        if (isSimilar(stack, Number.confirm()))
            return Action.CONFIRM;

        return Action.NONE;
    }

    /**
     * Possible enumeration
     */
    private enum Action {
        /**
         * Click '1' number stack
         */
        ONE,

        /**
         * Click '2' number stack
         */
        TWO,

        /**
         * Click '3' number stack
         */
        THREE,

        /**
         * Click '4' number stack
         */
        FOUR,

        /**
         * Click '5' number stack
         */
        FIVE,

        /**
         * Click '6' number stack
         */
        SIX,

        /**
         * Click '7' number stack
         */
        SEVEN,

        /**
         * Click '8' number stack
         */
        EIGHT,

        /**
         * Click '9' number stack
         */
        NINE,

        /**
         * Click '0' number stack
         */
        ZERO,

        /**
         * Erase number input
         */
        ERASE,

        /**
         * Confirm call or accept
         * call
         */
        CONFIRM,

        /**
         * Prevent errors
         */
        NONE;

        /**
         * Get the friendly name of the action, in case
         * the action was a number click, the number will
         * be returned instead
         *
         * @return the friendly action name
         */
        private String friendly() {
            switch (this) {
                case ONE:
                    return "1";
                case TWO:
                    return "2";
                case THREE:
                    return "3";
                case FOUR:
                    return "4";
                case FIVE:
                    return "5";
                case SIX:
                    return "6";
                case SEVEN:
                    return "7";
                case EIGHT:
                    return "8";
                case NINE:
                    return "9";
                case ZERO:
                    return "0";
                case ERASE:
                    return "erase";
                case CONFIRM:
                    return "confirm";
                case NONE:
                default:
                    return "";
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onInventoryOpen(InventoryOpenEvent e) {
        Inventory inventory = e.getInventory();
        HumanEntity entity = e.getPlayer();

        if (entity instanceof Player) {
            Player player = (Player) entity;
            User user = new User(player);
            ClientSession session = user.getSession();

            if (!session.isLogged() || !session.isTempLogged() && !user.getManager().getPin().replaceAll("\\s", "").isEmpty()) {
                e.setCancelled(!(inventory.getHolder() instanceof PinInventory));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onInventoryClose(InventoryCloseEvent e) {
        HumanEntity entity = e.getPlayer();

        if (entity instanceof Player) {
            Player player = (Player) entity;
            User user = new User(player);
            ClientSession session = user.getSession();

            if (!session.isPinLogged() && !user.getManager().getPin().replaceAll("\\s", "").isEmpty()) {
                plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                    PinInventory pin = new PinInventory(player);
                    pin.open();
                }, 5);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public final void menuInteract(InventoryInteractEvent e) {
        Player player = (Player) e.getWhoClicked();
        User user = new User(player);
        ClientSession session = user.getSession();

        if (!session.isPinLogged()) {
            Inventory inventory = e.getInventory();

            e.setCancelled(!(inventory.getHolder() instanceof PinInventory));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public final void menuClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        User user = new User(player);
        ClientSession session = user.getSession();

        if (!session.isPinLogged()) {
            Inventory inventory = e.getInventory();

            if (inventory.getHolder() instanceof PinInventory) {
                ItemStack clicked = e.getCurrentItem();
                PinInventory pin = new PinInventory(player);

                if (clicked != null) {
                    Action action = getAction(clicked);
                    switch (action) {
                        case ZERO:
                        case ONE:
                        case TWO:
                        case THREE:
                        case FOUR:
                        case FIVE:
                        case SIX:
                        case SEVEN:
                        case EIGHT:
                        case NINE:
                            pin.addInput(action.friendly());
                            break;
                        case CONFIRM:
                            pin.confirm();
                            break;
                        case ERASE:
                            pin.eraseInput();
                            break;
                        case NONE:
                        default:
                            break;
                    }
                }
            } else {
                //If the player is not pin logged and the inventory
                //is not a PinInventory, then close the inventory
                player.closeInventory();
            }

            e.setCancelled(true);
        }
    }
}
