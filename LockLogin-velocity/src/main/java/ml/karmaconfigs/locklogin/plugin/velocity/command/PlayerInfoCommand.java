package ml.karmaconfigs.locklogin.plugin.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.api.velocity.Console;
import ml.karmaconfigs.locklogin.api.account.AccountID;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.plugin.common.utils.Alias;
import ml.karmaconfigs.locklogin.plugin.common.utils.InstantParser;
import ml.karmaconfigs.locklogin.plugin.velocity.command.util.BungeeLikeCommand;
import ml.karmaconfigs.locklogin.plugin.velocity.command.util.SystemCommand;
import ml.karmaconfigs.locklogin.plugin.velocity.plugin.sender.AccountParser;
import ml.karmaconfigs.locklogin.plugin.velocity.plugin.sender.DataSender;
import ml.karmaconfigs.locklogin.plugin.velocity.plugin.sender.DataType;
import ml.karmaconfigs.locklogin.plugin.velocity.util.files.client.OfflineClient;
import ml.karmaconfigs.locklogin.plugin.velocity.util.files.data.lock.LockedAccount;
import ml.karmaconfigs.locklogin.plugin.velocity.util.files.data.lock.LockedData;
import ml.karmaconfigs.locklogin.plugin.velocity.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.velocity.util.player.User;

import java.time.Instant;
import java.util.*;

import static ml.karmaconfigs.locklogin.plugin.velocity.LockLogin.properties;
import static ml.karmaconfigs.locklogin.plugin.velocity.LockLogin.server;
import static ml.karmaconfigs.locklogin.plugin.velocity.permissibles.PluginPermission.infoRequest;
import static ml.karmaconfigs.locklogin.plugin.velocity.permissibles.PluginPermission.playerInfo;

@SystemCommand(command = "playerinfo")
public final class PlayerInfoCommand extends BungeeLikeCommand {

    /**
     * Initialize the bungee like command
     *
     * @param label the command label
     */
    public PlayerInfoCommand(String label) {
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
        Message messages = new Message();

        if (sender instanceof Player) {
            Player player = (Player) sender;
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
                                        break;
                                    case "#":
                                        name = target.replaceFirst("#", "");
                                        Optional<RegisteredServer> info = server.getServer(name);

                                        if (info.isPresent()) {
                                            Set<AccountManager> accounts = new HashSet<>();
                                            for (Player connection : info.get().getPlayersConnected()) {
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
                                    User tarUser = null;

                                    Optional<Player> tar = server.getPlayer(UUID.fromString(id.getId()));

                                    if (tar.isPresent())
                                        tarUser = new User(tar.get());

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
            Console.send(messages.prefix() + properties.getProperty("console_is_restricted", "&5&oFor security reasons, this command is restricted to players only"));
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