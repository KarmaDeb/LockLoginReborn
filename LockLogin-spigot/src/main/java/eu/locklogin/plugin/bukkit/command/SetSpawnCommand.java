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

import eu.locklogin.plugin.bukkit.command.util.SystemCommand;
import eu.locklogin.plugin.bukkit.util.player.User;
import ml.karmaconfigs.api.common.Console;
import eu.locklogin.plugin.bukkit.util.files.Message;
import eu.locklogin.plugin.bukkit.util.files.data.Spawn;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static eu.locklogin.plugin.bukkit.LockLogin.properties;
import static eu.locklogin.plugin.bukkit.plugin.PluginPermission.setSpawn;

@SystemCommand(command = "setloginspawn", bungeecord = true)
public final class SetSpawnCommand implements CommandExecutor {


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

            if (player.hasPermission(setSpawn())) {
                Spawn spawn = new Spawn(player.getWorld());
                spawn.save(player.getLocation());

                user.send(messages.prefix() + messages.spawnSet());
            } else {
                user.send(messages.prefix() + messages.permissionError(setSpawn()));
            }
        } else {
            Console.send(messages.prefix() + properties.getProperty("command_not_available", "&cThis command is not available for console"));
        }

        return false;
    }
}
