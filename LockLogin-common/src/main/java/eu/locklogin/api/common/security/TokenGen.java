package eu.locklogin.api.common.security;

import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.karma.KarmaSource;
import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.karma.file.element.KarmaElement;
import ml.karmaconfigs.api.common.karma.file.element.KarmaKeyArray;
import ml.karmaconfigs.api.common.karma.file.element.KarmaObject;
import ml.karmaconfigs.api.common.karmafile.KarmaFile;
import ml.karmaconfigs.api.common.karmafile.Key;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.file.PathUtilities;
import ml.karmaconfigs.api.common.utils.security.token.TokenGenerator;
import ml.karmaconfigs.api.common.utils.security.token.TokenStorage;
import ml.karmaconfigs.api.common.utils.security.token.exception.TokenExpiredException;
import ml.karmaconfigs.api.common.utils.security.token.exception.TokenIncorrectPasswordException;
import ml.karmaconfigs.api.common.utils.security.token.exception.TokenNotFoundException;
import ml.karmaconfigs.api.common.utils.string.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.UUID;

public final class TokenGen {

    private final static KarmaSource lockLogin = APISource.loadProvider("LockLogin");

    static {
        //Update from legacy KarmaFile to modern KarmaMain
        Path existingData = lockLogin.getDataPath().resolve("cache").resolve("keys").resolve("data.lldb");
        if (Files.exists(existingData)) {
            @SuppressWarnings("deprecation")
            KarmaFile legacy = new KarmaFile(existingData);

            String token = legacy.getString("LOCAL_TOKEN", "");
            String local_node = legacy.getString("DEFAULT_NODE", "");

            KarmaMain modern = new KarmaMain(lockLogin, "data.kf", "cache", "keys");

            modern.set("local_token", new KarmaObject(token));
            modern.set("local_node", new KarmaObject(local_node));

            KarmaElement element = modern.get("external_tokens");
            if (element == null || !element.isKeyArray())
                element = new KarmaKeyArray();

            KarmaKeyArray ka = element.getKeyArray();
            for (@SuppressWarnings("deprecation")Key k : legacy.getKeys(false)) {
                if (!k.getPath().equals("DEFAULT_NODE") && !k.getPath().equals("LOCAL_TOKEN")) {
                    ka.add(k.getPath(), new KarmaObject(k.getValue().toString()), false);
                }
            }
            modern.set("external_tokens", ka);

            if (modern.save()) {
                lockLogin.console().send("Updated token data", Level.OK);
                lockLogin.logger().scheduleLog(Level.INFO, "Updated token data to modern KarmaMain file format");

                legacy.delete();
                TokenStorage storage = new TokenStorage(lockLogin);
                storage.migrate(UUID.fromString(token));
            }
        }
    }

    /**
     * Generate the keys for the server
     *
     * @param password the keys password
     */
    public static void generate(final String password) {
        TokenStorage storage = new TokenStorage(lockLogin);

        boolean generate = false;
        try {
            String token = storage.load(find("local_token"), password);
            if (token != null) {
                lockLogin.console().send("Loaded and verified communication token", Level.INFO);
            } else {
                generate = true;
            }
        } catch (TokenNotFoundException ex) {
            lockLogin.console().send("Communication token could not be found, LockLogin will try to generate one", Level.WARNING);
            generate = true;
        } catch (TokenExpiredException ex) {
            lockLogin.console().send("Communication token has expired, LockLogin will destroy it and try to generate a new one", Level.WARNING);
            storage.destroy(find("local_token"), password);
            generate = true;
        } catch (TokenIncorrectPasswordException ex) {
            //Manually removing the token file should fix it without having to remove cache folder
            Path tokenFile = lockLogin.getDataPath().resolve("cache").resolve("tokens").resolve(find("local_token").toString().replace("-", ""));
            PathUtilities.destroy(tokenFile);

            lockLogin.console().send("Communication token has corrupted authentication data. Restart your server to fix it", Level.GRAVE);
        }

        if (generate) {
            String generated = TokenGenerator.generateToken();
            Calendar calendar = GregorianCalendar.getInstance();
            calendar.add(Calendar.MONTH, 6);

            UUID tokenID = storage.store(generated, password, calendar.toInstant());
            lockLogin.console().send("Communication token has been generated with ID {0}", Level.INFO, tokenID.toString());

            KarmaMain idData = new KarmaMain(lockLogin, "data.kf", "cache", "keys");
            idData.set("local_token", new KarmaObject(tokenID.toString()));
            idData.save();
        }
    }

    /**
     * Set the default node name
     *
     * @param name the node name
     */
    public static void assignDefaultNodeName(final String name) {
        KarmaMain idData = new KarmaMain(lockLogin, "data.kf", "cache", "keys");

        idData.set("local_node", new KarmaObject(name));
        idData.save();
    }

    /**
     * Assign a token to the node
     *
     * @param token    the token
     * @param node     the node
     * @param password the node password
     */
    public static void assign(final String token, final String node, final String password, final Instant expiration) {
        KarmaMain idData = new KarmaMain(lockLogin, "data.kf", "cache", "keys");

        KarmaElement external = null;
        if (idData.isSet("external_tokens")) {
            external = idData.get("external_tokens");
        }
        if (external == null || !external.isKeyArray())
            external = new KarmaKeyArray();

        KarmaKeyArray keys = external.getKeyArray();
        TokenStorage storage = new TokenStorage(lockLogin);
        UUID tokenID = storage.store(token, password, expiration);

        keys.add(node, new KarmaObject(tokenID.toString()), false);

        idData.set("external_tokens", keys);
        idData.save();
    }

    /**
     * Request the default node name
     *
     * @return the default node name
     */
    public static String requestNode() {
        KarmaMain idData = new KarmaMain(lockLogin, "data.kf", "cache", "keys");

        KarmaElement local = null;
        if (idData.isSet("local_node")) {
            local = idData.get("local_node");
        }

        if (local == null || !local.isString()) {
            local = new KarmaObject(StringUtils.generateString().create());

            idData.set("local_node", local);
            idData.save();
        }

        return local.getObjet().getString();
    }

    /**
     * Request a token
     *
     * @param node     the token node
     * @param password the token password
     * @return the token
     */
    public static String request(final String node, final String password) {
        TokenStorage storage = new TokenStorage(lockLogin);
        try {
            return storage.load(find(node), password);
        } catch (TokenNotFoundException ex) {
            lockLogin.console().send("Tried to fetch token from {0} but it does not exist, you may try restarting your server/network", Level.GRAVE, node);
        } catch (TokenExpiredException ex) {
            lockLogin.console().send("Tried to fetch token from {0} but it's expired, you may try restarting your server/network", Level.GRAVE, node);
        } catch (TokenIncorrectPasswordException ex) {
            lockLogin.console().send("Tried to fetch token from {0} but the password access token is incorrect", Level.GRAVE, node);
        }

        return null;
    }

    /**
     * Get a token uuid
     *
     * @param node the token name
     * @return the token UUID
     */
    public static UUID find(final String node) {
        KarmaMain idData = new KarmaMain(lockLogin, "data.kf", "cache", "keys");
        if (node.equals("local_token")) {
            if (idData.isSet("local_token")) {
                KarmaElement element = idData.get("local_token");
                if (element.isString())
                    return UUID.fromString(element.getObjet().getString());
            }
        } else {
            KarmaElement external = null;
            if (idData.isSet("external_tokens")) {
                external = idData.get("external_tokens");
            }
            if (external == null || !external.isKeyArray())
                external = new KarmaKeyArray();

            KarmaKeyArray keys = external.getKeyArray();
            if (keys.containsKey(node)) {
                KarmaElement value = keys.get(node);
                return UUID.fromString(value.getObjet().textValue());
            }
        }

        return UUID.fromString(UUID.randomUUID().toString());
    }

    /**
     * Get a token expiration
     *
     * @param node the token node
     * @return the token expiration
     */
    public static Instant expiration(final String node) {
        TokenStorage storage = new TokenStorage(lockLogin);
        return storage.expiration(find(node));
    }

    /**
     * Get if a token matches with the specified node
     *
     * @param token the token
     * @param node  the token node
     * @return if the token matches
     */
    public static boolean matches(final String token, final String node, final String password) {
        String loaded = request(node, password);
        if (loaded != null) {
            return loaded.equals(token);
        }

        return false;
    }
}
