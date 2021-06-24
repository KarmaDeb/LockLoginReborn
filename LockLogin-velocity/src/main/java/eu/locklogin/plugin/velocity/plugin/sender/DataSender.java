package eu.locklogin.plugin.velocity.plugin.sender;

/*
 * Private GSA code
 *
 * The use of this code
 * without GSA team authorization
 * will be a violation of
 * terms of use determined
 * in <a href="http://karmaconfigs.cf/license/"> here </a>
 * or (fallback domain) <a href="https://karmaconfigs.github.io/page/license"> here </a>
 */

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import eu.locklogin.api.file.ProxyConfiguration;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.utils.StringUtils;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.common.utils.MessagePool;
import eu.locklogin.api.common.utils.plugin.ServerDataStorager;
import eu.locklogin.plugin.velocity.util.files.Proxy;
import ml.karmaconfigs.api.common.utils.enums.Level;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static eu.locklogin.plugin.velocity.LockLogin.logger;
import static eu.locklogin.plugin.velocity.LockLogin.server;

@SuppressWarnings("UnstableApiUsage")
public final class DataSender {

    public final static String CHANNEL_PLAYER = "ll:account";
    public final static String PLUGIN_CHANNEL = "ll:plugin";
    public final static String ACCESS_CHANNEL = "ll:access";
    @SuppressWarnings("FieldMayBeFinal") //This could be modified by the cache loader, so it can't be final
    private static String key = StringUtils.randomString(18, StringUtils.StringGen.NUMBERS_AND_LETTERS, StringUtils.StringType.RANDOM_SIZE);

    private static final ConcurrentHashMultiset<MessagePool> data_pool = ConcurrentHashMultiset.create();

    /**
     * Send a plugin message on the player server
     *
     * @param player the player
     */
    public static void send(final Player player, final MessageData data) {
        if (!key.replaceAll("\\s", "").isEmpty()) {
            try {
                ServerConnection server = player.getCurrentServer().get();
                ServerInfo info = server.getServerInfo();

                if (ServerDataStorager.needsRegister(info.getName()) && ServerDataStorager.needsProxyKnowledge(info.getName()) && !data.getChannel().getName().equalsIgnoreCase(ACCESS_CHANNEL)) {
                    data_pool.add(new MessagePool(server.getServer(), data));
                } else {
                    server.sendPluginMessage(data.getChannel(), data.getData().toByteArray());
                }
            } catch (Throwable e) {
                logger.scheduleLog(Level.GRAVE, e);
                logger.scheduleLog(Level.INFO, "Error while sending a plugin message from Velocity");
            }
        } else {
            logger.scheduleLog(Level.GRAVE, "Tried to send plugin message with empty access key");
        }
    }

    /**
     * Send a plugin message to the server
     *
     * @param server the server
     */
    public static void send(final RegisteredServer server, final MessageData data) {
        if (!key.replaceAll("\\s", "").isEmpty()) {
            try {
                ServerInfo info = server.getServerInfo();

                if (ServerDataStorager.needsRegister(info.getName()) && ServerDataStorager.needsProxyKnowledge(info.getName()) && !data.getChannel().getName().equalsIgnoreCase(ACCESS_CHANNEL)) {
                    data_pool.add(new MessagePool(server, data));
                } else {
                    server.sendPluginMessage(data.getChannel(), data.getData().toByteArray());
                }
            } catch (Throwable e) {
                logger.scheduleLog(Level.GRAVE, e);
                logger.scheduleLog(Level.INFO, "Error while sending a plugin message from Velocity");
            }
        } else {
            logger.scheduleLog(Level.GRAVE, "Tried to send plugin message with empty access key");
        }
    }

    /**
     * Send a plugin message to the server
     *
     * @param channel the channel name
     * @param data    the data to send
     */
    public static void sendModule(final String channel, final byte[] data) {
        if (!key.replaceAll("\\s", "").isEmpty()) {
            try {
                Set<String> server_sents = new HashSet<>();

                for (Player player : server.getAllPlayers()) {
                    Optional<ServerConnection> tmp_server = player.getCurrentServer();
                    if (tmp_server.isPresent()) {
                        ServerConnection server = tmp_server.get();

                        ServerInfo info = server.getServerInfo();

                        if (!server_sents.contains(info.getName().toLowerCase())) {
                            server_sents.add(info.getName().toLowerCase());

                            if (!ServerDataStorager.needsRegister(info.getName()) && !ServerDataStorager.needsProxyKnowledge(info.getName())) {
                                ByteArrayDataOutput output = ByteStreams.newDataOutput();
                                ProxyConfiguration proxy = CurrentPlatform.getProxyConfiguration();

                                output.writeUTF(DataType.MODULE.name().toLowerCase());
                                output.writeUTF(proxy.getProxyID().toString());
                                output.writeUTF(key);
                                output.writeUTF(channel);
                                output.writeInt(data.length);
                                output.write(data);

                                server.sendPluginMessage(new LegacyChannelIdentifier(PLUGIN_CHANNEL), output.toByteArray());
                            }
                        }
                    }
                }
            } catch (Throwable e) {
                logger.scheduleLog(Level.GRAVE, e);
                logger.scheduleLog(Level.INFO, "Error while sending a plugin message from Velocity");
            }
        } else {
            logger.scheduleLog(Level.GRAVE, "Tried to send plugin message with empty access key");
        }
    }

    /**
     * Update data pool message
     *
     * @param name the server name that just registered
     */
    public static void updateDataPool(final String name) {
        for (MessagePool message : data_pool) {
            RegisteredServer server = (RegisteredServer) message.getServer();
            MessageData data = (MessageData) message.getMessage();

            if (server.getServerInfo().getName().equals(name)) {
                data_pool.remove(message);
                send(server, data);
            }
        }
    }

    /**
     * Get a message data builder instance
     *
     * @param type    the message type
     * @param owner   the message owner
     * @param channel the message channel name
     * @return a new message data builder instance
     */
    public static MessageDataBuilder getBuilder(final DataType type, final String channel, final Player owner) {
        return new MessageDataBuilder(type, owner).setChannel(channel);
    }

    public static class MessageDataBuilder {

        private final ByteArrayDataOutput output = ByteStreams.newDataOutput();

        private String channel = "";

        /**
         * Initialize the message data builder
         *
         * @param data the data type to send
         */
        MessageDataBuilder(final DataType data, final Player owner) throws IllegalArgumentException {
            ProxyConfiguration proxy = CurrentPlatform.getProxyConfiguration();

            output.writeUTF(data.name().toLowerCase());
            output.writeUTF(proxy.getProxyID().toString());
            output.writeUTF(key);
            if (owner != null) {
                output.writeUTF(owner.getGameProfile().getId().toString());
            } else {
                output.writeUTF(UUID.randomUUID().toString());
            }
        }

        /**
         * Set the message channel
         *
         * @param name the channel name
         * @return this instance
         */
        public final MessageDataBuilder setChannel(final String name) {
            channel = name;

            return this;
        }

        /**
         * Add text data to the final data
         *
         * @param data the data to add
         * @return this instance
         */
        public final MessageDataBuilder addTextData(final String data) {
            output.writeUTF(data);

            return this;
        }

        /**
         * Add boolean data to the final data
         *
         * @param data the data to add
         * @return this instance
         */
        public final MessageDataBuilder addBoolData(final boolean data) {
            output.writeBoolean(data);

            return this;
        }

        /**
         * Add integer data to the final data
         *
         * @param data the data to add
         * @return this instance
         */
        public final MessageDataBuilder addIntData(final int data) {
            output.writeInt(data);

            return this;
        }

        /**
         * Build the message
         *
         * @return the built message data
         */
        public final MessageData build() throws IllegalStateException {
            if (channel.replaceAll("\\s", "").isEmpty())
                throw new IllegalStateException("Tried to build message data with empty channel!");

            return new MessageData(output, channel);
        }
    }

    public static class MessageData {

        private final ByteArrayDataOutput output;
        private final String channel;

        /**
         * Initialize the message data
         *
         * @param data the data to send
         * @param name the channel name
         */
        public MessageData(final ByteArrayDataOutput data, final String name) {
            output = data;
            channel = name;
        }

        /**
         * Get the data to send
         *
         * @return the data to send
         */
        public final ByteArrayDataOutput getData() {
            return output;
        }

        /**
         * Get the channel name
         *
         * @return the channel name
         */
        public final LegacyChannelIdentifier getChannel() {
            return new LegacyChannelIdentifier(channel);
        }
    }
}
