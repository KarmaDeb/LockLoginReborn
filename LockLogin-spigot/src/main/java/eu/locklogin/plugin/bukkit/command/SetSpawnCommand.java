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

import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.module.plugin.client.permission.plugin.PluginPermissions;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.command.util.SystemCommand;
import eu.locklogin.plugin.bukkit.util.files.data.Spawn;
import eu.locklogin.plugin.bukkit.util.player.User;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static eu.locklogin.plugin.bukkit.LockLogin.console;
import static eu.locklogin.plugin.bukkit.LockLogin.properties;

@SystemCommand(command = "loginspawn", bungeecord = true, aliases = "setloginspawn")
public final class SetSpawnCommand implements CommandExecutor {

    private final Map<UUID, Location> preSpawnLocations = new HashMap<>();

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

            if (user.hasPermission(PluginPermissions.location_spawn())) {
                Spawn spawn = new Spawn(player.getWorld());

                if (args.length == 1 && args[0].equals("go")) {
                    spawn.teleport(player);
                } else {
                    spawn.save(player.getLocation());
                    user.send(messages.prefix() + messages.spawnSet());
                }
                //TODO: Deprecate all of the above

                if (args.length == 1) {
                    String sub = args[0].toLowerCase();
                    switch (sub) {
                        case "back":
                            Location preLocation = preSpawnLocations.remove(player.getUniqueId());
                            if (preLocation == null) {
                                //TODO: Create no back location message
                                return false;
                            }

                            player.teleport(preLocation);
                            //TODO: Create spawn teleported back message
                            break;
                        case "teleport":
                            Location location = player.getLocation();
                            if (Spawn.isAway(location, 15)) {
                                preSpawnLocations.put(player.getUniqueId(), location);
                            }

                            spawn.teleport(player);
                            //TODO: Create spawn teleported message
                            break;
                        case "remove":
                            spawn.remove();
                            //TODO: Create spawn removed message
                            break;
                        case "set":
                            spawn.save(player.getLocation());
                            user.send(messages.prefix() + messages.spawnSet());
                            break;
                    }
                }
            } else {
                user.send(messages.prefix() + messages.permissionError(PluginPermissions.location_spawn()));
            }
        } else {
            console.send(messages.prefix() + properties.getProperty("command_not_available", "&cThis command is not available for console"));
        }

        return false;
    }
}
