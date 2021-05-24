package ml.karmaconfigs.locklogin.plugin.bungee.command;

import ml.karmaconfigs.api.bungee.Console;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.account.AccountID;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.utils.platform.CurrentPlatform;
import ml.karmaconfigs.locklogin.plugin.bungee.command.util.SystemCommand;
import ml.karmaconfigs.locklogin.plugin.bungee.plugin.sender.AccountParser;
import ml.karmaconfigs.locklogin.plugin.bungee.plugin.sender.DataSender;
import ml.karmaconfigs.locklogin.plugin.common.session.PersistentSessionData;
import ml.karmaconfigs.locklogin.plugin.common.utils.DataType;
import ml.karmaconfigs.locklogin.plugin.bungee.util.files.client.OfflineClient;
import ml.karmaconfigs.locklogin.plugin.bungee.util.files.data.lock.LockedAccount;
import ml.karmaconfigs.locklogin.plugin.bungee.util.files.data.lock.LockedData;
import ml.karmaconfigs.locklogin.plugin.bungee.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.bungee.util.player.User;
import ml.karmaconfigs.locklogin.plugin.common.utils.plugin.Alias;
import ml.karmaconfigs.locklogin.plugin.common.utils.InstantParser;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.time.Instant;
import java.util.*;

import static ml.karmaconfigs.locklogin.plugin.bungee.LockLogin.plugin;
import static ml.karmaconfigs.locklogin.plugin.bungee.LockLogin.properties;
import static ml.karmaconfigs.locklogin.plugin.bungee.permissibles.PluginPermission.infoRequest;
import static ml.karmaconfigs.locklogin.plugin.bungee.permissibles.PluginPermission.playerInfo;

@SystemCommand(command = "playerinfo")
public final class PlayerInfoCommand extends Command {

    /**
     * Construct a new command with no permissions or aliases.
     *
     * @param name the name of this command
     */
    public PlayerInfoCommand(String name) {
        super(name);
    }

    /**
     * Execute this command with the specified sender and arguments.
     *
     * @param sender the executor of this command
     * @param args   arguments used to invoke this command
     */
    @Override
    public void execute(CommandSender sender, String[] args) {
        Message messages = new Message();

        if (sender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) sender;
            User user = new User(player);

            if (user.getSession().isValid()) {
                if (user.hasPermission(infoRequest())) {
                    OfflineClient offline;
                    AccountManager manager;
                    switch (args.length) {
                        case 0:
                            user.send(messages.prefix() + messages.infoUsage());
                            break;
                        case 1:
                            String target = args[0];
                            if (target.startsWith("@") || target.startsWith("#")) {
                                String name;
                                switch (target.substring(0, 1)) {
                                    case "@":
                                        name = target.replaceFirst("@", "");

                                        if (!name.equalsIgnoreCase("everyone") && !name.equalsIgnoreCase("persistent") && !(name.toLowerCase().startsWith("permission[") && name.endsWith("]"))) {
                                            Alias alias = new Alias(name);
                                            if (alias.exists()) {
                                                Set<AccountID> ids = alias.getUsers();
                                                Set<AccountManager> accounts = new HashSet<>();
                                                for (AccountID id : ids) {
                                                    offline = new OfflineClient(id);
                                                    manager = offline.getAccount();

                                                    if (manager != null)
                                                        accounts.add(manager);
                                                }

                                                AccountParser parser = new AccountParser(accounts);
                                                DataSender.send(player, DataSender.getBuilder(DataType.INFOGUI, DataSender.PLUGIN_CHANNEL).addTextData(parser.toString()).build());
                                            } else {
                                                user.send(messages.prefix() + messages.aliasNotFound(name));
                                            }
                                        } else {
                                            AccountParser parser;

                                            switch (name) {
                                                case "everyone":
                                                    Set<AccountManager> everyoneAccounts = new LinkedHashSet<>();

                                                    for (ProxiedPlayer online : plugin.getProxy().getPlayers()) {
                                                        manager = CurrentPlatform.getAccountManager(new Class[]{ProxiedPlayer.class}, online);
                                                        if (manager != null)
                                                            everyoneAccounts.add(manager);
                                                    }

                                                    parser = new AccountParser(everyoneAccounts);
                                                    DataSender.send(player, DataSender.getBuilder(DataType.INFOGUI, DataSender.PLUGIN_CHANNEL).addTextData(parser.toString()).build());
                                                case "persistent":
                                                    parser = new AccountParser(PersistentSessionData.getPersistentAccounts());
                                                    DataSender.send(player, DataSender.getBuilder(DataType.INFOGUI, DataSender.PLUGIN_CHANNEL).addTextData(parser.toString()).build());
                                                    break;
                                                default:
                                                    String permission = StringUtils.replaceLast(name.replaceFirst("permission\\[", ""), "]", "");

                                                    Set<AccountManager> permissionAccounts = new LinkedHashSet<>();

                                                    for (ProxiedPlayer online : plugin.getProxy().getPlayers()) {
                                                        if (online.hasPermission(permission)) {

                                                            manager = CurrentPlatform.getAccountManager(new Class[]{ProxiedPlayer.class}, online);
                                                            if (manager != null)
                                                                permissionAccounts.add(manager);
                                                        }
                                                    }

                                                    parser = new AccountParser(permissionAccounts);
                                                    DataSender.send(player, DataSender.getBuilder(DataType.INFOGUI, DataSender.PLUGIN_CHANNEL).addTextData(parser.toString()).build());
                                                    break;
                                            }
                                        }
                                        break;
                                    case "#":
                                        name = target.replaceFirst("#", "");
                                        ServerInfo info = plugin.getProxy().getServerInfo(name);

                                        if (info != null) {
                                            Set<AccountManager> accounts = new HashSet<>();
                                            for (ProxiedPlayer connection : info.getPlayers()) {
                                                User conUser = new User(connection);
                                                accounts.add(conUser.getManager());
                                            }

                                            AccountParser parser = new AccountParser(accounts);
                                            DataSender.send(player, DataSender.getBuilder(DataType.INFOGUI, DataSender.PLUGIN_CHANNEL).addTextData(parser.toString()).build());
                                        } else {
                                            user.send(messages.prefix() + messages.aliasNotFound(name));
                                        }
                                        break;
                                }
                            } else {
                                offline = new OfflineClient(target);
                                manager = offline.getAccount();

                                if (manager != null) {
                                    AccountID id = manager.getUUID();
                                    LockedAccount account = new LockedAccount(id);
                                    LockedData data = account.getData();
                                    User tarUser;

                                    try {
                                        tarUser = new User(plugin.getProxy().getPlayer(UUID.fromString(id.getId())));
                                    } catch (Throwable ex) {
                                        tarUser = null;
                                    }

                                    user.send(properties.getProperty("player_information_header", "&5&m--------------&r&e LockLogin player info &5&m--------------"));
                                    user.send("&r");

                                    List<String> parsed = parseMessages(data.isLocked());

                                    int msg = -1;
                                    user.send(StringUtils.formatString(parsed.get(++msg), data.isLocked()));
                                    if (data.isLocked()) {
                                        Instant date = data.getLockDate();
                                        InstantParser parser = new InstantParser(date);
                                        String dateString = parser.getYear() + " " + parser.getMonth() + " " + parser.getDay();

                                        user.send(StringUtils.formatString(parsed.get(++msg), data.getAdministrator()));
                                        user.send(StringUtils.formatString(parsed.get(++msg), dateString));
                                    }
                                    user.send(StringUtils.formatString(parsed.get(++msg), manager.getName()));
                                    user.send(StringUtils.formatString(parsed.get(++msg), manager.getUUID().getId()));
                                    user.send(StringUtils.formatString(parsed.get(++msg), (!manager.getPassword().replaceAll("\\s", "").isEmpty())));
                                    user.send(StringUtils.formatString(parsed.get(++msg), (tarUser != null ? tarUser.getSession().isLogged() : "false")));
                                    user.send(StringUtils.formatString(parsed.get(++msg), (tarUser != null ? tarUser.getSession().isTempLogged() : "false")));
                                    user.send(StringUtils.formatString(parsed.get(++msg), (manager.has2FA() && !manager.getGAuth().replaceAll("\\s", "").isEmpty())));
                                    user.send(StringUtils.formatString(parsed.get(++msg), (!manager.getPin().replaceAll("\\s", "").isEmpty())));
                                } else {
                                    user.send(messages.prefix() + messages.neverPlayer(target));
                                }
                            }
                            break;
                        default:
                            Set<AccountManager> accounts = new LinkedHashSet<>();

                            for (String name : args) {
                                offline = new OfflineClient(name);
                                manager = offline.getAccount();

                                if (manager != null) {
                                    accounts.add(manager);
                                }
                            }

                            AccountParser parser = new AccountParser(accounts);
                            DataSender.send(player, DataSender.getBuilder(DataType.INFOGUI, DataSender.PLUGIN_CHANNEL).addTextData(parser.toString()).build());
                            break;
                    }
                } else {
                    user.send(messages.prefix() + messages.permissionError(playerInfo()));
                }
            } else {
                user.send(messages.prefix() + properties.getProperty("session_not_valid", "&5&oYour session is invalid, try leaving and joining the server again"));
            }
        } else {
            Console.send(messages.prefix() + properties.getProperty("command_not_available", "&cThis command is not available for console"));
        }
    }

    /**
     * Parse the player info message read
     * from plugin_messages.properties
     *
     * @param condition if the player account is locked
     * @return the parsed player info message
     */
    private List<String> parseMessages(final boolean condition) {
        String[] propData = properties.getProperty("player_information_message", "&7Locked: &d{0},condition=&7Locked by: &d{0}%&7Locked since: &d{0};,&7Name: &d{0},&7Account ID: &d{0},&7Registered: &d{0},&7Logged: &d{0},&7Temp logged: &d{0},&72FA: &d{0},&7Pin: &d{0}").split(",");
        List<String> cmdMessages = new ArrayList<>();

        for (String propMSG : propData) {
            if (propMSG.startsWith("condition=")) {
                String[] conditionData = propMSG.split(";");

                String condition_message;
                if (condition) {
                    condition_message = conditionData[0].replaceFirst("condition=", "");
                } else {
                    try {
                        condition_message = conditionData[1];
                    } catch (Throwable ex) {
                        condition_message = "";
                    }
                }

                if (!condition_message.isEmpty()) {
                    if (condition_message.contains("%")) {
                        String[] conditionMsgData = condition_message.split("%");

                        cmdMessages.addAll(Arrays.asList(conditionMsgData));
                    } else {
                        cmdMessages.add(condition_message);
                    }
                }
            } else {
                cmdMessages.add(propMSG);
            }
        }

        return cmdMessages;
    }
}