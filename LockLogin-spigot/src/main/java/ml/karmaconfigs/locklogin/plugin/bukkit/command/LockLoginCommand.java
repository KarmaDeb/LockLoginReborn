package ml.karmaconfigs.locklogin.plugin.bukkit.command;

import ml.karmaconfigs.api.bukkit.Console;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.files.PluginConfiguration;
import ml.karmaconfigs.locklogin.api.modules.util.javamodule.JavaModuleLoader;
import ml.karmaconfigs.locklogin.api.modules.util.javamodule.JavaModuleManager;
import ml.karmaconfigs.locklogin.api.modules.PluginModule;
import ml.karmaconfigs.locklogin.api.modules.api.event.plugin.UpdateRequestEvent;
import ml.karmaconfigs.locklogin.api.modules.util.javamodule.updater.JavaModuleVersion;
import ml.karmaconfigs.locklogin.api.utils.platform.CurrentPlatform;
import ml.karmaconfigs.locklogin.plugin.bukkit.command.util.SystemCommand;
import ml.karmaconfigs.locklogin.plugin.bukkit.plugin.FileReloader;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.player.User;
import ml.karmaconfigs.locklogin.plugin.common.utils.plugin.ComponentFactory;
import ml.karmaconfigs.locklogin.plugin.common.web.VersionChecker;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.*;
import static ml.karmaconfigs.locklogin.plugin.bukkit.plugin.PluginPermission.*;

@SystemCommand(command = "locklogin")
public final class LockLoginCommand implements CommandExecutor {

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
        PluginConfiguration config = CurrentPlatform.getConfiguration();
        Message messages = new Message();

        VersionChecker checker = new VersionChecker(versionID);
        if (sender instanceof Player) {
            Player player = (Player) sender;
            User user = new User(player);

            switch (args.length) {
                case 0:
                    user.send("&5&oAvailable sub-commands:&7 /locklogin &e<reload>&7, &e<applyupdates>&7, &e<modules>&7, &e<version>&7, &e<changelog>&7, &e<check>");
                    break;
                case 1:
                    switch (args[0].toLowerCase()) {
                        case "reload":
                            if (player.hasPermission(reload())) {
                                FileReloader.reload(player);
                            } else {
                                user.send(messages.prefix() + messages.permissionError(reload()));
                            }
                            break;
                        case "applyupdates":
                            if (player.hasPermission(applyUpdates())) {
                                //BukkitManager.update(sender);
                                UpdateRequestEvent event = new UpdateRequestEvent(sender, player.hasPermission(applyUnsafeUpdates()), null);
                                JavaModuleManager.callEvent(event);
                            } else {
                                user.send(messages.prefix() + messages.permissionError(applyUpdates()));
                            }
                            break;
                        case "modules":
                            if (player.hasPermission(modules())) {
                                Set<PluginModule> modules = JavaModuleLoader.getModules();
                                ComponentFactory main = new ComponentFactory("&3Modules &8&o( &a" + modules.size() + " &8&o)&7: ");

                                int id = 0;
                                for (PluginModule module : modules) {
                                    JavaModuleVersion version = module.getManager().getVersionManager();

                                    ComponentFactory factory = new ComponentFactory("&e" + StringUtils.stripColor(module.name()) + (id == modules.size() - 1 ? "" : "&7, "));

                                    String hoverText = "\n&7Owner: &e" + module.author() + "\n&7Version: &e" + module.version() + "\n&7Description: &e" + module.description();

                                    try {
                                        if (version.updaterEnabled().get()) {
                                            hoverText = hoverText + "\n&7Latest: &e" + version.getLatest() + "\n\n&7Click me to download the latest version!";
                                            factory.click(ClickEvent.Action.OPEN_URL, version.getDownloadURL());
                                        } else {
                                            hoverText = hoverText + "\n&7Latest: &e" + version.getLatest() + "\n\n&7You are using the latest known module version!";
                                        }
                                    } catch (Throwable ex) {
                                        hoverText = hoverText + "\n&7Latest: &e" + version.getLatest() + "\n\n&7You are using the latest known module version!";
                                    }

                                    factory.hover(hoverText);

                                    main.addExtra(factory);
                                    id++;
                                }

                                user.send(main.get());
                            } else {
                                user.send(messages.prefix() + messages.permissionError(modules()));
                            }
                            break;
                        case "version":
                            if (player.hasPermission(version())) {
                                if (StringUtils.isNullOrEmpty(checker.getChangelog()))
                                    plugin.getServer().dispatchCommand(sender, "locklogin check");

                                user.send("&7Current version:&e " + versionID);
                                user.send("&7Latest version:&e " + checker.getLatestVersion());
                            } else {
                                user.send(messages.prefix() + messages.permissionError(version()));
                            }
                            break;
                        case "changelog":
                            if (player.hasPermission(changelog())) {
                                if (StringUtils.isNullOrEmpty(checker.getChangelog()))
                                    plugin.getServer().dispatchCommand(sender, "locklogin check");

                                user.send(checker.getChangelog().replace("\n", "{newline}"));
                            } else {
                                user.send(messages.prefix() + messages.permissionError(changelog()));
                            }
                            break;
                        case "check":
                            if (player.hasPermission(check())) {
                                checker.checkVersion(config.getUpdaterOptions().getChannel());
                                user.send("Checked for updates successfully");
                            } else {
                                user.send(messages.prefix() + messages.permissionError(check()));
                            }
                            break;
                        default:
                            user.send("&5&oAvailable sub-commands:&7 /locklogin &e<reload>&7, &e<applyupdates>&7, &e<modules>&7, &e<version>&7, &e<changelog>&7, &e<check>");
                            break;
                    }
                    break;
                default:
                    user.send("&5&oAvailable sub-commands:&7 /locklogin &e<reload>&7, &e<applyupdates>&7, &e<modules>&7, &e<version>&7, &e<changelog>&7, &e<check>");
                    break;
            }
        } else {
            switch (args.length) {
                case 0:
                    Console.send("&5&oAvailable sub-commands:&7 /locklogin &e<reload>&7, &e<applyupdates>&7, &e<modules>&7, &e<version>&7, &e<changelog>&7, &e<check>");
                    break;
                case 1:
                    switch (args[0].toLowerCase()) {
                        case "reload":
                            FileReloader.reload(null);
                            break;
                        case "applyupdates":
                            UpdateRequestEvent event = new UpdateRequestEvent(sender, true, null);
                            JavaModuleManager.callEvent(event);
                            break;
                        case "modules":
                            Set<PluginModule> modules = JavaModuleLoader.getModules();

                            int id = 0;
                            StringBuilder builder = new StringBuilder();
                            builder.append("&3Modules &8&o( &a").append(modules.size()).append(" &8&o)&7: ");
                            for (PluginModule module : modules) {
                                JavaModuleVersion version = module.getManager().getVersionManager();
                                try {
                                    if (version.updaterEnabled().get()) {
                                        builder.append("&c").append(StringUtils.stripColor(module.name())).append(" &f( &e").append(version.getDownloadURL()).append(" &f)").append(id == modules.size() - 1 ? "" : "&7, ");
                                    } else {
                                        builder.append("&e").append(StringUtils.stripColor(module.name())).append(id == modules.size() - 1 ? "" : "&7, ");
                                    }
                                } catch (Throwable ignored) {}
                                id++;
                            }

                            Console.send(builder.toString());
                            break;
                        case "version":
                            if (StringUtils.isNullOrEmpty(checker.getChangelog()))
                                plugin.getServer().dispatchCommand(sender, "locklogin check");

                            Console.send("&7Current version:&e {0}", versionID);
                            Console.send("&7Latest version:&e {0}", checker.getLatestVersion());
                            break;
                        case "changelog":
                            if (StringUtils.isNullOrEmpty(checker.getChangelog()))
                                plugin.getServer().dispatchCommand(sender, "locklogin check");

                            Console.send(checker.getChangelog());
                            break;
                        case "check":
                            checker.checkVersion(config.getUpdaterOptions().getChannel());
                            Console.send("Checked for updates successfully");
                            break;
                        default:
                            Console.send("&5&oAvailable sub-commands:&7 /locklogin &e<reload>&7, &e<applyupdates>&7, &e<modules>&7, &e<version>&7, &e<changelog>&7, &e<check>");
                            break;
                    }
                    break;
                default:
                    Console.send("&5&oAvailable sub-commands:&7 /locklogin &e<reload>&7, &e<applyupdates>&7, &e<modules>&7, &e<version>&7, &e<changelog>&7, &e<check>");
                    break;
            }
        }
        return false;
    }
}
