package ml.karmaconfigs.locklogin.plugin.bungee.listener;

import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.modules.util.javamodule.JavaModuleManager;
import ml.karmaconfigs.locklogin.plugin.bungee.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.bungee.util.player.User;
import ml.karmaconfigs.locklogin.plugin.common.security.AllowedCommand;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import static ml.karmaconfigs.locklogin.plugin.bungee.LockLogin.properties;

public final class ChatListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onChat(ChatEvent e) {
        if (e.getMessage().startsWith("/")) {
            parseCommand(e);
        } else {
            ProxiedPlayer player = (ProxiedPlayer) e.getSender();
            User user = new User(player);
            ClientSession session = user.getSession();

            Message messages = new Message();

            if (session.isValid()) {
                if (!session.isLogged() || !session.isTempLogged()) {
                    if (user.getManager().has2FA() && session.isLogged()) {
                        e.setCancelled(true);
                        user.send(messages.prefix() + messages.gAuthenticate());
                    } else {
                        if (!AllowedCommand.isAllowed(getCommand(e.getMessage()))) {
                            e.setCancelled(true);
                            return;
                        }
                    }
                }
            } else {
                //Instantly cancel the event if the player session is not validated by LockLogin
                user.send(messages.prefix() + properties.getProperty("session_not_valid", "&5&oYour session is invalid, try leaving and joining the server again"));
                e.setCancelled(true);
                return;
            }

            if (JavaModuleManager.parseCommand(e.getMessage())) {
                e.setCancelled(true);
                JavaModuleManager.fireCommand(player, e.getMessage());
            }
        }
    }

    /**
     * Parse the player command
     *
     * @param e the chat event
     */
    private void parseCommand(final ChatEvent e) {
        ProxiedPlayer player = (ProxiedPlayer) e.getSender();
        User user = new User(player);
        ClientSession session = user.getSession();

        Message messages = new Message();

        if (session.isValid()) {
            if (!session.isLogged()) {
                String command = getCommand(e.getMessage());
                e.setCancelled(!command.equals("register") && !command.equals("login") && !AllowedCommand.isAllowed(command));

                return;
            } else {
                if (!session.isTempLogged() && user.getManager().has2FA()) {
                    String command = getCommand(e.getMessage());

                    if (!command.equals("2fa")) {
                        e.setCancelled(true);
                        user.send(messages.prefix() + messages.gAuthenticate());
                    }
                }
            }
        } else {
            //Instantly cancel the event if the player session is not validated by LockLogin
            user.send(messages.prefix() + properties.getProperty("session_not_valid", "&5&oYour session is invalid, try leaving and joining the server again"));
            e.setCancelled(true);
            return;
        }

        if (JavaModuleManager.parseCommand(e.getMessage())) {
            e.setCancelled(true);
            JavaModuleManager.fireCommand(player, e.getMessage());
        }
    }

    /**
     * Get the main command from the cmd
     * even if it has :
     *
     * @param cmd the cmd
     * @return a command ignoring ":" prefix
     */
    private String getCommand(String cmd) {
        if (cmd.contains(":")) {
            try {
                String[] cmdData = cmd.split(":");

                if (cmdData[0] != null && !cmdData[0].isEmpty()) {
                    if (cmdData[1] != null && !cmdData[1].isEmpty()) {
                        return cmdData[1];
                    }
                }
            } catch (Throwable ignored) {
            }
            return cmd.split(" ")[0].replace("/", "");
        } else {
            if (cmd.contains(" ")) {
                return cmd.split(" ")[0].replace("/", "");
            } else {
                return cmd.replace("/", "");
            }
        }
    }
}
