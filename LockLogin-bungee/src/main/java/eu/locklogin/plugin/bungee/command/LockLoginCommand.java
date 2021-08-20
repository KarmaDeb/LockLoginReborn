package eu.locklogin.plugin.bungee.command;

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

import eu.locklogin.api.common.utils.plugin.ComponentFactory;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.module.PluginModule;
import eu.locklogin.api.module.plugin.api.event.plugin.UpdateRequestEvent;
import eu.locklogin.api.module.plugin.javamodule.ModuleLoader;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.module.plugin.javamodule.updater.JavaModuleVersion;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bungee.command.util.SystemCommand;
import eu.locklogin.plugin.bungee.plugin.FileReloader;
import eu.locklogin.plugin.bungee.util.player.User;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.api.common.version.VersionCheckType;
import ml.karmaconfigs.api.common.version.VersionUpdater;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.Set;

import static eu.locklogin.plugin.bungee.LockLogin.*;
import static eu.locklogin.plugin.bungee.permissibles.PluginPermission.version;
import static eu.locklogin.plugin.bungee.permissibles.PluginPermission.*;

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
        PluginMessages messages = CurrentPlatform.getMessages();

        VersionUpdater updater = VersionUpdater.createNewBuilder(plugin).withVersionType(VersionCheckType.RESOLVABLE_ID).withVersionResolver(versionID).build();
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
                                ModulePlugin.callEvent(event);
                            } else {
                                user.send(messages.prefix() + messages.permissionError(applyUpdates()));
                            }
                            break;
                        case "modules":
                            if (user.hasPermission(modules())) {
                                Set<PluginModule> modules = ModuleLoader.getModules();
                                ComponentFactory main = new ComponentFactory("&3Modules &8&o( &a" + modules.size() + " &8&o)&7: ");

                                int id = 0;
                                for (PluginModule module : modules) {
                                    JavaModuleVersion version = module.getPlugin().getVersionManager();
                                    ComponentFactory factory = new ComponentFactory("&e" + StringUtils.stripColor(module.name()) + (id == modules.size() - 1 ? "" : "&7, "));

                                    String hoverText = "\n&7Owner(s): &e" + module.singleAuthors() + "\n&7Version: &e" + module.version() + "\n&7Description: &e" + module.description();

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
                                user.send("&7Current version:&e " + updater.get().resolve(VersionUpdater.VersionFetchResult.VersionType.CURRENT));
                                user.send("&7Latest version:&e " + updater.get().resolve(VersionUpdater.VersionFetchResult.VersionType.LATEST));
                            } else {
                                user.send(messages.prefix() + messages.permissionError(version()));
                            }
                            break;
                        case "changelog":
                            if (user.hasPermission(changelog())) {
                                for (String str : updater.get().getChangelog()) {
                                    user.send(str);
                                }
                            } else {
                                user.send(messages.prefix() + messages.permissionError(changelog()));
                            }
                            break;
                        case "check":
                            if (user.hasPermission(check())) {
                                updater.fetch(true);
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
                case 3:
                    if (args[0].equalsIgnoreCase("modules")) {
                        String moduleName = args[2];
                        PluginModule module = ModuleLoader.getByFile(ModuleLoader.getModuleFile(moduleName));

                        if (module != null) {
                            switch (args[1].toLowerCase()) {
                                case "load":
                                    if (user.hasPermission(loadModules())) {
                                        if (module.load()) {
                                            user.send("&aModule " + moduleName + " has been loaded successfully");
                                        } else {
                                            user.send("&cModule " + moduleName + " failed to load, maybe is already loaded?");
                                        }
                                    } else {
                                        user.send(messages.prefix() + messages.permissionError(loadModules()));
                                    }
                                    break;
                                case "unload":
                                    if (user.hasPermission(unloadModules())) {
                                        if (module.unload()) {
                                            user.send("&aModule " + moduleName + " has been unloaded successfully");
                                        } else {
                                            user.send("&cModule " + moduleName + " failed to unload, maybe is not loaded?");
                                        }
                                    } else {
                                        user.send(messages.prefix() + messages.permissionError(unloadModules()));
                                    }
                                    break;
                                case "reload":
                                    if (user.hasPermission(reload())) {
                                        module.reload();
                                        user.send(messages.prefix() + "&aModule " + moduleName + " has been reloaded, check console for more info");
                                    } else {
                                        user.send(messages.prefix() + messages.permissionError(reload()));
                                    }
                                    break;
                                default:
                                    user.send("&5&oAvailable sub-commands:&7 /locklogin modules &e<load>&7, &e<unload>&7, &e<reload>&7 &e[module name]");
                                    break;
                            }
                        } else {
                            user.send("&cModule " + moduleName + " is not loaded or does not exist!");
                        }
                    } else {
                        user.send("&5&oAvailable sub-commands:&7 /locklogin &e<reload>&7, &e<applyupdates>&7, &e<modules>&7, &e<version>&7, &e<changelog>&7, &e<check>");
                    }
                    break;
                default:
                    user.send("&5&oAvailable sub-commands:&7 /locklogin &e<reload>&7, &e<applyupdates>&7, &e<modules>&7, &e<version>&7, &e<changelog>&7, &e<check>");
                    break;
            }
        } else {
            switch (args.length) {
                case 0:
                    console.send("&5&oAvailable sub-commands:&7 /locklogin &e<reload>&7, &e<applyupdates>&7, &e<modules>");
                    break;
                case 1:
                    switch (args[0].toLowerCase()) {
                        case "reload":
                            FileReloader.reload(null);
                            break;
                        case "applyupdates":
                            UpdateRequestEvent event = new UpdateRequestEvent(sender, true, null);
                            ModulePlugin.callEvent(event);
                            break;
                        case "modules":
                            Set<PluginModule> modules = ModuleLoader.getModules();

                            int id = 0;
                            StringBuilder builder = new StringBuilder();
                            builder.append("&3Modules &8&o( &a").append(modules.size()).append(" &8&o)&7: ");
                            for (PluginModule module : modules) {
                                JavaModuleVersion version = module.getPlugin().getVersionManager();
                                try {
                                    if (version.updaterEnabled().get()) {
                                        builder.append("&c").append(StringUtils.stripColor(module.name())).append(" &f( &e").append(version.getDownloadURL()).append(" &f)").append(id == modules.size() - 1 ? "" : "&7, ");
                                    } else {
                                        builder.append("&e").append(StringUtils.stripColor(module.name())).append(id == modules.size() - 1 ? "" : "&7, ");
                                    }
                                } catch (Throwable ignored) {
                                }
                                id++;
                            }

                            console.send(builder.toString());
                            break;
                        case "version":
                            console.send("&7Current version:&e {0}", updater.get().resolve(VersionUpdater.VersionFetchResult.VersionType.CURRENT));
                            console.send("&7Latest version:&e {0}", updater.get().resolve(VersionUpdater.VersionFetchResult.VersionType.LATEST));
                            break;
                        case "changelog":
                            for (String str : updater.get().getChangelog()) {
                                console.send(str);
                            }
                            break;
                        case "check":
                            updater.fetch(true);
                            console.send("Checked for updates successfully");
                            break;
                        default:
                            console.send("&5&oAvailable sub-commands:&7 /locklogin &e<reload>&7, &e<applyupdates>&7, &e<modules>&7, &e<version>&7, &e<changelog>&7, &e<check>");
                            break;
                    }
                    break;
                case 3:
                    if (args[0].equalsIgnoreCase("modules")) {
                        String moduleName = args[2];
                        PluginModule module = ModuleLoader.getByFile(ModuleLoader.getModuleFile(moduleName));

                        if (module != null) {
                            switch (args[1].toLowerCase()) {
                                case "load":
                                    if (module.load()) {
                                        console.send("&aModule " + moduleName + " has been loaded successfully");
                                    } else {
                                        console.send("&cModule " + moduleName + " failed to load, maybe is already loaded?");
                                    }
                                    break;
                                case "unload":
                                    if (module.unload()) {
                                        console.send("&aModule " + moduleName + " has been unloaded successfully");
                                    } else {
                                        console.send("&cModule " + moduleName + " failed to unload, maybe is not loaded?");
                                    }
                                    break;
                                case "reload":
                                    module.reload();
                                    break;
                                default:
                                    console.send("&5&oAvailable sub-commands:&7 /locklogin modules &e<load>&7, &e<unload>&7, &e<reload>&7 &e[module name]");
                                    break;
                            }
                        } else {
                            console.send("&cModule " + moduleName + " is not loaded or does not exist!");
                        }
                    } else {
                        console.send("&5&oAvailable sub-commands:&7 /locklogin &e<reload>&7, &e<applyupdates>&7, &e<modules>&7, &e<version>&7, &e<changelog>&7, &e<check>");
                    }
                    break;
                default:
                    console.send("&5&oAvailable sub-commands:&7 /locklogin &e<reload>&7, &e<applyupdates>&7, &e<modules>&7, &e<version>&7, &e<changelog>&7, &e<check>");
                    break;
            }
        }
    }
}
