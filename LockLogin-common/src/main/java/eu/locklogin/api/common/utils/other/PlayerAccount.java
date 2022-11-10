package eu.locklogin.api.common.utils.other;

import eu.locklogin.api.account.AccountID;
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientRanID;
import eu.locklogin.api.account.param.AccountConstructor;
import eu.locklogin.api.account.param.Parameter;
import eu.locklogin.api.encryption.CryptTarget;
import eu.locklogin.api.encryption.CryptoFactory;
import eu.locklogin.api.encryption.HashType;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.module.plugin.api.event.user.AccountRemovedEvent;
import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.api.util.platform.Platform;
import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.karma.KarmaSource;
import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.karma.file.element.KarmaElement;
import ml.karmaconfigs.api.common.karma.file.element.KarmaObject;
import ml.karmaconfigs.api.common.karmafile.KarmaFile;
import ml.karmaconfigs.api.common.karmafile.karmayaml.KarmaYamlManager;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.file.FileUtilities;
import ml.karmaconfigs.api.common.utils.file.PathUtilities;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
import ml.karmaconfigs.api.common.utils.uuid.UUIDType;
import ml.karmaconfigs.api.common.utils.uuid.UUIDUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * GNU LESSER GENERAL PUBLIC LICENSE
 * Version 2.1, February 1999
 * <p>
 * Copyright (C) 1991, 1999 Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * Everyone is permitted to copy and distribute verbatim copies
 * of this license document, but changing it is not allowed.
 * <p>
 * [This is the first released version of the Lesser GPL.  It also counts
 * as the successor of the GNU Library Public License, version 2, hence
 * the version number 2.1.]
 */
@SuppressWarnings("unused")
public final class PlayerAccount extends AccountManager {

    private final static KarmaSource source = APISource.loadProvider("LockLogin");
    private final static Console console = source.console();

    private final KarmaMain manager;

    static {
        Path backup = source.getDataPath().resolve("data").resolve("accounts_backup");
        Path original = source.getDataPath().resolve("data").resolve("accounts");

        if (Files.exists(original) && !Files.exists(backup)) {
            boolean success = false;
            try {
                AtomicBoolean error = new AtomicBoolean(false);

                Files.list(original).forEachOrdered((sub) -> {
                    Path newPath = backup.resolve(sub.getFileName().toString());

                    try {
                        PathUtilities.create(newPath);
                        Files.copy(sub, newPath, StandardCopyOption.REPLACE_EXISTING);
                    } catch (Throwable fcE) {
                        error.set(true);
                    }
                });

                success = !error.get();
            } catch (Throwable ex) {
                source.logger().scheduleLog(Level.GRAVE, ex);
                source.logger().scheduleLog(Level.INFO, "Failed to copy accounts data to backup");
            }

            ASCIIArtGenerator generator = new ASCIIArtGenerator();
            if (success) {
                generator.print("&c", "ATTENTION", 20, ASCIIArtGenerator.ASCIIArtFont.ART_FONT_SANS_SERIF, "*");

                source.console().send("-------------------------------------------");
                source.console().send("");
                source.console().send("&eDue to the new file system KarmaAPI introduced");
                source.console().send("&eand which LockLogin is now using. A backup of all");
                source.console().send("&ethe player data has been made. If something breaks");
                source.console().send("&edo not say you lost all your player data. Just restore");
                source.console().send("&ethe backup data and report the problem to RedDo discord");
                source.console().send("");
                source.console().send("&dDiscord: &7{0}", "https://discord.gg/jRFfsdxnJR");
                source.console().send("&dBackup: &7{0}", PathUtilities.getPrettyPath(backup));
                source.console().send("");
                source.console().send("-------------------------------------------");
            } else {
                generator.print("&c", "ERROR", 20, ASCIIArtGenerator.ASCIIArtFont.ART_FONT_SANS_SERIF, "*");

                source.console().send("-------------------------------------------");
                source.console().send("");
                source.console().send("&eDue to the new file system KarmaAPI introduced");
                source.console().send("&eand which LockLogin is now using. A backup of all");
                source.console().send("&ethe player data should have been done automatically");
                source.console().send("&ebut the plugin couldn't do it. Do it manually by copying");
                source.console().send("&e{0} &7to&e {1}.", PathUtilities.getPrettyPath(original), PathUtilities.getPrettyPath(backup));
                source.console().send("&eIf you need help, ask for support in our discord");
                source.console().send("");
                source.console().send("&dDiscord: &7{0}", "https://discord.gg/jRFfsdxnJR");
                source.console().send("");
                source.console().send("-------------------------------------------");

                source.console().send("");
                source.console().send("Exiting server as backup couldn't be done", Level.GRAVE);

                //System.exit(1); Exiting server is too much
                /*
                Instead we will set the plugin as disabled. We can do this in BungeeCord and
                Spigot, but I have no idea on how to do it in Velocity so...
                 */
                try {
                    Method unload;
                    if (CurrentPlatform.getPlatform().equals(Platform.BUKKIT)) {
                        Class<?> bukkitManager = Class.forName("eu.locklogin.module.manager.bukkit.manager.BukkitManager");
                        unload = bukkitManager.getDeclaredMethod("unload");
                    } else {
                        Class<?> bungeeManager = Class.forName("eu.locklogin.module.manager.bungee.manager.BungeeManager");
                        unload = bungeeManager.getDeclaredMethod("unload");
                    }

                    unload.invoke(null);
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Initialize the player file
     *
     * @param constructor the player file constructor
     *
     * @throws IllegalArgumentException if the constructor local name is not
     * 'accountid' or 'player' or if the parameter is null
     */
    public PlayerAccount(final @Nullable AccountConstructor<?> constructor) throws IllegalArgumentException {
        super(constructor);

        if (constructor != null) {
            Parameter<?> parameter = constructor.getParameter();
            if (parameter != null) {
                ClientRanID randomId;

                switch (parameter.getLocalName()) {
                    case "accountid":
                        AccountID id = (AccountID) parameter.getValue();
                        randomId = new ClientRanID(id);

                        break;
                    case "player":
                        ModulePlayer player = (ModulePlayer) parameter.getValue();
                        randomId = new ClientRanID(AccountID.fromUUID(player.getUUID()));

                        break;
                    default:
                        throw new IllegalArgumentException("Cannot initialize player file instance for unknown account constructor type: " + parameter.getLocalName());
                }

                manager = new KarmaMain(source, randomId.getAccountFile())
                        .internal(CurrentPlatform.getMain().getResourceAsStream("/templates/user.lldb"));

                try {
                    manager.validate();

                    manager.clearCache();
                    manager.preCache();
                } catch (Throwable ignored) {}
            } else {
                throw new IllegalArgumentException("Cannot initialize player file instance for invalid account constructor parameter");
            }
        } else {
            manager = null;
        }
    }

    /**
     * Migrate from LockLogin v1 player database
     */
    @SuppressWarnings("deprecation")
    public static void migrateV1() {
        File v1DataFolder = new File(source.getDataPath().toFile() + File.separator + "Users");
        File[] files = v1DataFolder.listFiles();

        if (files != null) {
            console.send("Initializing LockLogin v1 player database migration", Level.INFO);

            for (File file : files) {
                if (file.getName().endsWith(".yml")) {
                    console.send("Migrating account #" + file.getName().replace(".yml", ""), Level.INFO);
                    KarmaYamlManager oldManager = new KarmaYamlManager(source, file.getName(), "Users");

                    File newFile = new File(source.getDataPath().toFile() + File.separator + "data" + File.separator + "accounts", file.getName().replace(".yml", ".lldb"));
                    KarmaFile user = new KarmaFile(newFile);

                    String name = oldManager.getString("Player");
                    String password = oldManager.getString("Auth.Password");
                    String token = oldManager.getString("2FA.gAuth");
                    boolean fa = oldManager.getBoolean("2FA.enabled");

                    if (!user.exists()) {
                        user.create();

                        user.set("/// LockLogin user data file. -->");
                        user.set("/// Please do not modify this file -->");
                        user.set("/// until you know what you are doing! -->");

                        user.set("\n");

                        user.set("/// The first recorded player name -->");
                        user.set("PLAYER", name);

                        user.set("\n");

                        //UUID record wasn't a feature in that time...
                        user.set("/// The user UUID, used for offline API -->");
                        user.set("UUID", "");

                        user.set("\n");

                        user.set("/// The user password -->");
                        user.set("PASSWORD", password);

                        user.set("\n");

                        user.set("/// The user google auth token -->");
                        user.set("TOKEN", token);

                        user.set("\n");

                        //Pin didn't exist at that time, so let's just set it empty
                        user.set("/// The user pin -->");
                        user.set("PIN", "");

                        user.set("\n");

                        user.set("/// The user Google Auth status -->");
                    } else {
                        user.set("PLAYER", name);
                        user.set("UUID", "");
                        user.set("PASSWORD", password);
                        user.set("TOKEN", token);
                        user.set("PIN", "");
                    }
                    user.set("2FA", fa);
                }

                try {
                    Files.delete(file.toPath());
                } catch (Throwable ignored) {
                }
            }

            try {
                Files.delete(v1DataFolder.toPath());
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * Migrate from LockLogin v2 player database
     */
    @SuppressWarnings("deprecation")
    public static void migrateV2() {
        File v1DataFolder = new File(source.getDataPath().toFile() + File.separator + "playerdata");
        File[] files = v1DataFolder.listFiles();

        if (files != null) {
            console.send("Initializing LockLogin v2 player database migration", Level.INFO);

            for (File file : files) {
                if (file.getName().endsWith(".yml")) {
                    console.send("Migrating account #" + file.getName().replace(".yml", ""), Level.INFO);
                    KarmaYamlManager oldManager = new KarmaYamlManager(source, file.getName(), "playerdata");

                    File newFile = new File(source.getDataPath().toFile() + File.separator + "data" + File.separator + "accounts", file.getName().replace(".yml", ".lldb"));
                    KarmaFile user = new KarmaFile(newFile);

                    String name = oldManager.getString("Player");
                    String uuid = oldManager.getString("UUID");
                    String password = oldManager.getString("Password");
                    String token = oldManager.getString("GAuth");
                    String pin = oldManager.getString("Pin");
                    boolean fa = oldManager.getBoolean("2FA");

                    if (!user.exists()) {
                        user.create();

                        user.set("/// LockLogin user data file. -->");
                        user.set("/// Please do not modify this file -->");
                        user.set("/// until you know what you are doing! -->");

                        user.set("\n");

                        user.set("/// The first recorded player name -->");
                        user.set("PLAYER", name);

                        user.set("\n");

                        //UUID record wasn't a feature in that time...
                        user.set("/// The user UUID, used for offline API -->");
                        user.set("UUID", uuid);

                        user.set("\n");

                        user.set("/// The user password -->");
                        user.set("PASSWORD", password);

                        user.set("\n");

                        user.set("/// The user google auth token -->");
                        user.set("TOKEN", token);

                        user.set("\n");

                        //Pin didn't exist at that time, so let's just set it empty
                        user.set("/// The user pin -->");
                        user.set("PIN", pin);

                        user.set("\n");

                        user.set("/// The user Google Auth status -->");
                    } else {
                        user.set("PLAYER", name);
                        user.set("UUID", uuid);
                        user.set("PASSWORD", password);
                        user.set("TOKEN", token);
                        user.set("PIN", pin);
                    }
                    user.set("2FA", fa);
                }

                try {
                    Files.delete(file.toPath());
                } catch (Throwable ignored) {
                }
            }

            try {
                Files.delete(v1DataFolder.toPath());
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * Migrate from LockLogin v3 player database
     */
    public static void migrateV3() {
        File v2DataFolder = new File(source.getDataPath().toFile() + File.separator + "playerdata");
        File[] files = v2DataFolder.listFiles();

        if (files != null) {
            console.send("Initializing LockLogin v3 player database migration", Level.INFO);

            for (File file : files) {
                if (file.getName().endsWith(".lldb")) {
                    console.send("Migrating account #" + file.getName().replace(".lldb", ""), Level.INFO);

                    Path accountsFolder = new File(source.getDataPath().toFile() + File.separator + "data", "accounts").toPath();
                    Path newFile = accountsFolder.resolve(file.getName());

                    try {
                        if (!Files.exists(accountsFolder)) {
                            Files.createDirectories(accountsFolder);
                        }
                        if (!Files.exists(newFile)) {
                            Files.createFile(newFile);
                        }

                        Files.move(file.toPath(), newFile, StandardCopyOption.REPLACE_EXISTING);
                    } catch (Throwable ignored) {
                    }
                }

                try {
                    Files.delete(v2DataFolder.toPath());
                } catch (Throwable ignored) {
                }
            }
        }
    }

    @Override
    public boolean exists() {
        return manager.exists();
    }

    @Override
    public boolean create() {
        if (!manager.exists()) {
            try {
                manager.validate();
                return true;
            } catch (Throwable ex) {
                return false;
            }
        }

        return false;
    }

    @Override
    public boolean remove(final @NotNull String issuer) {
        try {
            Event event = new AccountRemovedEvent(this, issuer, null);
            ModulePlugin.callEvent(event);

            return Files.deleteIfExists(manager.getDocument());
        } catch (Throwable ex) {
            return false;
        }
    }

    @Override
    public void saveUUID(final @NotNull AccountID id) {
        manager.set("uuid", new KarmaObject(id.getId()));
        manager.save();
    }

    /**
     * Set the account 2FA status
     *
     * @param status the account 2FA status
     */
    @Override
    public void set2FA(final boolean status) {
        manager.set("2fa", new KarmaObject(status));
        manager.save();
    }

    /**
     * Import the values from the specified account manager
     *
     * @param account the account
     */
    @Override
    protected void importFrom(final @NotNull AccountManager account) {
        if (exists()) {
            manager.set("player", new KarmaObject(account.getName()));
            manager.set("uuid", new KarmaObject(account.getUUID().getId()));
            manager.set("password", new KarmaObject(account.getPassword()));
            manager.set("token", new KarmaObject(account.getGAuth()));
            manager.set("pin", new KarmaObject(account.getPin()));
            manager.set("2fa", new KarmaObject(account.has2FA()));

            manager.save();
        }
    }

    /**
     * Get the player name
     *
     * @return the player name
     */
    @Override
    public @NotNull String getName() {
        if (manager.isSet("player")) {
            KarmaElement element = manager.get("player");
            return element.getObjet().getString();
        }

        return "";
    }

    /**
     * Save the player name
     *
     * @param name the new player name
     */
    @Override
    public void setName(final @NotNull String name) {
        manager.set("player", new KarmaObject(name));
        manager.save();
    }

    /**
     * Get the player UUID
     *
     * @return the player UUID
     */
    @Override
    public @NotNull AccountID getUUID() {
        if (manager.isSet("uuid")) {
            KarmaElement element = manager.get("uuid");
            if (element.isString()) {
                String id = element.getObjet().getString();

                if (StringUtils.isNullOrEmpty(id)) {
                    String name = PathUtilities.getName(manager.getDocument(), false);
                    String extension = FileUtilities.getExtension(name);
                    UUID fixed = UUIDUtil.fromTrimmed(name.replace("." + extension, ""));

                    String nick = UUIDUtil.fetchNick(fixed);
                    if (nick != null && !nick.startsWith("Ratelimited")) {
                        setName(nick);

                        if (CurrentPlatform.isOnline()) {
                            id = UUIDUtil.fetch(nick, UUIDType.ONLINE).toString();
                        } else {
                            id = UUIDUtil.fetch(nick, UUIDType.OFFLINE).toString();
                        }

                        manager.set("uuid", new KarmaObject(id));
                        manager.save();
                    } else {
                        if (fixed != null) {
                            manager.set("uuid", new KarmaObject(fixed.toString()));
                            id = fixed.toString();
                            manager.save();
                        }
                    }
                }

                return AccountID.fromString(id);
            }
        }

        return AccountID.fromUUID(UUID.randomUUID());
    }

    /**
     * Get the player password
     *
     * @return the player password
     */
    @Override
    public @NotNull String getPassword() {
        if (manager.isSet("password")) {
            KarmaElement element = manager.get("password");
            return element.getObjet().getString();
        }

        return "";
    }

    /**
     * Set the player's password
     *
     * @param newPassword the new player password
     */
    @Override
    public void setPassword(final String newPassword) {
        CryptoFactory util = CryptoFactory.getBuilder().withPassword(newPassword).unsafe();
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        manager.set("password", new KarmaObject(util.hash(config.passwordEncryption(), config.encryptBase64())));
        manager.save();
    }

    /**
     * Get if the account is registered
     *
     * @return if the account is registered
     */
    @Override
    public boolean isRegistered() {
        return exists() && !StringUtils.isNullOrEmpty(getPassword());
    }

    /**
     * Save the account password unsafely
     *
     * @param newPassword the account password
     */
    @Override
    public void setUnsafePassword(final @Nullable String newPassword) {
        CryptoFactory util = CryptoFactory.getBuilder().withPassword(newPassword).unsafe();
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        manager.set("password", new KarmaObject(util.hash(config.passwordEncryption(), config.encryptBase64())));
        manager.save();
    }

    /**
     * Get player Google Authenticator token
     *
     * @return the player google auth token
     */
    @Override
    public @NotNull String getGAuth() {
        if (manager.isSet("token")) {
            KarmaElement element = manager.get("token");
            return element.getObjet().getString();
        }

        return "";
    }

    /**
     * Set the player Google Authenticator token
     *
     * @param token the token
     */
    @Override
    public void setGAuth(final String token) {
        CryptoFactory util = CryptoFactory.getBuilder().withPassword(token).unsafe();
        manager.set("token", new KarmaObject(util.toBase64(CryptTarget.PASSWORD)));
        manager.save();
    }

    /**
     * Save the account unsafe google auth token
     *
     * @param token the account google auth token
     */
    @Override
    public void setUnsafeGAuth(final @Nullable String token) {
        CryptoFactory util = CryptoFactory.getBuilder().withPassword(token).unsafe();
        manager.set("token", new KarmaObject(util.toBase64(CryptTarget.PASSWORD)));
        manager.save();
    }

    /**
     * Get the player pin
     *
     * @return the player pin
     */
    @Override
    public @NotNull String getPin() {
        PluginConfiguration configuration = CurrentPlatform.getConfiguration();
        if (configuration.enablePin()) {
            if (manager.isSet("pin")) {
                KarmaElement element = manager.get("pin");
                return element.getObjet().getString();
            }
        }

        return "";
    }

    /**
     * Set the player pin
     *
     * @param pin the pin
     */
    @Override
    public void setPin(final String pin) {
        CryptoFactory util = CryptoFactory.getBuilder().withPassword(pin).unsafe();
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        manager.set("pin", new KarmaObject(util.hash(config.pinEncryption(), config.encryptBase64())));
        manager.save();
    }

    /**
     * Save the account panic unsafe token
     *
     * @param token the account panic token
     */
    @Override
    public void setUnsafePanic(@Nullable String token) {
        CryptoFactory util = CryptoFactory.getBuilder().withPassword(token).unsafe();
        HashType hash = HashType.pickRandom();

        manager.set("panic", new KarmaObject(util.hash(hash, true)));
        manager.save();
    }

    /**
     * Get the account panic token
     *
     * @return the account panic token
     */
    @Override
    public @NotNull String getPanic() {
        PluginConfiguration configuration = CurrentPlatform.getConfiguration();
        if (configuration.enablePin()) {
            if (manager.isSet("panic")) {
                KarmaElement element = manager.get("panic");
                return element.getObjet().getString();
            }
        }

        return "";
    }

    /**
     * Save the account panic token
     *
     * @param token the panic token
     */
    @Override
    public void setPanic(@Nullable String token) {
        CryptoFactory factory = CryptoFactory.getBuilder().withToken(token).withPassword(token).unsafe();
        HashType detected = factory.getTokenHash();
        if (detected.equals(HashType.NONE) || detected.equals(HashType.UNKNOWN)) {
            manager.set("panic", new KarmaObject(factory.hash(HashType.pickRandom(), true)));
            manager.save();
        } else {
            manager.set("panic", new KarmaObject(token));
            manager.save();
        }
    }

    /**
     * Get if the account has pin
     *
     * @return if the account has pin
     */
    @Override
    public boolean hasPin() {
        return exists() && !StringUtils.isNullOrEmpty(getPin());
    }

    /**
     * Save the account unsafe pin
     *
     * @param pin the account pin
     */
    @Override
    public void setUnsafePin(@Nullable String pin) {
        CryptoFactory util = CryptoFactory.getBuilder().withPassword(pin).unsafe();
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        manager.set("pin", new KarmaObject(util.hash(config.pinEncryption(), config.encryptBase64())));
        manager.save();
    }

    /**
     * Check if the player has 2fa
     *
     * @return if the player has 2Fa in his account
     */
    @Override
    public boolean has2FA() {
        PluginConfiguration configuration = CurrentPlatform.getConfiguration();
        if (configuration.enable2FA()) {
            if (manager.isSet("2fa")) {
                KarmaElement element = manager.get("2fa");
                return element.getObjet().getBoolean();
            }
        }

        return false;
    }

    /**
     * Get the account creation time
     *
     * @return the account created time
     */
    @Override
    public @NotNull Instant getCreationTime() {
        try {
            BasicFileAttributes attr = Files.readAttributes(manager.getDocument(), BasicFileAttributes.class);
            return attr.creationTime().toInstant();
        } catch (Throwable ignored) {
        }

        return Instant.now();
    }

    /**
     * Get all the player accounts
     *
     * @return the player accounts
     */
    @Override
    public @NotNull Set<AccountManager> getAccounts() {
        Set<AccountManager> managers = new LinkedHashSet<>();

        Path accounts = source.getDataPath().resolve("data").resolve("accounts");
        if (Files.exists(accounts)) {
            try {
                Files.list(accounts).forEach((sub) -> {
                    String extension = PathUtilities.getExtension(sub);
                    String trimmedId = sub.getFileName().toString().replace("." + extension, "");

                    AccountManager manager = new PlayerAccount(AccountID.fromString(trimmedId));
                    AccountID uuid = manager.getUUID();
                    String name = manager.getName();
                    if (!uuid.getId().replaceAll("\\s", "").isEmpty() && !name.replaceAll("\\s", "").isEmpty())
                        managers.add(manager);
                });
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }

        return managers;
    }
}
