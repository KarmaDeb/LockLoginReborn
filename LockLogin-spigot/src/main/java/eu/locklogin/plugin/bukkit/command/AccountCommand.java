package eu.locklogin.plugin.bukkit.command;

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

import eu.locklogin.api.account.AccountID;
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.common.security.client.AccountData;
import eu.locklogin.api.common.session.PersistentSessionData;
import eu.locklogin.api.common.session.SessionCheck;
import eu.locklogin.api.encryption.CryptoUtil;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.module.plugin.api.event.user.AccountCloseEvent;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.LockLogin;
import eu.locklogin.plugin.bukkit.command.util.SystemCommand;
import eu.locklogin.plugin.bukkit.plugin.PluginPermission;
import eu.locklogin.plugin.bukkit.util.files.client.OfflineClient;
import eu.locklogin.plugin.bukkit.util.files.data.lock.LockedAccount;
import eu.locklogin.plugin.bukkit.util.files.data.lock.LockedData;
import eu.locklogin.plugin.bukkit.util.inventory.AltAccountsInventory;
import eu.locklogin.plugin.bukkit.util.player.ClientVisor;
import eu.locklogin.plugin.bukkit.util.player.User;
import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.api.common.utils.enums.Level;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static eu.locklogin.plugin.bukkit.LockLogin.fromPlayer;

@SystemCommand(command = "account")
public class AccountCommand implements CommandExecutor {

    /**
     * Executes the given command, returning its success.
     * <br>
     * If false is returned, then the "usage" plugin.yml entry for this command
     * (if defined) will be sent to the player.
     *
     * @param sender  Source of the command
     * @param command Command which was executed
     * @param label   Alias of the command which was used
     * @param args    Passed command arguments
     * @return true if a valid command, otherwise false
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        PluginConfiguration config = CurrentPlatform.getConfiguration();
        PluginMessages messages = CurrentPlatform.getMessages();

        if (sender instanceof Player) {
            Player player = (Player) sender;
            User user = new User(player);

            if (user.getSession().isValid()) {
                if (args.length == 0) {
                    user.send(messages.prefix() + messages.accountArguments());
                } else {
                    ClientSession session;
                    switch (args[0].toLowerCase()) {
                        case "change":
                            if (args.length == 3) {
                                String password = args[1];
                                String new_pass = args[2];

                                AccountManager manager = user.getManager();

                                CryptoUtil util = CryptoUtil.getBuilder().withPassword(password).withToken(manager.getPassword()).build();
                                if (util.validate()) {
                                    if (!password.equals(new_pass)) {
                                        manager.setPassword(new_pass);
                                        user.send(messages.prefix() + messages.changeDone());
                                    } else {
                                        user.send(messages.prefix() + messages.changeSame());
                                    }
                                } else {
                                    user.send(messages.prefix() + messages.incorrectPassword());
                                }
                            } else {
                                user.send(messages.prefix() + messages.change());
                            }
                            break;
                        case "unlock":
                            if (player.hasPermission(PluginPermission.account())) {
                                if (args.length == 2) {
                                    String target = args[1];
                                    OfflineClient offline = new OfflineClient(target);

                                    AccountManager manager = offline.getAccount();
                                    if (manager != null) {
                                        LockedAccount account = new LockedAccount(manager.getUUID());
                                        LockedData data = account.getData();

                                        if (data.isLocked()) {
                                            if (account.unlock()) {
                                                user.send(messages.prefix() + messages.accountUnLocked(target));
                                            } else {
                                                user.send(messages.prefix() + messages.accountNotLocked(target));
                                                LockLogin.logger.scheduleLog(Level.GRAVE, "Tried to unlock account of " + target + " but failed");
                                            }
                                        } else {
                                            user.send(messages.prefix() + messages.accountNotLocked(target));
                                        }
                                    } else {
                                        user.send(messages.prefix() + messages.neverPlayer(target));
                                    }
                                } else {
                                    user.send(messages.prefix() + messages.accountUnLock());
                                }
                            } else {
                                user.send(messages.prefix() + messages.permissionError(PluginPermission.unlockAccount()));
                            }
                            break;
                        case "close":
                            switch (args.length) {
                                case 1:
                                    session = user.getSession();
                                    session.setLogged(false);
                                    session.setPinLogged(false);
                                    session.set2FALogged(false);

                                    user.savePotionEffects();
                                    user.applySessionEffects();

                                    if (config.clearChat()) {
                                        for (int i = 0; i < 150; i++)
                                            LockLogin.plugin.getServer().getScheduler().runTaskAsynchronously(LockLogin.plugin, () -> player.sendMessage(""));
                                    }

                                    SessionCheck<Player> check = user.getChecker();
                                    LockLogin.plugin.getServer().getScheduler().runTaskAsynchronously(LockLogin.plugin, check);

                                    if (player.getLocation().getBlock().getType().name().contains("PORTAL"))
                                        user.setTempSpectator(true);

                                    if (config.hideNonLogged()) {
                                        ClientVisor visor = new ClientVisor(player);
                                        visor.vanish();
                                    }

                                    user.send(messages.prefix() + messages.closed());
                                    AccountCloseEvent self = new AccountCloseEvent(fromPlayer(player), user.getManager().getName(), null);
                                    ModulePlugin.callEvent(self);
                                    break;
                                case 2:
                                    if (player.hasPermission(PluginPermission.account())) {
                                        String tar_name = args[1];
                                        Player tar_p = LockLogin.plugin.getServer().getPlayer(tar_name);

                                        if (tar_p != null && tar_p.isOnline()) {
                                            User target = new User(tar_p);
                                            session = target.getSession();

                                            if (session.isValid() && session.isLogged() && session.isTempLogged()) {
                                                target.send(messages.prefix() + messages.forcedClose());
                                                player.performCommand("account close");
                                                user.send(messages.prefix() + messages.forcedCloseAdmin(fromPlayer(tar_p)));

                                                AccountCloseEvent issuer = new AccountCloseEvent(fromPlayer(tar_p), user.getManager().getName(), null);
                                                ModulePlugin.callEvent(issuer);
                                            } else {
                                                user.send(messages.prefix() + messages.targetAccessError(tar_name));
                                            }
                                        } else {
                                            user.send(messages.prefix() + messages.connectionError(tar_name));
                                        }
                                    } else {
                                        user.send(messages.prefix() + messages.permissionError(PluginPermission.closeAccount()));
                                    }
                                    break;
                                default:
                                    user.send(messages.prefix() + messages.close());
                                    break;
                            }
                            break;
                        case "remove":
                        case "delete":
                            switch (args.length) {
                                case 2:
                                    if (player.hasPermission(PluginPermission.account())) {
                                        String target = args[1];
                                        Player online = LockLogin.plugin.getServer().getPlayer(target);
                                        OfflineClient offline = new OfflineClient(target);

                                        AccountManager manager = offline.getAccount();
                                        if (manager != null) {
                                            LockedAccount account = new LockedAccount(manager.getUUID());
                                            manager.setPassword("");
                                            manager.setPin("");
                                            manager.setGAuth("");
                                            manager.set2FA(false);

                                            user.send(messages.prefix() + messages.forcedAccountRemovalAdmin(target));

                                            if (online != null) {
                                                User onlineUser = new User(online);
                                                onlineUser.kick(messages.forcedAccountRemoval(player.getDisplayName()));
                                            }

                                            account.lock(StringUtils.stripColor(player.getDisplayName()));
                                        } else {
                                            user.send(messages.prefix() + messages.neverPlayer(target));
                                        }
                                    } else {
                                        user.send(messages.prefix() + messages.permissionError(PluginPermission.delAccount()));
                                    }
                                    break;
                                case 3:
                                    AccountManager manager = user.getManager();
                                    session = user.getSession();

                                    String password = args[1];
                                    String confirmation = args[2];

                                    if (password.equals(confirmation)) {
                                        CryptoUtil util = CryptoUtil.getBuilder().withPassword(password).withToken(manager.getPassword()).build();
                                        if (util.validate()) {
                                            user.send(messages.prefix() + messages.accountRemoved());
                                            manager.remove(player.getName());

                                            //Completely restart the client session
                                            session.setPinLogged(false);
                                            session.set2FALogged(false);
                                            session.setLogged(false);
                                            session.invalidate();
                                            session.validate();

                                            SessionCheck<Player> check = user.getChecker().whenComplete(user::restorePotionEffects);
                                            LockLogin.plugin.getServer().getScheduler().runTaskAsynchronously(LockLogin.plugin, check);
                                        } else {
                                            user.send(messages.prefix() + messages.incorrectPassword());
                                        }
                                    } else {
                                        user.send(messages.prefix() + messages.removeAccountMatch());
                                    }
                                    break;
                                default:
                                    user.send(messages.prefix() + messages.remove());
                                    break;
                            }
                            break;
                        case "alts":
                            if (player.hasPermission(PluginPermission.altInfo())) {
                                if (args.length == 2) {
                                    String target = args[1];
                                    OfflineClient offline = new OfflineClient(target);
                                    AccountManager manager = offline.getAccount();

                                    if (manager != null) {
                                        AccountData data = new AccountData(null, manager.getUUID());
                                        Set<AccountID> accounts = data.getReverseAlts();

                                        new AltAccountsInventory(player, accounts);
                                    } else {
                                        user.send(messages.prefix() + messages.neverPlayer(target));
                                    }
                                } else {
                                    user.send(messages.prefix() + messages.lookupUsage());
                                }
                            } else {
                                user.send(messages.prefix() + messages.permissionError(PluginPermission.altInfo()));
                            }
                            break;
                        case "session":
                            if (config.enableSessions()) {
                                AccountManager manager = user.getManager();
                                PersistentSessionData persistent = new PersistentSessionData(manager.getUUID());
                                if (persistent.toggleSession()) {
                                    user.send(messages.prefix() + messages.sessionEnabled());
                                } else {
                                    user.send(messages.prefix() + messages.sessionDisabled());
                                }
                            } else {
                                user.send(messages.prefix() + messages.sessionServerDisabled());
                            }
                            break;
                        default:
                            user.send(messages.prefix() + messages.accountArguments());
                            break;
                    }
                }
            } else {
                user.send(messages.prefix() + LockLogin.properties.getProperty("session_not_valid", "&5&oYour session is invalid, try leaving and joining the server again"));
            }
        } else {
            if (args.length == 0) {
                Console.send(messages.prefix() + messages.accountArguments());
            } else {
                String tar_name;
                OfflineClient offline;
                AccountManager manager;

                switch (args[0].toLowerCase()) {
                    case "unlock":
                        tar_name = args[1];
                        offline = new OfflineClient(tar_name);

                        manager = offline.getAccount();
                        if (manager != null) {
                            LockedAccount account = new LockedAccount(manager.getUUID());
                            LockedData data = account.getData();

                            if (data.isLocked()) {
                                if (account.unlock()) {
                                    Console.send(messages.prefix() + messages.accountUnLocked(tar_name));
                                } else {
                                    Console.send(messages.prefix() + messages.accountNotLocked(tar_name));
                                    LockLogin.logger.scheduleLog(Level.GRAVE, "Tried to unlock account of {0} but failed", tar_name);
                                }
                            } else {
                                Console.send(messages.prefix() + messages.accountNotLocked(tar_name));
                            }
                        } else {
                            Console.send(messages.prefix() + messages.neverPlayer(tar_name));
                        }
                        break;
                    case "close":
                        if (args.length == 2) {
                            tar_name = args[1];
                            Player tar_p = LockLogin.plugin.getServer().getPlayer(tar_name);

                            if (tar_p != null && tar_p.isOnline()) {
                                User target = new User(tar_p);
                                ClientSession session = target.getSession();

                                if (session.isValid() && session.isLogged() && session.isTempLogged()) {
                                    target.send(messages.prefix() + messages.forcedClose());
                                    tar_p.performCommand("account close");
                                    Console.send(messages.prefix() + messages.forcedCloseAdmin(fromPlayer(tar_p)));

                                    AccountCloseEvent issuer = new AccountCloseEvent(fromPlayer(tar_p), config.serverName(), null);
                                    ModulePlugin.callEvent(issuer);
                                } else {
                                    Console.send(messages.prefix() + messages.targetAccessError(tar_name));
                                }
                            } else {
                                Console.send(messages.prefix() + messages.connectionError(tar_name));
                            }
                            break;
                        } else {
                            Console.send(messages.prefix() + messages.close());
                        }
                        break;
                    case "remove":
                    case "delete":
                        if (args.length == 2) {
                            String target = args[1];
                            Player online = LockLogin.plugin.getServer().getPlayer(target);
                            offline = new OfflineClient(target);

                            manager = offline.getAccount();
                            if (manager != null) {
                                LockedAccount account = new LockedAccount(manager.getUUID());
                                manager.setPassword("");
                                manager.setPin("");
                                manager.setGAuth("");
                                manager.set2FA(false);

                                if (online != null) {
                                    User onlineUser = new User(online);
                                    onlineUser.kick(messages.forcedAccountRemoval("{ServerName}"));
                                }

                                account.lock(StringUtils.stripColor("{ServerName}"));
                                Console.send(messages.prefix() + messages.forcedAccountRemovalAdmin(target));
                            } else {
                                Console.send(messages.prefix() + messages.neverPlayer(target));
                            }
                        } else {
                            Console.send(messages.prefix() + messages.remove());
                        }
                        break;
                    default:
                        Console.send(messages.prefix() + LockLogin.properties.getProperty("command_not_available", "&cThis command is not available for console"));
                }
            }
        }

        return false;
    }
}
