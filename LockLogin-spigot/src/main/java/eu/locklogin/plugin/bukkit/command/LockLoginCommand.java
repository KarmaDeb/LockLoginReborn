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

import eu.locklogin.api.common.utils.InstantParser;
import eu.locklogin.api.common.utils.plugin.ComponentFactory;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.module.PluginModule;
import eu.locklogin.api.module.plugin.api.event.plugin.UpdateRequestEvent;
import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.client.permission.plugin.PluginPermissions;
import eu.locklogin.api.module.plugin.javamodule.ModuleLoader;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.module.plugin.javamodule.updater.JavaModuleVersion;
import eu.locklogin.api.security.backup.BackupScheduler;
import eu.locklogin.api.security.backup.BackupStorage;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.TaskTarget;
import eu.locklogin.plugin.bukkit.command.util.SystemCommand;
import eu.locklogin.plugin.bukkit.plugin.FileReloader;
import eu.locklogin.plugin.bukkit.plugin.Manager;
import eu.locklogin.plugin.bukkit.util.player.User;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.timer.SchedulerUnit;
import ml.karmaconfigs.api.common.timer.SourceScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.version.checker.VersionUpdater;
import ml.karmaconfigs.api.common.version.updater.VersionType;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static eu.locklogin.plugin.bukkit.LockLogin.*;

@SystemCommand(command = "locklogin", bungee_command = "slocklogin", bungeecord = true)
@SuppressWarnings("unused")
public final class LockLoginCommand implements CommandExecutor {

    private boolean confirm_uninstall = false;

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
    @SuppressWarnings("deprecation")
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        PluginMessages messages = CurrentPlatform.getMessages();
        sender.sendMessage(StringUtils.toColor(messages.prefix() + properties.
                getProperty(
                        "processing_async",
                        "&dProcessing {0} command, please wait for feedback")
                .replace("{0}", label)));

        tryAsync(TaskTarget.COMMAND_EXECUTE, () -> {
            VersionUpdater updater = Manager.getUpdater();
            if (sender instanceof Player) {
                Player player = (Player) sender;
                User user = new User(player);

                switch (args.length) {
                    case 1:
                        switch (args[0].toLowerCase()) {
                            case "log":
                                if (user.hasPermission(PluginPermissions.web_log())) {
                                    user.send(messages.prefix() + "&cFeature temporally disabled");
                                } else {
                                    user.send(messages.prefix() + messages.permissionError(PluginPermissions.web_log()));
                                }
                                break;
                            case "reload":
                                FileReloader.reload(player); //Permission check is made internally... xD
                                break;
                            case "applyupdates":
                                if (user.hasPermission(PluginPermissions.updater_apply())) {
                                    for (Player online : plugin.getServer().getOnlinePlayers()) {
                                        if (!online.equals(sender)) {
                                            User u = new User(online);
                                            if (u.hasPermission(PluginPermissions.updater_apply())) {
                                                u.send(messages.prefix() + "&7" + StringUtils.stripColor(sender.getName()) + "&d is updating LockLogin");
                                            }
                                        }
                                    }

                                    plugin.console().send("{0} is updating LockLogin", Level.INFO, StringUtils.toColor(player.getName()));

                                    Event event = new UpdateRequestEvent(sender, null);
                                    ModulePlugin.callEvent(event);
                                } else {
                                    user.send(messages.prefix() + messages.permissionError(PluginPermissions.updater_apply()));
                                }
                                break;
                            case "modules":
                                if (user.hasPermission(PluginPermissions.module_list())) {
                                    user.send(messages.prefix() + "&dFetching modules info, please stand by");

                                    Set<PluginModule> modules = ModuleLoader.getModules();
                                    AtomicBoolean canPost = new AtomicBoolean(false);
                                    AtomicInteger id = new AtomicInteger(0);
                                    ComponentFactory main = new ComponentFactory("&3Modules &8&o( &a" + modules.size() + " &8&o)&7: ");

                                    for (PluginModule module : modules) {
                                        JavaModuleVersion version = module.getUpdater();
                                        version.fetch().whenComplete((result) -> {
                                            ComponentFactory factory = new ComponentFactory("&e" + StringUtils.stripColor(module.name()) + (id.getAndIncrement() == modules.size() ? "" : "&7, "));

                                            String hoverText = "\n&7Owner(s): &e" + module.singleAuthors() + "\n&7Version: &e" + module.version() + "\n&7Description: &e" + module.description();

                                            if (result.isUpdated()) {
                                                hoverText = hoverText + "\n&7Latest: &e" + result.getLatest() + "\n\n&7You are using the latest known module version!";
                                            } else {
                                                hoverText = hoverText + "\n&7Latest: &e" + result.getLatest() + "\n\n&7Click me to download the latest version!";
                                                factory.click(ClickEvent.Action.OPEN_URL, result.getUpdateURL());
                                            }

                                            factory.hover(hoverText);

                                            main.addExtra(factory);

                                            if (id.get() == modules.size()) {
                                                canPost.set(true);
                                            }
                                        });
                                    }

                                    SimpleScheduler timer = new SourceScheduler(plugin, 1, SchedulerUnit.SECOND, true).multiThreading(true);
                                    timer.restartAction(() -> {
                                        if (canPost.get()) {
                                            timer.cancel();
                                            user.send(main.get());
                                        }
                                    }).start();
                                } else {
                                    user.send(messages.prefix() + messages.permissionError(PluginPermissions.module_list()));
                                }
                                break;
                            case "version":
                                if (user.hasPermission(PluginPermissions.updater_version())) {
                                    user.send(messages.prefix() + "&dTrying to communicate with LockLogin website, please wait. This could take some seconds...");

                                    updater.get().whenComplete((result, error) -> {
                                        if (error == null) {
                                            user.send(messages.prefix() + "&7Current version:&e " + result.resolve(VersionType.CURRENT));
                                            user.send(messages.prefix() + "&7Latest version:&e " + result.resolve(VersionType.LATEST));
                                        } else {
                                            user.send(messages.prefix() + "&5&oFailed to fetch latest version");
                                        }
                                    });
                                } else {
                                    user.send(messages.prefix() + messages.permissionError(PluginPermissions.updater_version()));
                                }
                                break;
                            case "changelog":
                                if (user.hasPermission(PluginPermissions.updater_changelog())) {
                                    user.send(messages.prefix() + "&dTrying to communicate with LockLogin website, please wait. This could take some seconds...");

                                    updater.get().whenComplete((result, error) -> {
                                        if (error == null) {
                                            for (String str : result.getChangelog()) {
                                                user.send(str);
                                            }
                                        } else {
                                            user.send(messages.prefix() + "&5&oFailed to fetch latest changelog");
                                        }
                                    });
                                } else {
                                    user.send(messages.prefix() + messages.permissionError(PluginPermissions.updater_changelog()));
                                }
                                break;
                            case "synchronize":
                            case "remove":
                            case "execute":
                                if (user.hasPermission(PluginPermissions.web())) {
                                    user.send(messages.prefix() + "&cComing soon; LockLogin web panel");
                                } else {
                                    user.send(messages.prefix() + messages.permissionError(PluginPermissions.web()));
                                }
                                break;
                            case "check":
                                if (user.hasPermission(PluginPermissions.updater_check())) {
                                    user.send(messages.prefix() + "&dTrying to communicate with LockLogin website, please wait. This could take some seconds...");

                                    updater.fetch(true).whenComplete((result, error) -> {
                                        if (error == null) {
                                            user.send(messages.prefix() + "&dChecked for updates successfully");
                                        } else {
                                            user.send(messages.prefix() + "&5&oFailed to check for updates");
                                        }
                                    });
                                } else {
                                    user.send(messages.prefix() + messages.permissionError(PluginPermissions.updater_check()));
                                }
                                break;
                            default:
                                user.send(messages.prefix() + "&5&oAvailable sub-commands:&7 /locklogin &e<reload>&7, &e<applyupdates>&7, &e<modules>&7, &e<version>&7, &e<changelog>&7, &e<check>");
                                break;
                        }
                        break;
                    case 3:
                        if (args[0].equalsIgnoreCase("modules")) {
                            String moduleName = args[2];
                            File moduleFile = ModuleLoader.getModuleFile(moduleName);

                            if (moduleFile != null) {
                                PluginModule module = getLoader().getModule(moduleFile);

                                if (module != null) {
                                    switch (args[1].toLowerCase()) {
                                        case "load":
                                            if (user.hasPermission(PluginPermissions.module_load())) {
                                                if (!ModuleLoader.isLoaded(module) && module.load()) {
                                                    user.send(messages.prefix() + "&dModule " + moduleName + " has been loaded successfully");
                                                } else {
                                                    user.send(messages.prefix() + "&5&oModule " + moduleName + " failed to load, maybe is already loaded?");
                                                }
                                            } else {
                                                user.send(messages.prefix() + messages.permissionError(PluginPermissions.module_load()));
                                            }
                                            break;
                                        case "unload":
                                            if (user.hasPermission(PluginPermissions.module_unload())) {
                                                if (ModuleLoader.isLoaded(module) && module.unload()) {
                                                    user.send(messages.prefix() + "&dModule " + moduleName + " has been unloaded successfully");
                                                } else {
                                                    user.send(messages.prefix() + "&5&oModule " + moduleName + " failed to unload, maybe is not loaded?");
                                                }
                                            } else {
                                                user.send(messages.prefix() + messages.permissionError(PluginPermissions.module_unload()));
                                            }
                                            break;
                                        case "reload":
                                            if (user.hasPermission(PluginPermissions.module_reload())) {
                                                module.reload();
                                                user.send(messages.prefix() + "&dModule " + moduleName + " has been reloaded, check console for more info");
                                            } else {
                                                user.send(messages.prefix() + messages.permissionError(PluginPermissions.module_reload()));
                                            }
                                            break;
                                        default:
                                            user.send(messages.prefix() + "&5&oAvailable sub-commands:&7 /locklogin modules &e<load>&7, &e<unload>&7, &e<reload>&7 &e[module name]");
                                            break;
                                    }
                                } else {
                                    user.send(messages.prefix() + "&5&oModule " + moduleName + " does not exist!");
                                }
                            } else {
                                user.send(messages.prefix() + "&5&oModule " + moduleName + " is not loaded or does not exist!");
                            }
                        } else {
                            user.send(messages.prefix() + "&5&oAvailable sub-commands:&7 /locklogin &e<reload>&7, &e<applyupdates>&7, &e<modules>&7, &e<version>&7, &e<changelog>&7, &e<check>");
                        }
                        break;
                    default:
                        user.send(messages.prefix() + "&5&oAvailable sub-commands:&7 /locklogin &e<reload>&7, &e<applyupdates>&7, &e<modules>&7, &e<version>&7, &e<changelog>&7, &e<check>");
                        break;
                }
            } else {
                switch (args.length) {
                    case 1:
                        switch (args[0].toLowerCase()) {
                            case "log":
                                plugin.console().send("Feature temporally disabled", Level.INFO);
                                break;
                            case "reload":
                                FileReloader.reload(null);
                                break;
                            case "applyupdates":
                                for (Player online : plugin.getServer().getOnlinePlayers()) {
                                    if (!online.equals(sender)) {
                                        User user = new User(online);
                                        if (user.hasPermission(PluginPermissions.updater_apply())) {
                                            user.send(messages.prefix() + "&7" + StringUtils.stripColor(sender.getName()) + "&d is updating LockLogin");
                                        }
                                    }
                                }

                                Event event = new UpdateRequestEvent(sender, null);
                                ModulePlugin.callEvent(event);

                                break;
                            case "modules":
                                console.send(messages.prefix() + "&dFetching modules info, please stand by");

                                Set<PluginModule> modules = ModuleLoader.getModules();
                                AtomicBoolean canPost = new AtomicBoolean(false);
                                AtomicInteger id = new AtomicInteger(0);
                                StringBuilder builder = new StringBuilder();

                                builder.append(StringUtils.formatString("&3Modules &8&o( &a{0} &8&o)&7: ", modules.size()));

                                for (PluginModule module : modules) {
                                    JavaModuleVersion version = module.getUpdater();

                                    version.fetch().whenComplete((result) -> {
                                        builder.append(StringUtils.formatString("\n\n&7Name: &d{0}", module.name()))
                                                .append(StringUtils.formatString("\n&7Authors: &d{0}", module.singleAuthors()))
                                                .append(StringUtils.formatString("\n&7Version: &d{0}", module.version()))
                                                .append(StringUtils.formatString("\n&7Description: &d{0}", module.description()));
                                        if (result.isUpdated()) {
                                            builder.append("\n&7Version status: &dUp to date");
                                        } else {
                                            builder.append("\n&7Version status: &5&oOut of date")
                                                    .append(StringUtils.formatString("\n&7Update url: &d{0}", result.getUpdateURL()));
                                        }

                                        if (id.incrementAndGet() != modules.size()) {
                                            builder.append("\n\n&e------------------------------------");
                                        } else {
                                            canPost.set(true);
                                        }
                                    });
                                }

                                SimpleScheduler timer = new SourceScheduler(plugin, 1, SchedulerUnit.SECOND, true).multiThreading(true);
                                timer.restartAction(() -> {
                                    if (canPost.get()) {
                                        timer.cancel();
                                        console.send(builder.toString());
                                    }
                                }).start();
                                break;
                            case "version":
                                console.send(messages.prefix() + "&dTrying to communicate with LockLogin website, please wait. This could take some seconds...");

                                updater.get().whenComplete((result, error) -> {
                                    if (error == null) {
                                        console.send(messages.prefix() + "&7Current version:&e {0}", result.resolve(VersionType.CURRENT));
                                        console.send(messages.prefix() + "&7Latest version:&e {0}", result.resolve(VersionType.LATEST));
                                    } else {
                                        console.send(messages.prefix() + "&5&oFailed to fetch latest version");
                                    }
                                });
                                break;
                            case "changelog":
                                console.send(messages.prefix() + "&dTrying to communicate with LockLogin website, please wait. This could take some seconds...");

                                updater.get().whenComplete((result, error) -> {
                                    if (error == null) {
                                        for (String str : result.getChangelog()) {
                                            console.send(str);
                                        }
                                    } else {
                                        console.send(messages.prefix() + "&5&oFailed to fetch latest changelog");
                                    }
                                });
                                break;
                            case "synchronize":
                            case "remove":
                            case "execute":
                                console.send(messages.prefix() + "&cComing soon; LockLogin web panel");
                                break;
                            case "check":
                                console.send(messages.prefix() + "&dTrying to communicate with LockLogin website, please wait. This could take some seconds...");

                                updater.fetch(true).whenComplete((result, error) -> {
                                    if (error == null) {
                                        console.send(messages.prefix() + "&dChecked for updates successfully");
                                    } else {
                                        console.send(messages.prefix() + "&5&oFailed to check for updates");
                                    }
                                });

                                break;
                            case "backup":
                                console.send(messages.prefix() + "&dListing backups, please wait...");

                                BackupScheduler scheduler = CurrentPlatform.getBackupScheduler();
                                scheduler.fetchAll().whenComplete((backups) -> {
                                    if (backups.length > 0) {
                                        for (BackupStorage storage : backups) {
                                            String backup_id = storage.id();
                                            Instant backup_creation = storage.creation();
                                            InstantParser parser = new InstantParser(backup_creation);

                                            console.send("&7{0} &6- &e{1} ago &8(&f{2} accounts&8)", backup_id, parser.getDifference(), storage.accounts());
                                        }
                                    } else {
                                        console.send("No backups have been made yet!", Level.WARNING);
                                    }
                                });
                                break;
                            case "install": {
                                /*License license = CurrentPlatform.getLicense();
                                if (license == null) {
                                    PluginLicenseProvider provider = CurrentPlatform.getLicenseProvider();
                                    license = provider.request();

                                    if (license != null && license.install()) {
                                        console.send(messages.prefix() + "&dSuccessfully downloaded and installed license. Restart your server to apply changes");
                                        try {
                                            JarManager.changeField(CurrentPlatform.class, "current_license", license);
                                        } catch (Throwable ex) {
                                            ex.printStackTrace();
                                        }

                                        console.send("&e&lNEXT STEPS:");
                                        console.send("&7To completely install the license in the network, you must do a final thing. In each of your other servers console, run the command &e/locklogin sync&c {0}", license.syncKey());
                                    } else {
                                        if (license == null) {
                                            console.send(messages.prefix() + "&5&oAn error occurred while generating your license");
                                        } else {
                                            console.send(messages.prefix() + "&5&oAn error occurred while installing your license.");
                                        }
                                    }
                                } else {
                                    console.send(messages.prefix() + "&5&oCannot request a license");
                                }*/
                                console.send(messages.prefix() + "&5&oLockLogin web services has been shutdown");
                            }
                                break;
                            case "sync": {
                                /*License license = CurrentPlatform.getLicense();
                                if (license == null) {
                                    console.send(messages.prefix() + "&5&lYou don't have a license to synchronize with!");
                                } else {
                                    console.send(messages.prefix() + "&5&lYou already have a license! If you want to sync with another server, remove the current license (&7/locklogin uninstall&5&l).");
                                    console.send(messages.prefix() + "&aIf you want to sync this server license with another one, run the command &7/locklogin sync&f {0}&a on the other server", license.syncKey());
                                }*/
                                console.send(messages.prefix() + "&5&oLockLogin web services has been shutdown");
                            }
                                break;
                            case "uninstall": {
                                /*License license = CurrentPlatform.getLicense();
                                if (license != null) {
                                    if (confirm_uninstall) {
                                        Path file = license.getLocation().resolve("license.dat");
                                        if (PathUtilities.destroyWithResults(file)) {
                                            plugin.console().send("Successfully destroyed plugin license. Restart your server to apply changes", Level.INFO);

                                            try {
                                                JarManager.changeField(CurrentPlatform.class, "current_license", null);
                                            } catch (Throwable ex) {
                                                ex.printStackTrace();
                                            }
                                        } else {
                                            plugin.console().send("Failed to uninstall plugin license", Level.GRAVE);
                                        }
                                        confirm_uninstall = false;
                                    } else {
                                        console.send(messages.prefix() + "&dAre you sure you want to remove the license? (Run the command again to confirm)");
                                        confirm_uninstall = true;
                                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                            License current = CurrentPlatform.getLicense();
                                            if (current != null && confirm_uninstall) {
                                                confirm_uninstall = false;
                                                plugin.console().send("Cancelled uninstall operation automatically", Level.WARNING);
                                            }
                                        }, 20 * 5);
                                    }
                                } else {
                                    console.send(messages.prefix() + "&5&oYou don't have any license to install");
                                }*/
                                console.send(messages.prefix() + "&5&oLockLogin web services has been shutdown");
                            }
                                break;
                            default:
                                console.send(messages.prefix() + "&5&oAvailable sub-commands:&7 /locklogin &e<reload>&7, &e<applyupdates>&7, &e<modules>&7, &e<version>&7, &e<changelog>&7, &e<check>&7, &e<backup>");
                                break;
                        }
                        break;
                    case 2:
                        switch (args[0].toLowerCase()) {
                            case "backup":
                                String backup_name = args[1];
                                BackupScheduler scheduler = CurrentPlatform.getBackupScheduler();

                                if (backup_name.equalsIgnoreCase("create")) {
                                    console.send("Preparing to create backup, please wait", Level.INFO);
                                    scheduler.performBackup().whenComplete((id, error) -> {
                                        if (error == null) {
                                            console.send(messages.prefix() + "&dSuccessfully created backup with id {0}", id);
                                        } else {
                                            plugin.logger().scheduleLog(Level.GRAVE, error);
                                            plugin.logger().scheduleLog(Level.INFO, "Failed to create backup (command)");
                                            console.send(messages.prefix() + "&5&lFailed to create a backup ({0}). Read logs for more information", error.fillInStackTrace());
                                        }
                                    });
                                } else {
                                    plugin.console().send("Finding backup with id: {0}. Please wait", Level.INFO, backup_name);

                                    scheduler.fetch(backup_name).whenComplete((backup) -> {
                                        if (backup != null) {
                                            String id = backup.id();
                                            InstantParser parser = new InstantParser(backup.creation());
                                            int accounts = backup.accounts();

                                            plugin.console().send("&d&m---------------------------");
                                            plugin.console().send("");
                                            plugin.console().send("&7Backup ID:&e {0}", id);
                                            plugin.console().send("&7Created:&e {0}", parser.parse());
                                            plugin.console().send("&7Age:&e {0}", parser.getDifference());
                                            plugin.console().send("&7Accounts:&e {0}", accounts);
                                            plugin.console().send("");
                                            plugin.console().send("&7Run &d/locklogin backup remove {0}&7 to&c remove", id);
                                            plugin.console().send("&7Run &d/locklogin backup restore {0}&7 to&a restore", id);
                                        } else {
                                            plugin.console().send("Failed to fetch backup {0}. Does it exist?", Level.GRAVE, backup_name);
                                        }
                                    });
                                }
                                break;
                            case "sync": {
                                /*License license = CurrentPlatform.getLicense();
                                if (license == null) {
                                    PluginLicenseProvider provider = CurrentPlatform.getLicenseProvider();
                                    license = provider.sync(args[1]);

                                    if (license != null && license.install()) {
                                        console.send(messages.prefix() + "&dSuccessfully synchronized and installed license. Restart your server to apply changes");
                                        try {
                                            JarManager.changeField(CurrentPlatform.class, "current_license", license);
                                        } catch (Throwable ex) {
                                            ex.printStackTrace();
                                        }
                                    } else {
                                        if (license == null) {
                                            console.send(messages.prefix() + "&5&oAn error occurred while synchronizing your license");
                                        } else {
                                            console.send(messages.prefix() + "&5&oAn error occurred while installing your synchronized license.");
                                        }
                                    }
                                } else {
                                    console.send(messages.prefix() + "&5&lYou already have a license! If you want to sync another license, remove the current license (&7/locklogin uninstall&5&l).");
                                    console.send(messages.prefix() + "&aIf you want to sync this server license with another one, run the command &7/locklogin sync&f {0}&a on the other server", license.syncKey());
                                }*/
                                console.send(messages.prefix() + "&5&oLockLogin web services has been shutdown");
                            }
                        }
                        break;
                    case 3:
                        switch (args[0].toLowerCase()) {
                            case "modules":
                                String moduleName = args[2];
                                File moduleFile = ModuleLoader.getModuleFile(moduleName);

                                if (moduleFile != null) {
                                    PluginModule module = getLoader().getModule(moduleFile);

                                    if (module != null) {
                                        switch (args[1].toLowerCase()) {
                                            case "load":
                                                if (!ModuleLoader.isLoaded(module) && module.load()) {
                                                    console.send(messages.prefix() + "&dModule " + moduleName + " has been loaded successfully");
                                                } else {
                                                    console.send(messages.prefix() + "&5&oModule " + moduleName + " failed to load, maybe is already loaded?");
                                                }

                                                break;
                                            case "unload":
                                                if (ModuleLoader.isLoaded(module) && module.unload()) {
                                                    console.send(messages.prefix() + "&dModule " + moduleName + " has been unloaded successfully");
                                                } else {
                                                    console.send(messages.prefix() + "&5&oModule " + moduleName + " failed to unload, maybe is not loaded?");
                                                }

                                                break;
                                            case "reload":
                                                module.reload();
                                                break;
                                            default:
                                                console.send(messages.prefix() + "&5&oAvailable sub-commands:&7 /locklogin modules &e<load>&7, &e<unload>&7, &e<reload>&7 &e[module name]");
                                                break;
                                        }
                                    } else {
                                        console.send(messages.prefix() + "&5&oModule " + moduleName + " does not exist!");
                                    }
                                } else {
                                    console.send(messages.prefix() + "&5&oModule " + moduleName + " is not loaded or does not exist!");
                                }
                                break;
                            case "backup":
                                String backup_name = args[2];
                                BackupScheduler scheduler = CurrentPlatform.getBackupScheduler();
                                plugin.console().send("Finding backup with id: {0}. Please wait", Level.INFO, backup_name);

                                scheduler.fetch(backup_name).whenComplete((backup) -> {
                                    if (backup != null) {
                                        switch (args[1].toLowerCase()) {
                                            case "remove":
                                                if (backup.destroy()) {
                                                    console.send(messages.prefix() + "&dSuccessfully removed backup {0}", backup_name);
                                                } else {
                                                    console.send(messages.prefix() + "&5&lFailed to remove backup {0}", backup_name);
                                                }
                                                break;
                                            case "restore":
                                                console.send(messages.prefix() + "&dTrying to restore backup... please wait");
                                                scheduler.restore(backup, true).whenComplete((result, restored, error) -> {
                                                    if (result) {
                                                        console.send(messages.prefix() + "&dSuccessfully restored backup and {0} accounts", restored);
                                                    } else {
                                                        console.send(messages.prefix() + "&5&lFailed to restore backup. {0} accounts were able to be restored", restored);
                                                        if (error != null) {
                                                            error.printStackTrace();
                                                        }
                                                    }
                                                });
                                                break;
                                            case "purge":
                                                console.send(messages.prefix() + "&dPreparing to purge all the backups before {0}", backup_name);
                                                scheduler.purge(backup.creation()).whenComplete((removed) -> console.send(messages.prefix() + "&dSuccessfully destroyed {0} backups", removed));
                                                break;
                                            case "create":
                                                scheduler.performBackup(backup_name).whenComplete((result) -> {
                                                    if (result) {
                                                        console.send(messages.prefix() + "&dSuccessfully created backup with id {0}", backup_name);
                                                    } else {
                                                        console.send(messages.prefix() + "&5&lFailed to create a backup with id {0}. Does a backup with that id already exist?", backup_name);
                                                    }
                                                });
                                                break;
                                            default:
                                                console.send(messages.prefix() + "&5&oAvailable sub-commands:&7 /locklogin backup &e<remove>&7, &e<restore>&7, &e<purge>&7, &e<create>&7 &e[backup]");
                                                break;
                                        }
                                    } else {
                                        plugin.console().send("Failed to fetch backup {0}. Does it exist?", Level.GRAVE, backup_name);
                                    }
                                });
                                break;
                            default:
                                console.send(messages.prefix() + "&5&oAvailable sub-commands:&7 /locklogin &e<reload>&7, &e<applyupdates>&7, &e<modules>&7, &e<version>&7, &e<changelog>&7, &e<check>&7, &e<backup>");
                                break;
                        }
                        break;
                    case 4:
                        String key = args[1];
                        String user = args[2];
                        String pass = args[3];

                        switch (args[0].toLowerCase()) {
                            case "install": {
                                /*License license = CurrentPlatform.getLicense();
                                if (license == null) {
                                    try {
                                        UUID id = UUID.fromString(key);
                                        PluginLicenseProvider provider = CurrentPlatform.getLicenseProvider();
                                        license = provider.request(id, user, pass);

                                        if (license != null && license.install()) {
                                            console.send(messages.prefix() + "&dSuccessfully downloaded and installed license. Restart your server to apply changes");
                                            try {
                                                JarManager.changeField(CurrentPlatform.class, "current_license", license);
                                            } catch (Throwable ex) {
                                                ex.printStackTrace();
                                            }
                                        } else {
                                            if (license == null) {
                                                console.send(messages.prefix() + "&5&oAn error occurred while downloading your license");
                                            } else {
                                                console.send(messages.prefix() + "&5&oAn error occurred while installing your license.");
                                            }
                                        }
                                    } catch (IllegalArgumentException e) {
                                        console.send(messages.prefix() + "&5&oAn invalid license ID has been provided, please provide the license ID received when purcharsed the license");
                                    }
                                } else {
                                    console.send(messages.prefix() + "&5&oCannot request a license");
                                }*/
                                console.send(messages.prefix() + "&5&oLockLogin web services has been shutdown");
                            }
                            break;
                            case "sync":{
                                /*License license = CurrentPlatform.getLicense();
                                if (license == null) {
                                    PluginLicenseProvider provider = CurrentPlatform.getLicenseProvider();
                                    license = provider.sync(key, user, pass);

                                    if (license != null && license.install()) {
                                        console.send(messages.prefix() + "&dSuccessfully synchronized and installed license. Restart your server to apply changes");
                                        try {
                                            JarManager.changeField(CurrentPlatform.class, "current_license", license);
                                        } catch (Throwable ex) {
                                            ex.printStackTrace();
                                        }
                                    } else {
                                        if (license == null) {
                                            console.send(messages.prefix() + "&5&oAn error occurred while synchronizing your license");
                                        } else {
                                            console.send(messages.prefix() + "&5&oAn error occurred while installing your synchronized license.");
                                        }
                                    }
                                } else {
                                    console.send(messages.prefix() + "&5&lYou already have a license! If you want to sync with another server, remove the current license (&7/locklogin uninstall&5&l).");
                                    console.send(messages.prefix() + "&aIf you want to sync this server license with another one, run the command &7/locklogin sync&f {0}&a on the other server", license.syncKey());
                                }*/
                                console.send(messages.prefix() + "&5&oLockLogin web services has been shutdown");
                            }
                            break;
                        }
                        break;
                    default:
                        console.send(messages.prefix() + "&5&oAvailable sub-commands:&7 /locklogin &e<reload>&7, &e<applyupdates>&7, &e<modules>&7, &e<version>&7, &e<changelog>&7, &e<check>&7, &e<backup>&7, &e<install>&7, &e<sync>&7, &e<uninstall>");
                        break;
                }
            }
        });

        return false;
    }
}
