package eu.locklogin.plugin.bungee.command;

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
import eu.locklogin.api.common.utils.Channel;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.common.utils.other.LockedAccount;
import eu.locklogin.api.common.utils.other.RawPlayerAccount;
import eu.locklogin.api.common.utils.other.name.AccountNameDatabase;
import eu.locklogin.api.common.utils.plugin.ComponentFactory;
import eu.locklogin.api.encryption.CryptoFactory;
import eu.locklogin.api.encryption.Validation;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.file.options.PasswordConfig;
import eu.locklogin.api.module.plugin.api.event.user.AccountCloseEvent;
import eu.locklogin.api.module.plugin.api.event.user.UserChangePasswordEvent;
import eu.locklogin.api.module.plugin.api.event.user.UserChangePasswordEvent.ChangeResult;
import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.client.permission.plugin.PluginPermissions;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.security.Password;
import eu.locklogin.api.util.enums.ManagerType;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bungee.BungeeSender;
import eu.locklogin.plugin.bungee.com.message.DataMessage;
import eu.locklogin.plugin.bungee.command.util.SystemCommand;
import eu.locklogin.plugin.bungee.plugin.Manager;
import eu.locklogin.plugin.bungee.plugin.sender.AccountParser;
import eu.locklogin.plugin.bungee.util.files.client.OfflineClient;
import eu.locklogin.plugin.bungee.util.player.User;
import ml.karmaconfigs.api.common.security.token.TokenGenerator;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.utils.enums.Level;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static eu.locklogin.plugin.bungee.LockLogin.*;

@SystemCommand(command = "account")
@SuppressWarnings("unused")
public class  AccountCommand extends Command {

    private final static Map<String, String> confirmation = new ConcurrentHashMap<>();

    /**
     * Construct a new command with no permissions or aliases.
     *
     * @param name the name of this command
     */
    public AccountCommand(final String name, final List<String> aliases) {
        super(name, "", aliases.toArray(new String[0]));
    }

    /**
     * Execute this command with the specified sender and arguments.
     *
     * @param sender the executor of this command
     * @param tmpArgs   arguments used to invoke this command
     */
    @Override
    public void execute(CommandSender sender, String[] tmpArgs) {
        PluginConfiguration config = CurrentPlatform.getConfiguration();
        PluginMessages messages = CurrentPlatform.getMessages();

        if (sender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) sender;
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

                    return;
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
                                    ChangeResult result;
                                    if (!password.equals(new_pass)) {
                                        Password secure = new Password(new_pass);
                                        if (secure.isSecure()) {
                                            result = ChangeResult.ALLOWED;
                                        } else {
                                            result = (passwordConfig.block_unsafe() ? UserChangePasswordEvent.ChangeResult.DENIED_UNSAFE : UserChangePasswordEvent.ChangeResult.ALLOWED_UNSAFE);
                                        }
                                    } else {
                                        result = ChangeResult.DENIED_SAME;
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
                                                        for (ProxiedPlayer online : plugin.getProxy().getPlayers()) {
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
                                                    for (String msg : rs.getValue())
                                                        if (msg != null)
                                                            user.send(msg);
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
                                                        logger.scheduleLog(Level.GRAVE, "{0} tried to unlock account of {1} but failed", StringUtils.stripColor(player.getDisplayName()), target);
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
                                        user.removeSessionCheck();
                                        session = user.getSession();
                                        session.setLogged(false);
                                        session.setPinLogged(false);
                                        session.set2FALogged(false);

                                        Manager.sender.queue(BungeeSender.serverFromPlayer(player)).insert(DataMessage.newInstance(DataType.CLOSE, Channel.ACCOUNT, player)
                                                .getInstance().build());
                                        user.applySessionEffects();

                                        if (config.clearChat()) {
                                            for (int i = 0; i < 150; i++)
                                                plugin.getProxy().getScheduler().runAsync(plugin, () -> player.sendMessage(TextComponent.fromLegacyText("")));
                                        }

                                        SessionCheck<ProxiedPlayer> check = user.getChecker().whenComplete(user::restorePotionEffects);
                                        plugin.getProxy().getScheduler().runAsync(plugin, check);

                                        user.send(messages.prefix() + messages.closed());
                                        AccountCloseEvent self = new AccountCloseEvent(user.getModule(), user.getManager().getName(), null);
                                        ModulePlugin.callEvent(self);
                                    } else {
                                        user.send(messages.prefix() + messages.permissionError(PluginPermissions.account_close_self()));
                                    }
                                    break;
                                case 2:
                                    if (user.hasPermission(PluginPermissions.account_close())) {
                                        String tar_name = args[1];
                                        ProxiedPlayer tar_p = plugin.getProxy().getPlayer(tar_name);

                                        if (tar_p != null && tar_p.isConnected()) {
                                            User target = new User(tar_p);
                                            target.removeSessionCheck();
                                            session = target.getSession();

                                            if (session.isValid() && session.isLogged() && session.isTempLogged()) {
                                                target.send(messages.prefix() + messages.forcedClose());

                                                String cmd = "account close";
                                                UUID cmd_id = CommandProxy.mask(cmd, "close");
                                                String exec = CommandProxy.getCommand(cmd_id);

                                                target.performCommand(exec);
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
                        case "delete":
                        case "remove":
                            switch (args.length) {
                                case 2:
                                    if (user.hasPermission(PluginPermissions.account_remove())) {
                                        String target = args[1];
                                        AccountNameDatabase.find(target).whenComplete((nsr) -> {
                                            if (nsr.singleResult()) {
                                                ProxiedPlayer online = plugin.getProxy().getPlayer(target);
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
                                                            Manager.sender.queue(BungeeSender.serverFromPlayer(player)).insert(DataMessage.newInstance(DataType.CLOSE, Channel.ACCOUNT, player)
                                                                            .getInstance().build());

                                                            User onlineUser = new User(online);
                                                            onlineUser.removeSessionCheck();

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
                                        user.removeSessionCheck();
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
                                                        return;
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

                                                Manager.sender.queue(BungeeSender.serverFromPlayer(player)).insert(DataMessage.newInstance(DataType.CLOSE, Channel.ACCOUNT, player)
                                                        .getInstance().build());

                                                user.applySessionEffects();

                                                if (config.clearChat()) {
                                                    for (int i = 0; i < 150; i++)
                                                        plugin.getProxy().getScheduler().runAsync(plugin, () -> player.sendMessage(TextComponent.fromLegacyText("")));
                                                }

                                                SessionCheck<ProxiedPlayer> check = user.getChecker().whenComplete(user::restorePotionEffects);
                                                plugin.getProxy().getScheduler().runAsync(plugin, check);
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

                                    ProxiedPlayer online = plugin.getProxy().getPlayer(username);
                                    if (online != null) {
                                        user.send(messages.prefix() + messages.forceRegisterOnline(username));
                                        return;
                                    }

                                    AccountID id = AccountID.forUsername(username);
                                    AccountManager manager = CurrentPlatform.getAccountManager(ManagerType.CUSTOM, id);
                                    if (manager == null) {
                                        user.send(messages.prefix() + messages.forceRegisterError(username));
                                        return;
                                    }

                                    if (manager.isRegistered()) {
                                        user.send(messages.prefix() + messages.forceRegisterExists(username));
                                        return;
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
                                                Set<AccountID> ids = data.getReverseAlts();
                                                Set<AccountManager> accounts = new HashSet<>();

                                                Set<String> added_ids = new HashSet<>();
                                                for (AccountID id : ids) {
                                                    if (!added_ids.contains(id.getId())) {
                                                        added_ids.add(id.getId());

                                                        offline = new OfflineClient(id);
                                                        manager = offline.getAccount();

                                                        if (manager != null)
                                                            accounts.add(manager);
                                                    }
                                                }

                                                int sent = 0;
                                                int max = accounts.size();
                                                for (AccountManager account : accounts) {
                                                    player.sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(StringUtils.toColor("&aSending player accounts ( " + sent + " of " + max + " )")));

                                                    Manager.sender.queue(BungeeSender.serverFromPlayer(player)).insert(DataMessage.newInstance(DataType.PLAYER, Channel.PLUGIN, player)
                                                            .addProperty("account", StringUtils.serialize(RawPlayerAccount.fromPlayerAccount(account)))
                                                            .getInstance().build());

                                                    sent++;
                                                }

                                                AccountParser parser = new AccountParser(accounts);
                                                Manager.sender.queue(BungeeSender.serverFromPlayer(player)).insert(DataMessage.newInstance(DataType.LOOKUPGUI, Channel.PLUGIN, player)
                                                        .addProperty("player_info", parser.toString())
                                                        .getInstance().build());
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
                user.send(messages.prefix() + properties.getProperty("session_not_valid", "&5&oYour session is invalid, try leaving and joining the server again"));
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
                                                logger.scheduleLog(Level.GRAVE, "{0} tried to unlock account of {1} but failed", config.serverName(), tar_name);
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
                            ProxiedPlayer tar_p = plugin.getProxy().getPlayer(tar_name);

                            if (tar_p != null && tar_p.isConnected()) {
                                User target = new User(tar_p);
                                target.removeSessionCheck();
                                ClientSession session = target.getSession();

                                if (session.isValid() && session.isLogged() && session.isTempLogged()) {
                                    target.send(messages.prefix() + messages.forcedClose());

                                    String cmd = "account close";
                                    UUID cmd_id = CommandProxy.mask(cmd, "close");
                                    String exec = CommandProxy.getCommand(cmd_id);

                                    target.performCommand(exec);
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
                                    ProxiedPlayer online = plugin.getProxy().getPlayer(target);
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
                                                Manager.sender.queue(BungeeSender.serverFromPlayer(online)).insert(DataMessage.newInstance(DataType.CLOSE, Channel.ACCOUNT, online)
                                                        .getInstance().build());

                                                User onlineUser = new User(online);
                                                onlineUser.removeSessionCheck();

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

                            ProxiedPlayer online = plugin.getProxy().getPlayer(username);
                            if (online != null) {
                                console.send(messages.prefix() + messages.forceRegisterOnline(username));
                                return;
                            }

                            AccountID id = AccountID.forUsername(username);
                            AccountManager manager = CurrentPlatform.getAccountManager(ManagerType.CUSTOM, id);
                            if (manager == null) {
                                console.send(messages.prefix() + messages.forceRegisterError(username));
                                return;
                            }

                            if (manager.isRegistered()) {
                                console.send(messages.prefix() + messages.forceRegisterExists(username));
                                return;
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
                        console.send(messages.prefix() + properties.getProperty("command_not_available", "&cThis command is not available for console"));
                        break;
                }
            }
        }
    }
}
