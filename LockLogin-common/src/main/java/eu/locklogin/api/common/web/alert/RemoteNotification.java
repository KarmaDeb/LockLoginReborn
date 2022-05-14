package eu.locklogin.api.common.web.alert;

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

import com.google.gson.*;
import eu.locklogin.api.common.web.alert.remote.RemoteConfig;
import eu.locklogin.api.common.web.alert.remote.RemoteProxy;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.ProxyConfiguration;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.karma.KarmaSource;
import ml.karmaconfigs.api.common.timer.scheduler.LateScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.worker.AsyncLateScheduler;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.file.PathUtilities;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
import ml.karmaconfigs.api.common.utils.url.URLUtils;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * LockLogin notifications system
 */
public final class RemoteNotification {

    private final static KarmaSource plugin = APISource.loadProvider("LockLogin");

    private static String simple_json = "{}";

    /**
     * Check for new alerts
     *
     * @return when the alert has been fetched
     */
    public LateScheduler<Void> checkAlerts() {
        LateScheduler<Void> result = new AsyncLateScheduler<>();

        plugin.async().queue("fetch_notification", () -> {
            URL url = URLUtils.getOrBackup(
                    "https://karmaconfigs.ml/locklogin/alert.json",
                    "https://karmarepo.ml/locklogin/alert.json",
                    "https://karmadev.es/locklogin/alert.json",
                    "https://backup.karmaconfigs.ml/locklogin/alert.json",
                    "https://backup.karmarepo.ml/locklogin/alert.json",
                    "https://backup.karmadev.es/locklogin/alert.json",
                    "https://karmaconfigs.github.io/updates/LockLogin/alert.json");

            if (url != null) {
                ReadableByteChannel rbc = null;
                InputStream stream = null;
                FileOutputStream out = null;
                Path alert = null;

                try {
                    stream = url.openStream();
                    rbc = Channels.newChannel(stream);

                    alert = Files.createTempFile("alert_" + Instant.now().toEpochMilli(), ".json");
                    out = new FileOutputStream(alert.toFile());

                    out.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                } catch (Throwable ex) {
                    plugin.logger().scheduleLog(Level.GRAVE, ex);
                    plugin.logger().scheduleLog(Level.INFO, "Failed to retrieve plugin notifications");
                } finally {
                    try {
                        if (rbc != null)
                            rbc.close();

                        if (stream != null)
                            stream.close();

                        if (out != null)
                            out.close();
                    } catch (Throwable ignored) {}

                    if (alert != null) {
                        /*
                        Read the file, store its info and then remove it.
                         */
                        simple_json = StringUtils.listToString(PathUtilities.readAllLines(alert), false);

                        PathUtilities.destroy(alert);
                    }

                    result.complete(null);
                }
            }
        });

        return result;
    }

    /**
     * Get the alert message
     *
     * @return the alert message
     */
    public String getStartup() {
        Gson gson = new GsonBuilder().setLenient().create();
        JsonObject json = gson.fromJson(simple_json, JsonObject.class);

        List<String> messages = new ArrayList<>();
        if (json.has("startup")) {
            JsonElement element = json.get("startup");
            if (element.isJsonArray()) {
                JsonArray jsonMessage = element.getAsJsonArray();
                for (JsonElement subMessage : jsonMessage) {
                    if (subMessage.isJsonPrimitive()) {
                        JsonPrimitive primitive = subMessage.getAsJsonPrimitive();
                        if (primitive.isString()) {
                            messages.add(primitive.getAsString());
                        }
                    }
                }
            }
        }

        return StringUtils.listToString(messages, false);
    }

    /**
     * Get the notification
     *
     * @return the notification
     */
    public Notification getNotification() {
        Gson gson = new GsonBuilder().setLenient().create();
        JsonObject json = gson.fromJson(simple_json, JsonObject.class);

        Notification notification = new Notification(10, "&cFailed to retrieve notification from LockLogin notifications system. Are you connected to the internet?", false, false);
        if (json.has("notification")) {
            JsonElement nE = json.get("notification");
            if (nE.isJsonObject()) {
                JsonObject nO = nE.getAsJsonObject();

                if (nO.has("level") && nO.has("message") && nO.has("settings") && nO.has("forcedHash")) {
                    JsonElement noL = nO.get("level");
                    JsonElement noM = nO.get("message");
                    JsonElement noS = nO.get("settings");
                    JsonElement noF = nO.get("forcedHash");

                    if (noL.isJsonPrimitive() && noM.isJsonPrimitive() && noS.isJsonObject() && noF.isJsonObject()) {
                        JsonPrimitive noLP = noL.getAsJsonPrimitive();
                        JsonPrimitive noMP = noM.getAsJsonPrimitive();
                        JsonObject noSO = noS.getAsJsonObject();
                        JsonObject noFO = noF.getAsJsonObject();

                        if (noSO.has("config") && noSO.has("proxy")) {
                            JsonElement f_c = noSO.get("config");
                            JsonElement f_p = noSO.get("proxy");

                            if (f_c.isJsonPrimitive() && f_p.isJsonPrimitive()) {
                                JsonPrimitive c = f_c.getAsJsonPrimitive();
                                JsonPrimitive p = f_p.getAsJsonPrimitive();

                                if (c.isBoolean() && p.isBoolean()) {
                                    boolean force_config = c.getAsBoolean();
                                    boolean force_proxy = p.getAsBoolean();

                                    String hash = CurrentPlatform.getServerHash();
                                    //First we will look for exactly the hash. If not, globalize it
                                    if (!noFO.has(hash)) {
                                        if (hash.startsWith("dev_"))
                                            hash = "dev_"; //In development version.
                                        if (hash.startsWith("beta_"))
                                            hash = "beta_";
                                    }

                                    if (noFO.has(hash)) {
                                        JsonElement h_c = noFO.get(hash);

                                        if (h_c.isJsonObject()) {
                                            JsonObject h_o = h_c.getAsJsonObject();

                                            if (h_o.has("settings") && h_o.has("proxy")) {
                                                f_c = h_o.get("config");
                                                f_p = h_o.get("proxy");

                                                if (f_c.isJsonPrimitive() && f_p.isJsonPrimitive()) {
                                                    c = f_c.getAsJsonPrimitive();
                                                    p = f_p.getAsJsonPrimitive();

                                                    if (c.isBoolean() && p.isBoolean()) {
                                                        force_config = c.getAsBoolean();
                                                        force_proxy = p.getAsBoolean();
                                                    }
                                                }
                                            }
                                            if (h_o.has("level") && h_o.has("message")) {
                                                noL = h_o.get("level");
                                                noM = h_o.get("message");

                                                if (noL.isJsonPrimitive() && noM.isJsonPrimitive()) {
                                                    noLP = noL.getAsJsonPrimitive();
                                                    noMP = noM.getAsJsonPrimitive();
                                                }
                                            }
                                        }
                                    }

                                    if (noLP.isNumber() && noMP.isString()) {
                                        Number level = noLP.getAsNumber();
                                        String message = noMP.getAsString();

                                        notification = new Notification(level.intValue(), message, force_config, force_proxy);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return notification;
    }

    /**
     * Get the remote configuration
     *
     * @return the remote configuration
     */
    public PluginConfiguration getRemoteConfig() {
        return new RemoteConfig(simple_json);
    }

    /**
     * Get the remote proxy configuration
     *
     * @return the remote proxy configuration
     */
    public ProxyConfiguration getRemoteProxyConfig() {
        return new RemoteProxy(simple_json);
    }
}

