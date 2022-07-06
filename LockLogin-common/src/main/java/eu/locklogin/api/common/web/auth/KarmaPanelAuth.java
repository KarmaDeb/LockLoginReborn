package eu.locklogin.api.common.web.auth;

import com.google.gson.*;
import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.karma.KarmaSource;
import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.karma.file.element.KarmaElement;
import ml.karmaconfigs.api.common.timer.scheduler.LateScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.worker.AsyncLateScheduler;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.url.HttpUtil;
import ml.karmaconfigs.api.common.utils.url.Post;
import ml.karmaconfigs.api.common.utils.url.URLUtils;

import java.net.URL;

public final class KarmaPanelAuth {

    private final static KarmaSource plugin = APISource.loadProvider("LockLogin");
    private final static KarmaMain mail_assoc = new KarmaMain(plugin, ".mails", "data");

    private final String[] auth_hosts = new String[]{
            "https://karmaconfings.ml/api/?method=session&auth=login",
            "https://karmadev.es/api/?method=session&auth=login",
            "https://karmarepo.ml/api/?method=session&auth=login",
            "https://backup.karmaconfigs.ml/api/?method=session&auth=login",
            "https://backup.karmadev.es/api/?method=session&auth=login",
            "https://backup.karmarepo.ml/api/?method=session&auth=login",
    };

    private final String username;
    private final String password;
    private final String address;

    /**
     * Initialize the karma panel auth
     *
     * @param u the auth username
     * @param p the auth password
     */
    public KarmaPanelAuth(final String u, final String p) {
        username = u;
        password = p;

        String tmpAddress = "";
        if (mail_assoc.isSet(username)) {
            KarmaElement element = mail_assoc.get(username);
            if (element.isString()) {
                tmpAddress = element.getObjet().getString();
            }
        }

        address = tmpAddress;
    }

    /**
     * Tries to authenticate the user
     *
     * @return the user auth response
     */
    public LateScheduler<AuthResponse> authenticate() {
        LateScheduler<AuthResponse> response = new AsyncLateScheduler<>();

        for (String str : auth_hosts) {
            int code = URLUtils.getResponseCode(str);
            if (code == 200) {
                URL url = URLUtils.getOrNull(str);
                if (url != null) {
                    HttpUtil util = URLUtils.extraUtils(url);
                    if (util != null) {
                        plugin.async().queue("user_auth", () -> {
                            try {
                                String authResponse = util.getResponse(
                                        Post.newPost()
                                                .add("email", address)
                                                .add("username", username)
                                                .add("password", password)
                                                .add("stay", true)
                                                .add("noSession", true)
                                );

                                Gson gson = new GsonBuilder().setLenient().setPrettyPrinting().create();
                                JsonObject json = gson.fromJson(authResponse, JsonObject.class);

                                if (json.has("success") && json.has("message")) {
                                    JsonElement successElement = json.get("success");
                                    JsonElement messageElement = json.get("message");

                                    if (successElement.isJsonPrimitive() && messageElement.isJsonPrimitive()) {
                                        JsonPrimitive successPrimitive = successElement.getAsJsonPrimitive();
                                        JsonPrimitive messagePrimitive = messageElement.getAsJsonPrimitive();

                                        if (successPrimitive.isBoolean() && messagePrimitive.isString()) {
                                            String message = messagePrimitive.getAsString();

                                            AuthResponse r;
                                            if (successPrimitive.getAsBoolean()) {
                                                r = AuthResponse.SUCCESS.message(message);
                                            } else {
                                                r = AuthResponse.FAILED.message(message);
                                            }

                                            response.complete(r, null);
                                        }
                                    }
                                }
                            } catch (Throwable ex) {
                                plugin.logger().scheduleLog(Level.GRAVE, ex);
                                plugin.logger().scheduleLog(Level.INFO, "Failed to authenticate user {0} using LockLogin online authentication services", username);

                                response.complete(AuthResponse.NETWORK_ERROR, ex);
                            }
                        });

                        break;
                    }
                }
            }
        }

        return response;
    }
}
