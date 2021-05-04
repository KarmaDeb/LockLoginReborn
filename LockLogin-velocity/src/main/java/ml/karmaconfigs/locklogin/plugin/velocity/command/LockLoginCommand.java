package ml.karmaconfigs.locklogin.plugin.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.api.velocity.Console;
import ml.karmaconfigs.locklogin.api.modules.javamodule.JavaModuleLoader;
import ml.karmaconfigs.locklogin.api.modules.PluginModule;
import ml.karmaconfigs.locklogin.plugin.velocity.command.util.BungeeLikeCommand;
import ml.karmaconfigs.locklogin.plugin.velocity.command.util.SystemCommand;
import ml.karmaconfigs.locklogin.plugin.velocity.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.velocity.util.player.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.Set;

import static ml.karmaconfigs.locklogin.plugin.velocity.LockLogin.properties;

@SystemCommand(command = "locklogin")
public final class LockLoginCommand extends BungeeLikeCommand {

    /**
     * Initialize the bungee like command
     *
     * @param label the command label
     */
    public LockLoginCommand(String label) {
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

            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("modules")) {
                    Set<PluginModule> modules = JavaModuleLoader.getModules();

                    TextComponent main = Component.text().content("&3Modules &8&o( &a" + modules.size() + " &8&o)&7: ").build();

                    int id = 0;
                    for (PluginModule module : modules) {
                        Component factory = Component.text().content("&e" + StringUtils.stripColor(module.name()) + (id == modules.size() - 1 ? "" : "&7, ")).build();
                        HoverEvent<Component> hover = HoverEvent.showText(Component.text().content("\n&7Owner: &e" + module.author() + "\n&7Version: &e" + module.version() + "\n&7Description: &e" + module.description()).build());

                        main.append(factory.hoverEvent(hover));
                        id++;
                    }

                    user.send(main);
                } else {
                    user.send("&5&oAvailable sub-commands:&7 /locklogin &e<modules>");
                }
            } else {
                user.send("&5&oAvailable sub-commands:&7 /locklogin &e<modules>");
            }
        } else {
            Console.send(messages.prefix() + properties.getProperty("console_is_restricted", "&5&oFor security reasons, this command is restricted to players only"));
        }
    }
}
