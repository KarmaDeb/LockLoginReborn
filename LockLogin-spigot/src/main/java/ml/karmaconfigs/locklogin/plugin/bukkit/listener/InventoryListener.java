package ml.karmaconfigs.locklogin.plugin.bukkit.listener;

import ml.karmaconfigs.api.bukkit.soundutil.Sound;
import ml.karmaconfigs.api.bukkit.soundutil.SoundPlayer;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.inventory.AltAccountsInventory;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.inventory.PinInventory;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.inventory.PlayersInfoInventory;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.inventory.object.Button;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.player.User;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.plugin;

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
        if (isSimilar(stack, Button.one()))
            return Action.ONE;
        if (isSimilar(stack, Button.two()))
            return Action.TWO;
        if (isSimilar(stack, Button.three()))
            return Action.THREE;
        if (isSimilar(stack, Button.four()))
            return Action.FOUR;
        if (isSimilar(stack, Button.five()))
            return Action.FIVE;
        if (isSimilar(stack, Button.six()))
            return Action.SIX;
        if (isSimilar(stack, Button.seven()))
            return Action.SEVEN;
        if (isSimilar(stack, Button.eight()))
            return Action.EIGHT;
        if (isSimilar(stack, Button.nine()))
            return Action.NINE;
        if (isSimilar(stack, Button.zero()))
            return Action.ZERO;
        if (isSimilar(stack, Button.erase()))
            return Action.ERASE;
        if (isSimilar(stack, Button.confirm()))
            return Action.CONFIRM;
        if (isSimilar(stack, Button.next()))
            return Action.NEXT;
        if (isSimilar(stack, Button.back()))
            return Action.BACK;

        return Action.NONE;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onInventoryOpen(InventoryOpenEvent e) {
        Inventory inventory = e.getInventory();
        HumanEntity entity = e.getPlayer();

        if (entity instanceof Player) {
            Player player = (Player) entity;
            User user = new User(player);
            ClientSession session = user.getSession();

            if (!session.isPinLogged()) {
                if (!(inventory.getHolder() instanceof PinInventory)) {
                    e.setCancelled(true);
                }
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

            if (!session.isPinLogged()) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
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

        Inventory inventory = e.getInventory();
        ItemStack clicked = e.getCurrentItem();

        if (!session.isPinLogged()) {
            if (inventory.getHolder() instanceof PinInventory) {
                if (e.getClick() == ClickType.LEFT) {
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
                                SoundPlayer pling = new SoundPlayer(plugin, Sound.BLOCK_NOTE_BLOCK_PLING);
                                try {
                                    pling.playTo(player, 5, getNote(action.friendly()));
                                } catch (Throwable ignored) {
                                }

                                pin.addInput(action.friendly());
                                pin.updateInput();
                                break;
                            case CONFIRM:
                                pin.confirm();
                                break;
                            case ERASE:
                                pin.eraseInput();
                                pin.updateInput();

                                SoundPlayer hat = new SoundPlayer(plugin, Sound.BLOCK_NOTE_BLOCK_HAT);
                                hat.playTo(player, 5, 2.1);
                                break;
                            case NONE:
                            default:
                                break;
                        }
                    }
                }
            } else {
                //If the player is not pin logged and the inventory
                //is not a PinInventory, then close the inventory
                player.closeInventory();
            }

            e.setCancelled(true);
        } else {
            if (inventory.getHolder() instanceof AltAccountsInventory) {
                if (e.getClick() == ClickType.LEFT) {
                    if (clicked != null) {
                        Action action = getAction(clicked);
                        AltAccountsInventory alts;
                        switch (action) {
                            case NEXT:
                                alts = AltAccountsInventory.manager.getInventory(player);
                                if (alts == null)
                                    alts = new AltAccountsInventory(player, Collections.emptySet());

                                if (alts.getPlayerPage() + 1 < alts.getPages())
                                    alts.openPage(alts.getPlayerPage() + 1);
                                break;
                            case BACK:
                                alts = AltAccountsInventory.manager.getInventory(player);
                                if (alts == null)
                                    alts = new AltAccountsInventory(player, Collections.emptySet());

                                if (alts.getPlayerPage() - 1 > 0)
                                    alts.openPage(alts.getPlayerPage() - 1);
                                break;
                            default:
                                break;
                        }
                    }

                    e.setCancelled(true);
                }
            } else {
                if (inventory.getHolder() instanceof PlayersInfoInventory) {
                    if (e.getClick() == ClickType.LEFT) {
                        if (clicked != null) {
                            Action action = getAction(clicked);
                            PlayersInfoInventory infos;
                            switch (action) {
                                case NEXT:
                                    infos = PlayersInfoInventory.manager.getInventory(player);
                                    if (infos == null)
                                        infos = new PlayersInfoInventory(player, Collections.emptySet());

                                    if (infos.getPlayerPage() + 1 < infos.getPages())
                                        infos.openPage(infos.getPlayerPage() + 1);
                                    break;
                                case BACK:
                                    infos = PlayersInfoInventory.manager.getInventory(player);
                                    if (infos == null)
                                        infos = new PlayersInfoInventory(player, Collections.emptySet());

                                    if (infos.getPlayerPage() - 1 > 0)
                                        infos.openPage(infos.getPlayerPage() - 1);
                                    break;
                                default:
                                    break;
                            }
                        }

                        e.setCancelled(true);
                    }
                }
            }
        }
    }

    private double getNote(final String number) {
        try {
            int num = Integer.parseInt(number);
            switch (num) {
                case 0:
                    return 0.5;
                case 1:
                    return 0.6;
                case 2:
                    return 0.7;
                case 3:
                    return 1.1;
                case 4:
                    return 1.2;
                case 5:
                    return 1.3;
                case 6:
                    return 1.7;
                case 7:
                    return 1.8;
                case 8:
                    return 1.9;
                case 9:
                default:
                    return 2.0;
            }
        } catch (Throwable ignored) {
        }

        return 1D;
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
         * Confirm pin input
         */
        CONFIRM,

        /**
         * Next page
         */
        NEXT,

        /**
         * Previous page
         */
        BACK,

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
                case NEXT:
                    return "->";
                case BACK:
                    return "<-";
                case NONE:
                default:
                    return "";
            }
        }
    }
}
