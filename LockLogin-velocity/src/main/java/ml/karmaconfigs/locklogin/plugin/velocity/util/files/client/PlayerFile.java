package ml.karmaconfigs.locklogin.plugin.velocity.util.files.client;

import com.velocitypowered.api.proxy.Player;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.api.velocity.Console;
import ml.karmaconfigs.api.velocity.KarmaFile;
import ml.karmaconfigs.api.velocity.Util;
import ml.karmaconfigs.api.velocity.karmayaml.YamlManager;
import ml.karmaconfigs.locklogin.api.account.AccountID;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.account.AzuriomId;
import ml.karmaconfigs.locklogin.api.encryption.CryptTarget;
import ml.karmaconfigs.locklogin.api.encryption.CryptoUtil;
import ml.karmaconfigs.locklogin.api.files.PluginConfiguration;
import ml.karmaconfigs.locklogin.api.utils.platform.CurrentPlatform;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import static ml.karmaconfigs.locklogin.plugin.velocity.LockLogin.plugin;

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

    private final Player player;

    public PlayerFile() {
        manager = null;
        player = null;
    }

    public PlayerFile(final File managed) {
        player = null;

        manager = new KarmaFile(managed);
    }

    public PlayerFile(final Player managed) {
        player = managed;

        AzuriomId id = new AzuriomId(AccountID.fromUUID(managed.getUniqueId()));
        File file = id.getAccountFile();
        if (file.exists())
            manager = new KarmaFile(file);
        else
            manager = new KarmaFile(plugin, player.getUniqueId().toString().replace("-", "") + ".lldb", "data", "accounts");
    }

    public PlayerFile(final AccountID id) {
        player = null;

        Util util = new Util(plugin);

        File file = new File(util.getDataFolder() + File.separator + "data" + File.separator + "accounts", id.getId().replace("-", "") + ".lldb");
        manager = new KarmaFile(file);
    }

    /**
     * Migrate from LockLogin v1 player database
     */
    public static void migrateV1() {
        Util util = new Util(plugin);

        File v1DataFolder = new File(util.getDataFolder() + File.separator + "Users");
        File[] files = v1DataFolder.listFiles();

        if (files != null) {
            Console.send(plugin, "Initializing LockLogin v1 player database migration", Level.INFO);

            for (File file : files) {
                if (file.getName().endsWith(".yml")) {
                    Console.send(plugin, "Migrating account #" + file.getName().replace(".yml", ""), Level.INFO);
                    YamlManager oldManager = new YamlManager(plugin, file.getName(), "Users");

                    File newFile = new File(util.getDataFolder() + File.separator + "data" + File.separator + "accounts", file.getName().replace(".yml", ".lldb"));
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
                        user.set("PLAYER", (name != null ? name : ""));

                        user.set("\n");

                        //UUID record wasn't a feature in that time...
                        user.set("/// The user UUID, used for offline API -->");
                        user.set("UUID", "");

                        user.set("\n");

                        user.set("/// The user password -->");
                        user.set("PASSWORD", (password != null ? password : ""));

                        user.set("\n");

                        user.set("/// The user google auth token -->");
                        user.set("TOKEN", (token != null ? token : ""));

                        user.set("\n");

                        //Pin didn't exist at that time, so let's just set it empty
                        user.set("/// The user pin -->");
                        user.set("PIN", "");

                        user.set("\n");

                        user.set("/// The user Google Auth status -->");
                    } else {
                        user.set("PLAYER", (name != null ? name : ""));
                        user.set("UUID", "");
                        user.set("PASSWORD", (password != null ? password : ""));
                        user.set("TOKEN", (token != null ? token : ""));
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
        Util util = new Util(plugin);

        File v1DataFolder = new File(util.getDataFolder() + File.separator + "playerdata");
        File[] files = v1DataFolder.listFiles();

        if (files != null) {
            Console.send(plugin, "Initializing LockLogin v2 player database migration", Level.INFO);

            for (File file : files) {
                if (file.getName().endsWith(".yml")) {
                    Console.send(plugin, "Migrating account #" + file.getName().replace(".yml", ""), Level.INFO);
                    YamlManager oldManager = new YamlManager(plugin, file.getName(), "playerdata");

                    File newFile = new File(util.getDataFolder() + File.separator + "data" + File.separator + "accounts", file.getName().replace(".yml", ".lldb"));
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
                        user.set("PLAYER", (name != null ? name : ""));

                        user.set("\n");

                        //UUID record wasn't a feature in that time...
                        user.set("/// The user UUID, used for offline API -->");
                        user.set("UUID", (uuid != null ? uuid : ""));

                        user.set("\n");

                        user.set("/// The user password -->");
                        user.set("PASSWORD", (password != null ? password : ""));

                        user.set("\n");

                        user.set("/// The user google auth token -->");
                        user.set("TOKEN", (token != null ? token : ""));

                        user.set("\n");

                        //Pin didn't exist at that time, so let's just set it empty
                        user.set("/// The user pin -->");
                        user.set("PIN", (pin != null ? pin : ""));

                        user.set("\n");

                        user.set("/// The user Google Auth status -->");
                    } else {
                        user.set("PLAYER", (name != null ? name : ""));
                        user.set("UUID", (uuid != null ? uuid : ""));
                        user.set("PASSWORD", (password != null ? password : ""));
                        user.set("TOKEN", (token != null ? token : ""));
                        user.set("PIN", (pin != null ? pin : ""));
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

    @Override
    public boolean exists() {
        return manager.exists();
    }

    @Override
    public boolean create() {
        if (!manager.exists()) {
            manager.exportFromFile("templates/user.lldb");

            manager.applyKarmaAttribute();
            return true;
        }

        return false;
    }

    @Override
    public boolean remove() {
        try {
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
        return manager.getString("PIN", "").replace("PIN:", "");
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
        return manager.getBoolean("2FA", false);
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

        Util util = new Util(plugin);

        File[] files = new File(util.getDataFolder() + File.separator + "data" + File.separator + "accounts").listFiles();
        if (files != null) {
            for (File file : files) {
                PlayerFile manager = new PlayerFile(file);
                AccountID uuid = manager.getUUID();
                String name = manager.getName();
                if (!uuid.getId().replaceAll("\\s", "").isEmpty() && !name.replaceAll("\\s", "").isEmpty())
                    managers.add(manager);
            }
        }

        return managers;
    }
}
