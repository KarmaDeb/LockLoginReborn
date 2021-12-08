package eu.locklogin.plugin.velocity.command;

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

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import eu.locklogin.api.account.AccountID;
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.common.security.client.AccountData;
import eu.locklogin.api.common.session.PersistentSessionData;
import eu.locklogin.api.common.session.SessionCheck;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.common.utils.other.GlobalAccount;
import eu.locklogin.api.common.utils.other.name.AccountNameDatabase;
import eu.locklogin.api.encryption.CryptoFactory;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.module.plugin.api.event.user.AccountCloseEvent;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.velocity.command.util.BungeeLikeCommand;
import eu.locklogin.plugin.velocity.command.util.SystemCommand;
import eu.locklogin.plugin.velocity.permissibles.PluginPermission;
import eu.locklogin.plugin.velocity.plugin.sender.AccountParser;
import eu.locklogin.plugin.velocity.plugin.sender.DataSender;
import eu.locklogin.plugin.velocity.util.files.client.OfflineClient;
import eu.locklogin.plugin.velocity.util.files.data.lock.LockedAccount;
import eu.locklogin.plugin.velocity.util.files.data.lock.LockedData;
import eu.locklogin.plugin.velocity.util.player.User;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
import net.kyori.adventure.text.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static eu.locklogin.plugin.velocity.LockLogin.*;

@SystemCommand(command = "account")
public class AccountCommand extends BungeeLikeCommand {

    /**
     * Initialize the bungee like command
     *
     * @param name    the command name
     * @param aliases the command aliases
     */
    public AccountCommand(final String name, final List<String> aliases) {
        super(name, aliases.toArray(new String[0]));
    }

    /**
     * Execute this command with the specified sender and arguments.
     *
     * @param sender the executor of this command
     * @param args   arguments used to invoke this command
     */
    @Override
    public void execute(CommandSource sender, String[] args) {
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

                                CryptoFactory util = CryptoFactory.getBuilder().withPassword(password).withToken(manager.getPassword()).build();
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
                            if (user.hasPermission(PluginPermission.account())) {
                                if (args.length == 2) {
                                    String target = args[1];
                                    AccountNameDatabase.find(target).whenComplete((nsr) -> {
                                        if (nsr.singleResult()) {
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
                                                        logger.scheduleLog(Level.GRAVE, "Tried to unlock account of " + target + " but failed");
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

                                    DataSender.send(player, DataSender.getBuilder(DataType.CLOSE, DataSender.CHANNEL_PLAYER, player).build());

                                    user.applySessionEffects();

                                    if (config.clearChat()) {
                                        for (int i = 0; i < 150; i++)
                                            server.getScheduler().buildTask(plugin, () -> player.sendMessage(Component.text().content("").build()));
                                    }

                                    session.validate();

                                    if (!config.captchaOptions().isEnabled())
                                        session.setCaptchaLogged(true);


                                    SessionCheck<Player> check = user.getChecker().whenComplete(user::restorePotionEffects);
                                    server.getScheduler().buildTask(plugin, check);

                                    user.send(messages.prefix() + messages.closed());

                                    AccountCloseEvent self = new AccountCloseEvent(user.getModule(), player.getGameProfile().getName(), null);
                                    ModulePlugin.callEvent(self);
                                    break;
                                case 2:
                                    if (user.hasPermission(PluginPermission.account())) {
                                        String tar_name = args[1];
                                        Optional<Player> tar_p = server.getPlayer(tar_name);

                                        if (tar_p.isPresent() && tar_p.get().isActive()) {
                                            User target = new User(tar_p.get());
                                            session = target.getSession();

                                            if (session.isValid() && session.isLogged() && session.isTempLogged()) {
                                                target.send(messages.prefix() + messages.forcedClose());
                                                target.performCommand("account close");
                                                user.send(messages.prefix() + messages.forcedCloseAdmin(target.getModule()));

                                                AccountCloseEvent issuer = new AccountCloseEvent(target.getModule(), player.getGameProfile().getName(), null);
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
                                    if (user.hasPermission(PluginPermission.account())) {
                                        String target = args[1];
                                        AccountNameDatabase.find(target).whenComplete((nsr) -> {
                                            if (nsr.singleResult()) {
                                                Optional<Player> online = server.getPlayer(target);
                                                OfflineClient offline = new OfflineClient(target);

                                                AccountManager manager = offline.getAccount();
                                                if (manager != null) {
                                                    LockedAccount account = new LockedAccount(manager.getUUID());
                                                    if (account.getData().isLocked()) {
                                                        user.send(messages.prefix() + messages.neverPlayer(target));
                                                    } else {
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
                                                                StringUtils.stripColor(player.getGameProfile().getName()));

                                                        if (online.isPresent()) {
                                                            DataSender.send(online.get(), DataSender.getBuilder(DataType.CLOSE, DataSender.CHANNEL_PLAYER, online.get()).build());
                                                            User onlineUser = new User(online.get());

                                                            onlineUser.kick(messages.forcedAccountRemoval(player.getUsername()));
                                                        }

                                                        account.lock(StringUtils.stripColor(player.getUsername()));
                                                    }
                                                } else {
                                                    user.send(messages.prefix() + messages.neverPlayer(target));
                                                }
                                            } else {
                                                AccountNameDatabase.otherPossible(target).whenComplete((possible) -> user.send(messages.multipleNames(target, possible)));
                                            }
                                        });
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
                                        CryptoFactory util = CryptoFactory.getBuilder().withPassword(password).withToken(manager.getPassword()).build();
                                        if (util.validate()) {
                                            user.send(messages.prefix() + messages.accountRemoved());
                                            manager.remove(player.getGameProfile().getName());

                                            //Completely restart the client session
                                            session.setPinLogged(false);
                                            session.set2FALogged(false);
                                            session.setLogged(false);
                                            session.invalidate();
                                            session.validate();

                                            user.applySessionEffects();

                                            if (config.clearChat()) {
                                                for (int i = 0; i < 150; i++)
                                                    server.getScheduler().buildTask(plugin, () -> player.sendMessage(Component.text().content("").build()));
                                            }

                                            session.validate();

                                            if (!config.captchaOptions().isEnabled())
                                                session.setCaptchaLogged(true);

                                            SessionCheck<Player> check = user.getChecker().whenComplete(user::restorePotionEffects);
                                            server.getScheduler().buildTask(plugin, check);
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
                            if (user.hasPermission(PluginPermission.altInfo())) {
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

                                                for (AccountID id : ids) {
                                                    offline = new OfflineClient(id);
                                                    manager = offline.getAccount();

                                                    if (manager != null)
                                                        accounts.add(new GlobalAccount(manager));
                                                }

                                                int sent = 0;
                                                int max = accounts.size();
                                                for (AccountManager account : accounts) {
                                                    player.sendActionBar(Component.text().content(StringUtils.toColor("&aSending player accounts ( " + sent + " of " + max + " )")).build());

                                                    DataSender.send(player, DataSender.getBuilder(DataType.PLAYER, DataSender.PLUGIN_CHANNEL, player).addTextData(StringUtils.serialize(account)).build());
                                                    sent++;
                                                }

                                                AccountParser parser = new AccountParser(accounts);
                                                DataSender.send(player, DataSender.getBuilder(DataType.LOOKUPGUI, DataSender.PLUGIN_CHANNEL, player).addTextData(parser.toString()).build());
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
                user.send(messages.prefix() + properties.getProperty("session_not_valid", "&5&oYour session is invalid, try leaving and joining the server again"));
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
                                        LockedData data = account.getData();

                                        if (data.isLocked()) {
                                            if (account.unlock()) {
                                                console.send(messages.prefix() + messages.accountUnLocked(tar_name));
                                            } else {
                                                console.send(messages.prefix() + messages.accountNotLocked(tar_name));
                                                logger.scheduleLog(Level.GRAVE, "Tried to unlock account of {0} but failed", tar_name);
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
                            Optional<Player> tar_p = server.getPlayer(tar_name);

                            if (tar_p.isPresent() && tar_p.get().isActive()) {
                                User target = new User(tar_p.get());
                                ClientSession session = target.getSession();

                                if (session.isValid() && session.isLogged() && session.isTempLogged()) {
                                    target.send(messages.prefix() + messages.forcedClose());
                                    target.performCommand("account close");
                                    console.send(messages.prefix() + messages.forcedCloseAdmin(target.getModule()));

                                    AccountCloseEvent event = new AccountCloseEvent(target.getModule(), config.serverName(), null);
                                    ModulePlugin.callEvent(event);
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
                            tar_name = args[1];

                            AccountNameDatabase.find(tar_name).whenComplete((nsr) -> {
                                if (nsr.singleResult()) {
                                    Optional<Player> online = server.getPlayer(tar_name);
                                    OfflineClient offline = new OfflineClient(tar_name);

                                    AccountManager manager = offline.getAccount();
                                    if (manager != null) {
                                        LockedAccount account = new LockedAccount(manager.getUUID());
                                        if (account.getData().isLocked()) {
                                            console.send(messages.prefix() + messages.neverPlayer(tar_name));
                                        } else {
                                            manager.setUnsafePassword("");
                                            manager.setUnsafePin("");
                                            manager.setUnsafeGAuth("");
                                            manager.set2FA(false);

                                            console.send(messages.prefix() +
                                                            properties.getProperty(
                                                                    "account_removed",
                                                                    "&dAccount of {0} removed by {1}"),
                                                    tar_name,
                                                    config.serverName());

                                            if (online.isPresent()) {
                                                DataSender.send(online.get(), DataSender.getBuilder(DataType.CLOSE, DataSender.CHANNEL_PLAYER, online.get()).build());
                                                User onlineUser = new User(online.get());

                                                onlineUser.kick(messages.forcedAccountRemoval(config.serverName()));
                                            }

                                            account.lock("{ServerName}");
                                        }
                                    } else {
                                        console.send(messages.prefix() + messages.neverPlayer(tar_name));
                                    }
                                } else {
                                    AccountNameDatabase.otherPossible(tar_name).whenComplete((possible) -> console.send(messages.multipleNames(tar_name, possible)));
                                }
                            });
                        } else {
                            console.send(messages.prefix() + messages.remove());
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
