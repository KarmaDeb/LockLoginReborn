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

import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.module.plugin.client.permission.plugin.PluginPermissions;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.command.util.SystemCommand;
import eu.locklogin.plugin.bukkit.util.files.client.OfflineClient;
import eu.locklogin.plugin.bukkit.util.files.data.LastLocation;
import eu.locklogin.plugin.bukkit.util.player.User;
import ml.karmaconfigs.api.common.string.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static eu.locklogin.plugin.bukkit.LockLogin.console;
import static eu.locklogin.plugin.bukkit.LockLogin.properties;

@SystemCommand(command = "lastloc", bungeecord = true)
@SuppressWarnings("unused")
public final class LastLocationCommand implements CommandExecutor {

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
            ClientSession session = user.getSession();

            if (session.isValid()) {
                if (user.hasPermission(PluginPermissions.location_client())){
                    if (args.length == 2) {
                        String target = args[0];
                        String action = args[1];
                        LastLocation location;

                        switch (target.toLowerCase()) {
                            case "@all":
                                switch (action.toLowerCase()) {
                                    case "remove":
                                        LastLocation.removeAll();
                                        user.send(messages.prefix() + messages.locationsReset());
                                        break;
                                    case "fix":
                                        LastLocation.fixAll();
                                        user.send(messages.prefix() + messages.locationsFixed());
                                        break;
                                    default:
                                        user.send(messages.prefix() + messages.resetLocUsage());
                                        break;
                                }
                                break;
                            case "@me":
                                location = new LastLocation(player);

                                switch (action.toLowerCase()) {
                                    case "remove":
                                        location.remove();
                                        user.send(messages.prefix() + messages.locationReset(StringUtils.stripColor(player.getDisplayName())));
                                        break;
                                    case "fix":
                                        location.fix();
                                        user.send(messages.prefix() + messages.locationFixed(StringUtils.stripColor(player.getDisplayName())));
                                        break;
                                    default:
                                        user.send(messages.prefix() + messages.resetLocUsage());
                                        break;
                                }
                                break;
                            default:
                                OfflineClient offline = new OfflineClient(target);
                                AccountManager manager = offline.getAccount();

                                if (manager != null) {
                                    location = new LastLocation(manager.getUUID());

                                    switch (action.toLowerCase()) {
                                        case "remove":
                                            location.remove();
                                            user.send(messages.prefix() + messages.locationReset(target));
                                            break;
                                        case "fix":
                                            location.fix();
                                            user.send(messages.prefix() + messages.locationFixed(target));
                                            break;
                                        default:
                                            user.send(messages.prefix() + messages.resetLocUsage());
                                            break;
                                    }
                                } else {
                                    user.send(messages.prefix() + messages.neverPlayer(target));
                                }
                                break;
                        }
                    } else {
                        user.send(messages.prefix() + messages.resetLocUsage());
                    }
                } else {
                    user.send(messages.prefix() + messages.permissionError(PluginPermissions.location_client()));
                }
            } else {
                user.send(messages.prefix() + properties.getProperty("session_not_valid", "&5&oYour session is invalid, try leaving and joining the server again"));
            }
        } else {
            if (args.length == 2) {
                String target = args[0];
                String action = args[1];
                LastLocation location;

                if (target.equalsIgnoreCase("@all")) {
                    switch (action.toLowerCase()) {
                        case "remove":
                            LastLocation.removeAll();
                            console.send(messages.prefix() + messages.locationsReset());
                            break;
                        case "fix":
                            LastLocation.fixAll();
                            console.send(messages.prefix() + messages.locationsFixed());
                            break;
                        default:
                            console.send(messages.prefix() + messages.resetLocUsage());
                            break;
                    }
                } else {
                    OfflineClient offline = new OfflineClient(target);
                    AccountManager manager = offline.getAccount();

                    if (manager != null) {
                        location = new LastLocation(manager.getUUID());

                        switch (action.toLowerCase()) {
                            case "remove":
                                location.remove();
                                console.send(messages.prefix() + messages.locationReset(target));
                                break;
                            case "fix":
                                location.fix();
                                console.send(messages.prefix() + messages.locationFixed(target));
                                break;
                            default:
                                console.send(messages.prefix() + messages.resetLocUsage());
                                break;
                        }
                    } else {
                        console.send(messages.prefix() + messages.neverPlayer(target));
                    }
                }
            } else {
                console.send(messages.prefix() + messages.resetLocUsage());
            }
        }

        return false;
    }
}
