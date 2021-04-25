package ml.karmaconfigs.locklogin.plugin.bukkit.command;

import ml.karmaconfigs.api.bukkit.Console;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.account.AccountID;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.plugin.bukkit.command.util.PluginCommandType;
import ml.karmaconfigs.locklogin.plugin.bukkit.plugin.ConsoleAccount;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.client.OfflineClient;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.data.lock.LockedAccount;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.data.lock.LockedData;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.player.User;
import ml.karmaconfigs.locklogin.plugin.common.utils.InstantParser;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.*;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.*;
import static ml.karmaconfigs.locklogin.plugin.bukkit.permission.PluginPermission.*;

public final class PlayerInfoCommand extends PluginCommandType implements CommandExecutor {

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
        Message messages = new Message();

        if (sender instanceof Player) {
            Player player = (Player) sender;
            User user = new User(player);

            if (user.getSession().isValid()) {
                if (player.hasPermission(playerInfo())) {
                    if (args.length == 1) {
                        String target = args[0];
                        OfflineClient offline = new OfflineClient(target);
                        AccountManager manager = offline.getAccount();

                        if (manager != null) {
                            AccountID id = manager.getUUID();
                            LockedAccount account = new LockedAccount(id);
                            LockedData data = account.getData();
                            User tarUser;

                            try {
                                tarUser = new User(plugin.getServer().getPlayer(UUID.fromString(id.getId())));
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
                    } else {
                        user.send(messages.prefix() + messages.infoUsage());
                    }
                } else {
                    user.send(messages.prefix() + messages.permissionError(playerInfo()));
                }
            } else {
                user.send(messages.prefix() + properties.getProperty("session_not_valid", "&5&oYour session is invalid, try leaving and joining the server again"));
            }
        } else {
            ConsoleAccount console = new ConsoleAccount();
            if (console.isRegistered()) {
                if (args.length == 2) {
                    String target = args[0];
                    String password = args[1];

                    OfflineClient offline = new OfflineClient(target);
                    AccountManager manager = offline.getAccount();

                    if (console.validate(password)) {
                        if (manager != null) {
                            AccountID id = manager.getUUID();
                            LockedAccount account = new LockedAccount(id);
                            LockedData data = account.getData();
                            User tarUser;

                            try {
                                tarUser = new User(plugin.getServer().getPlayer(UUID.fromString(id.getId())));
                            } catch (Throwable ex) {
                                tarUser = null;
                            }

                            Console.send(properties.getProperty("player_information_header", "&5&m--------------&r&e LockLogin player info &5&m--------------"));
                            Console.send(" ");

                            List<String> parsed = parseMessages(data.isLocked());

                            int msg = -1;
                            Console.send(StringUtils.formatString(parsed.get(++msg), data.isLocked()));
                            if (data.isLocked()) {
                                Instant date = data.getLockDate();
                                InstantParser parser = new InstantParser(date);
                                String dateString = parser.getYear() + " " + parser.getMonth() + " " + parser.getDay();

                                Console.send(StringUtils.formatString(parsed.get(++msg), data.getAdministrator()));
                                Console.send(StringUtils.formatString(parsed.get(++msg), dateString));
                            }
                            Console.send(StringUtils.formatString(parsed.get(++msg), manager.getName()));
                            Console.send(StringUtils.formatString(parsed.get(++msg), manager.getUUID().getId()));
                            Console.send(StringUtils.formatString(parsed.get(++msg), (!manager.getPassword().replaceAll("\\s", "").isEmpty())));
                            Console.send(StringUtils.formatString(parsed.get(++msg), (tarUser != null ? tarUser.getSession().isLogged() : "false")));
                            Console.send(StringUtils.formatString(parsed.get(++msg), (tarUser != null ? tarUser.getSession().isTempLogged() : "false")));
                            Console.send(StringUtils.formatString(parsed.get(++msg), (manager.has2FA() && !manager.getGAuth().replaceAll("\\s", "").isEmpty())));
                            Console.send(StringUtils.formatString(parsed.get(++msg), (!manager.getPin().replaceAll("\\s", "").isEmpty())));
                        } else {
                            Console.send(messages.prefix() + messages.neverPlayer(target));
                        }
                    } else {
                        Console.send(messages.prefix() + messages.incorrectPassword());
                    }
                } else {
                    Console.send(messages.prefix() + messages.infoUsage() + "&r ( remember to include the console password as last argument )");
                }
            } else {
                Console.send(messages.prefix() + properties.getProperty("console_not_registered", "&5&oThe console must register to run protected commands!"));
            }
        }

        return false;
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

    /**
     * Get the plugin command name
     *
     * @return the plugin command
     */
    @Override
    public String command() {
        return "playerinfo";
    }
}
