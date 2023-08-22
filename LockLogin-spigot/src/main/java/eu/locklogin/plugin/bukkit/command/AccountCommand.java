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
import eu.locklogin.api.common.security.client.CommandProxy;
import eu.locklogin.api.common.session.SessionCheck;
import eu.locklogin.api.common.session.persistence.PersistentSessionData;
import eu.locklogin.api.common.utils.other.LockedAccount;
import eu.locklogin.api.common.utils.other.name.AccountNameDatabase;
import eu.locklogin.api.common.utils.plugin.ComponentFactory;
import eu.locklogin.api.encryption.CryptoFactory;
import eu.locklogin.api.encryption.Validation;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.file.options.PasswordConfig;
import eu.locklogin.api.module.plugin.api.event.user.AccountCloseEvent;
import eu.locklogin.api.module.plugin.api.event.user.UserChangePasswordEvent;
import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.client.permission.plugin.PluginPermissions;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import eu.locklogin.api.util.enums.ManagerType;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.LockLogin;
import eu.locklogin.plugin.bukkit.TaskTarget;
import eu.locklogin.plugin.bukkit.command.util.SystemCommand;
import eu.locklogin.plugin.bukkit.util.files.client.OfflineClient;
import eu.locklogin.plugin.bukkit.util.inventory.AltAccountsInventory;
import eu.locklogin.plugin.bukkit.util.player.ClientVisor;
import eu.locklogin.plugin.bukkit.util.player.User;
import ml.karmaconfigs.api.common.security.token.TokenGenerator;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.utils.enums.Level;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static eu.locklogin.plugin.bukkit.LockLogin.*;

@SystemCommand(command = "account")
@SuppressWarnings("unused")
public class AccountCommand implements CommandExecutor {

    private final static Map<String, String> confirmation = new ConcurrentHashMap<>();

    /**
     * Executes the given command, returning its success.
     * <br>
     * If false is returned, then the "usage" plugin.yml entry for this command
     * (if defined) will be sent to the player.
     *
     * @param sender  Source of the command
     * @param command Command which was executed
     * @param label   Alias of the command which was used
     * @param tmpArgs    Passed command arguments
     * @return true if a valid command, otherwise false
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] tmpArgs) {
        PluginConfiguration config = CurrentPlatform.getConfiguration();
        PluginMessages messages = CurrentPlatform.getMessages();

        if (sender instanceof Player) {
            Player player = (Player) sender;
            User user = new User(player);

            if (user.getSession().isValid()) {
                boolean validated = false;

                String[] args = new String[0];
                if (tmpArgs.length >= 1) {
                    String last_arg = tmpArgs[tmpArgs.length - 1];
                    try {
                        UUID command_id = UUID.fromString(last_arg);
                        args = CommandProxy.getArguments(command_id);
                        validated = true;
                    } catch (Throwable ignored) {}
                }

                if (!validated) {
                    if (!user.getSession().isLogged()) {
                        user.send(messages.prefix() + messages.register());
                    } else {
                        if (user.getSession().isTempLogged()) {
                            user.send(messages.prefix() + messages.gAuthenticate());
                        } else {
                            user.send(messages.prefix() + messages.alreadyRegistered());
                        }
                    }

                    return false;
                }

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

                                PasswordConfig passwordConfig = config.passwordConfig();
                                Map.Entry<Boolean, String[]> rs = passwordConfig.check(new_pass);

                                CryptoFactory util = CryptoFactory.getBuilder().withPassword(password).withToken(manager.getPassword()).build();
                                if (util.validate(Validation.ALL)) {
                                    UserChangePasswordEvent.ChangeResult result;
                                    if (!password.equals(new_pass)) {
                                        if (rs.getKey()) {
                                            result = UserChangePasswordEvent.ChangeResult.ALLOWED;
                                        } else {
                                            result = (passwordConfig.block_unsafe() ? UserChangePasswordEvent.ChangeResult.DENIED_UNSAFE : UserChangePasswordEvent.ChangeResult.ALLOWED_UNSAFE);
                                        }
                                    } else {
                                        result = UserChangePasswordEvent.ChangeResult.DENIED_SAME;
                                    }

                                    Event event = new UserChangePasswordEvent(user.getModule(), result);
                                    ModulePlugin.callEvent(event);

                                    if (event.isHandled()) {
                                        user.send(messages.prefix() + event.getHandleReason());
                                    } else {
                                        switch (result) {
                                            case ALLOWED:
                                                manager.setPassword(new_pass);
                                                user.send(messages.prefix() + messages.changeDone());
                                                break;
                                            case DENIED_SAME:
                                                user.send(messages.prefix() + messages.changeSame());
                                                break;
                                            case DENIED_UNSAFE:
                                            case ALLOWED_UNSAFE:
                                            default:
                                                if (result.equals(UserChangePasswordEvent.ChangeResult.ALLOWED_UNSAFE)) {
                                                    manager.setPassword(new_pass);
                                                    user.send(messages.prefix() + messages.loginInsecure());

                                                    if (passwordConfig.warn_unsafe()) {
                                                        for (Player online : plugin.getServer().getOnlinePlayers()) {
                                                            User staff = new User(online);
                                                            if (staff.hasPermission(PluginPermissions.warn_password())) {
                                                                staff.send(messages.prefix() + messages.passwordWarning());
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    user.send(messages.prefix() + messages.passwordInsecure());
                                                }

                                                if (passwordConfig.warn_unsafe()) {
                                                    for (String msg : rs.getValue()) {
                                                        if (msg != null)
                                                            user.send(msg);
                                                    }
                                                }
                                                break;
                                        }
                                    }
                                } else {
                                    user.send(messages.prefix() + messages.incorrectPassword());
                                }
                            } else {
                                user.send(messages.prefix() + messages.change());
                            }
                            break;
                        case "sync":
                            //TODO: Synchronize player account with an account in the panel
                            break;
                        case "unlock":
                            if (user.hasPermission(PluginPermissions.account_unlock())) {
                                if (args.length == 2) {
                                    String target = args[1];
                                    AccountNameDatabase.find(target).whenComplete((nsr) -> {
                                        if (nsr.singleResult()) {
                                            OfflineClient offline = new OfflineClient(target);

                                            AccountManager manager = offline.getAccount();
                                            if (manager != null) {
                                                LockedAccount account = new LockedAccount(manager.getUUID());

                                                if (account.isLocked()) {
                                                    if (account.release()) {
                                                        user.send(messages.prefix() + messages.accountUnLocked(target));
                                                    } else {
                                                        user.send(messages.prefix() + messages.accountNotLocked(target));
                                                        LockLogin.logger.scheduleLog(Level.GRAVE, "{0} tried to unlock account of {1} but failed", StringUtils.stripColor(player.getDisplayName()), target);
                                                    }
                                                } else {
                                                    user.send(messages.prefix() + messages.accountNotLocked(target));
                                                }
                                            } else {
                                                user.send(messages.prefix() + messages.neverPlayer(target));
                                            }
                                        } else {
                                            AccountNameDatabase.otherPossible(target).whenComplete((possible) -> user.send(messages.multipleNames(target, possible)));
                                        }
                                    });
                                } else {
                                    user.send(messages.prefix() + messages.accountUnLock());
                                }
                            } else {
                                user.send(messages.prefix() + messages.permissionError(PluginPermissions.account_unlock()));
                            }
                            break;
                        case "close":
                            switch (args.length) {
                                case 1:
                                    if (user.hasPermission(PluginPermissions.account_close_self())) {
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
                                            visor.toggleView();
                                        }

                                        user.send(messages.prefix() + messages.closed());
                                        AccountCloseEvent self = new AccountCloseEvent(user.getModule(), user.getManager().getName(), null);
                                        ModulePlugin.callEvent(self);

                                        ModulePlayer module = user.getModule();
                                        if (!module.hasPermission(PluginPermissions.leave_silent())) {
                                            String message = messages.playerLeave(module);
                                            if (!StringUtils.isNullOrEmpty(message)) {
                                                Bukkit.getServer().broadcastMessage(StringUtils.toColor(message));
                                            }
                                        }
                                    } else {
                                        user.send(messages.prefix() + messages.permissionError(PluginPermissions.account_close_self()));
                                    }
                                    break;
                                case 2:
                                    if (user.hasPermission(PluginPermissions.account_close())) {
                                        String tar_name = args[1];
                                        Player tar_p = LockLogin.plugin.getServer().getPlayer(tar_name);

                                        if (tar_p != null && tar_p.isOnline()) {
                                            User target = new User(tar_p);
                                            session = target.getSession();

                                            if (session.isValid() && session.isLogged() && session.isTempLogged()) {
                                                target.send(messages.prefix() + messages.forcedClose());

                                                String cmd = "account close";
                                                UUID cmd_id = CommandProxy.mask(cmd, "close");
                                                String exec = CommandProxy.getCommand(cmd_id);

                                                trySync(TaskTarget.COMMAND_FORCE, () -> tar_p.performCommand(exec + " " + cmd_id));
                                                user.send(messages.prefix() + messages.forcedCloseAdmin(target.getModule()));

                                                AccountCloseEvent issuer = new AccountCloseEvent(target.getModule(), user.getManager().getName(), null);
                                                ModulePlugin.callEvent(issuer);
                                            } else {
                                                user.send(messages.prefix() + messages.targetAccessError(tar_name));
                                            }
                                        } else {
                                            user.send(messages.prefix() + messages.connectionError(tar_name));
                                        }
                                    } else {
                                        user.send(messages.prefix() + messages.permissionError(PluginPermissions.account_close()));
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
                                    if (user.hasPermission(PluginPermissions.account_remove())) {
                                        String target = args[1];
                                        AccountNameDatabase.find(target).whenComplete((nsr) -> {
                                            if (nsr.singleResult()) {
                                                Player online = LockLogin.plugin.getServer().getPlayer(target);
                                                OfflineClient offline = new OfflineClient(target);

                                                AccountManager manager = offline.getAccount();
                                                if (manager != null) {
                                                    LockedAccount account = new LockedAccount(manager.getUUID());
                                                    if (account.isLocked()) {
                                                        user.send(messages.prefix() + messages.neverPlayer(target));
                                                    } else {
                                                        if (!manager.getPanic().isEmpty()) {
                                                            String stored = confirmation.getOrDefault(player.getUniqueId().toString(), null);
                                                            if (stored == null || !stored.equalsIgnoreCase(target)) {
                                                                user.send(messages.prefix() + "Client has a panic token, this won't be removed even after removing the client account. Run the command again to proceed anyway");
                                                                confirmation.put(player.getUniqueId().toString(), stored);
                                                                return;
                                                            }
                                                        }

                                                        confirmation.remove(player.getUniqueId().toString());
                                                        manager.setUnsafePassword("");
                                                        manager.setUnsafePin("");
                                                        manager.setUnsafeGAuth("");
                                                        manager.set2FA(false);

                                                        user.send(messages.prefix() + messages.forcedAccountRemovalAdmin(target));
                                                        console.send(messages.prefix() +
                                                                        properties.getProperty(
                                                                                "account_removed",
                                                                                "&dAccount of {0} removed by {1}"),
                                                                target,
                                                                StringUtils.stripColor(player.getName()));

                                                        if (online != null) {
                                                            User onlineUser = new User(online);
                                                            onlineUser.kick(messages.forcedAccountRemoval(player.getDisplayName()));
                                                        }

                                                        account.lock(StringUtils.stripColor(player.getDisplayName()));
                                                    }
                                                } else {
                                                    user.send(messages.prefix() + messages.neverPlayer(target));
                                                }
                                            } else {
                                                AccountNameDatabase.otherPossible(target).whenComplete((possible) -> user.send(messages.multipleNames(target, possible)));
                                            }
                                        });
                                    } else {
                                        user.send(messages.prefix() + messages.permissionError(PluginPermissions.account_remove()));
                                    }
                                    break;
                                case 3:
                                    if (user.hasPermission(PluginPermissions.account_remove_self())) {
                                        AccountManager manager = user.getManager();
                                        session = user.getSession();

                                        String password = args[1];
                                        String confirmation = args[2];

                                        if (password.equals(confirmation)) {
                                            CryptoFactory util = CryptoFactory.getBuilder().withPassword(password).withToken(manager.getPassword()).build();
                                            if (util.validate(Validation.ALL)) {
                                                if (!manager.getPanic().isEmpty()) {
                                                    String stored = AccountCommand.confirmation.getOrDefault(player.getUniqueId().toString(), null);
                                                    if (stored == null || !stored.equalsIgnoreCase(player.getUniqueId().toString())) {
                                                        user.send(messages.prefix() + "&cYou have a panic token, removing your account will result in also removing it. Run the command again to proceed anyway");
                                                        AccountCommand.confirmation.put(player.getUniqueId().toString(), player.getUniqueId().toString());
                                                        return false;
                                                    }
                                                }

                                                AccountCommand.confirmation.remove(player.getUniqueId().toString());
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
                                    } else {
                                        user.send(messages.prefix() + messages.permissionError(PluginPermissions.account_remove_self()));
                                    }
                                    break;
                                default:
                                    user.send(messages.prefix() + messages.remove());
                                    break;
                            }
                            break;
                        case "create":
                        case "register":
                            if (user.hasPermission(PluginPermissions.account_register())) {
                                if (args.length == 3) {
                                    String username = args[1];
                                    String password = args[2];

                                    Player online = plugin.getServer().getPlayer(username);
                                    if (online != null) {
                                        user.send(messages.prefix() + messages.forceRegisterOnline(username));
                                        return false;
                                    }

                                    AccountID id = AccountID.forUsername(username);
                                    AccountManager manager = CurrentPlatform.getAccountManager(ManagerType.CUSTOM, id);
                                    if (manager == null) {
                                        user.send(messages.prefix() + messages.forceRegisterError(username));
                                        return false;
                                    }

                                    if (manager.isRegistered()) {
                                        user.send(messages.prefix() + messages.forceRegisterExists(username));
                                        return false;
                                    }

                                    manager.setPassword(password);
                                    manager.setName(username);
                                    manager.saveUUID(id);

                                    user.send(messages.prefix() + messages.forceRegisterSuccess(username));
                                } else {
                                    user.send(messages.prefix() + messages.forceRegisterUsage());
                                }
                            } else {
                                user.send(messages.prefix() + messages.permissionError(PluginPermissions.account_register()));
                            }
                            break;
                        case "alts":
                            if (user.hasPermission(PluginPermissions.info_alt())) {
                                if (args.length == 2) {
                                    String target = args[1];
                                    AccountNameDatabase.find(target).whenComplete((nsr) -> {
                                        if (nsr.singleResult()) {
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
                                            AccountNameDatabase.otherPossible(target).whenComplete((possible) -> user.send(messages.multipleNames(target, possible)));
                                        }
                                    });
                                } else {
                                    user.send(messages.prefix() + messages.lookupUsage());
                                }
                            } else {
                                user.send(messages.prefix() + messages.permissionError(PluginPermissions.info_alt()));
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
                        case "protect":
                            AccountManager manager = user.getManager();
                            if (manager.getPanic().isEmpty()) {
                                String password = TokenGenerator.generateLiteral(32);

                                user.send(messages.panicRequested());
                                ComponentFactory cf = new ComponentFactory(StringUtils.toColor("&7Panic token: &e" + password))
                                        .hover(StringUtils.toColor("&bClick to copy"))
                                        .click(ClickEvent.Action.SUGGEST_COMMAND, password);
                                user.send(cf.get());

                                manager.setPanic(password);
                            } else {
                                user.send(messages.prefix() + messages.panicAlready());
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
            if (tmpArgs.length == 0) {
                console.send(messages.prefix() + messages.accountArguments());
            } else {
                String tar_name;

                switch (tmpArgs[0].toLowerCase()) {
                    case "unlock":
                        if (tmpArgs.length == 2) {
                            tar_name = tmpArgs[1];
                            AccountNameDatabase.find(tar_name).whenComplete((nsr) -> {
                                if (nsr.singleResult()) {
                                    OfflineClient offline = new OfflineClient(tar_name);

                                    AccountManager manager = offline.getAccount();
                                    if (manager != null) {
                                        LockedAccount account = new LockedAccount(manager.getUUID());

                                        if (account.isLocked()) {
                                            if (account.release()) {
                                                console.send(messages.prefix() + messages.accountUnLocked(tar_name));
                                            } else {
                                                console.send(messages.prefix() + messages.accountNotLocked(tar_name));
                                                LockLogin.logger.scheduleLog(Level.GRAVE, "{0} tried to unlock account of {1} but failed", config.serverName(), tar_name);
                                            }
                                        } else {
                                            console.send(messages.prefix() + messages.accountNotLocked(tar_name));
                                        }
                                    } else {
                                        console.send(messages.prefix() + messages.neverPlayer(tar_name));
                                    }
                                } else {
                                    AccountNameDatabase.otherPossible(tar_name).whenComplete((possible) -> console.send(messages.multipleNames(tar_name, possible)));
                                }
                            });
                        } else {
                            console.send(messages.prefix() + messages.accountUnLock());
                        }
                        break;
                    case "close":
                        if (tmpArgs.length == 2) {
                            tar_name = tmpArgs[1];
                            Player tar_p = LockLogin.plugin.getServer().getPlayer(tar_name);

                            if (tar_p != null && tar_p.isOnline()) {
                                User target = new User(tar_p);
                                ClientSession session = target.getSession();

                                if (session.isValid() && session.isLogged() && session.isTempLogged()) {
                                    target.send(messages.prefix() + messages.forcedClose());

                                    String cmd = "account close";
                                    UUID cmd_id = CommandProxy.mask(cmd, "close");
                                    String exec = CommandProxy.getCommand(cmd_id);

                                    trySync(TaskTarget.COMMAND_FORCE, () -> tar_p.performCommand(exec + " " + cmd_id));
                                    console.send(messages.prefix() + messages.forcedCloseAdmin(target.getModule()));

                                    AccountCloseEvent issuer = new AccountCloseEvent(target.getModule(), config.serverName(), null);
                                    ModulePlugin.callEvent(issuer);
                                } else {
                                    console.send(messages.prefix() + messages.targetAccessError(tar_name));
                                }
                            } else {
                                console.send(messages.prefix() + messages.connectionError(tar_name));
                            }
                            break;
                        } else {
                            console.send(messages.prefix() + messages.close());
                        }
                        break;
                    case "remove":
                    case "delete":
                        if (tmpArgs.length == 2) {
                            String target = tmpArgs[1];
                            AccountNameDatabase.find(target).whenComplete((nsr) -> {
                                if (nsr.singleResult()) {
                                    Player online = plugin.getServer().getPlayer(nsr.getUniqueId());
                                    OfflineClient offline = new OfflineClient(target);

                                    AccountManager manager = offline.getAccount();
                                    if (manager != null) {
                                        LockedAccount account = new LockedAccount(manager.getUUID());
                                        if (account.isLocked()) {
                                            console.send(messages.prefix() + messages.neverPlayer(target));
                                        } else {
                                            if (!manager.getPanic().isEmpty()) {
                                                String stored = confirmation.getOrDefault(config.serverName(), null);
                                                if (stored == null || !stored.equalsIgnoreCase(target)) {
                                                    console.send(messages.prefix() + "&cClient has a panic token, this won't be removed even after removing the client account. Run the command again to proceed anyway");
                                                    confirmation.put(config.serverName(), target);

                                                    return;
                                                }
                                            }

                                            confirmation.remove(config.serverName());
                                            manager.setUnsafePassword("");
                                            manager.setUnsafePin("");
                                            manager.setUnsafeGAuth("");
                                            manager.set2FA(false);

                                            console.send(messages.prefix() +
                                                            properties.getProperty(
                                                                    "account_removed",
                                                                    "&dAccount of {0} removed by {1}"),
                                                    target,
                                                    config.serverName());

                                            if (online != null) {
                                                User onlineUser = new User(online);
                                                onlineUser.kick(messages.forcedAccountRemoval(config.serverName()));
                                            }

                                            account.lock("{ServerName}");
                                        }
                                    } else {
                                        console.send(messages.prefix() + messages.neverPlayer(target));
                                    }
                                } else {
                                    AccountNameDatabase.otherPossible(target).whenComplete((possible) -> console.send(messages.multipleNames(target, possible)));
                                }
                            });
                        } else {
                            console.send(messages.prefix() + messages.remove());
                        }
                        break;
                    case "create":
                    case "register":
                        if (tmpArgs.length == 3) {
                            String username = tmpArgs[1];
                            String password = tmpArgs[2];

                            Player online = plugin.getServer().getPlayer(username);
                            if (online != null) {
                                console.send(messages.prefix() + messages.forceRegisterOnline(username));
                                return false;
                            }

                            AccountID id = AccountID.forUsername(username);
                            AccountManager manager = CurrentPlatform.getAccountManager(ManagerType.CUSTOM, id);
                            if (manager == null) {
                                console.send(messages.prefix() + messages.forceRegisterError(username));
                                return false;
                            }

                            if (manager.isRegistered()) {
                                console.send(messages.prefix() + messages.forceRegisterExists(username));
                                return false;
                            }

                            manager.setPassword(password);
                            manager.setName(username);
                            manager.saveUUID(id);

                            console.send(messages.prefix() + messages.forceRegisterSuccess(username));
                        } else {
                            console.send(messages.prefix() + messages.forceRegisterUsage());
                        }
                        break;
                    default:
                        console.send(messages.prefix() + LockLogin.properties.getProperty("command_not_available", "&cThis command is not available for console"));
                        break;
                }
            }
        }

        return false;
    }
}
