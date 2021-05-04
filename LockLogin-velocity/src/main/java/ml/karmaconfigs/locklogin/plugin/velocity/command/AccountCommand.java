package ml.karmaconfigs.locklogin.plugin.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.api.velocity.Console;
import ml.karmaconfigs.api.velocity.timer.AdvancedPluginTimer;
import ml.karmaconfigs.locklogin.api.account.AccountID;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.files.PluginConfiguration;
import ml.karmaconfigs.locklogin.api.encryption.CryptoUtil;
import ml.karmaconfigs.locklogin.api.utils.platform.CurrentPlatform;
import ml.karmaconfigs.locklogin.plugin.common.security.client.AccountData;
import ml.karmaconfigs.locklogin.plugin.common.session.SessionDataContainer;
import ml.karmaconfigs.locklogin.plugin.velocity.command.util.BungeeLikeCommand;
import ml.karmaconfigs.locklogin.plugin.velocity.command.util.SystemCommand;
import ml.karmaconfigs.locklogin.plugin.velocity.plugin.sender.AccountParser;
import ml.karmaconfigs.locklogin.plugin.velocity.plugin.sender.DataSender;
import ml.karmaconfigs.locklogin.plugin.velocity.plugin.sender.DataType;
import ml.karmaconfigs.locklogin.plugin.velocity.util.files.client.OfflineClient;
import ml.karmaconfigs.locklogin.plugin.velocity.util.files.data.lock.LockedAccount;
import ml.karmaconfigs.locklogin.plugin.velocity.util.files.data.lock.LockedData;
import ml.karmaconfigs.locklogin.plugin.velocity.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.velocity.util.player.SessionCheck;
import ml.karmaconfigs.locklogin.plugin.velocity.util.player.User;
import net.kyori.adventure.text.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static ml.karmaconfigs.locklogin.plugin.velocity.LockLogin.*;
import static ml.karmaconfigs.locklogin.plugin.velocity.permissibles.PluginPermission.*;

@SystemCommand(command = "account")
public class AccountCommand extends BungeeLikeCommand {

    /**
     * Initialize the bungee like command
     *
     * @param label the command label
     */
    public AccountCommand(String label) {
        super(label);
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
        Message messages = new Message();

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
                            if (user.hasPermission(account())) {
                                if (args.length == 1) {
                                    String target = args[0];
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
                                    user.send(messages.prefix() + messages.accountUnLock());
                                }
                            } else {
                                user.send(messages.prefix() + messages.permissionError(unlockAccount()));
                            }
                            break;
                        case "close":
                            switch (args.length) {
                                case 1:
                                    session = user.getSession();
                                    session.setLogged(false);
                                    session.setPinLogged(false);
                                    session.set2FALogged(false);

                                    DataSender.send(player, DataSender.getBuilder(DataType.CLOSE, DataSender.CHANNEL_PLAYER).build());

                                    user.applySessionEffects();

                                    if (config.clearChat()) {
                                        for (int i = 0; i < 150; i++)
                                            server.getScheduler().buildTask(plugin, () -> player.sendMessage(Component.text().content("").build()));
                                    }

                                    session.validate();

                                    if (!config.captchaOptions().isEnabled())
                                        session.setCaptchaLogged(true);

                                    AdvancedPluginTimer tmp_timer = null;
                                    if (!session.isCaptchaLogged()) {
                                        tmp_timer = new AdvancedPluginTimer(plugin, 1, true);
                                        tmp_timer.addAction(() -> {
                                            player.sendActionBar(Component.text().content(StringUtils.toColor(messages.captcha(session.getCaptcha()))).build());
                                        }).start();
                                    }

                                    AdvancedPluginTimer timer = tmp_timer;
                                    SessionCheck check = new SessionCheck(player, target -> {
                                        player.sendActionBar(Component.text().content("").build());
                                        if (timer != null)
                                            timer.setCancelled();
                                    }, target -> {
                                        player.sendActionBar(Component.text().content("").build());
                                        if (timer != null)
                                            timer.setCancelled();
                                    });

                                    server.getScheduler().buildTask(plugin, check);

                                    user.send(messages.prefix() + messages.closed());
                                    SessionDataContainer.setLogged(SessionDataContainer.getLogged() - 1);
                                    break;
                                case 2:
                                    if (user.hasPermission(account())) {
                                        String tar_name = args[1];
                                        Optional<Player> tar_p = server.getPlayer(tar_name);

                                        if (tar_p.isPresent() && tar_p.get().isActive()) {
                                            User target = new User(tar_p.get());
                                            session = target.getSession();

                                            if (session.isValid() && session.isLogged() && session.isTempLogged()) {
                                                target.send(messages.prefix() + messages.forcedClose());
                                                user.performCommand("/account close");
                                                user.send(messages.prefix() + messages.forcedCloseAdmin(tar_p.get()));

                                                SessionDataContainer.setLogged(SessionDataContainer.getLogged() - 1);
                                            } else {
                                                user.send(messages.prefix() + messages.targetAccessError(tar_name));
                                            }
                                        } else {
                                            user.send(messages.prefix() + messages.connectionError(tar_name));
                                        }
                                    } else {
                                        user.send(messages.prefix() + messages.permissionError(closeAccount()));
                                    }
                                    break;
                                default:
                                    user.send(messages.prefix() + messages.close());
                                    break;
                            }
                            break;
                        case "remove":
                            switch (args.length) {
                                case 2:
                                    if (user.hasPermission(account())) {
                                        String target = args[1];
                                        Optional<Player> online = server.getPlayer(target);
                                        OfflineClient offline = new OfflineClient(target);

                                        AccountManager manager = offline.getAccount();
                                        if (manager != null) {
                                            LockedAccount account = new LockedAccount(manager.getUUID());

                                            manager.set2FA(false);
                                            manager.setGAuth(null);
                                            manager.setPassword(null);
                                            manager.setPin(null);

                                            user.send(messages.prefix() + messages.forcedAccountRemovalAdmin(target));

                                            if (online.isPresent()) {
                                                DataSender.send(online.get(), DataSender.getBuilder(DataType.CLOSE, DataSender.CHANNEL_PLAYER).build());
                                                User onlineUser = new User(online.get());

                                                onlineUser.kick(messages.forcedAccountRemoval(player.getUsername()));
                                            }

                                            account.lock(StringUtils.stripColor(player.getUsername()));

                                            SessionDataContainer.setRegistered(SessionDataContainer.getRegistered() - 1);
                                        } else {
                                            user.send(messages.prefix() + messages.neverPlayer(target));
                                        }
                                    } else {
                                        user.send(messages.prefix() + messages.permissionError(delAccount()));
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

                                            manager.setPassword(null);
                                            manager.setPin(null);
                                            manager.setGAuth(null);
                                            manager.set2FA(false);

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

                                            AdvancedPluginTimer tmp_timer = null;
                                            if (!session.isCaptchaLogged()) {
                                                tmp_timer = new AdvancedPluginTimer(plugin, 1, true);
                                                tmp_timer.addAction(() -> {
                                                    player.sendActionBar(Component.text().content(StringUtils.toColor(messages.captcha(session.getCaptcha()))).build());
                                                }).start();
                                            }

                                            AdvancedPluginTimer timer = tmp_timer;
                                            SessionCheck check = new SessionCheck(player, target -> {
                                                player.sendActionBar(Component.text().content("").build());
                                                if (timer != null)
                                                    timer.setCancelled();
                                            }, target -> {
                                                player.sendActionBar(Component.text().content("").build());
                                                if (timer != null)
                                                    timer.setCancelled();
                                            });

                                            server.getScheduler().buildTask(plugin, check);

                                            SessionDataContainer.setRegistered(SessionDataContainer.getRegistered() - 1);
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
                            if (user.hasPermission(altInfo())) {
                                if (args.length == 2) {
                                    String target = args[1];
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
                                                accounts.add(manager);
                                        }

                                        AccountParser parser = new AccountParser(accounts);
                                        DataSender.send(player, DataSender.getBuilder(DataType.LOOKUPGUI, DataSender.PLUGIN_CHANNEL).addTextData(parser.toString()).build());
                                    } else {
                                        user.send(messages.prefix() + messages.neverPlayer(target));
                                    }
                                } else {
                                    user.send(messages.prefix() + messages.lookupUsage());
                                }
                            } else {
                                user.send(messages.prefix() + messages.permissionError(altInfo()));
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
            Console.send(messages.prefix() + properties.getProperty("console_is_restricted", "&5&oFor security reasons, this command is restricted to players only"));
        }
    }
}
