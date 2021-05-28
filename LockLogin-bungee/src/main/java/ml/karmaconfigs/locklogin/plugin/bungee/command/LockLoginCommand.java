package ml.karmaconfigs.locklogin.plugin.bungee.command;

import ml.karmaconfigs.api.bungee.Console;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.files.PluginConfiguration;
import ml.karmaconfigs.locklogin.api.modules.util.javamodule.JavaModuleLoader;
import ml.karmaconfigs.locklogin.api.modules.util.javamodule.JavaModuleManager;
import ml.karmaconfigs.locklogin.api.modules.PluginModule;
import ml.karmaconfigs.locklogin.api.modules.api.event.plugin.UpdateRequestEvent;
import ml.karmaconfigs.locklogin.api.modules.util.javamodule.updater.JavaModuleVersion;
import ml.karmaconfigs.locklogin.api.utils.platform.CurrentPlatform;
import ml.karmaconfigs.locklogin.plugin.bungee.command.util.SystemCommand;
import ml.karmaconfigs.locklogin.plugin.bungee.plugin.FileReloader;
import ml.karmaconfigs.locklogin.plugin.bungee.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.bungee.util.player.User;
import ml.karmaconfigs.locklogin.plugin.common.utils.plugin.ComponentFactory;
import ml.karmaconfigs.locklogin.plugin.common.web.VersionChecker;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.Set;

import static ml.karmaconfigs.locklogin.plugin.bungee.LockLogin.*;
import static ml.karmaconfigs.locklogin.plugin.bungee.permissibles.PluginPermission.*;

@SystemCommand(command = "locklogin")
public final class LockLoginCommand extends Command {

    /**
     * Construct a new command with no permissions or aliases.
     *
     * @param name the name of this command
     */
    public LockLoginCommand(String name) {
        super(name);
    }

    /**
     * Execute this command with the specified sender and arguments.
     *
     * @param sender the executor of this command
     * @param args   arguments used to invoke this command
     */
    @Override
    public void execute(CommandSender sender, String[] args) {
        PluginConfiguration config = CurrentPlatform.getConfiguration();
        Message messages = new Message();

        VersionChecker checker = new VersionChecker(versionID);
        if (sender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) sender;
            User user = new User(player);

            switch (args.length) {
                case 0:
                    user.send("&5&oAvailable sub-commands:&7 /locklogin &e<reload>&7, &e<applyupdates>&7, &e<modules>");
                    break;
                case 1:
                    switch (args[0].toLowerCase()) {
                        case "reload":
                            if (user.hasPermission(reload())) {
                                FileReloader.reload(player);
                            } else {
                                user.send(messages.prefix() + messages.permissionError(reload()));
                            }
                            break;
                        case "applyupdates":
                            if (user.hasPermission(applyUpdates())) {
                                UpdateRequestEvent event = new UpdateRequestEvent(sender, user.hasPermission(applyUnsafeUpdates()), null);
                                JavaModuleManager.callEvent(event);
                            } else {
                                user.send(messages.prefix() + messages.permissionError(applyUpdates()));
                            }
                            break;
                        case "modules":
                            if (user.hasPermission(modules())) {
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
                            if (user.hasPermission(version())) {
                                if (StringUtils.isNullOrEmpty(checker.getLatestVersion()))
                                    ProxyServer.getInstance().getPluginManager().dispatchCommand(sender, "locklogin check");

                                user.send("&7Current version:&e " + versionID);
                                user.send("&7Latest version:&e " + checker.getLatestVersion());
                            } else {
                                user.send(messages.prefix() + messages.permissionError(version()));
                            }
                            break;
                        case "changelog":
                            if (user.hasPermission(changelog())) {
                                if (StringUtils.isNullOrEmpty(checker.getChangelog()))
                                    ProxyServer.getInstance().getPluginManager().dispatchCommand(sender, "locklogin check");

                                user.send(checker.getChangelog().replace("\n", "{newline}"));
                            } else {
                                user.send(messages.prefix() + messages.permissionError(changelog()));
                            }
                            break;
                        case "check":
                            if (user.hasPermission(check())) {
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
                    Console.send("&5&oAvailable sub-commands:&7 /locklogin &e<reload>&7, &e<applyupdates>&7, &e<modules>");
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
                            if (StringUtils.isNullOrEmpty(checker.getLatestVersion()))
                                ProxyServer.getInstance().getPluginManager().dispatchCommand(sender, "locklogin check");

                            Console.send("&7Current version:&e {0}", versionID);
                            Console.send("&7Latest version:&e {0}", checker.getLatestVersion());
                            break;
                        case "changelog":
                            if (StringUtils.isNullOrEmpty(checker.getChangelog()))
                                ProxyServer.getInstance().getPluginManager().dispatchCommand(sender, "locklogin check");

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
    }
}
