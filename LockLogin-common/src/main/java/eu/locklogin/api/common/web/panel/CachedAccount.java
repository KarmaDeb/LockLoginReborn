package eu.locklogin.api.common.web.panel;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import eu.locklogin.api.account.AccountID;
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.param.AccountConstructor;
import eu.locklogin.api.account.param.Parameter;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CachedAccount extends AccountManager {

    private final static Map<String, CachedAccount> cache = new ConcurrentHashMap<>();
    private final static Set<String> command_queue = new LinkedHashSet<>();

    private String username = "";
    private AccountID id = AccountID.fromUUID(UUID.randomUUID());
    private String password = "";
    private String token = "";
    private String pin = "";
    private boolean googleAuth = false;
    private String panic = "";
    private String creation = "";

    /**
     * Initialize the account manager
     *
     * @param constructor the account manager
     *                    constructor
     */
    public CachedAccount(@Nullable AccountConstructor<?> constructor) {
        super(constructor);

        if (constructor != null && constructor.getType() != null && constructor.getParameter() != null) {
            Class<?> type = constructor.getType();
            Parameter<?> param = constructor.getParameter();

            if (type.isAssignableFrom(JsonObject.class)) {
                JsonObject object = (JsonObject) param.getValue();

                JsonElement n = object.get("nick");
                JsonElement i = object.get("uuid");
                JsonElement p = object.get("password");
                JsonElement t = object.get("token");
                JsonElement pi = object.get("pin");
                JsonElement g = object.get("2fa");
                JsonElement pa = object.get("panic");
                JsonElement c = object.get("creation");

                if (areAllPrimitive(n, i, p, t, pi, g, pa, c)) {
                    JsonPrimitive nn = n.getAsJsonPrimitive();
                    JsonPrimitive ii = i.getAsJsonPrimitive();
                    JsonPrimitive pp = p.getAsJsonPrimitive();
                    JsonPrimitive tt = t.getAsJsonPrimitive();
                    JsonPrimitive pipi = pi.getAsJsonPrimitive();
                    JsonPrimitive gg = g.getAsJsonPrimitive();
                    JsonPrimitive papa = pa.getAsJsonPrimitive();
                    JsonPrimitive cc = c.getAsJsonPrimitive();

                    if (nn.isString() && ii.isString() && pp.isString() && tt.isString() && pipi.isString() && gg.isBoolean() && papa.isString() && cc.isString()) {
                        username = nn.getAsString();
                        id = AccountID.fromString(ii.getAsString());
                        password = pp.getAsString();
                        token = tt.getAsString();
                        pin = pipi.getAsString();
                        googleAuth = gg.getAsBoolean();
                        panic = papa.getAsString();
                        creation = cc.getAsString();

                        CachedAccount cached = cache.getOrDefault(id.getId(), null);
                        if (cached == null) {
                            cache.put(id.getId(), this);
                        } else {
                            cached.check(username, id, password, token, pin, googleAuth, panic, creation);
                        }
                    } else {
                        throw new RuntimeException("Failed to initialize user because required fields are not the expected type");
                    }
                }
            }
        }
    }

    private static boolean areAllPrimitive(final JsonElement... element) {
        for (JsonElement e : element) {
            if (!e.isJsonPrimitive())
                return false;
        }

        return true;
    }

    /**
     * Check the values
     *
     * @param n the name to check with
     * @param i the uuid to check with
     * @param p the pass to check with
     * @param t the Google token to check with
     * @param pi the pin to check with
     * @param g the Google auth status to check with
     * @param pa the panic token to check with
     * @param c the creation time to check with
     */
    public void check(final String n, final AccountID i, final String p, final String t, final String pi, final boolean g, final String pa, final String c) {
        if (!username.equals(n)) {
            username = n;
        }
        if (!id.getId().equals(i.getId())) {
            id = i;
        }
        if (!password.equals(p)) {
            password = p;
        }
        if (!token.equals(t)) {
            token = t;
        }
        if (!pin.equals(pi)) {
            pin = pi;
        }
        if (googleAuth != g) {
            googleAuth = g;
        }
        if (!panic.equals(pa)) {
            panic = pa;
        }
        if (!creation.equals(c)) {
            creation = c;
        }
    }

    /**
     * Check if the file exists
     *
     * @return if the file exists
     */
    @Override
    public boolean exists() {
        return !StringUtils.areNullOrEmpty(username, id, password, token, pin, password);
    }

    /**
     * Tries to create the account
     *
     * @return if the account could be created
     */
    @Override
    public boolean create() {
        return true;
    }

    /**
     * Tries to remove the account
     *
     * @param issuer the account removal issuer
     * @return if the account could be removed
     */
    @Override
    public boolean remove(final @NotNull String issuer) {
        return false;
    }

    /**
     * Save the account id
     *
     * @param id the account id
     */
    @Override
    public void saveUUID(final @NotNull AccountID id) {
        throw new IllegalStateException("Cannot modify account ID of cached account. While using KarmaPanel account system, server owners don't own his users accounts.");
    }

    /**
     * Save the account 2FA status
     *
     * @param status the account 2FA status
     */
    @Override
    public void set2FA(final boolean status) {
        googleAuth = status;
    }

    /**
     * Import the values from the specified account manager
     *
     * @param account the account
     */
    @Override
    protected void importFrom(final @NotNull AccountManager account) {
        username = account.getName();
        id = account.getUUID();
        password = account.getPassword();
        token = account.getGAuth();
        pin = account.getPin();
        googleAuth = account.has2FA();
        panic = account.getPanic();
        creation = account.getCreationTime().toString();

        //TODO: Send update to KarmaPanel
    }

    /**
     * Get the account id
     *
     * @return the account id
     */
    @Override
    public @NotNull AccountID getUUID() {
        return id;
    }

    /**
     * Get the account name
     *
     * @return the account name
     */
    @Override
    public @NotNull String getName() {
        return username;
    }

    /**
     * Save the account name
     *
     * @param name the account name
     */
    @Override
    public void setName(final @NotNull String name) {
        username = name;

        throw new IllegalStateException("Cannot modify account ID of cached account. While using KarmaPanel account system, server owners don't own his users accounts.");
    }

    /**
     * Save the account password unsafely
     *
     * @param password the account password
     */
    @Override
    public void setUnsafePassword(final @Nullable String password) {
        throw new IllegalStateException("Cannot modify account ID of cached account. While using KarmaPanel account system, server owners don't own his users accounts.");
    }

    /**
     * Get the account password
     *
     * @return the account password
     */
    @Override
    public @NotNull String getPassword() {
        return password;
    }

    /**
     * Save the account password
     *
     * @param password the account password
     */
    @Override
    public void setPassword(final @Nullable String password) {
        throw new IllegalStateException("Cannot modify account ID of cached account. While using KarmaPanel account system, server owners don't own his users accounts.");
    }

    /**
     * Get if the account is registered
     *
     * @return if the account is registered
     */
    @Override
    public boolean isRegistered() {
        return !StringUtils.isNullOrEmpty(password);
    }

    /**
     * Save the account unsafe google auth token
     *
     * @param token the account google auth token
     */
    @Override
    public void setUnsafeGAuth(final @Nullable String token) {
        throw new IllegalStateException("Cannot modify account ID of cached account. While using KarmaPanel account system, server owners don't own his users accounts.");
    }

    /**
     * Get the account google auth token
     *
     * @return the account google auth
     * token
     */
    @Override
    public @NotNull String getGAuth() {
        return token;
    }

    /**
     * Save the account google auth token
     *
     * @param token the account google auth
     *              token
     */
    @Override
    public void setGAuth(final @Nullable String token) {
        throw new IllegalStateException("Cannot modify account ID of cached account. While using KarmaPanel account system, server owners don't own his users accounts.");
    }

    /**
     * Save the account unsafe pin
     *
     * @param pin the account pin
     */
    @Override
    public void setUnsafePin(final @Nullable String pin) {
        throw new IllegalStateException("Cannot modify account ID of cached account. While using KarmaPanel account system, server owners don't own his users accounts.");
    }

    /**
     * Get the account pin
     *
     * @return the account pin
     */
    @Override
    public @NotNull String getPin() {
        return pin;
    }

    /**
     * Save the account pin
     *
     * @param pin the account pin
     */
    @Override
    public void setPin(final @Nullable String pin) {
        throw new IllegalStateException("Cannot modify account ID of cached account. While using KarmaPanel account system, server owners don't own his users accounts.");
    }

    /**
     * Save the account panic unsafe token
     *
     * @param token the account panic token
     */
    @Override
    public void setUnsafePanic(final @Nullable String token) {
        throw new IllegalStateException("Cannot modify account ID of cached account. While using KarmaPanel account system, server owners don't own his users accounts.");
    }

    /**
     * Get the account panic token
     *
     * @return the account panic token
     */
    @Override
    public @NotNull String getPanic() {
        return panic;
    }

    /**
     * Save the account panic token
     *
     * @param token the panic token
     */
    @Override
    public void setPanic(final @Nullable String token) {
        throw new IllegalStateException("Cannot modify account ID of cached account. While using KarmaPanel account system, server owners don't own his users accounts.");
    }

    /**
     * Get if the account has pin
     *
     * @return if the account has pin
     */
    @Override
    public boolean hasPin() {
        return !StringUtils.isNullOrEmpty(pin);
    }

    /**
     * Check if the account has 2FA
     *
     * @return if the account has 2FA
     */
    @Override
    public boolean has2FA() {
        return googleAuth;
    }

    /**
     * Get the account creation time
     *
     * @return the account created time
     */
    @Override
    public @NotNull Instant getCreationTime() {
        return Instant.parse(creation);
    }

    /**
     * Get a list of accounts
     *
     * @return a list of all the
     * available accounts
     */
    @Override
    public @NotNull Set<AccountManager> getAccounts() {
        return new HashSet<>(cache.values());
    }
}
