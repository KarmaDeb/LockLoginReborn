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

import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.premium.PremiumDatabase;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.LockLogin;
import eu.locklogin.plugin.bukkit.command.util.SystemCommand;
import eu.locklogin.plugin.bukkit.util.player.User;

import ml.karmaconfigs.api.common.minecraft.UUIDFetcher;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.utils.uuid.UUIDType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static eu.locklogin.plugin.bukkit.LockLogin.*;

@SystemCommand(command = "premium")
@SuppressWarnings("unused")
public class PremiumCommand implements CommandExecutor {

    private final static Set<UUID> confirmation = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Executes the given command, returning its success.
     * <br>
     * If false is returned, then the "usage" plugin.yml entry for this command
     * (if defined) will be sent to the player.
     *
     * @param sender  Source of the command
     * @param command Command which was executed
     * @param label   Alias of the command which was used
     * @param tmpArgs    Passed command arguments
     * @return true if a valid command, otherwise false
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] tmpArgs) {
        PluginConfiguration config = CurrentPlatform.getConfiguration();
        PluginMessages messages = CurrentPlatform.getMessages();

        sender.sendMessage(StringUtils.toColor(messages.prefix() + properties.
                getProperty(
                        "processing_async",
                        "&dProcessing {0} command, please wait for feedback")
                .replace("{0}", label)));

        if (sender instanceof Player) {
            Player player = (Player) sender;
            User user = new User(player);

            if (user.getSession().isValid()) {
                PremiumDatabase database = CurrentPlatform.getPremiumDatabase();

                UUID online_uuid = UUIDFetcher.fetchUUID(player.getName(), UUIDType.ONLINE);
                UUID offline_uuid = UUIDFetcher.fetchUUID(player.getName(), UUIDType.OFFLINE);

                if (online_uuid == null) online_uuid = player.getUniqueId();
                if (offline_uuid == null) offline_uuid = player.getUniqueId();

                if (online_uuid.equals(offline_uuid)) {
                    user.send(messages.prefix() + messages.premiumFailAuth());
                } else {
                    if (database.isPremium(offline_uuid)) {
                        if (database.setPremium(offline_uuid, false)) {
                            ClientSession session = user.getSession();
                            session.setPinLogged(false);
                            session.set2FALogged(false);
                            session.setLogged(false);

                            user.kick(messages.premiumDisabled());
                        } else {
                            user.send(messages.prefix() + messages.premiumError());
                        }
                    } else {
                        if (confirmation.contains(player.getUniqueId())) {
                            confirmation.remove(player.getUniqueId());

                            if (database.setPremium(offline_uuid, true)) {
                                user.kick(messages.premiumEnabled());
                            } else {
                                user.send(messages.prefix() + messages.premiumError());
                            }
                        } else {
                            user.send(messages.premiumWarning());
                            confirmation.add(player.getUniqueId());
                            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                confirmation.remove(player.getUniqueId());
                            }, 20 * 10);
                        }
                    }
                }
            } else {
                user.send(messages.prefix() + LockLogin.properties.getProperty("session_not_valid", "&5&oYour session is invalid, try leaving and joining the server again"));
            }
        } else {
            console.send(messages.prefix() + LockLogin.properties.getProperty("command_not_available", "&cThis command is not available for console"));
        }

        return false;
    }
}
