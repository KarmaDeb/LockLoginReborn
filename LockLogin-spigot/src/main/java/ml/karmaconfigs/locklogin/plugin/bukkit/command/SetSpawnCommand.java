package ml.karmaconfigs.locklogin.plugin.bukkit.command;

import ml.karmaconfigs.api.bukkit.Console;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.locklogin.plugin.bukkit.command.util.SystemCommand;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.data.Spawn;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.player.User;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.plugin;
import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.properties;
import static ml.karmaconfigs.locklogin.plugin.bukkit.plugin.PluginPermission.setSpawn;

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
        if (sender instanceof Player) {
            Player player = (Player) sender;
            User user = new User(player);

            Message messages = new Message();

            if (player.hasPermission(setSpawn())) {
                Spawn spawn = new Spawn(player.getWorld());
                spawn.save(player.getLocation());

                user.send(messages.prefix() + messages.spawnSet());
            } else {
                user.send(messages.prefix() + messages.permissionError(setSpawn()));
            }
        } else {
            Console.send(plugin, properties.getProperty("only_console_spawn", "&5&oThe console does not have a valid location!"), Level.INFO);
        }

        return false;
    }
}
