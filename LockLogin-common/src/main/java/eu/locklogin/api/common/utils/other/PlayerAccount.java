package eu.locklogin.api.common.utils.other;

import eu.locklogin.api.account.AccountID;
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientRanID;
import eu.locklogin.api.account.param.AccountConstructor;
import eu.locklogin.api.account.param.Parameter;
import eu.locklogin.api.encryption.CryptTarget;
import eu.locklogin.api.encryption.CryptoFactory;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.module.plugin.api.event.user.AccountRemovedEvent;
import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.karma.KarmaSource;
import ml.karmaconfigs.api.common.karmafile.KarmaFile;
import ml.karmaconfigs.api.common.karmafile.karmayaml.KarmaYamlManager;
import ml.karmaconfigs.api.common.utils.UUIDUtil;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.file.FileUtilities;
import ml.karmaconfigs.api.common.utils.file.PathUtilities;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

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

    private final KarmaFile manager;

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

                manager = new KarmaFile(randomId.getAccountFile());

                String name = manager.getString("PLAYER", "");
                String uuid = manager.getString("UUID", "");
                String password = manager.getString("PASSWORD", "");
                String token = manager.getString("TOKEN", "");
                String pin = manager.getString("PIN", "");
                String gAuth = manager.getString("2FA", "");
                if (name.startsWith("PLAYER:")) {
                    manager.set("PLAYER", name.replaceFirst("PLAYER:", ""));
                } else {
                    if (name.startsWith("PLAYER: ")) {
                        manager.set("PLAYER", name.replaceFirst("PLAYER: ", ""));
                    }
                }

                if (uuid.startsWith("UUID:")) {
                    manager.set("UUID", uuid.replaceFirst("UUID:", ""));
                } else {
                    if (uuid.startsWith("UUID: ")) {
                        manager.set("UUID", uuid.replaceFirst("UUID: ", ""));
                    }
                }

                if (password.startsWith("PASSWORD:")) {
                    manager.set("PASSWORD", password.replaceFirst("PASSWORD:", ""));
                } else {
                    if (password.startsWith("PASSWORD: ")) {
                        manager.set("PASSWORD", password.replaceFirst("PASSWORD: ", ""));
                    }
                }

                if (token.startsWith("TOKEN:")) {
                    manager.set("TOKEN", token.replaceFirst("TOKEN:", ""));
                } else {
                    if (token.startsWith("TOKEN: ")) {
                        manager.set("TOKEN", token.replaceFirst("TOKEN: ", ""));
                    }
                }

                if (pin.startsWith("PIN:")) {
                    manager.set("PIN", pin.replaceFirst("PIN:", ""));
                } else {
                    if (pin.startsWith("PIN: ")) {
                        manager.set("PIN", pin.replaceFirst("PIN: ", ""));
                    }
                }

                if (gAuth.startsWith("2FA:")) {
                    manager.set("2FA", gAuth.replaceFirst("2FA:", ""));
                } else {
                    if (gAuth.startsWith("2FA: ")) {
                        manager.set("2FA", gAuth.replaceFirst("2FA: ", ""));
                    }
                }
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
            manager.exportFromFile(CurrentPlatform.getMain().getResourceAsStream("/templates/user.lldb"));

            manager.applyKarmaAttribute();
            return true;
        }

        return false;
    }

    @Override
    public boolean remove(final @NotNull String issuer) {
        try {
            Event event = new AccountRemovedEvent(this, issuer, null);
            ModulePlugin.callEvent(event);

            return Files.deleteIfExists(manager.getFile().toPath());
        } catch (Throwable ex) {
            return false;
        }
    }

    @Override
    public void saveUUID(final @NotNull AccountID id) {
        manager.set("UUID", id.getId());
    }

    /**
     * Set the account 2FA status
     *
     * @param status the account 2FA status
     */
    @Override
    public void set2FA(final boolean status) {
        manager.set("2FA", status);
    }

    /**
     * Import the values from the specified account manager
     *
     * @param account the account
     */
    @Override
    protected void importFrom(final @NotNull AccountManager account) {
        if (exists()) {
            manager.set("PLAYER", account.getName());
            manager.set("UUID", account.getUUID().getId());
            manager.set("PASSWORD", account.getPassword());
            manager.set("TOKEN", account.getGAuth());
            manager.set("PIN", account.getPin());
            manager.set("2FA", account.has2FA());
        }
    }

    /**
     * Get the player name
     *
     * @return the player name
     */
    @Override
    public @NotNull String getName() {
        return manager.getString("PLAYER", "").replace("PLAYER:", "");
    }

    /**
     * Save the player name
     *
     * @param name the new player name
     */
    @Override
    public void setName(final @NotNull String name) {
        manager.set("PLAYER", name);
    }

    /**
     * Get the player UUID
     *
     * TODO: Make the method use {@link ml.karmaconfigs.api.common.utils.uuid.UUIDUtil} instead of using the deprecated one
     * TODO: Make the method to not return a 'nullable' object as LockLogin's AccountManager standard requires it
     *
     * @return the player UUID
     */
    @Override
    public @NotNull AccountID getUUID() {
        String id = manager.getString("UUID", UUID.randomUUID().toString());
        if (StringUtils.isNullOrEmpty(id)) {
            String name = manager.getFile().getName();
            String extension = FileUtilities.getExtension(name);
            UUID fixed = UUIDUtil.fromTrimmed(name.replace("." + extension, ""));

            String nick = UUIDUtil.fetchNick(fixed);
            if (nick != null) {
                setName(nick);

                if (CurrentPlatform.isOnline()) {
                    id = UUIDUtil.fetchMinecraftUUID(nick).toString();
                } else {
                    id = UUIDUtil.forceMinecraftOffline(nick).toString();
                }

                manager.set("UUID", id);
            } else {
                if (fixed != null) {
                    manager.set("UUID", fixed.toString());
                    id = fixed.toString();
                }
            }
        }

        return AccountID.fromString(id);
    }

    /**
     * Get the player password
     *
     * @return the player password
     */
    @Override
    public @NotNull String getPassword() {
        return manager.getString("PASSWORD", "").replace("PASSWORD:", "");
    }

    /**
     * Set the player's password
     *
     * @param newPassword the new player password
     */
    @Override
    public void setPassword(final String newPassword) {
        CryptoFactory util = CryptoFactory.getBuilder().withPassword(newPassword).build();
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        manager.set("PASSWORD", util.hash(config.passwordEncryption(), config.encryptBase64()));
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
    public void setUnsafePassword(final @NotNull String newPassword) {
        CryptoFactory util = CryptoFactory.getBuilder().withPassword(newPassword).unsafe();
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        manager.set("PASSWORD", util.hash(config.passwordEncryption(), config.encryptBase64()));
    }

    /**
     * Get player Google Authenticator token
     *
     * @return the player google auth token
     */
    @Override
    public @NotNull String getGAuth() {
        return manager.getString("TOKEN", "").replace("TOKEN:", "");
    }

    /**
     * Set the player Google Authenticator token
     *
     * @param token the token
     */
    @Override
    public void setGAuth(final String token) {
        CryptoFactory util = CryptoFactory.getBuilder().withPassword(token).build();
        manager.set("TOKEN", util.toBase64(CryptTarget.PASSWORD));
    }

    /**
     * Save the account unsafe google auth token
     *
     * @param token the account google auth token
     */
    @Override
    public void setUnsafeGAuth(final @NotNull String token) {
        CryptoFactory util = CryptoFactory.getBuilder().withPassword(token).unsafe();
        manager.set("TOKEN", util.toBase64(CryptTarget.PASSWORD));
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
            return manager.getString("PIN", "").replace("PIN:", "");
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
        CryptoFactory util = CryptoFactory.getBuilder().withPassword(pin).build();
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        manager.set("PIN", util.hash(config.pinEncryption(), config.encryptBase64()));
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
    public void setUnsafePin(@NotNull String pin) {
        CryptoFactory util = CryptoFactory.getBuilder().withPassword(pin).unsafe();
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        manager.set("PIN", util.hash(config.pinEncryption(), config.encryptBase64()));
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
            return manager.getBoolean("2FA", false);
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
            BasicFileAttributes attr = Files.readAttributes(manager.getFile().toPath(), BasicFileAttributes.class);
            return attr.creationTime().toInstant();
        } catch (Throwable ignored) {
        }

        return Instant.now();
    }

    /**
     * Get all the player accounts
     *
     * TODO: Remove null check ( it shouldn't be null anyway since 1.13.16 )
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
                    if (uuid != null) {
                        String name = manager.getName();
                        if (!uuid.getId().replaceAll("\\s", "").isEmpty() && !name.replaceAll("\\s", "").isEmpty())
                            managers.add(manager);
                    }
                });
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }

        return managers;
    }
}
