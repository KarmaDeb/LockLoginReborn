package eu.locklogin.api.common.security.backup;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import eu.locklogin.api.account.AccountID;
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.common.utils.other.LockedAccount;
import eu.locklogin.api.common.utils.other.PlayerAccount;
import eu.locklogin.api.security.backup.BackupScheduler;
import eu.locklogin.api.security.backup.BackupStorage;
import eu.locklogin.api.security.backup.data.AccountBackup;
import eu.locklogin.api.security.backup.data.LockAccountBackup;
import eu.locklogin.api.util.enums.ManagerType;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.data.path.PathUtilities;
import ml.karmaconfigs.api.common.karma.source.APISource;
import ml.karmaconfigs.api.common.karma.source.KarmaSource;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.string.random.RandomString;
import ml.karmaconfigs.api.common.string.text.TextContent;
import ml.karmaconfigs.api.common.string.text.TextType;
import ml.karmaconfigs.api.common.timer.scheduler.BiLateScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.LateScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.worker.AsyncBiLateScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.worker.AsyncLateScheduler;
import ml.karmaconfigs.api.common.utils.enums.Level;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Backup task, to keep player accounts safe
 */
public final class BackupTask implements BackupScheduler {


    private final static KarmaSource plugin = APISource.loadProvider("LockLogin");

    /**
     * Perform a backup
     *
     * @return the backup id
     */
    @Override
    public LateScheduler<String> performBackup() {
        LateScheduler<String> result = new AsyncLateScheduler<>();

        plugin.async().queue("create_backup", () -> {
            String id = UUID.randomUUID().toString().replace("-", "");
            String backup_name = new RandomString(RandomString.createBuilder()
                    .withContent(TextContent.ONLY_LETTERS)
                    .withType(TextType.RANDOM_SIZE)
                    .withSize(16)).create();
            Gson gson = new GsonBuilder().create();
            try {
                AccountManager manager = CurrentPlatform.getAccountManager(ManagerType.CUSTOM, null);
                if (manager != null) {
                    try {
                        JsonObject main = new JsonObject();
                        JsonArray array_accounts = new JsonArray();

                        Set<AccountManager> accounts = manager.getAccounts();
                        if (!accounts.isEmpty()) {
                            int real_accounts = 0;

                            for (AccountManager account : accounts) {
                                AccountID uuid = account.getUUID();

                                try {
                                    JsonObject json_account = new JsonObject();

                                    String name = account.getName();
                                    String password = account.getPassword();
                                    String pin = account.getPin();
                                    String gAuth = account.getGAuth();
                                    String panic = account.getPanic();
                                    Instant creation = account.getCreationTime();

                                    boolean hasPassword = account.isRegistered();
                                    boolean hasPin = account.hasPin();
                                    boolean has2fa = account.has2FA();
                                    boolean hasPanic = account.hasPanic();

                                    LockedAccount locked = new LockedAccount(uuid);

                                    JsonObject json_password = new JsonObject();
                                    json_password.addProperty("has", hasPassword);
                                    json_password.addProperty("value", password);

                                    JsonObject json_pin = new JsonObject();
                                    json_pin.addProperty("has", hasPin);
                                    json_pin.addProperty("value", pin);

                                    JsonObject json_2fa = new JsonObject();
                                    json_2fa.addProperty("has", has2fa);
                                    json_2fa.addProperty("value", gAuth);

                                    JsonObject json_panic = new JsonObject();
                                    json_panic.addProperty("has", hasPanic);
                                    json_panic.addProperty("value", panic);

                                    JsonObject json_lock = new JsonObject();
                                    json_lock.addProperty("status", locked.isLocked());
                                    if (locked.isLocked()) {
                                        json_lock.addProperty("issuer", locked.getIssuer());
                                        json_lock.addProperty("locked", locked.getLockDate().toEpochMilli());
                                    } else {
                                        real_accounts++;
                                        json_lock.addProperty("issuer", "");
                                        json_lock.addProperty("locked", 0L);
                                    }

                                    json_account.addProperty("id", uuid.getId());
                                    json_account.addProperty("name", name);
                                    json_account.add("password", json_password);
                                    json_account.add("pin", json_pin);
                                    json_account.add("2fa", json_2fa);
                                    json_account.add("panic", json_panic);
                                    json_account.addProperty("creation", creation.toEpochMilli());
                                    json_account.add("lock", json_lock);

                                    array_accounts.add(json_account);
                                } catch (Throwable ex) {
                                    plugin.logger().scheduleLog(Level.GRAVE, ex);
                                    plugin.logger().scheduleLog(Level.INFO, "Failed to perform backup of {0}", Level.INFO, uuid.getId());

                                    plugin.console().send("An error occurred while performing backup for {0}. See logs for more information", Level.GRAVE, uuid.getId());
                                }
                            }

                            main.addProperty("backup", id);
                            main.addProperty("accounts", real_accounts);
                            main.addProperty("date", Instant.now().toEpochMilli());
                            main.add("data", array_accounts);

                            plugin.async().queue("save_backup", () -> {
                                try {
                                    Path destination = plugin.getDataPath().resolve("data").resolve("backups");
                                    PathUtilities.createDirectory(destination);
                                    Path file = destination.resolve(backup_name + ".json");
                                    Files.createFile(file);

                                    String json = gson.toJson(main);
                                    Files.write(file, json.getBytes(StandardCharsets.UTF_8));
                                    result.complete(id);
                                } catch (Throwable ex) {
                                    result.complete(id, ex);
                                }
                            });
                        } else {
                            result.complete(id, new Exception("Cannot backup without accounts"));
                        }
                    } catch (Throwable ex) {
                        result.complete(id, ex);
                    }
                } else {
                    result.complete(id, new Exception("Cannot backup while using an invalid account manager"));
                }
            } catch (Throwable unexpected) {
                result.complete(id, unexpected);
            }
        });

        return result;
    }

    /**
     * Perform a backup
     *
     * @param id the backup id
     * @return if the backup was able to be created
     */
    @Override
    public LateScheduler<Boolean> performBackup(final String id) {
        LateScheduler<Boolean> result = new AsyncLateScheduler<>();

        plugin.async().queue("create_backup", () -> {
            String backup_name = new RandomString(RandomString.createBuilder()
                    .withContent(TextContent.ONLY_LETTERS)
                    .withType(TextType.RANDOM_SIZE)
                    .withSize(16)).create();
            Gson gson = new GsonBuilder().create();
            try {
                AccountManager manager = CurrentPlatform.getAccountManager(ManagerType.CUSTOM, null);
                if (manager != null) {
                    try {
                        JsonObject main = new JsonObject();
                        JsonArray array_accounts = new JsonArray();

                        Set<AccountManager> accounts = manager.getAccounts();
                        if (!accounts.isEmpty()) {
                            int real_accounts = 0;

                            for (AccountManager account : accounts) {
                                AccountID uuid = account.getUUID();

                                try {
                                    JsonObject json_account = new JsonObject();

                                    String name = account.getName();
                                    String password = account.getPassword();
                                    String pin = account.getPin();
                                    String gAuth = account.getGAuth();
                                    String panic = account.getPanic();
                                    Instant creation = account.getCreationTime();

                                    boolean hasPassword = account.isRegistered();
                                    boolean hasPin = account.hasPin();
                                    boolean has2fa = account.has2FA();
                                    boolean hasPanic = account.hasPanic();

                                    LockedAccount locked = new LockedAccount(uuid);

                                    JsonObject json_password = new JsonObject();
                                    json_password.addProperty("has", hasPassword);
                                    json_password.addProperty("value", password);

                                    JsonObject json_pin = new JsonObject();
                                    json_pin.addProperty("has", hasPin);
                                    json_pin.addProperty("value", pin);

                                    JsonObject json_2fa = new JsonObject();
                                    json_2fa.addProperty("has", has2fa);
                                    json_2fa.addProperty("value", gAuth);

                                    JsonObject json_panic = new JsonObject();
                                    json_panic.addProperty("has", hasPanic);
                                    json_panic.addProperty("value", panic);

                                    JsonObject json_lock = new JsonObject();
                                    json_lock.addProperty("status", locked.isLocked());
                                    if (locked.isLocked()) {
                                        json_lock.addProperty("issuer", locked.getIssuer());
                                        json_lock.addProperty("locked", locked.getLockDate().toEpochMilli());
                                    } else {
                                        real_accounts++;
                                        json_lock.addProperty("issuer", "");
                                        json_lock.addProperty("locked", 0L);
                                    }

                                    json_account.addProperty("id", uuid.getId());
                                    json_account.addProperty("name", name);
                                    json_account.add("password", json_password);
                                    json_account.add("pin", json_pin);
                                    json_account.add("2fa", json_2fa);
                                    json_account.add("panic", json_panic);
                                    json_account.addProperty("creation", creation.toEpochMilli());
                                    json_account.add("lock", json_lock);

                                    array_accounts.add(json_account);
                                } catch (Throwable ex) {
                                    plugin.logger().scheduleLog(Level.GRAVE, ex);
                                    plugin.logger().scheduleLog(Level.INFO, "Failed to perform backup of {0}", Level.INFO, uuid.getId());

                                    plugin.console().send("An error occurred while performing backup for {0}. See logs for more information", Level.GRAVE, uuid.getId());
                                }
                            }

                            main.addProperty("backup", id);
                            main.addProperty("accounts", real_accounts);
                            main.addProperty("date", Instant.now().toEpochMilli());
                            main.add("data", array_accounts);

                            plugin.async().queue("save_backup", () -> {
                                try {
                                    Path destination = plugin.getDataPath().resolve("data").resolve("backups");
                                    if (!Files.exists(destination)) {
                                        PathUtilities.createDirectory(destination);
                                        Path file = destination.resolve(backup_name + ".json");
                                        Files.createFile(file);

                                        String json = gson.toJson(main);
                                        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
                                        result.complete(true);
                                    } else {
                                        result.complete(false);
                                    }
                                } catch (Throwable ex) {
                                    result.complete(false, ex);
                                }
                            });
                        } else {
                            result.complete(false);
                        }
                    } catch (Throwable ex) {
                        result.complete(false, ex);
                    }
                } else {
                    result.complete(false, new Exception("Cannot backup while using an invalid account manager"));
                }
            } catch (Throwable unexpected) {
                result.complete(false, unexpected);
            }
        });

        return result;
    }

    /**
     * Fetch all the backups
     *
     * @return all the backups
     */
    @Override
    public LateScheduler<BackupStorage[]> fetchAll() {
        LateScheduler<BackupStorage[]> task = new AsyncLateScheduler<>();

        plugin.async().queue("fetch_backups", () -> {
            TreeSet<BackupStorage> storage = new TreeSet<>();
            Path directory = plugin.getDataPath().resolve("data").resolve("backups");
            try (Stream<Path> collection = Files.list(directory)) {
                List<Path> stored = collection.filter(Files::isRegularFile).collect(Collectors.toList());
                Gson gson = new GsonBuilder().create();
                for (Path p : stored) {
                    String extension = PathUtilities.getExtension(p);
                    if (extension.equals("json")) {
                        try {
                            JsonObject info = gson.fromJson(Files.newBufferedReader(p, StandardCharsets.UTF_8), JsonObject.class);
                            BackupStorage st = new LocalBackupStorage(p, info);
                            storage.add(st);
                        } catch (Throwable ignored) {}
                    }
                }
            } catch (Throwable ignored) {
            }

            task.complete(storage.stream().sorted().toArray(BackupStorage[]::new));
        });

        return task;
    }

    /**
     * Fetch a backup
     *
     * @param id the backup id
     * @return the backup
     */
    @Override
    public LateScheduler<BackupStorage> fetch(final String id) {
        LateScheduler<BackupStorage> task = new AsyncLateScheduler<>();

        plugin.async().queue("fetch_backups", () -> {
            BackupStorage target = null;
            Path directory = plugin.getDataPath().resolve("data").resolve("backups");
            try (Stream<Path> collection = Files.list(directory)) {
                List<Path> stored = collection.filter(Files::isRegularFile).collect(Collectors.toList());
                Gson gson = new GsonBuilder().create();
                for (Path p : stored) {
                    String extension = PathUtilities.getExtension(p);
                    if (extension.equals("json")) {
                        try {
                            JsonObject info = gson.fromJson(Files.newBufferedReader(p, StandardCharsets.UTF_8), JsonObject.class);
                            if (info.has("backup") && info.get("backup").getAsString().equals(id)) {
                                target = new LocalBackupStorage(p, info);
                                break;
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            } catch (Throwable ignored) {
            }

            task.complete(target);
        });

        return task;
    }

    /**
     * Purge all the backups behind the provided date
     *
     * @param limit the purge limit
     * @return the removed backups
     */
    @Override
    public LateScheduler<Integer> purge(final Instant limit) {
        LateScheduler<Integer> task = new AsyncLateScheduler<>();

        fetchAll().whenComplete((backups) -> {
            Set<BackupStorage> remove = new HashSet<>();
            for (BackupStorage backup : backups) {
                if (backup.creation().isBefore(limit)) {
                    remove.add(backup);
                }
            }

            int removed = 0;
            for (BackupStorage backup : remove) {
                if (backup.destroy()) {
                    removed++;
                }
            }

            task.complete(removed);
        });

        return task;
    }

    /**
     * Destroy all the backups that are between
     * the provided instants
     *
     * @param from the minimum
     * @param to   the maximum
     * @return the removed backups
     */
    @Override
    public LateScheduler<Integer> purgeBetween(final Instant from, final Instant to) {
        LateScheduler<Integer> task = new AsyncLateScheduler<>();

        fetchAll().whenComplete((backups) -> {
            Set<BackupStorage> remove = new HashSet<>();
            for (BackupStorage backup : backups) {
                if (backup.creation().isBefore(to) && backup.creation().isAfter(from)) {
                    remove.add(backup);
                }
            }

            int removed = 0;
            for (BackupStorage backup : remove) {
                if (backup.destroy()) {
                    removed++;
                }
            }

            task.complete(removed);
        });

        return task;
    }

    /**
     * Destroy all the backups above the provided instant
     *
     * @param start the time to start
     * @return the removed backups
     */
    @Override
    public LateScheduler<Integer> purgeFrom(final Instant start) {
        LateScheduler<Integer> task = new AsyncLateScheduler<>();

        fetchAll().whenComplete((backups) -> {
            Set<BackupStorage> remove = new HashSet<>();
            for (BackupStorage backup : backups) {
                if (backup.creation().isAfter(start)) {
                    remove.add(backup);
                }
            }

            int removed = 0;
            for (BackupStorage backup : remove) {
                if (backup.destroy()) {
                    removed++;
                }
            }

            task.complete(removed);
        });

        return task;
    }

    /**
     * Restore a single backup
     *
     * @param storage the backup to restore
     * @param force force the backup. If true (default), all data will be replaced
     * @return if the backup was able to restore and
     * the restored accounts
     */
    @Override
    public BiLateScheduler<Boolean, Integer> restore(final BackupStorage storage, final boolean force) {
        BiLateScheduler<Boolean, Integer> task = new AsyncBiLateScheduler<>();

        plugin.async().queue("restore_backup", () -> {
            try {
                AccountBackup[] accounts = storage.getAccounts();
                int restored = 0;
                for (AccountBackup bk : accounts) {
                    try {
                        PlayerAccount account = new PlayerAccount(bk.id());
                        account.create();

                        account.setRaw("player", bk.name());
                        account.setRaw("uuid", bk.id().getId());
                        if (bk.hasPassword() || force)
                            account.setRaw("password", bk.password());
                        if (!StringUtils.isNullOrEmpty(bk.googleAuthToken()) || force)
                            account.setRaw("token", bk.googleAuthToken());
                        if (bk.hasPin() || force)
                            account.setRaw("pin", bk.pin());
                        account.setRaw("2fa", bk.has2fa());

                        if (bk.hasPanicToken() || force)
                            account.setRaw("panic", bk.panic());

                        LockAccountBackup lbk = bk.locker();
                        LockedAccount locked = new LockedAccount(bk.id());

                        if (lbk != null) {
                            locked.lock(lbk.issuer());
                            locked.setLockDate(lbk.date());
                        } else {
                            if (locked.isLocked())
                                locked.release();
                        }

                        restored++;
                    } catch (Throwable ex) {
                        task.complete(false, restored, ex);
                        break;
                    }
                }

                task.complete(true, restored);
            } catch (Throwable ex) {
                task.complete(false, 0, ex);
            }
        });

        return task;
    }

    /**
     * Restore from a backup to another
     *
     * @param from the backup to start from
     * @param to   the backup to store until
     * @param force force the backup. If true (default), all data will be replaced
     * @return if the backup was able to restore and
     * the restored accounts
     */
    @Override
    public BiLateScheduler<Boolean, Integer> restore(final BackupStorage from, final BackupStorage to, final boolean force) {
        BiLateScheduler<Boolean, Integer> task = new AsyncBiLateScheduler<>();

        Instant from_time = from.creation();
        Instant to_time = to.creation();
        fetchAll().whenComplete((backups) -> {
            Set<BackupStorage> restore = new HashSet<>();
            for (BackupStorage backup : backups) {
                if ((backup.creation().isBefore(to_time) && backup.creation().isAfter(from_time))
                        || backup.id().equals(from.id()) || backup.id().equals(to.id())) {
                    restore.add(backup);
                }
            }

            int restored = 0;
            for (BackupStorage backup : restore) {
                try {
                    AccountBackup[] accounts = backup.getAccounts();
                    for (AccountBackup bk : accounts) {
                        try {
                            PlayerAccount account = new PlayerAccount(bk.id());
                            account.create();

                            account.setRaw("player", bk.name());
                            account.setRaw("uuid", bk.id().getId());
                            if (bk.hasPassword() || force)
                                account.setRaw("password", bk.password());
                            if (!StringUtils.isNullOrEmpty(bk.googleAuthToken()) || force)
                                account.setRaw("token", bk.googleAuthToken());
                            if (bk.hasPin() || force)
                                account.setRaw("pin", bk.pin());
                            account.setRaw("2fa", bk.has2fa());

                            if (bk.hasPanicToken() || force)
                                account.setRaw("panic", bk.panic());

                            LockAccountBackup lbk = bk.locker();
                            LockedAccount locked = new LockedAccount(bk.id());

                            if (lbk != null) {
                                locked.lock(lbk.issuer());
                                locked.setLockDate(lbk.date());
                            } else {
                                if (locked.isLocked())
                                    locked.release();
                            }

                            restored++;
                        } catch (Throwable ex) {
                            ex.printStackTrace();
                            break;
                        }
                    }
                } catch (Throwable ex) {
                    task.complete(false, restored, ex);
                    break;
                }
            }

            task.complete(true, restored);
        });

        return task;
    }

    /**
     * Restore all the backups starting from
     * the specified one
     *
     * @param from the backup to start from
     * @param force force the backup. If true (default), all data will be replaced
     * @return if the backup was able to restore and
     * the restored accounts
     */
    @Override
    public BiLateScheduler<Boolean, Integer> restoreAllFrom(final BackupStorage from, final boolean force) {
        BiLateScheduler<Boolean, Integer> task = new AsyncBiLateScheduler<>();

        Instant from_time = from.creation();
        fetchAll().whenComplete((backups) -> {
            Set<BackupStorage> restore = new HashSet<>();
            for (BackupStorage backup : backups) {
                if (backup.creation().isAfter(from_time) || backup.id().equals(from.id())) {
                    restore.add(backup);
                }
            }

            int restored = 0;
            for (BackupStorage backup : restore) {
                try {
                    AccountBackup[] accounts = backup.getAccounts();
                    for (AccountBackup bk : accounts) {
                        try {
                            PlayerAccount account = new PlayerAccount(bk.id());
                            account.create();

                            account.setRaw("player", bk.name());
                            account.setRaw("uuid", bk.id().getId());
                            if (bk.hasPassword() || force)
                                account.setRaw("password", bk.password());
                            if (!StringUtils.isNullOrEmpty(bk.googleAuthToken()) || force)
                                account.setRaw("token", bk.googleAuthToken());
                            if (bk.hasPin() || force)
                                account.setRaw("pin", bk.pin());
                            account.setRaw("2fa", bk.has2fa());

                            if (bk.hasPanicToken() || force)
                                account.setRaw("panic", bk.panic());

                            LockAccountBackup lbk = bk.locker();
                            LockedAccount locked = new LockedAccount(bk.id());

                            if (lbk != null) {
                                locked.lock(lbk.issuer());
                                locked.setLockDate(lbk.date());
                            } else {
                                if (locked.isLocked())
                                    locked.release();
                            }

                            restored++;
                        } catch (Throwable ex) {
                            ex.printStackTrace();
                            break;
                        }
                    }
                } catch (Throwable ex) {
                    task.complete(false, restored, ex);
                    break;
                }
            }

            task.complete(true, restored);
        });

        return task;
    }

    /**
     * Restore all the backups until the
     * specified one
     *
     * @param to the backup to stop on
     * @param force force the backup. If true (default), all data will be replaced
     * @return if the backup was able to restore and
     * the restored accounts
     */
    @Override
    public BiLateScheduler<Boolean, Integer> restoreAllTo(final BackupStorage to, final boolean force) {
        BiLateScheduler<Boolean, Integer> task = new AsyncBiLateScheduler<>();

        Instant to_time = to.creation();
        fetchAll().whenComplete((backups) -> {
            Set<BackupStorage> restore = new HashSet<>();
            for (BackupStorage backup : backups) {
                if (backup.creation().isBefore(to_time) || backup.id().equals(to.id())) {
                    restore.add(backup);
                }
            }

            int restored = 0;
            for (BackupStorage backup : restore) {
                try {
                    AccountBackup[] accounts = backup.getAccounts();
                    for (AccountBackup bk : accounts) {
                        try {
                            PlayerAccount account = new PlayerAccount(bk.id());
                            account.create();

                            account.setRaw("player", bk.name());
                            account.setRaw("uuid", bk.id().getId());
                            if (bk.hasPassword() || force)
                                account.setRaw("password", bk.password());
                            if (!StringUtils.isNullOrEmpty(bk.googleAuthToken()) || force)
                                account.setRaw("token", bk.googleAuthToken());
                            if (bk.hasPin() || force)
                                account.setRaw("pin", bk.pin());
                            account.setRaw("2fa", bk.has2fa());

                            if (bk.hasPanicToken() || force)
                                account.setRaw("panic", bk.panic());

                            LockAccountBackup lbk = bk.locker();
                            LockedAccount locked = new LockedAccount(bk.id());

                            if (lbk != null) {
                                locked.lock(lbk.issuer());
                                locked.setLockDate(lbk.date());
                            } else {
                                if (locked.isLocked())
                                    locked.release();
                            }

                            restored++;
                        } catch (Throwable ex) {
                            ex.printStackTrace();
                            break;
                        }
                    }
                } catch (Throwable ex) {
                    task.complete(false, restored, ex);
                    break;
                }
            }

            task.complete(true, restored);
        });

        return task;
    }
}
