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

import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.premium.PremiumDatabase;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bungee.command.util.SystemCommand;
import eu.locklogin.plugin.bungee.util.player.User;
import ml.karmaconfigs.api.common.utils.uuid.UUIDType;
import ml.karmaconfigs.api.common.utils.uuid.UUIDUtil;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static eu.locklogin.plugin.bungee.LockLogin.*;

@SystemCommand(command = "premium")
@SuppressWarnings("unused")
public class PremiumCommand extends Command {

    private final static Set<UUID> confirmation = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Construct a new command with no permissions or aliases.
     *
     * @param name the name of this command
     */
    public PremiumCommand(final String name, final List<String> aliases) {
        super(name, "", aliases.toArray(new String[0]));
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
        PluginMessages messages = CurrentPlatform.getMessages();

        if (sender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) sender;
            User user = new User(player);

            if (user.getSession().isValid()) {
                PremiumDatabase database = CurrentPlatform.getPremiumDatabase();
                UUID online_uuid = UUIDUtil.fetch(player.getName(), UUIDType.ONLINE);
                UUID offline_uuid = UUIDUtil.fetch(player.getName(), UUIDType.OFFLINE);

                if (online_uuid.equals(offline_uuid)) {
                    user.send(messages.prefix() + messages.premiumFailAuth());
                } else {
                    if (database.isPremium(online_uuid)) {
                        if (database.setPremium(online_uuid, false)) {
                            user.kick(messages.premiumDisabled());
                        } else {
                            user.send(messages.prefix() + messages.premiumError());
                        }
                    } else {
                        if (confirmation.contains(player.getUniqueId())) {
                            confirmation.remove(player.getUniqueId());

                            if (database.setPremium(online_uuid, true)) {
                                user.kick(messages.premiumEnabled());
                            } else {
                                user.send(messages.prefix() + messages.premiumError());
                            }
                        } else {
                            user.send(messages.premiumWarning());
                            confirmation.add(player.getUniqueId());
                            plugin.getProxy().getScheduler().schedule(plugin, () -> {
                                confirmation.remove(player.getUniqueId());
                            }, 10, TimeUnit.SECONDS);
                        }
                    }
                }
            } else {
                user.send(messages.prefix() + properties.getProperty("session_not_valid", "&5&oYour session is invalid, try leaving and joining the server again"));
            }
        } else {
            console.send(messages.prefix() + properties.getProperty("command_not_available", "&cThis command is not available for console"));
        }
    }
}
