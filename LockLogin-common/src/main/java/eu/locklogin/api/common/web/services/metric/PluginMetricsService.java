package eu.locklogin.api.common.web.services.metric;

import com.google.gson.JsonObject;
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.common.security.client.Name;
import eu.locklogin.api.common.utils.plugin.FloodGateUtil;
import eu.locklogin.api.common.web.services.socket.SocketClient;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.util.enums.ManagerType;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.api.util.platform.ModuleServer;
import io.socket.client.Socket;
import ml.karmaconfigs.api.common.karma.source.KarmaSource;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.timer.SchedulerUnit;
import ml.karmaconfigs.api.common.timer.SourceScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import ml.karmaconfigs.api.common.utils.enums.Level;

import java.util.concurrent.atomic.AtomicInteger;

public final class PluginMetricsService {

    private static KarmaSource plugin;
    private static SocketClient socket;
    private static SimpleScheduler scheduler;

    public PluginMetricsService(final KarmaSource src, final SocketClient s) {
        plugin = src;
        socket = s;
    }

    /**
     * Start the service
     */
    public void start() {
        if (socket != null) {
            if (scheduler == null) {
                Socket fl_cl = socket.client();
                if (fl_cl.connected()) {
                    JsonObject statistics = new JsonObject();
                    PluginConfiguration configuration = CurrentPlatform.getConfiguration();

                    ModuleServer server = CurrentPlatform.getServer();

                    statistics.addProperty("share", configuration.sharePlugin());
                    statistics.addProperty("online", server.getOnlinePlayers().size());
                    statistics.addProperty("port", server.port());
                    statistics.addProperty("platform", CurrentPlatform.getPlatform().name().toLowerCase());
                    fl_cl.emit("stats", statistics);

                    AccountManager manager = CurrentPlatform.getAccountManager(ManagerType.CUSTOM, null);
                    if (manager != null) {
                        AtomicInteger time = new AtomicInteger(1);
                        manager.getAccounts().forEach((account) -> {
                            String name = account.getName();

                            if (!StringUtils.isNullOrEmpty(name)) {
                                SimpleScheduler tS = new SourceScheduler(plugin, time.getAndIncrement(), SchedulerUnit.SECOND, false);
                                tS.endAction(() -> register(name));
                                tS.start();
                            }
                        });
                    }
                }

                scheduler = new SourceScheduler(plugin, 5, SchedulerUnit.MINUTE, true);
                scheduler.restartAction(() -> {
                    Socket cl = socket.client();
                    if (cl.connected()) {
                        JsonObject statistics = new JsonObject();
                        PluginConfiguration configuration = CurrentPlatform.getConfiguration();

                        ModuleServer server = CurrentPlatform.getServer();

                        statistics.addProperty("share", configuration.sharePlugin());
                        statistics.addProperty("online", server.getOnlinePlayers().size());
                        statistics.addProperty("port", server.port());
                        statistics.addProperty("platform", CurrentPlatform.getPlatform().name().toLowerCase());
                        cl.emit("stats", statistics);
                    }
                });

                scheduler.start();
            }
        } else {
            plugin.console().send("Failed to register plugin statistics", Level.WARNING);
        }
    }

    /**
     * Register a client
     *
     * @param name the client name
     */
    public static void register(String name) {
        if (socket != null) {
            Socket cl = socket.client();

            if (!StringUtils.isNullOrEmpty(name)) {
                JsonObject info = new JsonObject();
                Name check = new Name(name);
                if (check.notValid()) {
                    if (FloodGateUtil.hasFloodgate()) {
                        FloodGateUtil util = new FloodGateUtil(null);
                        name = util.removeNamePrefix(name);
                    } else {
                        name = null;
                    }
                }

                if (name != null) {
                    info.addProperty("name", name);
                    cl.emit("register", info);
                }
            }
        } else {
            plugin.console().send("Failed to register plugin statistics", Level.WARNING);
        }
    }
}
