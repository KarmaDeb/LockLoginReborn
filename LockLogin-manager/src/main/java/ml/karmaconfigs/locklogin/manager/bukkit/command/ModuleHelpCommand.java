package ml.karmaconfigs.locklogin.manager.bukkit.command;

import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.locklogin.api.modules.api.command.Command;
import ml.karmaconfigs.locklogin.api.modules.api.command.help.HelpPage;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.player.User;
import org.bukkit.entity.Player;

public final class ModuleHelpCommand extends Command {

    /**
     * Initialize the module help command
     */
    public ModuleHelpCommand() {
        super("Shows the plugin module commands", "helpme");
    }

    /**
     * Process the command when
     * its fired
     *
     * @param arg        the used argument
     * @param sender     the command sender
     * @param parameters the command parameters
     */
    @Override
    public void processCommand(final String arg, final Object sender, final String... parameters) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            User user = new User(player);

            user.send("&5&m--------------&r&e LockLogin module commands &5&m--------------");

            if (parameters.length == 0) {
                HelpPage page = new HelpPage(0);
                page.scan();

                for (String str : page.getHelp()) user.send(str);
            } else {
                try {
                    int pageNumber = Integer.parseInt(parameters[0]);
                    if (HelpPage.getPages() < pageNumber)
                        pageNumber = HelpPage.getPages();
                    if (pageNumber < 0)
                        pageNumber = 0;

                    HelpPage page = new HelpPage(pageNumber);
                    page.scan();

                    for (String str : page.getHelp()) user.send(str);
                } catch (Throwable ex) {
                    HelpPage page = new HelpPage(0);
                    page.scan();

                    for (String str : page.getHelp()) user.send(str);
                }
            }
        } else {
            Console.send("&5&m--------------&r&e LockLogin module commands &5&m--------------");

            if (parameters.length == 0) {
                HelpPage page = new HelpPage(0);
                page.scan();

                for (String str : page.getHelp()) Console.send(str);
            } else {
                try {
                    int pageNumber = Integer.parseInt(parameters[0]);
                    if (HelpPage.getPages() < pageNumber)
                        pageNumber = HelpPage.getPages();
                    if (pageNumber < 0)
                        pageNumber = 0;

                    HelpPage page = new HelpPage(pageNumber);
                    page.scan();

                    for (String str : page.getHelp()) Console.send(str);
                } catch (Throwable ex) {
                    HelpPage page = new HelpPage(0);
                    page.scan();

                    for (String str : page.getHelp()) Console.send(str);
                }
            }
        }
    }
}
