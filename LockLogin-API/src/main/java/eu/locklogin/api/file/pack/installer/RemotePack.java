package eu.locklogin.api.file.pack.installer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ml.karmaconfigs.api.common.karma.source.APISource;
import ml.karmaconfigs.api.common.timer.scheduler.LateScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.worker.AsyncLateScheduler;
import ml.karmaconfigs.api.common.utils.url.HttpUtil;
import ml.karmaconfigs.api.common.utils.url.URLUtils;

import java.net.URL;

/**
 * Remote pack
 */
@SuppressWarnings("unused")
public final class RemotePack {

    private final String id;

    /**
     * Initialize the remote pack
     *
     * @param i the remote pack ID
     */
    public RemotePack(final String i) {
        id = i;
    }

    /**
     * Fetch the language pack data
     *
     * @return the language pack data
     */
    public LateScheduler<PackData> fetchData() {
        LateScheduler<PackData> result = new AsyncLateScheduler<>();

        APISource.loadProvider("LockLogin").async().queue("load_pack", () -> {
            String[] hosts = new String[]{
                    "https://karmadev.es/",
                    "https://karmaconfigs.ml/",
                    "https://karmarepo.ml/",
                    "https://backup.karmadev.es/",
                    "https://backup.karmaconfigs.ml/",
                    "https://backup.karmarepo.ml/"
            };
            URL url = URLUtils.getOrBackup(hosts);

            if (url != null) {
                URL request_url = URLUtils.append(url, "api/v2/locklogin/lang/" + id);
                HttpUtil extra = URLUtils.extraUtils(request_url);
                if (extra != null) {
                    String response = extra.getResponse();
                    Gson gson = new GsonBuilder().create();

                    JsonElement element = gson.fromJson(response, JsonElement.class);
                    if (!element.isJsonObject()) {
                        result.complete(null);
                        return;
                    }

                    JsonObject object = element.getAsJsonObject();
                    if (object.has("authors") && object.has("name")) {
                        object.has("updated");
                    }
                } else {
                    result.complete(null);
                }
            } else {
                result.complete(null);
            }
        });

        return result;
    }
}
