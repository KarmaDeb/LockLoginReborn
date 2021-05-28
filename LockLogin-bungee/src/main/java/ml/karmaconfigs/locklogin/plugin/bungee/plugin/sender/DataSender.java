package ml.karmaconfigs.locklogin.plugin.bungee.plugin.sender;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.plugin.bungee.util.files.Proxy;
import ml.karmaconfigs.locklogin.plugin.common.utils.DataType;
import ml.karmaconfigs.locklogin.plugin.common.utils.plugin.ServerDataStorager;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static ml.karmaconfigs.locklogin.plugin.bungee.LockLogin.logger;
import static ml.karmaconfigs.locklogin.plugin.bungee.LockLogin.plugin;

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
    public static void send(final ProxiedPlayer player, final MessageData data) {
        if (key.replaceAll("\\s", "").isEmpty()) {
            key = StringUtils.randomString(18, StringUtils.StringGen.NUMBERS_AND_LETTERS, StringUtils.StringType.RANDOM_SIZE);
            logger.scheduleLog(Level.INFO, "Generated proxy communication key");
        }

        try {
            ServerInfo server = player.getServer().getInfo();
            if (!ServerDataStorager.needsRegister(server.getName()) && !ServerDataStorager.needsProxyKnowledge(server.getName()) || data.getChannel().equalsIgnoreCase(ACCESS_CHANNEL))
                server.sendData(data.getChannel(), data.getData().toByteArray());
        } catch (Throwable e) {
            logger.scheduleLog(Level.GRAVE, e);
            logger.scheduleLog(Level.INFO, "Error while sending a plugin message from BungeeCord");
        }
    }

    /**
     * Send a plugin message to the server
     *
     * @param server the server
     */
    public static void send(final ServerInfo server, final MessageData data) {
        if (key.replaceAll("\\s", "").isEmpty()) {
            key = StringUtils.randomString(18, StringUtils.StringGen.NUMBERS_AND_LETTERS, StringUtils.StringType.RANDOM_SIZE);
            logger.scheduleLog(Level.INFO, "Generated proxy communication key");
        }

        try {
            if (!ServerDataStorager.needsRegister(server.getName()) && !ServerDataStorager.needsProxyKnowledge(server.getName()) || data.getChannel().equalsIgnoreCase(ACCESS_CHANNEL))
                server.sendData(data.getChannel(), data.getData().toByteArray());
        } catch (Throwable e) {
            logger.scheduleLog(Level.GRAVE, e);
            logger.scheduleLog(Level.INFO, "Error while sending a plugin message from BungeeCord");
        }
    }

    /**
     * Send a plugin message to the server
     *
     * @param channel the channel name
     * @param data the data to send
     */
    public static void sendModule(final String channel, final byte[] data) {
        if (key.replaceAll("\\s", "").isEmpty()) {
            key = StringUtils.randomString(18, StringUtils.StringGen.NUMBERS_AND_LETTERS, StringUtils.StringType.RANDOM_SIZE);
            logger.scheduleLog(Level.INFO, "Generated proxy communication key");
        }

        try {
            Set<String> server_sents = new HashSet<>();

            for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
                Server server = player.getServer();

                if (server != null) {
                    ServerInfo info = server.getInfo();

                    if (!server_sents.contains(info.getName().toLowerCase())) {
                        server_sents.add(info.getName().toLowerCase());

                        if (!ServerDataStorager.needsRegister(info.getName()) && !ServerDataStorager.needsProxyKnowledge(info.getName()) || channel.equalsIgnoreCase(ACCESS_CHANNEL)) {
                            ByteArrayDataOutput output = ByteStreams.newDataOutput();
                            Proxy proxy = new Proxy();

                            output.writeUTF(DataType.MODULE.name().toLowerCase());
                            output.writeUTF(proxy.getProxyID().toString());
                            output.writeUTF(key);
                            output.writeUTF(channel);
                            output.writeInt(data.length);
                            output.write(data);

                            server.sendData(PLUGIN_CHANNEL, output.toByteArray());
                        }
                    }
                }
            }
        } catch (Throwable e) {
            logger.scheduleLog(Level.GRAVE, e);
            logger.scheduleLog(Level.INFO, "Error while sending a plugin message from BungeeCord");
        }
    }

    /**
     * Get a message data builder instance
     *
     * @param type    the message type
     * @param owner the message owner
     * @param channel the message channel name
     * @return a new message data builder instance
     */
    public static MessageDataBuilder getBuilder(final DataType type, final String channel, final ProxiedPlayer owner) {
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
        MessageDataBuilder(final DataType data, final ProxiedPlayer owner) {
            if (key.replaceAll("\\s", "").isEmpty()) {
                key = StringUtils.randomString(18, StringUtils.StringGen.NUMBERS_AND_LETTERS, StringUtils.StringType.RANDOM_SIZE);
                logger.scheduleLog(Level.INFO, "Generated proxy communication key");
            }

            Proxy proxy = new Proxy();

            output.writeUTF(data.name().toLowerCase());
            output.writeUTF(proxy.getProxyID().toString());
            output.writeUTF(key);
            if (owner != null) {
                output.writeUTF(owner.getUniqueId().toString());
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
        public final String getChannel() {
            return channel;
        }
    }
}
