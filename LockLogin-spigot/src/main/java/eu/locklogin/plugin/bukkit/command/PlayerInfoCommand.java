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
import eu.locklogin.api.common.session.PersistentSessionData;
import eu.locklogin.api.common.utils.InstantParser;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.file.plugin.Alias;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.command.util.SystemCommand;
import eu.locklogin.plugin.bukkit.util.files.client.OfflineClient;
import eu.locklogin.plugin.bukkit.util.files.data.lock.LockedAccount;
import eu.locklogin.plugin.bukkit.util.files.data.lock.LockedData;
import eu.locklogin.plugin.bukkit.util.inventory.PlayersInfoInventory;
import eu.locklogin.plugin.bukkit.util.player.User;
import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.api.common.utils.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.*;

import static eu.locklogin.plugin.bukkit.LockLogin.plugin;
import static eu.locklogin.plugin.bukkit.LockLogin.properties;
import static eu.locklogin.plugin.bukkit.plugin.PluginPermission.playerInfo;

@SystemCommand(command = "playerinfo")
public final class PlayerInfoCommand implements CommandExecutor {

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
        PluginMessages messages = CurrentPlatform.getMessages();

        if (sender instanceof Player) {
            Player player = (Player) sender;
            User user = new User(player);

            if (user.getSession().isValid()) {
                if (player.hasPermission(playerInfo())) {
                    OfflineClient offline;
                    AccountManager manager;
                    switch (args.length) {
                        case 0:
                            user.send(messages.prefix() + messages.infoUsage());
                            break;
                        case 1:
                            String target = args[0];
                            if (target.startsWith("@")) {
                                String name = target.replaceFirst("@", "");

                                if (!name.equalsIgnoreCase("everyone") && !name.equalsIgnoreCase("persistent") && !(name.toLowerCase().startsWith("permission[") && name.endsWith("]"))) {
                                    Alias alias = new Alias(name);
                                    if (alias.exists()) {
                                        Set<AccountID> accounts = alias.getUsers();
                                        new PlayersInfoInventory(player, accounts);
                                    } else {
                                        user.send(messages.prefix() + messages.aliasNotFound(name));
                                    }
                                } else {
                                    switch (name) {
                                        case "everyone":
                                            Set<AccountID> everyoneAccounts = new LinkedHashSet<>();

                                            for (Player online : plugin.getServer().getOnlinePlayers()) {
                                                manager = CurrentPlatform.getAccountManager(new Class[]{Player.class}, online);
                                                if (manager != null)
                                                    everyoneAccounts.add(manager.getUUID());
                                            }

                                            new PlayersInfoInventory(player, everyoneAccounts);
                                        case "persistent":
                                            Set<AccountID> accountIDs = new LinkedHashSet<>();
                                            for (AccountManager account : PersistentSessionData.getPersistentAccounts()) accountIDs.add(account.getUUID());

                                            new PlayersInfoInventory(player, accountIDs);
                                            break;
                                        default:
                                            String permission = StringUtils.replaceLast(name.replaceFirst("permission\\[", ""), "]", "");

                                            Set<AccountID> permissionAccounts = new LinkedHashSet<>();

                                            for (Player online : plugin.getServer().getOnlinePlayers()) {
                                                if (online.hasPermission(permission)) {

                                                    manager = CurrentPlatform.getAccountManager(new Class[]{Player.class}, online);
                                                    if (manager != null)
                                                        permissionAccounts.add(manager.getUUID());
                                                }
                                            }

                                            new PlayersInfoInventory(player, permissionAccounts);
                                            break;
                                    }
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
                                        tarUser = new User(plugin.getServer().getPlayer(UUID.fromString(id.getId())));
                                    } catch (Throwable ex) {
                                        tarUser = null;
                                    }

                                    user.send(properties.getProperty("player_information_header", "&5&m--------------&r&e LockLogin player info &5&m--------------"));
                                    user.send("&r");

                                    List<String> parsed = parseMessages(data.isLocked());

                                    InstantParser creation_parser = new InstantParser(manager.getCreationTime());
                                    String creation_date = StringUtils.formatString("{0}/{1}/{2}", creation_parser.getDay(), creation_parser.getMonth(), creation_parser.getYear());

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
                                    user.send(StringUtils.formatString(parsed.get(++msg), (manager.hasPin())));
                                    user.send(StringUtils.formatString(parsed.get(++msg), creation_date, creation_parser.getDifference(Instant.now())));
                                } else {
                                    user.send(messages.prefix() + messages.neverPlayer(target));
                                }
                            }
                            break;
                        default:
                            Set<AccountID> accounts = new LinkedHashSet<>();

                            for (String name : args) {
                                offline = new OfflineClient(name);
                                manager = offline.getAccount();

                                if (manager != null) {
                                    accounts.add(manager.getUUID());
                                }
                            }

                            new PlayersInfoInventory(player, accounts);
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
        String[] propData = properties.getProperty("player_information_message", "&7Locked: &d{0},condition=&7Locked by: &d{0}%&7Locked since: &d{0};,&7Name: &d{0},&7Account ID: &d{0},&7Registered: &d{0},&7Logged: &d{0},&7Temp logged: &d{0},&72FA: &d{0},&7Pin: &d{0},&7Created on: &d{0} ( {1} ago )").split(",");
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
