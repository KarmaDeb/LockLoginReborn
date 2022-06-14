package eu.locklogin.api.common.web.editor;

import com.google.gson.*;
import eu.locklogin.api.common.utils.FileInfo;
import eu.locklogin.api.common.web.editor.cookie.EditorCookies;
import eu.locklogin.api.common.web.editor.task.PendingTask;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.karma.KarmaSource;
import ml.karmaconfigs.api.common.timer.scheduler.BiLateScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.LateScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.worker.AsyncBiLateScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.worker.AsyncLateScheduler;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * This uses POST, I may change this in a future
 * to use sockets
 */
public final class WebRequest {

    private final static KarmaSource locklogin = APISource.loadProvider("LockLogin");
    private final static CookieStore cookies = new EditorCookies();

    private final Set<PendingTask> tasks = new LinkedHashSet<>();

    /**
     * Make ping to the web panel
     *
     * @return if the ping result is pong
     */
    public LateScheduler<Boolean> ping() {
        LateScheduler<Boolean> result = new AsyncLateScheduler<>();

        locklogin.async().queue("editor_ping", () -> {
            JsonObject response = makeRequest(new BasicNameValuePair("command", "ping"));

            if (response != null) {
                if (response.has("response")) {
                    JsonElement pingResponse = response.get("response");
                    if (pingResponse.isJsonPrimitive()) {
                        JsonPrimitive primitive = pingResponse.getAsJsonPrimitive();
                        if (primitive.isString() && primitive.getAsString().equalsIgnoreCase("pong")) {
                            result.complete(true);
                        }
                    }
                }
            }

            result.complete(false);
        });

        return result;
    }

    /**
     * Authenticate the current server. This is used only for
     * debug purposes. It's not actually needed in real time
     * communication ( not for now )
     *
     * @return If the server could be authenticated
     */
    public BiLateScheduler<Boolean, String> authenticate() {
        BiLateScheduler<Boolean, String> result = new AsyncBiLateScheduler<>();

        locklogin.async().queue("editor_auth", () -> {
            JsonObject response = makeRequest(
                    new BasicNameValuePair("server", CurrentPlatform.getServerHash()),
                    new BasicNameValuePair("key", CurrentPlatform.getConfiguration().serverKey())
            );

            if (response != null) {
                if (response.has("status")) {
                    JsonElement status = response.get("status");
                    if (status.isJsonObject()) {
                        JsonObject statusData = status.getAsJsonObject();

                        if (statusData.has("code") && statusData.has("message")) {
                            JsonElement statusCode = statusData.get("code");
                            JsonElement statusMessage = statusData.get("message");

                            if (statusCode.isJsonPrimitive() && statusMessage.isJsonPrimitive()) {
                                JsonPrimitive codePrimitive = statusCode.getAsJsonPrimitive();
                                JsonPrimitive messagePrimitive = statusMessage.getAsJsonPrimitive();

                                if (codePrimitive.isNumber() && messagePrimitive.isString()) {
                                    int code = codePrimitive.getAsNumber().intValue();
                                    String message = messagePrimitive.getAsString();

                                    if (code == 2) {
                                        //Successfully authenticated
                                        result.complete(true, message);
                                    } else {
                                        result.complete(false, message);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            result.complete(false, "Could not communicate LockLogin web editor host");
        });

        return result;
    }

    /**
     * Make a request
     *
     * @param postData the post data
     * @return the response
     */
    private JsonObject makeRequest(final BasicNameValuePair... postData) {
        URL host = FileInfo.getEditorURL(null);

        if (host != null) {
            try {
                HttpClient client = HttpClients.custom()
                        .disableContentCompression()
                        .disableDefaultUserAgent()
                        .setDefaultCookieStore(cookies) //We want us to be able to store authentication data
                        .build();

                HttpPost post = new HttpPost(host.toURI());

                List<NameValuePair> param = Arrays.asList(postData);

                post.setEntity(new UrlEncodedFormEntity(param, StandardCharsets.UTF_8));

                HttpResponse response = client.execute(post);

                Header[] contentType = response.getHeaders("Content-type");
                boolean json = false;
                for (Header header : contentType) {
                    if (header.getValue().equals("application/json")) {
                        json = true;
                    }
                }

                if (json) {
                    HttpEntity entity = response.getEntity();

                    InputStream content = entity.getContent();
                    Scanner scanner = new Scanner(content);

                    StringBuilder builder = new StringBuilder();
                    while (scanner.hasNextLine())
                        builder.append(scanner.nextLine());

                    String data = builder.toString();

                    Gson gson = new GsonBuilder().setLenient().create();
                    JsonElement element = gson.fromJson(data, JsonElement.class);

                    if (element.isJsonObject()) {
                        return element.getAsJsonObject();
                    }
                }
            } catch (Throwable ignored) {}
        }

        return null;
    }
}
