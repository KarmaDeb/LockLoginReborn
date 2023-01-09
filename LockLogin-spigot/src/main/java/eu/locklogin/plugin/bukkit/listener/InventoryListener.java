package eu.locklogin.plugin.bukkit.listener;

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

import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.plugin.bungee.data.BungeeDataStorager;
import eu.locklogin.plugin.bukkit.util.inventory.AltAccountsInventory;
import eu.locklogin.plugin.bukkit.util.inventory.PinInventory;
import eu.locklogin.plugin.bukkit.util.inventory.PlayersInfoInventory;
import eu.locklogin.plugin.bukkit.util.player.User;
import ml.karmaconfigs.api.common.string.StringUtils;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static eu.locklogin.plugin.bukkit.LockLogin.plugin;
import static eu.locklogin.plugin.bukkit.util.inventory.object.Button.Action;

public final class InventoryListener implements Listener {

    private final Set<InventoryInteractEvent> cancelled = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static Sound NOTE_PLING = null;
    private static Sound NOTE_HAT = null;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryOpen(InventoryOpenEvent e) {
        InventoryView view = e.getView();
        HumanEntity entity = e.getPlayer();

        if (entity instanceof Player) {
            Player player = (Player) entity;
            User user = new User(player);
            ClientSession session = user.getSession();

            if (!session.isLogged() || !session.isTempLogged()) {
                PluginMessages messages = CurrentPlatform.getMessages();
                if (!StringUtils.toColor(view.getTitle()).equals(StringUtils.toColor(messages.pinTitle()))) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClose(InventoryCloseEvent e) {
        HumanEntity entity = e.getPlayer();

        if (entity instanceof Player) {
            Player player = (Player) entity;
            User user = new User(player);
            ClientSession session = user.getSession();

            if (!session.isPinLogged()) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> plugin.sync().queue("inventory_update", () -> {
                    BungeeDataStorager storage = new BungeeDataStorager();

                    if (user.getManager().hasPin() || storage.needsPinConfirmation(player)) {
                        PinInventory pin = new PinInventory(player);
                        pin.open();
                    }
                }), 10);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void menuInteract(InventoryInteractEvent e) {
        if (!cancelled.contains(e)) {
            Player player = (Player) e.getWhoClicked();
            User user = new User(player);
            ClientSession session = user.getSession();

            if (!session.isPinLogged()) {
                InventoryView view = e.getView();
                PluginMessages messages = CurrentPlatform.getMessages();

                if (!StringUtils.toColor(view.getTitle()).equals(StringUtils.toColor(messages.pinTitle()))) {
                    cancelled.add(e);
                    e.setCancelled(true);
                }
            }
        } else {
            cancelled.remove(e);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void menuClick(InventoryClickEvent e) {
        if (!cancelled.contains(e)) {
            Player player = (Player) e.getWhoClicked();
            User user = new User(player);

            ClientSession session = user.getSession();
            PluginMessages messages = CurrentPlatform.getMessages();
            InventoryView view = e.getView();

            ItemStack clicked = e.getCurrentItem();

            if (!session.isPinLogged()) {
                if (StringUtils.toColor(view.getTitle()).equals(StringUtils.toColor(messages.pinTitle()))) {
                    if (e.getClick() == ClickType.LEFT) {
                        PinInventory pin = new PinInventory(player);

                        if (clicked != null) {
                            Action action = Action.getAction(clicked);
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
                                    pin.updateInput();

                                    if (NOTE_PLING == null) {
                                        Sound[] sounds = Sound.values();
                                        for (Sound snd : sounds) {
                                            if (snd.name().endsWith("NOTE_PLING") || snd.name().endsWith("NOTE_BLOCK_PLING")) {
                                                NOTE_PLING = snd;
                                                break;
                                            }
                                        }
                                    }

                                    if (NOTE_PLING != null)
                                        player.playSound(player.getLocation(), NOTE_PLING, 2f, getNote(action.friendly()));

                                    break;
                                case CONFIRM:
                                    pin.confirm();
                                    break;
                                case ERASE:
                                    pin.eraseInput();
                                    pin.updateInput();

                                    if (NOTE_HAT == null) {
                                        Sound[] sounds = Sound.values();
                                        for (Sound snd : sounds) {
                                            if (snd.name().endsWith("NOTE_HAT") || snd.name().endsWith("NOTE_BLOCK_HAT")) {
                                                NOTE_HAT = snd;
                                                break;
                                            }
                                        }
                                    }

                                    if (NOTE_HAT != null) player.playSound(player.getLocation(), NOTE_HAT, 2f, 2f);

                                    break;
                                case NONE:
                                default:
                                    break;
                            }
                        }
                    }
                } else {
                    PinInventory pin = new PinInventory(player);
                    pin.open();
                }

                e.setCancelled(true);
                cancelled.add(e);
            } else {
                if (StringUtils.toColor(view.getTitle()).equals(StringUtils.toColor(messages.pinTitle()))) {
                    e.setCancelled(true);
                    player.closeInventory();
                    return;
                }

                if (StringUtils.toColor(view.getTitle()).equals(StringUtils.toColor(messages.altTitle()))) {
                    if (e.getClick() == ClickType.LEFT) {
                        if (clicked != null) {
                            Action action = Action.getAction(clicked);
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
                        cancelled.add(e);
                    }
                } else {
                    if (StringUtils.toColor(view.getTitle()).equals(StringUtils.toColor(messages.infoTitle()))) {
                        if (e.getClick() == ClickType.LEFT) {
                            if (clicked != null) {
                                Action action = Action.getAction(clicked);
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
                            cancelled.add(e);
                        }
                    }
                }
            }
        } else {
            cancelled.remove(e);
        }
    }

    /**
     * Get the play note of the number
     *
     * @param number the clicked number
     * @return the number note
     */
    private float getNote(final String number) {
        try {
            int num = Integer.parseInt(number);
            switch (num) {
                case 0:
                    return 0.5f;
                case 1:
                    return 0.6f;
                case 2:
                    return 0.7f;
                case 3:
                    return 1.1f;
                case 4:
                    return 1.2f;
                case 5:
                    return 1.3f;
                case 6:
                    return 1.7f;
                case 7:
                    return 1.8f;
                case 8:
                    return 1.9f;
                case 9:
                default:
                    return 2.0f;
            }
        } catch (Throwable ignored) {
        }

        return 1f;
    }
}
