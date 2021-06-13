package eu.locklogin.plugin.velocity.plugin;

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

import com.velocitypowered.api.proxy.Player;
import eu.locklogin.plugin.velocity.util.files.Proxy;
import ml.karmaconfigs.api.common.Console;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.module.plugin.api.command.help.HelpPage;
import eu.locklogin.api.module.plugin.api.event.plugin.PluginStatusChangeEvent;
import eu.locklogin.api.module.plugin.javamodule.JavaModuleManager;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.velocity.permissibles.PluginPermission;
import eu.locklogin.plugin.velocity.util.files.Config;
import eu.locklogin.plugin.velocity.util.files.Message;
import eu.locklogin.plugin.velocity.util.player.User;

import static eu.locklogin.plugin.velocity.LockLogin.properties;
import static eu.locklogin.plugin.velocity.permissibles.PluginPermission.reload_config;
import static eu.locklogin.plugin.velocity.permissibles.PluginPermission.reload_messages;

public class FileReloader {

    /**
     * Reload the plugin files and other
     *
     * @param player the player that called the action
     */
    public static void reload(final Player player) {
        PluginStatusChangeEvent reload_start = new PluginStatusChangeEvent(PluginStatusChangeEvent.Status.RELOAD_START, null);
        JavaModuleManager.callEvent(reload_start);

        Message message = new Message();
        if (player != null) {
            User user = new User(player);

            if (user.hasPermission(reload_config()) || user.hasPermission(reload_messages())) {
                if (user.hasPermission(reload_config())) {
                    if (Config.manager.reload()) {
                        Config.manager.checkValues();

                        user.send(message.prefix() + properties.getProperty("reload_config", "&aReloaded config file"));

                        PluginConfiguration config = CurrentPlatform.getConfiguration();
                        CurrentPlatform.setPrefix(config.getModulePrefix());
                    }
                    if (Proxy.manager.reload()) {
                        user.send(message.prefix() + properties.getProperty("reload_proxy", "&aReloaded proxy configuration"));
                    }
                }
                if (user.hasPermission(reload_messages())) {
                    if (Message.manager.reload()) {
                        user.send(message.prefix() + properties.getProperty("reload_messages", "&aReloaded messages file"));
                    }
                }

                if (user.hasPermission(PluginPermission.reload())) {
                    user.send(message.prefix() + properties.getProperty("restart_systems", "&7Restarting version checker and plugin alert systems"));

                    Manager.restartVersionChecker();
                    Manager.restartAlertSystem();
                }
            } else {
                user.send(message.prefix() + message.permissionError(PluginPermission.reload()));
            }
        } else {
            if (Config.manager.reload()) {
                Config.manager.checkValues();

                Console.send(message.prefix() + properties.getProperty("reload_config", "&aReloaded config file"));

                PluginConfiguration config = CurrentPlatform.getConfiguration();
                CurrentPlatform.setPrefix(config.getModulePrefix());
            }
            if (Proxy.manager.reload()) {
                Console.send(message.prefix() + properties.getProperty("reload_proxy", "&aReloaded proxy configuration"));
            }
            if (Message.manager.reload()) {
                Console.send(message.prefix() + properties.getProperty("reload_messages", "&aReloaded messages file"));
            }

            Console.send(message.prefix() + properties.getProperty("restart_systems", "&7Restarting version checker and plugin alert systems"));

            Manager.restartVersionChecker();
            Manager.restartAlertSystem();

            HelpPage.updatePagesPrefix();
        }

        PluginStatusChangeEvent reload_finish = new PluginStatusChangeEvent(PluginStatusChangeEvent.Status.RELOAD_END, null);
        JavaModuleManager.callEvent(reload_finish);
    }
}