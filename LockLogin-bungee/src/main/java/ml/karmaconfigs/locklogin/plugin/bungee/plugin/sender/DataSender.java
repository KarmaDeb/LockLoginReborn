package ml.karmaconfigs.locklogin.plugin.bungee.plugin.sender;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.encryption.CryptoUtil;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import static ml.karmaconfigs.locklogin.plugin.bungee.LockLogin.logger;

@SuppressWarnings("UnstableApiUsage")
public final class DataSender {

    public final static String CHANNEL_PLAYER = "ll:account";
    public final static String PLUGIN_CHANNEL = "ll:plugin";
    @SuppressWarnings("FieldMayBeFinal") //This could be modified by the cache loader, so it can't be final
    private static String key = StringUtils.randomString(18, StringUtils.StringGen.NUMBERS_AND_LETTERS, StringUtils.StringType.RANDOM_SIZE);

    /**
     * Send a plugin message on the player server
     *
     * @param player the player
     */
    public static void send(final ProxiedPlayer player, final MessageData data) {
        if (!key.replaceAll("\\s", "").isEmpty()) {
            if (player != null && player.getServer() != null && player.isConnected()) {
                try {
                    player.getServer().getInfo().sendData(data.getChannel(), data.getData().toByteArray());
                } catch (Throwable e) {
                    logger.scheduleLog(Level.GRAVE, e);
                    logger.scheduleLog(Level.INFO, "Error while sending a plugin message from BungeeCord");
                }
            }
        }
    }

    /**
     * Send a plugin message to the server
     *
     * @param server the server
     */
    public static void send(final ServerInfo server, final MessageData data) {
        if (!key.replaceAll("\\s", "").isEmpty()) {
            try {
                server.sendData(data.getChannel(), data.getData().toByteArray());
            } catch (Throwable e) {
                logger.scheduleLog(Level.GRAVE, e);
                logger.scheduleLog(Level.INFO, "Error while sending a plugin message from BungeeCord");
            }
        }
    }

    /**
     * Validate the server token
     *
     * @param token the password token
     * @return if the token is valid
     */
    public static boolean validate(final String token) {
        return CryptoUtil.getBuilder().withPassword(key).withToken(token).build().validate();
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
            String sub;

            switch (data) {
                case JOIN:
                    sub = "join";
                    break;
                case QUIT:
                    sub = "quit";
                    break;
                case VALIDATION:
                    sub = "validate";
                    break;
                case CAPTCHA:
                    sub = "captchalog";
                    break;
                case SESSION:
                    sub = "login";
                    break;
                case PIN:
                    sub = "pin";
                    break;
                case GAUTH:
                    sub = "2fa";
                    break;
                case CLOSE:
                    sub = "unlogin";
                    break;
                case EFFECTS:
                    sub = "effects";
                    break;
                case INVALIDATION:
                    sub = "invalidation";
                    break;
                case MESSAGES:
                    sub = "messages";
                    break;
                case CONFIG:
                    sub = "configuration";
                    break;
                case LOGGED:
                    sub = "logged_amount";
                    break;
                case REGISTERED:
                    sub = "register_amount";
                    break;
                case INFOGUI:
                    sub = "info";
                    break;
                case LOOKUPGUI:
                    sub = "lookup";
                    break;
                default:
                    throw new IllegalArgumentException("Unknown data type: " + data.name());
            }

            output.writeUTF(sub);
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
        public final String getChannel() {
            return channel;
        }
    }
}
