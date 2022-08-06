package eu.locklogin.api.common.web.panel;

import com.google.gson.*;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.karma.KarmaSource;
import ml.karmaconfigs.api.common.timer.scheduler.LateScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.worker.AsyncLateScheduler;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.url.HttpUtil;
import ml.karmaconfigs.api.common.utils.url.URLUtils;
import ml.karmaconfigs.api.common.utils.url.helper.Post;

import java.net.URL;
import java.util.UUID;
import java.util.function.BiConsumer;

public final class KarmaPanelAuth {

    private final static KarmaSource plugin = APISource.loadProvider("LockLogin");

    private final static String first_try_host = "https://karmaconfigs.ml/2k22panel/api/";

    private final static String[] api_hosts = new String[]{
            "https://karmadev.es/2k22panel/api/",
            "https://karmarepo.ml/2k22panel/api/",
            "https://backup.karmaconfigs.ml/2k22panel/api/",
            "https://backup.karmadev.es/2k22panel/api/",
            "https://backup.karmarepo.ml/2k22panel/api/",
    };

    private final String username;
    private final UUID uuid;
    private final String password;

    /**
     * Initialize the karma panel auth
     *
     * @param u the auth username
     * @param i the user UUID
     * @param p the auth password
     */
    public KarmaPanelAuth(final String u, final UUID i, final String p) {
        username = u;
        uuid = i;
        password = p;
    }

    /**
     * Tries to get the online account of a client
     *
     * @return the client online account
     */
    public LateScheduler<CachedAccount> requestAccount() {
        LateScheduler<CachedAccount> response = new AsyncLateScheduler<>();

        URL validHost = URLUtils.getOrBackup(first_try_host, api_hosts);
        if (validHost != null) {
            plugin.async().queue("fetch_user_task", () -> {
                HttpUtil utilities = URLUtils.extraUtils(validHost);

                if (utilities != null) {
                    String webResponse = utilities.getResponse(Post.newPost()
                            .add("method", "locklogin")
                            .add("action", "fetch_profile")
                            .add("server", CurrentPlatform.getServerHash())
                            .add("name", username)
                            .add("content", password));

                    Gson gson = new GsonBuilder().setLenient().create();

                    JsonElement unknownElement = gson.fromJson(webResponse, JsonElement.class);
                    if (unknownElement.isJsonObject()) {
                        JsonObject jsonResponse = unknownElement.getAsJsonObject();

                        if (jsonResponse.has("success") && jsonResponse.has("message")) {
                            JsonElement successCode = jsonResponse.get("success");
                            JsonElement messageInfo = jsonResponse.get("message");

                            if (successCode.isJsonPrimitive() && messageInfo.isJsonPrimitive()) {
                                JsonPrimitive successPrimitive = successCode.getAsJsonPrimitive();
                                JsonPrimitive messagePrimitive = messageInfo.getAsJsonPrimitive();

                                if (successPrimitive.isBoolean() && messagePrimitive.isString()) {
                                    boolean success = successPrimitive.getAsBoolean();
                                    String message = messagePrimitive.getAsString();

                                    if (success) {
                                        /*
                                        I'm still trying to figure out how I will actually implement this in the plugin.

                                        The easiest way would be to renew the entire account system, but that requires lot of time
                                        that I don't have.

                                        What I think I am going to do is to make the cached account execute web API commands
                                        and then taking the response, but that requires time, time that will require me to make
                                        the current account system asynchronous.
                                         */
                                    } else {
                                        response.complete(null, new Error(message));
                                    }
                                }
                            }
                        }
                    }
                }

                if (!response.isCompleted()) response.complete(null, new Error("Unknown error"));
            });
        } else {
            plugin.console().send("Failed to request account of user {0}. Are we connected to the internet?", Level.GRAVE, username);
        }

        return response;
    }
}
