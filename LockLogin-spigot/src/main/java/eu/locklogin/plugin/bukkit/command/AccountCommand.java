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
import eu.locklogin.api.common.security.Password;
import eu.locklogin.api.common.security.client.AccountData;
import eu.locklogin.api.common.session.PersistentSessionData;
import eu.locklogin.api.common.session.SessionCheck;
import eu.locklogin.api.common.utils.other.LockedAccount;
import eu.locklogin.api.common.utils.other.name.AccountNameDatabase;
import eu.locklogin.api.encryption.CryptoFactory;
import eu.locklogin.api.encryption.Validation;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.module.plugin.api.event.user.AccountCloseEvent;
import eu.locklogin.api.module.plugin.api.event.user.UserChangePasswordEvent;
import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.client.permission.plugin.PluginPermissions;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.LockLogin;
import eu.locklogin.plugin.bukkit.TaskTarget;
import eu.locklogin.plugin.bukkit.command.util.SystemCommand;
import eu.locklogin.plugin.bukkit.util.files.client.OfflineClient;
import eu.locklogin.plugin.bukkit.util.inventory.AltAccountsInventory;
import eu.locklogin.plugin.bukkit.util.player.ClientVisor;
import eu.locklogin.plugin.bukkit.util.player.User;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.security.token.TokenGenerator;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
import net.md_5.bungee.api.chat.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static eu.locklogin.plugin.bukkit.LockLogin.*;

@SystemCommand(command = "account")
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
                    ClientSession session = user.getSession();
                    switch (args[0].toLowerCase()) {
                        case "change":
                            if (args.length == 3) {
                                String password = args[1];
                                String new_pass = args[2];

                                AccountManager manager = user.getManager();

                                CryptoFactory util = CryptoFactory.getBuilder().withPassword(password).withToken(manager.getPassword()).build();
                                if (util.validate(Validation.ALL)) {
                                    UserChangePasswordEvent.ChangeResult result;
                                    if (!password.equals(new_pass)) {
                                        Password secure = new Password(new_pass);
                                        if (secure.isSecure()) {
                                            result = UserChangePasswordEvent.ChangeResult.ALLOWED;
                                        } else {
                                            result = (config.blockUnsafePasswords() ? UserChangePasswordEvent.ChangeResult.DENIED_UNSAFE : UserChangePasswordEvent.ChangeResult.ALLOWED_UNSAFE);
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
                                            case ALLOWED_UNSAFE:
                                                manager.setPassword(new_pass);
                                                user.send(messages.prefix() + messages.loginInsecure());
                                                break;
                                            case DENIED_SAME:
                                                user.send(messages.prefix() + messages.changeSame());
                                                break;
                                            case DENIED_UNSAFE:
                                            default:
                                                user.send(messages.prefix() + messages.passwordInsecure());
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
                                                trySync(TaskTarget.COMMAND_FORCE, () -> tar_p.performCommand("account close"));
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
                                    break;
                                default:
                                    user.send(messages.prefix() + messages.remove());
                                    break;
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
                            /*
                            TODO: I must implement LockLogin web panel into this. ( I MUST ALSO END THE PANEL TOO. I HAVE TO DO MANY THINGS NOW )

                            AccountManager manager = user.getManager();
                            if (StringUtils.isNullOrEmpty(manager.getPanic())) {
                                String token = TokenGenerator.generateLiteral(32);

                                user.send(messages.panicRequested());

                            } else {
                                user.send(messages.prefix() + messages.panicAlready());
                            }*/
                            //user.send(messages.prefix() + "&5TODO"); Fuck it, I'll make it local temporally
                            AccountManager manager = user.getManager();
                            if (manager.getPanic().isEmpty()) {
                                String password = TokenGenerator.generateLiteral(32);

                                user.send(messages.panicRequested());
                                TextComponent component = new TextComponent(StringUtils.toColor("&7Panic token: &c" + password));
                                try {
                                    component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder().append(StringUtils.toColor("&bClick to copy")).create()));
                                } catch (Throwable ex) {
                                    component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{new TextComponent(StringUtils.toColor("&bClick to copy"))}));
                                }
                                component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, password));
                                user.send(component);

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
            if (args.length == 0) {
                console.send(messages.prefix() + messages.accountArguments());
            } else {
                String tar_name;

                switch (args[0].toLowerCase()) {
                    case "unlock":
                        if (args.length == 2) {
                            tar_name = args[1];
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
                        if (args.length == 2) {
                            tar_name = args[1];
                            Player tar_p = LockLogin.plugin.getServer().getPlayer(tar_name);

                            if (tar_p != null && tar_p.isOnline()) {
                                User target = new User(tar_p);
                                ClientSession session = target.getSession();

                                if (session.isValid() && session.isLogged() && session.isTempLogged()) {
                                    target.send(messages.prefix() + messages.forcedClose());
                                    trySync(TaskTarget.COMMAND_FORCE, () -> tar_p.performCommand("account close"));
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
                        if (args.length == 2) {
                            String target = args[1];
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
                    default:
                        console.send(messages.prefix() + LockLogin.properties.getProperty("command_not_available", "&cThis command is not available for console"));
                        break;
                }
            }
        }

        return false;
    }
}
