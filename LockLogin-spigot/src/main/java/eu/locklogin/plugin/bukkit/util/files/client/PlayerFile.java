package eu.locklogin.plugin.bukkit.util.files.client;

import eu.locklogin.api.common.utils.other.GlobalAccount;
import eu.locklogin.api.module.plugin.api.event.user.AccountRemovedEvent;
import eu.locklogin.api.module.plugin.javamodule.JavaModuleManager;
import eu.locklogin.plugin.bukkit.Main;
import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.api.common.karmafile.KarmaFile;
import ml.karmaconfigs.api.common.karmafile.karmayaml.KarmaYamlManager;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.api.common.utils.enums.Level;
import eu.locklogin.api.account.AccountID;
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.AzuriomId;
import eu.locklogin.api.encryption.CryptTarget;
import eu.locklogin.api.encryption.CryptoUtil;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.util.platform.CurrentPlatform;
import org.bukkit.OfflinePlayer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import static eu.locklogin.plugin.bukkit.LockLogin.plugin;

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
public final class PlayerFile extends AccountManager {

    private final KarmaFile manager;

    private final OfflinePlayer player;

    public PlayerFile() {
        manager = null;
        player = null;
    }

    public PlayerFile(final File managed) {
        player = null;

        manager = new KarmaFile(managed);
    }

    public PlayerFile(final OfflinePlayer managed) {
        player = managed;

        AzuriomId id = new AzuriomId(AccountID.fromUUID(managed.getUniqueId()));
        File file = id.getAccountFile();
        if (file.exists())
            manager = new KarmaFile(file);
        else
            manager = new KarmaFile(plugin, player.getUniqueId().toString().replace("-", "") + ".lldb", "data", "accounts");
    }

    /**
     * Migrate from LockLogin v1 player database
     */
    public static void migrateV1() {
        File v1DataFolder = new File(plugin.getDataFolder() + File.separator + "Users");
        File[] files = v1DataFolder.listFiles();

        if (files != null) {
            Console.send(plugin, "Initializing LockLogin v1 player database migration", Level.INFO);

            for (File file : files) {
                if (file.getName().endsWith(".yml")) {
                    Console.send(plugin, "Migrating account #" + file.getName().replace(".yml", ""), Level.INFO);
                    KarmaYamlManager oldManager = new KarmaYamlManager(plugin, file.getName(), "Users");

                    File newFile = new File(plugin.getDataFolder() + File.separator + "data" + File.separator + "accounts", file.getName().replace(".yml", ".lldb"));
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
        File v1DataFolder = new File(plugin.getDataFolder() + File.separator + "playerdata");
        File[] files = v1DataFolder.listFiles();

        if (files != null) {
            Console.send(plugin, "Initializing LockLogin v2 player database migration", Level.INFO);

            for (File file : files) {
                if (file.getName().endsWith(".yml")) {
                    Console.send(plugin, "Migrating account #" + file.getName().replace(".yml", ""), Level.INFO);
                    KarmaYamlManager oldManager = new KarmaYamlManager(plugin, file.getName(), "playerdata");

                    File newFile = new File(plugin.getDataFolder() + File.separator + "data" + File.separator + "accounts", file.getName().replace(".yml", ".lldb"));
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
        File v2DataFolder = new File(plugin.getDataFolder() + File.separator + "playerdata");
        File[] files = v2DataFolder.listFiles();

        if (files != null) {
            Console.send(plugin, "Initializing LockLogin v3 player database migration", Level.INFO);

            for (File file : files) {
                if (file.getName().endsWith(".lldb")) {
                    Console.send(plugin, "Migrating account #" + file.getName().replace(".lldb", ""), Level.INFO);
                    KarmaYamlManager oldManager = new KarmaYamlManager(plugin, file.getName(), "playerdata");

                    File newFile = new File(plugin.getDataFolder() + File.separator + "data" + File.separator + "accounts", file.getName());
                    KarmaFile user = new KarmaFile(newFile);

                    try {
                        Files.move(file.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (Throwable ignored) {}
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
            manager.exportFromFile(Main.class.getResourceAsStream("/templates/user.lldb"));

            manager.applyKarmaAttribute();
            return true;
        }

        return false;
    }

    @Override
    public boolean remove(final String issuer) {
        try {
            AccountRemovedEvent event = new AccountRemovedEvent(new GlobalAccount(this), issuer, null);
            JavaModuleManager.callEvent(event);

            return Files.deleteIfExists(manager.getFile().toPath());
        } catch (Throwable ex) {
            return false;
        }
    }

    @Override
    public void saveUUID(final AccountID id) {
        manager.set("UUID", id.getId());
    }

    /**
     * Set the account 2FA status
     *
     * @param status the account 2FA status
     */
    @Override
    public final void set2FA(final boolean status) {
        manager.set("2FA", status);
    }

    /**
     * Get the player name
     *
     * @return the player name
     */
    @Override
    public final String getName() {
        return manager.getString("PLAYER", "").replace("PLAYER:", "");
    }

    /**
     * Save the player name
     *
     * @param name the new player name
     */
    @Override
    public final void setName(final String name) {
        manager.set("PLAYER", name);
    }

    /**
     * Get the player UUID
     *
     * @return the player UUID
     */
    @Override
    public final AccountID getUUID() {
        try {
            return AccountID.fromUUID(UUID.fromString(manager.getString("UUID", UUID.randomUUID().toString()).replace("UUID:", "")));
        } catch (Throwable ex) {
            return AccountID.fromTrimmed(manager.getString("UUID", UUID.randomUUID().toString().replace("-", "")).replace("UUID:", ""));
        }
    }

    /**
     * Get the player password
     *
     * @return the player password
     */
    @Override
    public final String getPassword() {
        return manager.getString("PASSWORD", "").replace("PASSWORD:", "");
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
     * Set the player's password
     *
     * @param newPassword the new player password
     */
    @Override
    public final void setPassword(final String newPassword) {
        if (newPassword != null) {
            CryptoUtil util = CryptoUtil.getBuilder().withPassword(newPassword).build();
            PluginConfiguration config = CurrentPlatform.getConfiguration();

            manager.set("PASSWORD", util.hash(config.passwordEncryption(), true));
        } else {
            manager.set("PASSWORD", "");
        }
    }

    /**
     * Get player Google Authenticator token
     *
     * @return the player google auth token
     */
    @Override
    public final String getGAuth() {
        return manager.getString("TOKEN", "").replace("TOKEN:", "");
    }

    /**
     * Set the player Google Authenticator token
     *
     * @param token the token
     */
    @Override
    public final void setGAuth(final String token) {
        if (token != null) {
            CryptoUtil util = CryptoUtil.getBuilder().withPassword(token).build();

            manager.set("TOKEN", util.toBase64(CryptTarget.PASSWORD));
        } else {
            manager.set("TOKEN", "");
        }
    }

    /**
     * Get the player pin
     *
     * @return the player pin
     */
    @Override
    public final String getPin() {
        PluginConfiguration configuration = CurrentPlatform.getConfiguration();
        if (configuration.enablePin()) {
            return manager.getString("PIN", "").replace("PIN:", "");
        }

        return "";
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
     * Set the player pin
     *
     * @param pin the pin
     */
    @Override
    public final void setPin(final String pin) {
        if (pin != null) {
            CryptoUtil util = CryptoUtil.getBuilder().withPassword(pin).build();
            PluginConfiguration config = CurrentPlatform.getConfiguration();

            manager.set("PIN", util.hash(config.pinEncryption(), true));
        } else {
            manager.set("PIN", "");
        }
    }

    /**
     * Check if the player has 2fa
     *
     * @return if the player has 2Fa in his account
     */
    @Override
    public final boolean has2FA() {
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
    public Instant getCreationTime() {
        try {
            BasicFileAttributes attr = Files.readAttributes(manager.getFile().toPath(), BasicFileAttributes.class);
            return attr.creationTime().toInstant();
        } catch (Throwable ignored) {
        }

        return Instant.now();
    }

    @Override
    public Set<AccountManager> getAccounts() {
        Set<AccountManager> managers = new LinkedHashSet<>();

        File[] files = new File(plugin.getDataFolder() + File.separator + "data" + File.separator + "accounts").listFiles();
        if (files != null) {
            for (File file : files) {
                KarmaFile manager = new KarmaFile(file);
                String uuid = manager.getString("UUID", "");
                if (!uuid.replaceAll("\\s", "").isEmpty()) {
                    try {
                        OfflinePlayer offline = plugin.getServer().getOfflinePlayer(UUID.fromString(uuid));
                        AccountManager acManager = new PlayerFile(offline);

                        managers.add(acManager);
                    } catch (Throwable ignored) {
                    }
                }
            }
        }

        return managers;
    }
}
