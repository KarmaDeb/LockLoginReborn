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

import eu.locklogin.plugin.bukkit.LockLogin;
import eu.locklogin.plugin.bukkit.util.player.User;
import eu.locklogin.api.account.ClientSession;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class InteractListener implements Listener {

    //Item pickup is handled as interact listener as
    //the player is technically interacting with the
    //dropped item
    @EventHandler(priority = EventPriority.LOWEST)
    public final void onPickup(EntityPickupItemEvent e) {
        if (!e.isCancelled()) {
            Entity entity = e.getEntity();
            if (entity instanceof Player) {
                Player player = (Player) entity;
                User user = new User(player);
                ClientSession session = user.getSession();

                if (user.isLockLoginUser()) {
                    if (session.isValid()) {
                        e.setCancelled(!session.isCaptchaLogged() || !session.isLogged() || !session.isTempLogged());
                    } else {
                        e.setCancelled(true);
                    }
                }
            }
        }
    }

    //Item pickup is handled as interact listener as
    //the player is technically interacting with the
    //dropped item
    @EventHandler(priority = EventPriority.LOWEST)
    public final void onDrop(PlayerDropItemEvent e) {
        if (!e.isCancelled()) {
            Player player = e.getPlayer();
            User user = new User(player);
            ClientSession session = user.getSession();

            if (user.isLockLoginUser()) {
                if (session.isValid()) {
                    e.setCancelled(!session.isCaptchaLogged() || !session.isLogged() || !session.isTempLogged());
                } else {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public final void onGeneralInteract(PlayerInteractEvent e) {
        if (e.useInteractedBlock() != Event.Result.DENY || e.useItemInHand() != Event.Result.DENY) {
            Player player = e.getPlayer();
            User user = new User(player);
            ClientSession session = user.getSession();

            if (user.isLockLoginUser()) {
                if (session.isValid()) {
                    if (!session.isCaptchaLogged() || !session.isLogged() || !session.isTempLogged()) {
                        if (e.getAction() == Action.PHYSICAL) {
                            if (e.getClickedBlock() != null && !e.getClickedBlock().getType().equals(Material.AIR)) {
                                if (e.getClickedBlock().getType().name().contains("PLATE")) {
                                    e.getClickedBlock().setMetadata("DisableBlock", new FixedMetadataValue(LockLogin.plugin, LockLogin.plugin));
                                } else {
                                    e.setCancelled(true);
                                }
                            } else {
                                e.setCancelled(true);
                            }
                        } else {
                            e.setCancelled(true);
                        }
                    } else {
                        if (session.isCaptchaLogged() && session.isLogged() && session.isTempLogged()) {
                            if (e.getAction().equals(Action.PHYSICAL)) {
                                if (e.getClickedBlock() != null && !e.getClickedBlock().getType().equals(Material.AIR)) {
                                    if (e.getClickedBlock().hasMetadata("DisableBlock")) {
                                        e.getClickedBlock().removeMetadata("DisableBlock", LockLogin.plugin);
                                    }
                                }
                            }
                        }
                    }
                } else {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public final void onSignal(BlockRedstoneEvent e) {
        Block block = e.getBlock();
        if (block.hasMetadata("DisableBlock")) {
            e.setNewCurrent(0);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public final void onEntityInteract(PlayerInteractEntityEvent e) {
        if (!e.isCancelled()) {
            Player player = e.getPlayer();
            User user = new User(player);
            ClientSession session = user.getSession();

            if (user.isLockLoginUser()) {
                if (session.isValid()) {
                    e.setCancelled(!session.isCaptchaLogged() || !session.isLogged() || !session.isTempLogged());
                } else {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public final void atEntityInteract(PlayerInteractAtEntityEvent e) {
        if (!e.isCancelled()) {
            Player player = e.getPlayer();
            User user = new User(player);
            ClientSession session = user.getSession();

            if (user.isLockLoginUser()) {
                if (session.isValid()) {
                    e.setCancelled(!session.isCaptchaLogged() || !session.isLogged() || !session.isTempLogged());
                } else {
                    e.setCancelled(true);
                }
            }
        }
    }
}
