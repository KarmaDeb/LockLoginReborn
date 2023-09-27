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
import eu.locklogin.api.module.PluginModule;
import eu.locklogin.api.module.plugin.api.event.user.AccountRemovedEvent;
import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import eu.locklogin.api.security.LockLoginRuntime;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.console.Console;
import ml.karmaconfigs.api.common.data.file.FileUtilities;
import ml.karmaconfigs.api.common.data.path.PathUtilities;
import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.karma.file.element.types.Element;
import ml.karmaconfigs.api.common.karma.file.element.types.ElementPrimitive;
import ml.karmaconfigs.api.common.karma.source.APISource;
import ml.karmaconfigs.api.common.karma.source.KarmaSource;
import ml.karmaconfigs.api.common.minecraft.UUIDFetcher;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.uuid.UUIDType;
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
import java.util.stream.Stream;

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

                KarmaMain tmpManager = null;
                try {
                    tmpManager = new KarmaMain(randomId.getAccountFile())
                            .internal(CurrentPlatform.getMain().getResourceAsStream("/templates/user.lldb"));

                    try {
                        tmpManager.validate();

                        tmpManager.clearCache();
                        tmpManager.preCache();
                    } catch (Throwable ignored) {}
                } catch (Throwable ex) {
                    console.send("Failed to read account of {0} ({1})", Level.GRAVE, PathUtilities.getPrettyPath(randomId.getAccountFile()), ex.fillInStackTrace());
                }

                manager = tmpManager;
            } else {
                throw new IllegalArgumentException("Cannot initialize player file instance for invalid account constructor parameter");
            }
        } else {
            manager = null;
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
        manager.preCache();

        return false;
    }

    @Override
    public boolean remove(final @NotNull String issuer) throws SecurityException {
        LockLoginRuntime.checkSecurity(false);

        try {
            Event event = new AccountRemovedEvent(this, issuer, null);
            ModulePlugin.callEvent(event);
            manager.clearCache();

            PluginModule module = LockLoginRuntime.getMethodCaller();
            if (module == null) {
                return Files.deleteIfExists(manager.getDocument());
            }
        } catch (Throwable ex) {
            return false;
        }

        return false;
    }

    /**
     * Set a raw value
     *
     * @param key the key
     * @param value the value
     */
    public void setRaw(final String key, final String value) throws SecurityException {
        LockLoginRuntime.checkSecurity(true);

        manager.setRaw(key, value);
        manager.save();
    }

    /**
     * Set a raw value
     *
     * @param key the key
     * @param value the value
     */
    public void setRaw(final String key, final boolean value) throws SecurityException {
        LockLoginRuntime.checkSecurity(true);

        manager.setRaw(key, value);
        manager.save();
    }

    @Override
    public void saveUUID(final @NotNull AccountID id) throws SecurityException {
        LockLoginRuntime.checkSecurity(false);

        PluginModule module = LockLoginRuntime.getMethodCaller();
        if (module != null) {
            source.logger().scheduleLog(Level.INFO, "Module {0} changed client UUID from {0} to {1}",
                    module.name(),
                    getUUID().getId(),
                    id.getId());
        }

        manager.setRaw("uuid", id.getId());
        manager.save();
    }

    /**
     * Set the account 2FA status
     *
     * @param status the account 2FA status
     */
    @Override
    public void set2FA(final boolean status) {
        LockLoginRuntime.checkSecurity(false);

        PluginModule module = LockLoginRuntime.getMethodCaller();
        if (module != null) {
            source.logger().scheduleLog(Level.INFO, "Module {0} changed client status of {1} from {2} to {3}",
                    module.name(),
                    getUUID().getId(),
                    has2FA(),
                    status);
        }

        manager.setRaw("2fa", status);
        manager.save();
    }

    /**
     * Import the values from the specified account manager
     *
     * @param account the account
     */
    @Override
    protected void importFrom(final @NotNull AccountManager account) {
        LockLoginRuntime.checkSecurity(false);

        manager.setRaw("player", account.getName());
        manager.setRaw("uuid", account.getUUID().getId());
        manager.setRaw("password",account.getPassword());
        manager.setRaw("token", account.getGAuth());
        manager.setRaw("pin", account.getPin());
        manager.setRaw("2fa", account.has2FA());

        PluginModule module = LockLoginRuntime.getMethodCaller();
        if (module != null) {
            source.console().send("Module {0} called method PlayerAccount#importFrom which has protected access. LockLogin will prevent this from happening in a future", Level.GRAVE, module.name());
        }

        manager.save();
    }

    /**
     * Get the player name
     *
     * @return the player name
     */
    @Override
    public @NotNull String getName() {
        if (manager.isSet("player")) {
            Element<?> element = manager.get("player");
            if (element.isPrimitive()) {
                ElementPrimitive primitive = element.getAsPrimitive();
                if (primitive.isString()) {
                    return primitive.asString();
                }
            }
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
        LockLoginRuntime.checkSecurity(false);

        PluginModule module = LockLoginRuntime.getMethodCaller();
        if (module != null) {
            source.logger().scheduleLog(Level.INFO, "Module {0} changed client name of {1} from {2} to {3}",
                    module.name(),
                    getUUID().getId(),
                    getName(),
                    name);
        }

        manager.setRaw("player", name);
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
            Element<?> element = manager.get("uuid");
            if (element.isPrimitive()) {
                ElementPrimitive primitive = element.getAsPrimitive();

                if (primitive.isString()) {
                    String id = primitive.asString();

                    if (StringUtils.isNullOrEmpty(id)) {
                        String name = PathUtilities.getName(manager.getDocument(), false);
                        String extension = FileUtilities.getExtension(name);
                        UUID fixed = UUIDFetcher.fromSimple(name.replace("." + extension, ""));

                        id = fixed.toString();

                        manager.setRaw("uuid", id);
                        manager.save();
                    }

                    return AccountID.fromString(id);
                }
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
        LockLoginRuntime.checkSecurity(false);

        PluginModule module =LockLoginRuntime.getMethodCaller();
        if (module != null) {
            source.logger().scheduleLog(Level.INFO, "Module {0} requested password of {1}",
                    module.name(),
                    getUUID().getId());
        }

        if (manager.isSet("password")) {
            Element<?> element = manager.get("password");
            if (element.isPrimitive()) {
                ElementPrimitive primitive = element.getAsPrimitive();
                if (primitive.isString()) return primitive.asString();
            }
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
        LockLoginRuntime.checkSecurity(false);

        PluginModule module = LockLoginRuntime.getMethodCaller();
        if (module != null) {
            source.logger().scheduleLog(Level.INFO, "Module {0} changed client password of {1}",
                    module.name(),
                    getUUID().getId());
        }

        CryptoFactory util = CryptoFactory.getBuilder().withPassword(newPassword).unsafe();
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        manager.setRaw("password", util.hash(config.passwordEncryption(), config.encryptBase64()));
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
        LockLoginRuntime.checkSecurity(false);

        PluginModule module = LockLoginRuntime.getMethodCaller();
        if (module != null) {
            source.logger().scheduleLog(Level.INFO, "Module {0} changed client password of {1}",
                    module.name(),
                    getUUID().getId());
        }

        CryptoFactory util = CryptoFactory.getBuilder().withPassword(newPassword).unsafe();
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        manager.setRaw("password", util.hash(config.passwordEncryption(), config.encryptBase64()));
        manager.save();
    }

    /**
     * Get player Google Authenticator token
     *
     * @return the player google auth token
     */
    @Override
    public @NotNull String getGAuth() {
        LockLoginRuntime.checkSecurity(false);

        PluginModule module =LockLoginRuntime.getMethodCaller();
        if (module != null) {
            source.logger().scheduleLog(Level.INFO, "Module {0} requested google auth token of {1}",
                    module.name(),
                    getUUID().getId());
        }

        if (manager.isSet("token")) {
            Element<?> element = manager.get("token");
            if (element.isPrimitive()) {
                ElementPrimitive primitive = element.getAsPrimitive();
                if (primitive.isString()) return primitive.asString();
            }
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
        LockLoginRuntime.checkSecurity(false);

        PluginModule module = LockLoginRuntime.getMethodCaller();
        if (module != null) {
            source.logger().scheduleLog(Level.INFO, "Module {0} changed client google auth token of {1}",
                    module.name(),
                    getUUID().getId());
        }

        CryptoFactory util = CryptoFactory.getBuilder().withPassword(token).unsafe();
        manager.setRaw("token", util.toBase64(CryptTarget.PASSWORD));
        manager.save();
    }

    /**
     * Save the account unsafe google auth token
     *
     * @param token the account google auth token
     */
    @Override
    public void setUnsafeGAuth(final @Nullable String token) {
        LockLoginRuntime.checkSecurity(false);

        PluginModule module = LockLoginRuntime.getMethodCaller();
        if (module != null) {
            source.logger().scheduleLog(Level.INFO, "Module {0} changed client google auth token of {1}",
                    module.name(),
                    getUUID().getId());
        }

        CryptoFactory util = CryptoFactory.getBuilder().withPassword(token).unsafe();
        manager.setRaw("token", util.toBase64(CryptTarget.PASSWORD));
        manager.save();
    }

    /**
     * Get the player pin
     *
     * @return the player pin
     */
    @Override
    public @NotNull String getPin() {
        LockLoginRuntime.checkSecurity(false);

        PluginModule module =LockLoginRuntime.getMethodCaller();
        if (module != null) {
            source.logger().scheduleLog(Level.INFO, "Module {0} requested pin of {1}",
                    module.name(),
                    getUUID().getId());
        }

        PluginConfiguration configuration = CurrentPlatform.getConfiguration();
        if (configuration.enablePin()) {
            if (manager.isSet("pin")) {
                Element<?> element = manager.get("pin");
                if (element.isPrimitive()) {
                    ElementPrimitive primitive = element.getAsPrimitive();
                    if (primitive.isString()) return primitive.asString();
                }
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
        LockLoginRuntime.checkSecurity(false);

        PluginModule module = LockLoginRuntime.getMethodCaller();
        if (module != null) {
            source.logger().scheduleLog(Level.INFO, "Module {0} changed client pin of {1}",
                    module.name(),
                    getUUID().getId());
        }

        CryptoFactory util = CryptoFactory.getBuilder().withPassword(pin).unsafe();
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        manager.setRaw("pin", util.hash(config.pinEncryption(), config.encryptBase64()));
        manager.save();
    }

    /**
     * Save the account panic unsafe token
     *
     * @param token the account panic token
     */
    @Override
    public void setUnsafePanic(@Nullable String token) {
        LockLoginRuntime.checkSecurity(false);

        PluginModule module = LockLoginRuntime.getMethodCaller();
        if (module != null) {
            source.logger().scheduleLog(Level.INFO, "Module {0} changed client panic of {1}",
                    module.name(),
                    getUUID().getId());
        }

        CryptoFactory util = CryptoFactory.getBuilder().withPassword(token).unsafe();
        HashType hash = HashType.pickRandom();

        manager.setRaw("panic", util.hash(hash, true));
        manager.save();
    }

    /**
     * Get the account panic token
     *
     * @return the account panic token
     */
    @Override
    public @NotNull String getPanic() {
        LockLoginRuntime.checkSecurity(false);

        PluginModule module =LockLoginRuntime.getMethodCaller();
        if (module != null) {
            source.logger().scheduleLog(Level.INFO, "Module {0} requested panic token of {1}",
                    module.name(),
                    getUUID().getId());
        }

        PluginConfiguration configuration = CurrentPlatform.getConfiguration();
        if (configuration.enablePin()) {
            if (manager.isSet("panic")) {
                Element<?> element = manager.get("panic");
                if (element.isPrimitive()) {
                    ElementPrimitive primitive = element.getAsPrimitive();
                    if (primitive.isString()) return primitive.asString();
                }
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
        LockLoginRuntime.checkSecurity(false);

        PluginModule module = LockLoginRuntime.getMethodCaller();
        if (module != null) {
            source.logger().scheduleLog(Level.INFO, "Module {0} changed client panic of {1}",
                    module.name(),
                    getUUID().getId());
        }

        CryptoFactory factory = CryptoFactory.getBuilder().withToken(token).withPassword(token).unsafe();
        HashType detected = factory.getTokenHash();
        if (detected.equals(HashType.NONE) || detected.equals(HashType.UNKNOWN)) {
            manager.setRaw("panic", factory.hash(HashType.pickRandom(), true));
            manager.save();
        } else {
            manager.setRaw("panic", token);
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
        LockLoginRuntime.checkSecurity(false);

        PluginModule module = LockLoginRuntime.getMethodCaller();
        if (module != null) {
            source.logger().scheduleLog(Level.INFO, "Module {0} changed client pin of {1}",
                    module.name(),
                    getUUID().getId());
        }

        CryptoFactory util = CryptoFactory.getBuilder().withPassword(pin).unsafe();
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        manager.setRaw("pin", util.hash(config.pinEncryption(), config.encryptBase64()));
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
                Element<?> element = manager.get("2fa");
                if (element.isPrimitive()) {
                    ElementPrimitive primitive = element.getAsPrimitive();
                    if (primitive.isBoolean()) return primitive.asBoolean();
                }
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
            try(Stream<Path> files = Files.list(accounts)) {
                files.forEach((sub) -> {
                    String extension = PathUtilities.getExtension(sub);
                    String trimmedId = sub.getFileName().toString().replace("." + extension, "");

                    PlayerAccount manager = new PlayerAccount(AccountID.fromString(trimmedId));
                    if (manager.manager != null) {
                        AccountID uuid = manager.getUUID();
                        if (!StringUtils.isNullOrEmpty(uuid.getId()))
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
