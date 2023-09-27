package eu.locklogin.plugin.bungee.com;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import eu.locklogin.api.common.communication.DataSender;
import eu.locklogin.api.common.communication.Packet;
import eu.locklogin.api.common.communication.queue.DataQueue;
import eu.locklogin.api.common.utils.Channel;
import eu.locklogin.api.common.utils.plugin.ServerDataStorage;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bungee.com.queue.MessageQueue;
import ml.karmaconfigs.api.common.timer.SchedulerUnit;
import ml.karmaconfigs.api.common.timer.SourceScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static eu.locklogin.plugin.bungee.LockLogin.plugin;

@SuppressWarnings("UnstableApiUsage")
public class BungeeDataSender extends DataSender<ServerInfo> {

    private final static Map<ServerInfo, DataQueue> serverQues = new ConcurrentHashMap<>();;

    public BungeeDataSender() {
        PluginConfiguration config = CurrentPlatform.getConfiguration();
        String com = config.comKey();

        SimpleScheduler scheduler = new SourceScheduler(plugin, 250, SchedulerUnit.MILLISECOND, true).multiThreading(true);
        Gson gson = new GsonBuilder().setLenient().create();

        scheduler.restartAction(() -> {
            for (ServerInfo server : serverQues.keySet()) {
                if (server == null || server.getPlayers().isEmpty()) {
                    continue;
                }

                DataQueue queue = serverQues.get(server);
                Packet next_data = queue.next();

                if (next_data == null) {
                    queue.cancel();
                    continue;
                }

                Collection<ProxiedPlayer> players = server.getPlayers();
                if (players.isEmpty()) {
                    queue.cancel();
                    continue;
                }

                ByteArrayDataInput in = ByteStreams.newDataInput(next_data.packetData());
                String line = in.readUTF();

                JsonObject json = gson.fromJson(line, JsonObject.class);
                Channel channel = Channel.fromName(json.remove("channel").getAsString());

                if (channel == null) {
                    queue.consume();
                    continue;
                }

                if (!channel.equals(Channel.ACCESS)) {
                    if (ServerDataStorage.needsProxyKnowledge(server.getName()) || queue.locked()) {
                        queue.shift();
                        continue;
                    }
                }

                json.addProperty("key", com);
                json.addProperty("server", server.getName());

                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF(gson.toJson(json));

                server.sendData(channel.getName(), out.toByteArray());
                queue.consume();
            }
        });

        scheduler.start();
    }

    /**
     * Get the data queue
     *
     * @param info the queue name
     * @return the data queue
     */
    @Override
    public DataQueue queue(final ServerInfo info) {
        if (info != null) {
            return serverQues.computeIfAbsent(info, (nm) -> new MessageQueue());
        }

        return new MessageQueue();
    }
}
