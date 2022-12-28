package eu.locklogin.plugin.bungee.com;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import eu.locklogin.api.common.communication.DataSender;
import eu.locklogin.api.common.communication.queue.DataQueue;
import eu.locklogin.api.common.utils.Channel;
import eu.locklogin.api.common.utils.plugin.ServerDataStorage;
import eu.locklogin.api.encryption.CryptoFactory;
import eu.locklogin.api.encryption.Validation;
import eu.locklogin.plugin.bungee.BungeeSender;
import eu.locklogin.plugin.bungee.com.queue.MessageQueue;
import ml.karmaconfigs.api.common.timer.SchedulerUnit;
import ml.karmaconfigs.api.common.timer.SourceScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;

import java.util.Collection;

import static eu.locklogin.plugin.bungee.LockLogin.console;
import static eu.locklogin.plugin.bungee.LockLogin.plugin;

@SuppressWarnings("UnstableApiUsage")
public class BungeeDataSender extends DataSender {

    @SuppressWarnings("FieldMayBeFinal")
    private static String com = "";
    @SuppressWarnings("FieldMayBeFinal")
    private static String secret = "";

    public BungeeDataSender() {
        SimpleScheduler scheduler = new SourceScheduler(plugin, 250, SchedulerUnit.MILLISECOND, true).multiThreading(true);
        scheduler.restartAction(() -> {
            Gson gson = new GsonBuilder().setLenient().create();

            for (String server : QueueHandler.server_queues.keySet()) {
                DataQueue queue = QueueHandler.server_queues.computeIfAbsent(server, (nm) -> new MessageQueue());
                byte[] next_data = queue.next();
                ServerInfo sv = ProxyServer.getInstance().getServerInfo(server);

                if (next_data == null || sv == null) {
                    queue.cancel();
                    continue;
                }

                Collection<ProxiedPlayer> players = sv.getPlayers();
                if (players.isEmpty()) {
                    queue.cancel();
                    continue;
                }
                ServerInfo target = null;

                for (ProxiedPlayer player : players) {
                    Server connected = player.getServer();
                    if (connected != null && connected.getInfo() != null) {
                        target = connected.getInfo();
                        if (target != null) {
                            break;
                        }
                    }
                }
                if (target == null) {
                    queue.cancel();
                    continue;
                }

                ByteArrayDataInput in = ByteStreams.newDataInput(next_data);
                String line = in.readUTF();

                JsonObject json = gson.fromJson(line, JsonObject.class);
                String channel = json.remove("channel").getAsString();
                if (!Channel.valueOf(channel).equals(Channel.ACCESS)) {
                    if (ServerDataStorage.needsProxyKnowledge(server) || queue.locked()) {
                        queue.shift();
                        continue;
                    }
                }

                console.send("Server {0} has data to send ({1})", server, line);
                json.addProperty("socket", BungeeSender.useSocket);
                json.addProperty("proxy", secret);
                json.addProperty("key", com);
                json.addProperty("server", server);

                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF(gson.toJson(json));

                sv.sendData(channel, out.toByteArray());
                queue.consume();
            }
        });

        scheduler.start();
    }

    /**
     * Get the data queue
     *
     * @param name the queue name
     * @return the data queue
     */
    @Override
    public DataQueue queue(final String name) {
        return QueueHandler.server_queues.computeIfAbsent(name, (nm) -> new MessageQueue());
    }

    /**
     * Validate the com hash
     *
     * @param hash the communication hash
     * @return if the com has is valid
     */
    public static boolean validate(final String hash) {
        return CryptoFactory.getBuilder().withPassword(com).withToken(hash).build().validate(Validation.ALL);
    }
}
