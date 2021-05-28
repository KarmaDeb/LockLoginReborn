package ml.karmaconfigs.locklogin.manager.bungee.command;

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

import ml.karmaconfigs.locklogin.api.modules.api.command.Command;
import ml.karmaconfigs.locklogin.api.modules.api.command.help.HelpPage;
import ml.karmaconfigs.locklogin.manager.bungee.Main;
import ml.karmaconfigs.locklogin.plugin.bungee.util.player.User;
import net.md_5.bungee.api.connection.ProxiedPlayer;

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
        if (sender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) sender;
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
            Main.manager.getConsoleSender().sendMessage("&5&m--------------&r&e LockLogin module commands &5&m--------------");

            if (parameters.length == 0) {
                HelpPage page = new HelpPage(0);
                page.scan();

                for (String str : page.getHelp()) Main.manager.getConsoleSender().sendMessage(str);
            } else {
                try {
                    int pageNumber = Integer.parseInt(parameters[0]);
                    if (HelpPage.getPages() < pageNumber)
                        pageNumber = HelpPage.getPages();
                    if (pageNumber < 0)
                        pageNumber = 0;

                    HelpPage page = new HelpPage(pageNumber);
                    page.scan();

                    for (String str : page.getHelp()) Main.manager.getConsoleSender().sendMessage(str);
                } catch (Throwable ex) {
                    HelpPage page = new HelpPage(0);
                    page.scan();

                    for (String str : page.getHelp()) Main.manager.getConsoleSender().sendMessage(str);
                }
            }
        }
    }
}
