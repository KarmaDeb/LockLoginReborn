package eu.locklogin.api.common.web.editor.task;

import com.google.gson.*;
import eu.locklogin.api.encryption.server.ServerKey;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.util.Base64;

/**
 * Pending panel tasks
 */
public final class PendingTask {

    private final JsonObject json;

    /**
     * Initialize the pending task
     *
     * @param data the task data
     */
    public PendingTask(final String data) {
        SecretKey key = ServerKey.getKey();
        IvParameterSpec iv = ServerKey.getSpec();
        JsonObject tmp = null;
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key, iv);

            byte[] decoded = cipher.doFinal(Base64.getDecoder().decode(data));
            String realJson = new String(decoded);

            Gson gson = new GsonBuilder().setLenient().create();
            JsonElement element = gson.fromJson(realJson, JsonElement.class);
            if (element.isJsonObject())
                tmp = element.getAsJsonObject();
        } catch (Throwable ignored) {}

        json = tmp;
    }

    /**
     * Get the task
     *
     * @return the task
     */
    public EditorTask getTask() {
        if (json != null) {
            if (json.has("task")) {
                JsonElement taskElem = json.get("task");
                if (taskElem.isJsonPrimitive()) {
                    JsonPrimitive primitive = taskElem.getAsJsonPrimitive();
                    if (primitive.isString()) {
                        String task = primitive.getAsString();

                        switch (task.toLowerCase()) {
                            case "add_user":
                                return EditorTask.ADD_USER;
                            case "del_user":
                                return EditorTask.DEL_USER;
                            case "mod_user":
                                return EditorTask.MOD_USER;
                            case "mod_conf":
                                return EditorTask.MOD_CONF;
                            case "mod_lang":
                                return EditorTask.MOD_LANG;
                            case "exec_cmd":
                                return EditorTask.EXEC_CMD;
                            case "none":
                            default:
                                return EditorTask.NONE;
                        }
                    }
                }
            }
        }

        return EditorTask.NONE;
    }
}
