package ml.karmaconfigs.locklogin.plugin.velocity.plugin.sender;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.plugin.common.utils.DataType;
import ml.karmaconfigs.locklogin.plugin.common.utils.plugin.ServerDataStorager;
import ml.karmaconfigs.locklogin.plugin.velocity.util.files.Proxy;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static ml.karmaconfigs.locklogin.plugin.velocity.LockLogin.logger;
import static ml.karmaconfigs.locklogin.plugin.velocity.LockLogin.server;

@SuppressWarnings("UnstableApiUsage")
public final class DataSender {

    public final static String CHANNEL_PLAYER = "ll:account";
    public final static String PLUGIN_CHANNEL = "ll:plugin";
    public final static String ACCESS_CHANNEL = "ll:access";
    @SuppressWarnings("FieldMayBeFinal") //This could be modified by the cache loader, so it can't be final
    private static String key = StringUtils.randomString(18, StringUtils.StringGen.NUMBERS_AND_LETTERS, StringUtils.StringType.RANDOM_SIZE);

    /**
     * Send a plugin message on the player server
     *
     * @param player the player
     */
    public static void send(final Player player, final MessageData data) {
        if (!key.replaceAll("\\s", "").isEmpty()) {
            if (player != null && player.getCurrentServer().isPresent() && player.isActive()) {
                try {
                    ServerConnection server = player.getCurrentServer().get();
                    ServerInfo info = server.getServerInfo();

                    if (!ServerDataStorager.needsRegister(info.getName()) && !ServerDataStorager.needsProxyKnowledge(info.getName()) || data.getChannel().getName().equalsIgnoreCase(ACCESS_CHANNEL))
                        server.sendPluginMessage(data.getChannel(), data.getData().toByteArray());
                } catch (Throwable e) {
                    logger.scheduleLog(Level.GRAVE, e);
                    logger.scheduleLog(Level.INFO, "Error while sending a plugin message from Velocity");
                }
            } else {
                logger.scheduleLog(Level.INFO, "Failed to send plugin message: ");
                logger.scheduleLog(Level.INFO, "\n```yaml\nPlayer null: {0}\nServer null: {1}\nConnected: {2}\n```\n\n", player == null, (player == null || !player.getCurrentServer().isPresent()), (player != null && player.isActive()));
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

                if (!ServerDataStorager.needsRegister(info.getName()) && !ServerDataStorager.needsProxyKnowledge(info.getName()) || data.getChannel().getName().equalsIgnoreCase(ACCESS_CHANNEL))
                    server.sendPluginMessage(data.getChannel(), data.getData().toByteArray());
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
     * @param data the data to send
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
                                Proxy proxy = new Proxy();

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
     * Get a message data builder instance
     *
     * @param type    the message type
     * @param channel the message channel name
     * @return a new message data builder instance
     */
    public static MessageDataBuilder getBuilder(final DataType type, final String channel) {
        return new MessageDataBuilder(type).setChannel(channel);
    }

    public static class MessageDataBuilder {

        private final ByteArrayDataOutput output = ByteStreams.newDataOutput();

        private String channel = "";

        /**
         * Initialize the message data builder
         *
         * @param data the data type to send
         */
        MessageDataBuilder(final DataType data) throws IllegalArgumentException {
            Proxy proxy = new Proxy();

            output.writeUTF(data.name().toLowerCase());
            output.writeUTF(proxy.getProxyID().toString());
            output.writeUTF(key);
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
