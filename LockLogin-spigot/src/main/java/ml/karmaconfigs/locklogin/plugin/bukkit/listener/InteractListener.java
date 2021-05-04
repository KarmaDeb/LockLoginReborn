package ml.karmaconfigs.locklogin.plugin.bukkit.listener;

import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.player.User;
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

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.plugin;

public class InteractListener implements Listener {

    //Item pickup is handled as interact listener as
    //the player is technically interacting with the
    //dropped item
    @EventHandler(priority = EventPriority.HIGHEST)
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
    @EventHandler(priority = EventPriority.HIGHEST)
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

    @EventHandler(priority = EventPriority.HIGHEST)
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
                                    e.getClickedBlock().setMetadata("DisableBlock", new FixedMetadataValue(plugin, plugin));
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
                                        e.getClickedBlock().removeMetadata("DisableBlock", plugin);
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onSignal(BlockRedstoneEvent e) {
        Block block = e.getBlock();
        if (block.hasMetadata("DisableBlock")) {
            e.setNewCurrent(0);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
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

    @EventHandler(priority = EventPriority.HIGHEST)
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
