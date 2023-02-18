package eu.locklogin.api.common.web.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.locklogin.api.common.license.RawLicense;
import eu.locklogin.api.common.web.services.socket.SocketClient;
import eu.locklogin.api.plugin.PluginLicenseProvider;
import eu.locklogin.api.plugin.license.License;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.engineio.client.transports.Polling;
import io.socket.engineio.client.transports.WebSocket;
import ml.karmaconfigs.api.common.data.path.PathUtilities;
import ml.karmaconfigs.api.common.karma.source.APISource;
import ml.karmaconfigs.api.common.karma.source.KarmaSource;
import ml.karmaconfigs.api.common.utils.url.URLUtils;
import okhttp3.OkHttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.routing.RoutingSupport;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static io.socket.client.IO.Options;

public class LockLoginSocket implements SocketClient, PluginLicenseProvider {

    private final URI uri;
    private final URI req_uri;
    private final URI ins_uri;
    private final static int port = 2053;

    private final static String[] required_license_fields = new String[]{
            "base",
            "version",
            "sync",
            "key",
            "owner",
            "expiration",
            "servers",
            "storage",
            "free"
    };

    private static Socket socket = null;

    public LockLoginSocket() {
        String[] tries = new String[]{
                "https://backup.karmadev.es/",
                "https://backup.karmaconfigs.ml/",
                "https://backup.karmarepo.ml/",
                "https://karmadev.es/",
                "https://karmaconfigs.ml/",
                "https://karmarepo.ml/"
        };

        URL working = URLUtils.getOrBackup(tries);
        URI tmpUri = URI.create("https://karmadev.es:" + port + "/");
        URI tmpReqUri = URI.create("https://karmadev.es:2083/validate");
        URI tmpInsUri = URI.create("https://karmadev.es:2083/install");
        if (URLUtils.getOrBackup("https://karmadev.es/") == null) {
            if (working != null) {
                try {
                    tmpUri = URI.create("https://" + working.toURI().getHost() + ":" + port + "/");
                    tmpReqUri = URI.create("https://" + working.toURI().getHost() + ":2083/validate");
                    tmpInsUri = URI.create("https://" + working.toURI().getHost() + ":2083/install");
                } catch (Throwable ignored) {
                }
            }
        }

        uri = tmpUri;
        req_uri = tmpReqUri;
        ins_uri = tmpInsUri;
        /*uri = URI.create("http://localhost:2053");
        req_uri = URI.create("http://localhost:2083/validate");
        ins_uri = URI.create("http://localhost:2083/install");*/
    }

    /**
     * Get the statistic server
     *
     * @return the statistic server
     */
    @Override
    public URI server() {
        return uri;
    }

    /**
     * Get the socket client
     *
     * @return the socket client
     */
    @Override
    public Socket client() {
        if (socket == null) {
            Options options = new Options();
            options.secure = false;
            options.multiplex = false;
            options.forceNew = false;
            options.transports = new String[]{Polling.NAME, WebSocket.NAME};
            options.upgrade = true;
            options.rememberUpgrade = true;
            options.reconnection = true;
            options.reconnectionAttempts = 5;
            options.reconnectionDelay = 1;
            options.reconnectionDelayMax = 5;
            options.randomizationFactor = 0.5;
            options.auth = new ConcurrentHashMap<>();

            OkHttpClient client = new OkHttpClient.Builder()
                    .readTimeout(1, TimeUnit.MINUTES)
                    .build();

            options.callFactory = client;
            options.webSocketFactory = client;

            socket = IO.socket(uri, options);
        }

        return socket;
    }

    /**
     * Check the license
     *
     * @param license the license file
     * @return the license if the signature is valid
     */
    @Override
    public License fetch(final File license) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            FileBody fileBody = new FileBody(license);
            try (HttpEntity entity = MultipartEntityBuilder.create()
                    .addPart("file", fileBody)
                    .build()) {

                HttpPost request = new HttpPost(req_uri);
                request.setEntity(entity);

                HttpHost host = RoutingSupport.determineHost(request);
                try (ClassicHttpResponse response = client.executeOpen(host, request, null)) {
                    try (HttpEntity ent = response.getEntity()) {
                        Header[] contentType = response.getHeaders("Content-type");
                        boolean json = false;
                        for (Header header : contentType) {
                            if (header.getValue().contains("application/json")) {
                                json = true;
                                break;
                            }
                        }

                        String raw_response = EntityUtils.toString(ent);
                        if (json) {
                            Gson gson = new GsonBuilder().create();
                            JsonElement element = gson.fromJson(raw_response, JsonElement.class);

                            if (element.isJsonObject()) {
                                JsonObject information = element.getAsJsonObject();
                                if (hasAll(information, required_license_fields)) {
                                    JsonObject owner = information.getAsJsonObject("owner");
                                    JsonObject expiration_data = information.getAsJsonObject("expiration");

                                    String bValue = information.get("base").getAsString();
                                    String version = information.get("version").getAsString();
                                    String sync = information.get("sync").getAsString();
                                    String key = information.get("key").getAsString();
                                    String name = owner.get("name").getAsString();
                                    String contact = owner.get("contact").getAsString();
                                    long stamp_granted = expiration_data.get("granted").getAsLong();
                                    long stamp_expires = expiration_data.get("expires").getAsLong();
                                    int servers = information.get("servers").getAsInt();
                                    long capacity = information.get("storage").getAsLong();
                                    boolean free = information.get("free").getAsBoolean();

                                    return RawLicense.builder()
                                            .base64(bValue)
                                            .version(version)
                                            .sync(sync)
                                            .com(key)
                                            .name(name)
                                            .contact(contact)
                                            .created(stamp_granted)
                                            .expiration(stamp_expires)
                                            .proxies(servers)
                                            .storage(capacity)
                                            .free(free)
                                            .build();
                                }
                            }
                        }
                    }
                }
            } catch (HttpException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Synchronize an existing license
     *
     * @param key the license key
     * @return the license
     */
    @Override
    public License sync(final String key) {
        KarmaSource source = APISource.loadProvider("LockLogin");
        Path license_location = source.getDataPath().resolve("cache");
        Path license_path = license_location.resolve("license.dat");
        if (Files.exists(license_path)) {
            License license = fetch(license_path);
            if (license != null) {
                license.setInstallLocation(license_location);
                return license;
            }
        }

        try(CloseableHttpClient client = HttpClients.createDefault()) {
            ClassicHttpRequest post = new HttpPost(ins_uri);
            JsonObject msg = new JsonObject();
            msg.addProperty("sync", key);

            Gson gson = new GsonBuilder().create();
            try (StringEntity post_data = new StringEntity(gson.toJson(msg))) {
                post.setEntity(post_data);
                post.addHeader("Content-Type", "application/json");

                HttpHost host = RoutingSupport.determineHost(post);
                try (ClassicHttpResponse response = client.executeOpen(host, post, null)) {
                    String license_base = EntityUtils.toString(response.getEntity());

                    if (!license_base.equalsIgnoreCase("error")) {
                        byte[] data = Base64.getDecoder().decode(license_base);

                        Path license_file = Files.createTempFile("locklogin_", "_license");
                        PathUtilities.create(license_file);
                        Files.write(license_file, data);

                        License license = fetch(license_file); //If it returns null, it basically means that the license failed to register
                        if (license != null) {
                            license.setInstallLocation(license_location);
                            return license;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Synchronize an existing license
     *
     * @param key      the license key
     * @param username the license username
     * @param password the license password
     * @return the license
     */
    @Override
    public License sync(String key, String username, String password) {
        KarmaSource source = APISource.loadProvider("LockLogin");
        Path license_location = source.getDataPath().resolve("cache");
        Path license_path = license_location.resolve("license.dat");
        if (Files.exists(license_path)) {
            License license = fetch(license_path);
            if (license != null) {
                license.setInstallLocation(license_location);
                return license;
            }
        }

        try(CloseableHttpClient client = HttpClients.createDefault()) {
            ClassicHttpRequest post = new HttpPost(ins_uri);
            JsonObject msg = new JsonObject();
            msg.addProperty("sync", key);
            msg.addProperty("username", username);
            msg.addProperty("password", password);

            Gson gson = new GsonBuilder().create();
            try (StringEntity post_data = new StringEntity(gson.toJson(msg))) {
                post.setEntity(post_data);
                post.addHeader("Content-Type", "application/json");

                HttpHost host = RoutingSupport.determineHost(post);
                try (ClassicHttpResponse response = client.executeOpen(host, post, null)) {
                    String license_base = EntityUtils.toString(response.getEntity());

                    if (!license_base.equalsIgnoreCase("error")) {
                        byte[] data = Base64.getDecoder().decode(license_base);

                        Path license_file = Files.createTempFile("locklogin_", "_license");
                        PathUtilities.create(license_file);
                        Files.write(license_file, data);

                        License license = fetch(license_file); //If it returns null, it basically means that the license failed to register
                        if (license != null) {
                            license.setInstallLocation(license_location);
                            return license;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Request a new free license
     *
     * @return the license
     */
    @Override
    public License request() {
        KarmaSource source = APISource.loadProvider("LockLogin");
        Path license_location = source.getDataPath().resolve("cache");
        Path license_path = license_location.resolve("license.dat");
        if (Files.exists(license_path)) {
            License license = fetch(license_path);
            if (license != null) {
                license.setInstallLocation(license_location);
                return license;
            }
        }

        try(CloseableHttpClient client = HttpClients.createDefault()) {
            ClassicHttpRequest post = new HttpPost(ins_uri);
            HttpHost host = RoutingSupport.determineHost(post);
            try (ClassicHttpResponse response = client.executeOpen(host, post, null)) {
                String license_base = EntityUtils.toString(response.getEntity());

                if (!license_base.equalsIgnoreCase("error")) {
                    byte[] data = Base64.getDecoder().decode(license_base);

                    Path license_file = Files.createTempFile("locklogin_", "_license");
                    PathUtilities.create(license_file);
                    Files.write(license_file, data);

                    License license = fetch(license_file); //If it returns null, it basically means that the license failed to register
                    if (license != null) {
                        license.setInstallLocation(license_location);
                        return license;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Request a new private license
     *
     * @param id       the license id
     * @param username the private license name
     * @param password the private license password
     * @return the new private license
     */
    @Override
    public License request(final UUID id, final String username, final String password) {
        KarmaSource source = APISource.loadProvider("LockLogin");
        Path license_location = source.getDataPath().resolve("cache");
        Path license_path = license_location.resolve("license.dat");
        if (Files.exists(license_path)) {
            License license = fetch(license_path);
            if (license != null) {
                license.setInstallLocation(license_location);
                return license;
            }
        }

        try(CloseableHttpClient client = HttpClients.createDefault()) {
            ClassicHttpRequest post = new HttpPost(ins_uri);
            JsonObject msg = new JsonObject();
            msg.addProperty("license", id.toString());
            msg.addProperty("username", username);
            msg.addProperty("password", password);

            Gson gson = new GsonBuilder().create();
            try (StringEntity post_data = new StringEntity(gson.toJson(msg))) {
                post.setEntity(post_data);
                post.addHeader("Content-Type", "application/json");

                HttpHost host = RoutingSupport.determineHost(post);
                try (ClassicHttpResponse response = client.executeOpen(host, post, null)) {
                    String license_base = EntityUtils.toString(response.getEntity());

                    if (!license_base.equalsIgnoreCase("error")) {
                        byte[] data = Base64.getDecoder().decode(license_base);

                        Path license_file = Files.createTempFile("locklogin_", "_license");
                        PathUtilities.create(license_file);
                        Files.write(license_file, data);

                        License license = fetch(license_file); //If it returns null, it basically means that the license failed to register
                        if (license != null) {
                            license.setInstallLocation(license_location);
                            return license;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Get if the json object has all the keys
     *
     * @param object the object
     * @param names the keys
     * @return if the object has all the keys
     */
    boolean hasAll(final JsonObject object, final String... names) {
        for (String key : names) {
            if (!object.has(key))
                return false;
        }

        return true;
    }
}
