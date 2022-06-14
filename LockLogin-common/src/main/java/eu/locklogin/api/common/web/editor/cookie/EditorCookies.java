package eu.locklogin.api.common.web.editor.cookie;

import com.google.gson.*;
import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.utils.file.PathUtilities;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

public final class EditorCookies implements CookieStore {

    private final static Gson gson = new GsonBuilder().setLenient().setPrettyPrinting().create();
    private final static Path cookies = APISource.getOriginal(false).getDataPath().resolve("cache").resolve("locklogin").resolve("cookies.json");

    static {
        PathUtilities.create(cookies);

        String data = StringUtils.listToString(PathUtilities.readAllLines(cookies), false);
        if (StringUtils.isNullOrEmpty(data)) {
            try {
                Files.write(cookies, "{}".getBytes(), StandardOpenOption.CREATE);
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Adds an {@link Cookie}, replacing any existing equivalent cookies.
     * If the given cookie has already expired it will not be added, but existing
     * values will still be removed.
     *
     * @param cookie the {@link Cookie cookie} to be added
     */
    @Override
    public void addCookie(Cookie cookie) {
        cookie = EditorCookie.fromCookie(cookie); //We want the cookie to be an editor cookie

        JsonObject main = gson.fromJson(StringUtils.listToString(PathUtilities.readAllLines(cookies), false), JsonObject.class);
        String path = cookie.getPath();
        if (StringUtils.isNullOrEmpty(path))
            path = "/";

        JsonArray currentPath = new JsonArray();
        if (main.has(path)) {
            JsonElement p = main.get(path);
            if (p.isJsonArray())
                currentPath = p.getAsJsonArray();
        }

        JsonObject data = new JsonObject();
        int index = -1;
        for (int i = 0; i < currentPath.size(); i++) {
            JsonElement element = currentPath.get(i);
            if (element.isJsonObject()) {
                JsonObject tmpData = element.getAsJsonObject();
                if (tmpData.has("name")) {
                    JsonElement tmpName = tmpData.get("name");
                    if (tmpName.isJsonPrimitive()) {
                        JsonPrimitive tmpPrimitive = tmpName.getAsJsonPrimitive();
                        if (tmpPrimitive.isString()) {
                            String name = tmpPrimitive.getAsString();
                            String cName = cookie.getName();
                            if (StringUtils.isNullOrEmpty(cName))
                                cName = "undefined";

                            if (name.equalsIgnoreCase(cName)) {
                                data = tmpData;
                                index = i;
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (!cookie.isExpired(new Date())) {
            System.out.println("Cookie is not expired!");

            String name = cookie.getName();
            String value = cookie.getValue();
            String domain = cookie.getDomain();
            String info = cookie.getComment();
            String info_url = cookie.getCommentURL();

            data.addProperty("name", name);
            data.addProperty("value", value);
            data.addProperty("domain", domain);
            data.addProperty("info", info);
            data.addProperty("info_url", info_url);
            data.addProperty("version", cookie.getVersion());
            data.addProperty("persistent", cookie.isPersistent());
            data.addProperty("secure", cookie.isSecure());
            data.addProperty("expiration", cookie.getExpiryDate().toInstant().toString());

            JsonArray ports = new JsonArray();
            for (int port : cookie.getPorts())
                ports.add(port);

            data.add("ports", ports);

            if (index == -1) {
                currentPath.add(data);
            } else {
                currentPath.set(index, data);
            }
        } else {
            System.out.println("Cookie is expired!");
            currentPath.remove(index); //If the cookie is expired, we want to remove it
        }

        main.add(path, currentPath);
        try {
            String json = gson.toJson(main);
            Files.write(cookies, json.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Returns all cookies contained in this store.
     *
     * @return all cookies
     */
    @Override
    public List<Cookie> getCookies() {
        List<Cookie> stored = new ArrayList<>();

        JsonObject main = gson.fromJson(StringUtils.listToString(PathUtilities.readAllLines(cookies), false), JsonObject.class);
        JsonArray currentPath = new JsonArray();
        Set<String> keys = main.keySet();
        for (String path : keys) {
            JsonElement p = main.get(path);
            if (p.isJsonArray())
                currentPath = p.getAsJsonArray();

            for (int i = 0; i < currentPath.size(); i++) {
                JsonElement element = currentPath.get(i);
                if (element.isJsonObject()) {
                    JsonObject data = element.getAsJsonObject();

                    if (data.has("name")
                            && data.has("value")
                            && data.has("domain")
                            && data.has("info")
                            && data.has("info_url")
                            && data.has("version")
                            && data.has("persistent")
                            && data.has("secure")
                            && data.has("expiration")
                            && data.has("ports")) {
                        JsonElement nameElement = data.get("name");
                        JsonElement valueElement = data.get("value");
                        JsonElement domainElement = data.get("domain");
                        JsonElement infoElement = data.get("info");
                        JsonElement infoURLElement = data.get("info_url");
                        JsonElement versionElement = data.get("version");
                        JsonElement persistentElement = data.get("persistent");
                        JsonElement secureElement = data.get("secure");
                        JsonElement expirationElement = data.get("expiration");
                        JsonElement portsElement = data.get("ports");

                        if (areString(nameElement, valueElement, domainElement, infoElement, infoURLElement, expirationElement)
                                && areNumber(versionElement)
                                && areBoolean(persistentElement, secureElement)
                                && portsElement.isJsonArray()) {
                            String name = toString(nameElement);
                            String value = toString(valueElement);
                            String domain = toString(domainElement);
                            String info = toString(infoElement);
                            String info_url = toString(infoURLElement);
                            String tmp_expiration = toString(expirationElement);

                            int version = toInt(versionElement);

                            boolean persistent = toBoolean(persistentElement);
                            boolean secure = toBoolean(secureElement);

                            Instant expiration = Instant.now();
                            if (!StringUtils.isNullOrEmpty(tmp_expiration))
                                expiration = Instant.parse(tmp_expiration);

                            int[] ports = toArray(portsElement);

                            EditorCookie cookie = new EditorCookie(
                                    path,
                                    name,
                                    value,
                                    domain,
                                    info,
                                    info_url,
                                    version,
                                    persistent,
                                    secure,
                                    Date.from(expiration),
                                    ports
                            );

                            stored.add(cookie);
                        }
                    }
                }
            }
        }

        return stored;
    }

    /**
     * Removes all of {@link Cookie}s in this store that have expired by
     * the specified {@link Date}.
     *
     * @param date the date limit
     * @return true if any cookies were purged.
     */
    @Override
    public boolean clearExpired(final Date date) {
        List<Cookie> cookies = getCookies();
        int size = cookies.size();
        cookies.forEach((cookie -> {
            if (cookie.isExpired(new Date())) {
                addCookie(cookie);
            }
        }));

        cookies = getCookies();
        return cookies.size() < size;
    }

    /**
     * Clears all cookies.
     */
    @Override
    public void clear() {
        JsonObject main = gson.fromJson(StringUtils.listToString(PathUtilities.readAllLines(cookies), false), JsonObject.class);
        Set<String> keys = main.keySet();
        for (String path : keys)
            main.remove(path);

        try {
            gson.toJson(main, Files.newBufferedWriter(cookies));
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Get the element as string or null
     *
     * @param element the json element
     * @return the json element string value
     */
    private String toString(final JsonElement element) {
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isString())
                return primitive.getAsString();
        }

        return null;
    }

    /**
     * Get the element as int or {@link Integer#MAX_VALUE}
     *
     * @param element the json element
     * @return the json element int value
     */
    private int toInt(final JsonElement element) {
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isNumber())
                return primitive.getAsInt();
        }

        return Integer.MAX_VALUE;
    }

    /**
     * Get the element as boolean
     *
     * @param element the json element
     * @return the json element boolean value
     */
    private boolean toBoolean(final JsonElement element) {
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isBoolean())
                return primitive.getAsBoolean();
        }

        return false;
    }

    /**
     * Get the element as int array
     *
     * @param element the json element
     * @return the json element int array value
     */
    private int[] toArray(final JsonElement element) {
        List<Integer> numbers = new ArrayList<>();

        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            array.forEach((val) -> {
                if (val.isJsonPrimitive()) {
                    JsonPrimitive primitive = val.getAsJsonPrimitive();
                    if (primitive.isNumber())
                        numbers.add(primitive.getAsInt());
                }
            });
        }

        int[] ports = new int[numbers.size()];
        for (int i = 0; i < ports.length; i++)
            ports[i] = numbers.get(i);

        return ports;
    }

    /**
     * Get if the specified elements are strings
     *
     * @param elements the json elements
     * @return if the elements are strings
     */
    private boolean areString(final JsonElement... elements) {
        for (JsonElement element : elements) {
            if (element.isJsonPrimitive()) {
                JsonPrimitive primitive = element.getAsJsonPrimitive();
                if (!primitive.isString()) {
                    return false;
                }
            } else {
                return false;
            }
        }

        return true;
    }

    /**
     * Get if the specified elements are numbers
     *
     * @param elements the json elements
     * @return if the elements are numbers
     */
    private boolean areNumber(final JsonElement... elements) {
        for (JsonElement element : elements) {
            if (element.isJsonPrimitive()) {
                JsonPrimitive primitive = element.getAsJsonPrimitive();
                if (!primitive.isNumber()) {
                    return false;
                }
            } else {
                return false;
            }
        }

        return true;
    }

    /**
     * Get if the specified elements are booleans
     *
     * @param elements the json elements
     * @return if the elements are booleans
     */
    private boolean areBoolean(final JsonElement... elements) {
        for (JsonElement element : elements) {
            if (element.isJsonPrimitive()) {
                JsonPrimitive primitive = element.getAsJsonPrimitive();
                if (!primitive.isBoolean()) {
                    return false;
                }
            } else {
                return false;
            }
        }

        return true;
    }
}
