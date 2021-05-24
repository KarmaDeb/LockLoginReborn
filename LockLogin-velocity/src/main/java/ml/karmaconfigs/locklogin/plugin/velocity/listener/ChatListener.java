package ml.karmaconfigs.locklogin.plugin.velocity.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.modules.util.javamodule.JavaModuleManager;
import ml.karmaconfigs.locklogin.plugin.common.security.AllowedCommand;
import ml.karmaconfigs.locklogin.plugin.velocity.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.velocity.util.player.User;

import static ml.karmaconfigs.locklogin.plugin.velocity.LockLogin.properties;

public final class ChatListener {

    @Subscribe(order = PostOrder.FIRST)
    public final void onChat(PlayerChatEvent e) {
        Player player = e.getPlayer();
        User user = new User(player);
        ClientSession session = user.getSession();

        Message messages = new Message();

        if (session.isValid()) {
            if (!session.isLogged() || !session.isTempLogged()) {
                if (user.getManager().has2FA() && session.isLogged()) {
                    e.setResult(PlayerChatEvent.ChatResult.denied());
                    user.send(messages.prefix() + messages.gAuthenticate());
                } else {
                    if (!AllowedCommand.isAllowed(getCommand(e.getMessage()))) {
                        e.setResult(PlayerChatEvent.ChatResult.denied());
                        return;
                    }
                }
            }
        } else {
            //Instantly cancel the event if the player session is not validated by LockLogin
            user.send(messages.prefix() + properties.getProperty("session_not_valid", "&5&oYour session is invalid, try leaving and joining the server again"));
            e.setResult(PlayerChatEvent.ChatResult.denied());
            return;
        }

        if (JavaModuleManager.parseCommand(e.getMessage())) {
            e.setResult(PlayerChatEvent.ChatResult.denied());
            JavaModuleManager.fireCommand(player, e.getMessage());
        }
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onCommand(CommandExecuteEvent e) {
        if (e.getCommandSource() instanceof Player) {
            Player player = (Player) e.getCommandSource();

            User user = new User(player);
            ClientSession session = user.getSession();

            Message messages = new Message();

            if (session.isValid()) {
                if (!session.isLogged()) {
                    String command = getCommand(e.getCommand());
                    if (!command.equals("register") && !command.equals("login") && !AllowedCommand.isAllowed(command)) {
                        e.setResult(CommandExecuteEvent.CommandResult.denied());
                    }

                    return;
                } else {
                    if (!session.isTempLogged() && user.getManager().has2FA()) {
                        String command = getCommand(e.getCommand());

                        if (!command.equals("2fa")) {
                            e.setResult(CommandExecuteEvent.CommandResult.denied());
                            user.send(messages.prefix() + messages.gAuthenticate());
                        }
                    }
                }
            } else {
                //Instantly cancel the event if the player session is not validated by LockLogin
                user.send(messages.prefix() + properties.getProperty("session_not_valid", "&5&oYour session is invalid, try leaving and joining the server again"));
                e.setResult(CommandExecuteEvent.CommandResult.denied());
                return;
            }

            if (JavaModuleManager.parseCommand(e.getCommand())) {
                e.setResult(CommandExecuteEvent.CommandResult.denied());
                JavaModuleManager.fireCommand(player, e.getCommand());
            }
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
