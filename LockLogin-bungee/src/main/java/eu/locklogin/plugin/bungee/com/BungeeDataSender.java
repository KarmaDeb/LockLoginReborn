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
import eu.locklogin.api.plugin.license.License;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bungee.Main;
import eu.locklogin.plugin.bungee.com.queue.MessageQueue;
import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.timer.SchedulerUnit;
import ml.karmaconfigs.api.common.timer.SourceScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import ml.karmaconfigs.api.common.utils.enums.Level;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;

import static eu.locklogin.plugin.bungee.LockLogin.logger;
import static eu.locklogin.plugin.bungee.LockLogin.plugin;

@SuppressWarnings("UnstableApiUsage")
public class BungeeDataSender extends DataSender {

    public BungeeDataSender() {
        License license = CurrentPlatform.getLicense();
        if (license != null) {
            String com = license.comKey();

            SimpleScheduler scheduler = new SourceScheduler(plugin, 250, SchedulerUnit.MILLISECOND, true).multiThreading(true);
            scheduler.restartAction(() -> {
                Gson gson = new GsonBuilder().setLenient().create();

                for (ServerInfo server : QueueHandler.bungee_server_queues.keySet()) {
                    DataQueue queue = QueueHandler.bungee_server_queues.get(server);
                    Packet next_data = queue.next();

                    if (next_data == null || server == null) {
                        queue.cancel();
                        return;
                    }

                    Collection<ProxiedPlayer> players = server.getPlayers();
                    if (players.isEmpty()) {
                        queue.cancel();
                        return;
                    }

                    ByteArrayDataInput in = ByteStreams.newDataInput(next_data.packetData());
                    String line = in.readUTF();

                    JsonObject json = gson.fromJson(line, JsonObject.class);
                    Channel channel = Channel.fromName(json.remove("channel").getAsString());

                    if (channel == null) {
                        queue.consume();
                        return;
                    }

                    if (!channel.equals(Channel.ACCESS)) {
                        if (ServerDataStorage.needsProxyKnowledge(server.getName()) || queue.locked()) {
                            queue.shift();
                            return;
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
        } else {
            plugin.console().send("IMPORTANT! Please synchronize this server with your proxy license (/locklogin sync) or install a license (/locklogin install) and synchronize the installed license with your network to enable LockLogin BungeeCord", Level.GRAVE);
        }
    }

    /**
     * Get the data queue
     *
     * @param name the queue name
     * @return the data queue
     */
    @Override
    public DataQueue queue(final String name) {
        ServerInfo info = plugin.getProxy().getServerInfo(name);

        if (info != null) {
            return QueueHandler.bungee_server_queues.computeIfAbsent(info, (nm) -> new MessageQueue());
        }

        return new MessageQueue();
    }
}
