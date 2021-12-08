package eu.locklogin.api.common.security;

import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.karma.KarmaSource;
import ml.karmaconfigs.api.common.karmafile.KarmaFile;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.security.token.TokenGenerator;
import ml.karmaconfigs.api.common.utils.security.token.TokenStorage;
import ml.karmaconfigs.api.common.utils.security.token.exception.TokenExpiredException;
import ml.karmaconfigs.api.common.utils.security.token.exception.TokenIncorrectPasswordException;
import ml.karmaconfigs.api.common.utils.security.token.exception.TokenNotFoundException;
import ml.karmaconfigs.api.common.utils.string.StringUtils;

import java.time.Instant;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.UUID;

public final class TokenGen {

    private final static KarmaSource lockLogin = APISource.loadProvider("LockLogin");

    /**
     * Generate the keys for the server
     *
     * @param password the keys password
     */
    public static void generate(final String password) {
        TokenStorage storage = new TokenStorage(lockLogin);

        boolean generate = false;
        try {
            String token = storage.load(find("LOCAL_TOKEN"), password);
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
            storage.destroy(find("LOCAL_TOKEN"), password);
            generate = true;
        } catch (TokenIncorrectPasswordException ex) {
            lockLogin.console().send("Advanced administrator power is needed, please remove the folder plugins/LockLogin/cache/ and restart your server", Level.GRAVE);
        }

        if (generate) {
            String generated = TokenGenerator.generateToken();
            Calendar calendar = GregorianCalendar.getInstance();
            calendar.add(Calendar.MONTH, 6);

            UUID tokenID = storage.store(generated, password, calendar.toInstant());
            lockLogin.console().send("Communication token has been generated with ID {0}", Level.INFO, tokenID.toString());

            KarmaFile idData = new KarmaFile(lockLogin, "data.lldb", "cache", "keys");
            idData.set("LOCAL_TOKEN", tokenID.toString());
        }
    }

    /**
     * Set the default node name
     *
     * @param name the node name
     */
    public static void assignDefaultNodeName(final String name) {
        KarmaFile idData = new KarmaFile(lockLogin, "data.lldb", "cache", "keys");
        idData.set("DEFAULT_NODE", name);
    }

    /**
     * Assign a token to the node
     *
     * @param token    the token
     * @param node     the node
     * @param password the node password
     */
    public static void assign(final String token, final String node, final String password, final Instant expiration) throws Exception {
        KarmaFile idData = new KarmaFile(lockLogin, "data.lldb", "cache", "keys");

        TokenStorage storage = new TokenStorage(lockLogin);
        UUID tokenID = storage.store(token, password, expiration);

        idData.set(node, tokenID);
    }

    /**
     * Request the default node name
     *
     * @return the default node name
     */
    public static String requestNode() {
        KarmaFile idData = new KarmaFile(lockLogin, "data.lldb", "cache", "keys");
        return idData.getString("DEFAULT_NODE", StringUtils.generateString().create());
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
        KarmaFile idData = new KarmaFile(lockLogin, "data.lldb", "cache", "keys");
        return UUID.fromString(idData.getString(node, UUID.randomUUID().toString()));
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
