package eu.locklogin.api.common.security.backup;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.locklogin.api.account.AccountID;
import eu.locklogin.api.common.security.backup.data.JsonBackupAccount;
import eu.locklogin.api.common.security.backup.data.JsonLockAccount;
import eu.locklogin.api.security.backup.BackupStorage;
import eu.locklogin.api.security.backup.data.AccountBackup;
import eu.locklogin.api.security.backup.data.LockAccountBackup;
import ml.karmaconfigs.api.common.data.path.PathUtilities;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class LocalBackupStorage implements BackupStorage {

    private final Path container;
    private final JsonObject data;

    /**
     * Initialize the local backup storage
     *
     * @param p the backup container
     * @param info the backup information
     */
    public LocalBackupStorage(final Path p, final JsonObject info) {
        container = p;
        data = info;
    }

    /**
     * Get the backup id
     *
     * @return the backup id
     */
    @Override
    public String id() {
        if (data.has("backup")) {
            return data.get("backup").getAsString();
        }

        return null;
    }

    /**
     * Get all the backup accounts
     *
     * @return the account backups
     */
    @Override
    public AccountBackup[] getAccounts() {
        List<AccountBackup> backups = new ArrayList<>();

        if (data.has("data")) {
            JsonArray array = data.getAsJsonArray("data");
            for (JsonElement element : array) {
                if (element.isJsonObject()) {
                    JsonObject object = element.getAsJsonObject();
                    JsonObject password_object = object.getAsJsonObject("password");
                    JsonObject pin_object = object.getAsJsonObject("pin");
                    JsonObject gauth_object = object.getAsJsonObject("2fa");
                    JsonObject panic_object = object.getAsJsonObject("panic");
                    JsonObject lock_object = object.getAsJsonObject("lock");

                    AccountID id = AccountID.fromString(object.get("id").getAsString());
                    String name = object.get("name").getAsString();
                    String password = password_object.get("value").getAsString();
                    String pin = pin_object.get("value").getAsString();
                    String token = gauth_object.get("value").getAsString();
                    String panic = panic_object.get("value").getAsString();
                    long creation = object.get("creation").getAsLong();
                    String lock_issuer = lock_object.get("issuer").getAsString();
                    long lock_time = lock_object.get("locked").getAsLong();

                    boolean hasPassword = password_object.get("has").getAsBoolean();
                    boolean hasPin = pin_object.get("has").getAsBoolean();
                    boolean hasToken = gauth_object.get("has").getAsBoolean();
                    boolean hasPanic = panic_object.get("has").getAsBoolean();
                    boolean isLock = lock_object.get("status").getAsBoolean();

                    LockAccountBackup locker = null;
                    if (isLock) {
                        locker = new JsonLockAccount(lock_issuer, lock_time);
                    }

                    AccountBackup account = new JsonBackupAccount(id, name, password, pin, token, panic, creation, locker, hasPassword, hasPin, hasToken, hasPanic);
                    backups.add(account);
                }
            }
        }

        return backups.toArray(new AccountBackup[0]);
    }

    /**
     * Get an account from its account ID
     *
     * @param id the id to find with
     * @return the account
     */
    @Override
    public AccountBackup find(final AccountID id) {
        AccountBackup match = null;

        if (data.has("data")) {
            JsonArray array = data.getAsJsonArray("data");
            for (JsonElement element : array) {
                if (element.isJsonObject()) {
                    JsonObject object = element.getAsJsonObject();
                    if (object.get("id").getAsString().equals(id.getId())) {
                        JsonObject password_object = object.getAsJsonObject("password");
                        JsonObject pin_object = object.getAsJsonObject("pin");
                        JsonObject gauth_object = object.getAsJsonObject("2fa");
                        JsonObject panic_object = object.getAsJsonObject("panic");
                        JsonObject lock_object = object.getAsJsonObject("lock");


                        String name = object.get("name").getAsString();
                        String password = password_object.get("value").getAsString();
                        String pin = pin_object.get("value").getAsString();
                        String token = gauth_object.get("value").getAsString();
                        String panic = panic_object.get("value").getAsString();
                        long creation = object.get("creation").getAsLong();
                        String lock_issuer = lock_object.get("issuer").getAsString();
                        long lock_time = lock_object.get("locked").getAsLong();

                        boolean hasPassword = password_object.get("has").getAsBoolean();
                        boolean hasPin = pin_object.get("has").getAsBoolean();
                        boolean hasToken = gauth_object.get("has").getAsBoolean();
                        boolean hasPanic = panic_object.get("has").getAsBoolean();
                        boolean isLock = lock_object.get("status").getAsBoolean();

                        LockAccountBackup locker = null;
                        if (isLock) {
                            locker = new JsonLockAccount(lock_issuer, lock_time);
                        }

                        match = new JsonBackupAccount(id, name, password, pin, token, panic, creation, locker, hasPassword, hasPin, hasToken, hasPanic);
                        break;
                    }
                }
            }
        }

        return match;
    }

    /**
     * Get when the backup was created
     *
     * @return the backup creation time
     */
    @Override
    public Instant creation() {
        if (data.has("date")) {
            long millis = data.get("date").getAsLong();
            return Instant.ofEpochMilli(millis);
        }

        return Instant.now();
    }

    /**
     * Get all the backup accounts
     *
     * @return the backup accounts
     */
    @Override
    public int accounts() {
        if (data.has("accounts")) {
            return data.get("accounts").getAsInt();
        }

        return getAccounts().length;
    }

    /**
     * Destroy the backup
     *
     * @return if the backup could be destroyed
     */
    @Override
    public boolean destroy() {
        return PathUtilities.destroyWithResults(container);
    }

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     *
     * <p>The implementor must ensure <tt>sgn(x.compareTo(y)) ==
     * -sgn(y.compareTo(x))</tt> for all <tt>x</tt> and <tt>y</tt>.  (This
     * implies that <tt>x.compareTo(y)</tt> must throw an exception iff
     * <tt>y.compareTo(x)</tt> throws an exception.)
     *
     * <p>The implementor must also ensure that the relation is transitive:
     * <tt>(x.compareTo(y)&gt;0 &amp;&amp; y.compareTo(z)&gt;0)</tt> implies
     * <tt>x.compareTo(z)&gt;0</tt>.
     *
     * <p>Finally, the implementor must ensure that <tt>x.compareTo(y)==0</tt>
     * implies that <tt>sgn(x.compareTo(z)) == sgn(y.compareTo(z))</tt>, for
     * all <tt>z</tt>.
     *
     * <p>It is strongly recommended, but <i>not</i> strictly required that
     * <tt>(x.compareTo(y)==0) == (x.equals(y))</tt>.  Generally speaking, any
     * class that implements the <tt>Comparable</tt> interface and violates
     * this condition should clearly indicate this fact.  The recommended
     * language is "Note: this class has a natural ordering that is
     * inconsistent with equals."
     *
     * <p>In the foregoing description, the notation
     * <tt>sgn(</tt><i>expression</i><tt>)</tt> designates the mathematical
     * <i>signum</i> function, which is defined to return one of <tt>-1</tt>,
     * <tt>0</tt>, or <tt>1</tt> according to whether the value of
     * <i>expression</i> is negative, zero or positive.
     *
     * @param other the object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     * is less than, equal to, or greater than the specified object.
     * @throws NullPointerException if the specified object is null
     * @throws ClassCastException   if the specified object's type prevents it
     *                              from being compared to this object.
     */
    @Override
    public int compareTo(final @NotNull BackupStorage other) {
        Instant current = Instant.now();
        if (data.has("date")) {
            long millis = data.get("date").getAsLong();
            current = Instant.ofEpochMilli(millis);
        }

        return current.compareTo(other.creation());
    }
}
