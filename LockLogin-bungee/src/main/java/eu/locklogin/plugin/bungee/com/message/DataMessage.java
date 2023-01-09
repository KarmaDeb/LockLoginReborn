package eu.locklogin.plugin.bungee.com.message;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import eu.locklogin.api.common.communication.Packet;
import eu.locklogin.api.common.utils.Channel;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.plugin.bungee.BungeeSender;
import lombok.Builder;

import java.util.Arrays;
import java.util.UUID;

/**
 * Data message builder
 */
@SuppressWarnings("Unused")
@Builder(builderMethodName = "newInstance", buildMethodName = "getInstance")
public final class DataMessage {

    private JsonObject json;

    public static DataMessageBuilder newInstance(final DataType type, final Channel channel) {
        return new DataMessageBuilder(type, channel);
    }

    @SuppressWarnings("UnstableApiUsage")
    public Packet build() {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        Gson gson = new GsonBuilder().setLenient().create();
        String str = gson.toJson(json);
        UUID id = UUID.nameUUIDFromBytes(("Message:" + str).getBytes());

        out.writeUTF(str);

        return new Packet() {
            @Override
            public UUID packetID() {
                return id;
            }

            @Override
            public byte[] packetData() {
                return out.toByteArray();
            }
        };
    }

    public static class DataMessageBuilder {

        private final String[] protect = {"data_type", "channel", "socket"};

        public DataMessageBuilder(final DataType t, final Channel channel) {
            json = new JsonObject();

            json.addProperty("socket", BungeeSender.useSocket);
            json.addProperty("data_type", t.name());
            json.addProperty("channel", channel.name());
        }

        @SuppressWarnings("unused")
        private DataMessageBuilder json(final JsonObject ob) {
            json = ob;
            return this;
        }

        public DataMessageBuilder addProperty(final String name, final UUID value) {
            if (Arrays.stream(protect).noneMatch(name::equalsIgnoreCase)) {
                json.addProperty(name, value.toString());
            }
            return this;
        }

        public DataMessageBuilder addProperty(final String name, final String value) {
            if (Arrays.stream(protect).noneMatch(name::equalsIgnoreCase)) {
                json.addProperty(name, value);
            }
            return this;
        }

        public DataMessageBuilder addProperty(final String name, final boolean value) {
            if (Arrays.stream(protect).noneMatch(name::equalsIgnoreCase)) {
                json.addProperty(name, value);
            }
            return this;
        }

        public DataMessageBuilder addProperty(final String name, final Number value) {
            if (Arrays.stream(protect).noneMatch(name::equalsIgnoreCase)) {
                json.addProperty(name, value);
            }
            return this;
        }

        public DataMessageBuilder addProperty(final String name, final Character character) {
            if (Arrays.stream(protect).noneMatch(name::equalsIgnoreCase)) {
                json.addProperty(name, character);
            }
            return this;
        }

        public DataMessageBuilder add(final String name, final DataMessage sub) {
            if (Arrays.stream(protect).noneMatch(name::equalsIgnoreCase)) {
                json.add(name, sub.json);
            }
            return this;
        }

        public DataMessageBuilder add(final String name, final DataMessageBuilder sub) {
            if (Arrays.stream(protect).noneMatch(name::equalsIgnoreCase)) {
                json.add(name, sub.json);
            }
            return this;
        }
    }
}
