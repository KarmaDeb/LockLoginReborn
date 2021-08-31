package eu.locklogin.module.manager.bukkit.command;

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

import eu.locklogin.api.module.plugin.api.command.Command;
import eu.locklogin.api.module.plugin.api.command.help.HelpPage;
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import eu.locklogin.api.module.plugin.javamodule.sender.ModuleSender;
import eu.locklogin.plugin.bukkit.util.player.User;

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
    public void processCommand(final String arg, final ModuleSender sender, final String... parameters) {
        if (sender instanceof ModulePlayer) {
            ModulePlayer player = (ModulePlayer) sender;
            User user = new User(player.getPlayer());

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
            sender.sendMessage("&5&m--------------&r&e LockLogin module commands &5&m--------------");

            if (parameters.length == 0) {
                HelpPage page = new HelpPage(0);
                page.scan();

                for (String str : page.getHelp()) sender.sendMessage(str);
            } else {
                try {
                    int pageNumber = Integer.parseInt(parameters[0]);
                    if (HelpPage.getPages() < pageNumber)
                        pageNumber = HelpPage.getPages();
                    if (pageNumber < 0)
                        pageNumber = 0;

                    HelpPage page = new HelpPage(pageNumber);
                    page.scan();

                    for (String str : page.getHelp()) sender.sendMessage(str);
                } catch (Throwable ex) {
                    HelpPage page = new HelpPage(0);
                    page.scan();

                    for (String str : page.getHelp()) sender.sendMessage(str);
                }
            }
        }
    }
}
