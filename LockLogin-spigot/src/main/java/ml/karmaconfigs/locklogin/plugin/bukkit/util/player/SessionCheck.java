package ml.karmaconfigs.locklogin.plugin.bukkit.util.player;

import ml.karmaconfigs.api.bukkit.timer.AdvancedPluginTimer;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.configuration.Config;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.messages.Message;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.*;

public final class SessionCheck implements Runnable {

    private final Player player;
    private final Set<UUID> under_check = new HashSet<>();

    private final Consumer<Player> authAction;
    private final Consumer<Player> failAction;

    /**
     * Initialize the status checker
     *
     * @param target the target over who
     *               perform the check
     * @param onAuth the action to perform when
     *               the player auths successfully
     * @param onFail the action to perform
     *               if the player doesn't auth at time
     */
    public SessionCheck(final Player target, final Consumer<Player> onAuth, final Consumer<Player> onFail) {
        player = target;
        authAction = onAuth;
        failAction = onFail;
    }

    /**
     * Check the user session status
     */
    @Override
    public final void run() {
        if (!under_check.contains(player.getUniqueId())) {
            System.out.println("Starting session check");
            under_check.add(player.getUniqueId());

            User user = new User(player);
            Config config = new Config();
            Message messages = new Message();

            int time = config.registerOptions().timeOut();
            if (user.isRegistered()) {
                time = config.loginOptions().timeOut();
                user.send(messages.prefix() + messages.login());
            } else {
                user.send(messages.prefix() + messages.register());
            }

            AdvancedPluginTimer timer = new AdvancedPluginTimer(plugin, time, false).setAsync(false);
            timer.addAction(() -> {
                ClientSession session = user.getSession();
                if (!session.isLogged()) {
                    int timer_time = timer.getTime();

                    if (user.isRegistered()) {
                        if (!StringUtils.isNullOrEmpty(messages.loginTitle(timer_time)) || !StringUtils.isNullOrEmpty(messages.loginSubtitle(timer_time)))
                            user.send(messages.loginTitle(timer_time), messages.loginSubtitle(timer_time));
                    } else {
                        if (!StringUtils.isNullOrEmpty(messages.registerTitle(timer_time)) || !StringUtils.isNullOrEmpty(messages.registerSubtitle(timer_time)))
                            user.send(messages.registerTitle(timer_time), messages.registerSubtitle(timer_time));
                    }
                } else {
                    timer.setCancelled();
                }
            }).addActionOnEnd(() -> {
                ClientSession session = user.getSession();

                if (!session.isLogged())
                    user.kick((user.isRegistered() ? messages.loginTimeOut() : messages.registerTimeOut()));
                user.restorePotionEffects();

                under_check.remove(player.getUniqueId());

                if (failAction != null)
                    failAction.accept(player);
            }).addActionOnCancel(() -> {
                user.restorePotionEffects();
                under_check.remove(player.getUniqueId());

                if (authAction != null)
                    authAction.accept(player);
            });

            timer.start();
            startMessageTask();
        }
    }

    /**
     * Initialize the session message
     * task
     */
    private void startMessageTask() {
        Config config = new Config();
        Message messages = new Message();
        User user = new User(player);

        int time = config.registerOptions().getMessageInterval();
        if (user.isRegistered())
            time = config.loginOptions().getMessageInterval();

        AdvancedPluginTimer timer = new AdvancedPluginTimer(plugin, time, true);
        timer.addActionOnEnd(() -> {
            ClientSession session = user.getSession();
            if (!session.isLogged()) {
                if (user.isRegistered())
                    user.send(messages.prefix() + messages.login());
                else
                    user.send(messages.prefix() + messages.register());
            } else {
                timer.setCancelled();
            }
        });
        timer.start();
    }
}
