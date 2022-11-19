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

import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.plugin.bungee.data.BungeeDataStorager;
import eu.locklogin.plugin.bukkit.util.inventory.PinInventory;
import eu.locklogin.plugin.bukkit.util.player.User;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public final class OtherListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageEvent e) {
        if (!e.isCancelled()) {
            if (!e.getCause().equals(EntityDamageEvent.DamageCause.FALL)) {
                Entity entity = e.getEntity();
                if (entity instanceof Player) {
                    Player player = (Player) entity;
                    User user = new User(player);
                    if (user.isLockLoginUser()) {
                        ClientSession session = user.getSession();
                        if (session.isValid()) {
                            if (!session.isLogged() || !session.isTempLogged() || !session.isCaptchaLogged()) {
                                e.setCancelled(true);

                                if (e.getCause().equals(EntityDamageEvent.DamageCause.VOID)) {
                                    Block highest = player.getWorld().getHighestBlockAt(player.getLocation());
                                    player.teleport(highest.getLocation().add(0D, 1D, 0D));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamageByEntity(EntityDamageByEntityEvent e) {
        if (!e.isCancelled()) {
            Entity target = e.getEntity();
            Entity damager = e.getDamager();

            PluginMessages messages = CurrentPlatform.getMessages();

            if (target instanceof Player) {
                Player victim = (Player) target;
                User tar_user = new User(victim);

                if (tar_user.isLockLoginUser()) {
                    ClientSession tar_session = tar_user.getSession();
                    if (tar_session.isValid()) {
                        if (!tar_session.isLogged() || !tar_session.isTempLogged() || !tar_session.isCaptchaLogged()) {
                            if (damager instanceof Player) {
                                Player attacker = (Player) damager;
                                User at_user = new User(attacker);

                                at_user.send(messages.prefix() + messages.notVerified(tar_user.getModule()));
                                e.setCancelled(true);
                            } else {
                                e.setCancelled(true);
                                damager.remove();
                            }
                        }
                    }
                }
            } else {
                if (damager instanceof Player) {
                    Player attacker = (Player) damager;
                    User at_user = new User(attacker);

                    if (at_user.isLockLoginUser()) {
                        ClientSession session = at_user.getSession();
                        e.setCancelled(!session.isLogged() || !session.isTempLogged() || !session.isCaptchaLogged());
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMove(PlayerMoveEvent e) {
        if (!e.isCancelled()) {
            Player player = e.getPlayer();
            User user = new User(player);
            if (user.isLockLoginUser()) {
                ClientSession session = user.getSession();

                if (session.isValid()) {
                    if (!session.isLogged() || !session.isTempLogged()) {
                        Block from = e.getFrom().getBlock();

                        if (e.getTo() != null) {
                            Block to = e.getTo().getBlock();

                            if (from.getLocation().getX() != to.getLocation().getX() || from.getLocation().getZ() != to.getLocation().getZ()) {
                                e.setCancelled(true);
                            } else {
                                e.setCancelled(to.getLocation().getY() > from.getLocation().getY());
                            }

                            if (session.isLogged()) {
                                PluginConfiguration configuration = CurrentPlatform.getConfiguration();
                                if (configuration.isBungeeCord()) {
                                    BungeeDataStorager storager = new BungeeDataStorager();
                                    if (storager.needsPinConfirmation(player)) {
                                        PinInventory inventory = new PinInventory(player);
                                        inventory.open();
                                    }
                                } else {
                                    AccountManager manager = user.getManager();
                                    if (manager.hasPin() && !session.isPinLogged()) {
                                        PinInventory inventory = new PinInventory(player);
                                        inventory.open();
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
    public void onBlockPace(BlockPlaceEvent e) {
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
    public void onBlockBreak(BlockBreakEvent e) {
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

    //Prevent player hunger level going down if not logged
    @EventHandler(priority = EventPriority.LOWEST)
    public void onHunger(FoodLevelChangeEvent e) {
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
