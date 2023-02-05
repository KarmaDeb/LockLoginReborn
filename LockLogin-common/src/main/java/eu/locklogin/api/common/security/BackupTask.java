package eu.locklogin.api.common.security;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.util.enums.ManagerType;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.data.path.PathUtilities;
import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.karma.source.APISource;
import ml.karmaconfigs.api.common.karma.source.KarmaSource;
import ml.karmaconfigs.api.common.string.random.RandomString;
import ml.karmaconfigs.api.common.string.text.TextContent;
import ml.karmaconfigs.api.common.string.text.TextType;
import ml.karmaconfigs.api.common.utils.enums.Level;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Backup task, to keep player accounts safe
 */
public final class BackupTask {

    private final static KarmaSource plugin = APISource.loadProvider("LockLogin");

    public static void performBackup() {
        Gson gson = new GsonBuilder().create();

        AccountManager manager = CurrentPlatform.getAccountManager(ManagerType.CUSTOM, null);
        if (manager != null) {
            plugin.console().send("Initializing periodical backup for player accounts", Level.INFO);

            JsonObject main = new JsonObject();
            Set<AccountManager> accounts = manager.getAccounts();

            main.addProperty("accounts", accounts.size());
            accounts.forEach((account) -> {
                JsonObject data = new JsonObject();
                data.addProperty("name", account.getName());
                data.addProperty("password", account.getPassword());
                data.addProperty("token", account.getGAuth());
                data.addProperty("pin", account.getPin());
                data.addProperty("2fa", account.has2FA());
                data.addProperty("panic", account.getPanic());
                data.addProperty("creation", account.getCreationTime().toEpochMilli());

                main.add(account.getUUID().getId(), data);
            });

            String id = new RandomString(RandomString.createBuilder()
                    .withContent(TextContent.ONLY_LETTERS)
                    .withType(TextType.RANDOM_SIZE)
                    .withSize(16)).create();

            Path backupFile = plugin.getDataPath().resolve("data").resolve("backup").resolve(id).resolve("backup.json");
            String backup = gson.toJson(main);

            try {
                PathUtilities.create(backupFile);

                Files.write(backupFile, backup.getBytes());
                plugin.console().send("Successfully made a backup for {0} accounts", Level.INFO, accounts.size());
            } catch (Throwable ex) {
                plugin.logger().scheduleLog(Level.GRAVE, ex);
                plugin.logger().scheduleLog(Level.INFO, "Failed to perform periodical backup");

                plugin.console().send("Failed to perform periodical account backup", Level.GRAVE);
            }
        }
    }

    /**
     * Restore a backup
     *
     * @param id the backup id
     * @return if the restore could be done
     */
    public static boolean restore(final String id) {
        Path file = plugin.getDataPath().resolve("data").resolve("backup").resolve(id).resolve("backup.json");
        if (PathUtilities.isValidPath(file)) {
            try {
                Gson gson = new GsonBuilder().create();
                JsonObject data = gson.fromJson(Files.newBufferedReader(file), JsonObject.class);

                int accounts = data.get("accounts").getAsInt();
                int restored = 0;
                for (String key : data.keySet()) {
                    if (!key.equals("accounts")) {
                        JsonObject sub_data = data.get(key).getAsJsonObject();

                        String file_name = key.replace("-", "");
                        KarmaMain account_file = new KarmaMain(plugin, file_name + ".lldb", "data", "accounts")
                                .internal(BackupTask.class.getResourceAsStream("/templates/user.lldb"));
                        if (!account_file.exists())
                            account_file.exportDefaults();

                        account_file.setRaw("player", sub_data.get("name").getAsString());
                        account_file.setRaw("uuid", key);
                        account_file.setRaw("password", sub_data.get("password").getAsString());
                        account_file.setRaw("token", sub_data.get("token").getAsString());
                        account_file.setRaw("pin", sub_data.get("pin").getAsString());
                        account_file.setRaw("2fa", sub_data.get("2fa").getAsString());
                        account_file.setRaw("panic", sub_data.get("panic").getAsString());

                        if (account_file.save())
                            restored++;
                    }
                }

                plugin.console().send("Restored {0} of {1} accounts from backup {2}", Level.INFO, restored, accounts, id);
            } catch (Throwable ex) {
                plugin.logger().scheduleLog(Level.GRAVE, ex);
                return false;
            }
        }

        return false;
    }

    /**
     * Get all the backups that have been done
     *
     * @return all the backups that have been done
     */
    public static Map<String, Instant> getBackups() {
        Path directory = plugin.getDataPath().resolve("data").resolve("backup");
        Map<String, Instant> backups = new HashMap<>();
        if (Files.isDirectory(directory)) {
            try (Stream<Path> collection = Files.list(directory)) {
                List<Path> stored = collection.filter(Files::isDirectory).collect(Collectors.toList());
                for (Path p : stored) {
                    String name = p.getFileName().toString();
                    BasicFileAttributes attributes = Files.readAttributes(p, BasicFileAttributes.class);
                    FileTime creation = attributes.creationTime();

                    backups.put(name, creation.toInstant());
                }
            } catch (Throwable ignored) {
            }
        }

        return backups;
    }
}
