package eu.locklogin.plugin.bukkit.util.inventory;

/*
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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import eu.locklogin.api.account.AccountID;
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.common.utils.InstantParser;
import eu.locklogin.api.common.utils.other.LockedAccount;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.util.files.client.OfflineClient;
import eu.locklogin.plugin.bukkit.util.inventory.object.Button;
import eu.locklogin.plugin.bukkit.util.inventory.object.SkullCache;
import eu.locklogin.plugin.bukkit.util.player.User;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

import static eu.locklogin.plugin.bukkit.LockLogin.plugin;
import static eu.locklogin.plugin.bukkit.LockLogin.properties;

public final class PlayersInfoInventory implements InventoryHolder {

    private static final HashMap<UUID, Integer> playerPage = new HashMap<>();
    private static final HashMap<UUID, PlayersInfoInventory> inventories = new HashMap<>();

    private final ArrayList<Inventory> pages = new ArrayList<>();
    private final Player player;

    /**
     * Initialize the infinite inventory page
     *
     * @param owner the inventory owner
     * @param uuids the id of players to show in the GUI
     */
    public PlayersInfoInventory(final Player owner, final Set<AccountID> uuids) {
        player = owner;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            if (player != null && player.isOnline()) {
                Inventory page = getBlankPage();

                for (AccountID uuid : uuids) {
                    OfflineClient offline = new OfflineClient(uuid);
                    AccountManager manager = offline.getAccount();

                    if (manager != null) {
                        ItemStack item = getPlayerHead(player.getName());
                        ItemMeta meta = item.getItemMeta();
                        assert meta != null;

                        meta.setDisplayName(StringUtils.toColor("&f" + manager.getName()));

                        LockedAccount locked = new LockedAccount(manager.getUUID());

                        User tarUser;

                        try {
                            tarUser = new User(plugin.getServer().getPlayer(UUID.fromString(manager.getUUID().getId())));
                        } catch (Throwable ex) {
                            tarUser = null;
                        }

                        List<String> parsed = parseMessages(locked.isLocked());
                        List<String> lore = new ArrayList<>();

                        InstantParser creation_parser = new InstantParser(manager.getCreationTime());
                        String creation_date = StringUtils.formatString("{0}/{1}/{2}", creation_parser.getDay(), creation_parser.getMonth(), creation_parser.getYear());

                        int msg = -1;
                        lore.add(StringUtils.formatString(parsed.get(++msg), locked.isLocked()));
                        if (locked.isLocked()) {
                            Instant date = locked.getLockDate();
                            InstantParser parser = new InstantParser(date);
                            String dateString = parser.getYear() + " " + parser.getMonth() + " " + parser.getDay();

                            lore.add(StringUtils.formatString(parsed.get(++msg), locked.getIssuer()));
                            lore.add(StringUtils.formatString(parsed.get(++msg), dateString));
                        }
                        lore.add(StringUtils.formatString(parsed.get(++msg), manager.getName()));
                        lore.add(StringUtils.formatString(parsed.get(++msg), manager.getUUID().getId()));
                        lore.add(StringUtils.formatString(parsed.get(++msg), (!manager.getPassword().replaceAll("\\s", "").isEmpty())));
                        lore.add(StringUtils.formatString(parsed.get(++msg), (tarUser != null ? tarUser.getSession().isLogged() : "false")));
                        lore.add(StringUtils.formatString(parsed.get(++msg), (tarUser != null ? tarUser.getSession().isTempLogged() : "false")));
                        lore.add(StringUtils.formatString(parsed.get(++msg), (manager.has2FA() && !manager.getGAuth().replaceAll("\\s", "").isEmpty())));
                        lore.add(StringUtils.formatString(parsed.get(++msg), manager.hasPin()));
                        lore.add(StringUtils.formatString(parsed.get(++msg), creation_date, creation_parser.getDifference()));

                        meta.setLore(StringUtils.toColor(lore));

                        item.setItemMeta(meta);

                        page.addItem(item);
                    }
                }

                pages.add(page);
                playerPage.put(player.getUniqueId(), 0);
                inventories.put(player.getUniqueId(), this);

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.openInventory(pages.get(0));
                    playerPage.put(player.getUniqueId(), 0);
                });
            }
        });
    }

    /**
     * Initialize the infinite inventory page
     *
     * @param owner    the inventory owner
     * @param accounts the account manager instances
     */
    public PlayersInfoInventory(final Set<AccountManager> accounts, final Player owner) {
        player = owner;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            if (player != null && player.isOnline()) {
                Inventory page = getBlankPage();

                for (AccountManager manager : accounts) {
                    ItemStack item = getPlayerHead(player.getName());
                    ItemMeta meta = item.getItemMeta();
                    assert meta != null;

                    meta.setDisplayName(StringUtils.toColor("&f" + manager.getName()));

                    LockedAccount locked = new LockedAccount(manager.getUUID());

                    User tarUser;

                    try {
                        tarUser = new User(plugin.getServer().getPlayer(UUID.fromString(manager.getUUID().getId())));
                    } catch (Throwable ex) {
                        tarUser = null;
                    }

                    List<String> parsed = parseMessages(locked.isLocked());
                    List<String> lore = new ArrayList<>();

                    InstantParser creation_parser = new InstantParser(manager.getCreationTime());
                    String creation_date = StringUtils.formatString("{0}/{1}/{2}", creation_parser.getDay(), creation_parser.getMonth(), creation_parser.getYear());

                    int msg = -1;
                    lore.add(StringUtils.formatString(parsed.get(++msg), locked.isLocked()));
                    if (locked.isLocked()) {
                        Instant date = locked.getLockDate();
                        InstantParser parser = new InstantParser(date);
                        String dateString = parser.getYear() + " " + parser.getMonth() + " " + parser.getDay();

                        lore.add(StringUtils.formatString(parsed.get(++msg), locked.getIssuer()));
                        lore.add(StringUtils.formatString(parsed.get(++msg), dateString));
                    }
                    lore.add(StringUtils.formatString(parsed.get(++msg), manager.getName()));
                    lore.add(StringUtils.formatString(parsed.get(++msg), manager.getUUID().getId()));
                    lore.add(StringUtils.formatString(parsed.get(++msg), (!manager.getPassword().replaceAll("\\s", "").isEmpty())));
                    lore.add(StringUtils.formatString(parsed.get(++msg), (tarUser != null ? tarUser.getSession().isLogged() : "false")));
                    lore.add(StringUtils.formatString(parsed.get(++msg), (tarUser != null ? tarUser.getSession().isTempLogged() : "false")));
                    lore.add(StringUtils.formatString(parsed.get(++msg), (manager.has2FA() && !manager.getGAuth().replaceAll("\\s", "").isEmpty())));
                    lore.add(StringUtils.formatString(parsed.get(++msg), manager.hasPin()));
                    lore.add(StringUtils.formatString(parsed.get(++msg), creation_date, creation_parser.getDifference()));

                    meta.setLore(StringUtils.toColor(lore));

                    item.setItemMeta(meta);

                    page.addItem(item);
                }

                pages.add(page);
                playerPage.put(player.getUniqueId(), 0);
                inventories.put(player.getUniqueId(), this);

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.openInventory(pages.get(0));
                    playerPage.put(player.getUniqueId(), 0);
                });
            }
        });
    }

    /**
     * Get a skull item with the specified owner
     *
     * @return a SkullItem
     */
    @SuppressWarnings("deprecation")
    private static ItemStack getSkull() {
        ItemStack skull;

        try {
            skull = new ItemStack(Material.PLAYER_HEAD, 1);
        } catch (Throwable ex) {
            skull = new ItemStack(Material.valueOf("SKULL_ITEM"), 1, (byte) 3);
        }

        return skull;
    }

    @SuppressWarnings("deprecation")
    private static ItemStack getPlayerHead(String owner) {
        SkullCache cache = new SkullCache(owner);
        String value;
        if (cache.needsCache() || cache.get() == null) {
            value = getHeadValue(owner);

            if (value != null)
                cache.save(value);
        } else {
            value = cache.get();
        }
        if (value == null)
            value = "";

        ItemStack skull = getSkull();
        UUID hashAsId = new UUID(value.hashCode(), value.hashCode());
        return Bukkit.getUnsafe().modifyItemStack(skull,
                "{SkullOwner:{Id:\"" + hashAsId + "\",Properties:{textures:[{Value:\"" + value + "\"}]}}}"
        );
    }

    /**
     * Get the head value
     *
     * @param name the player name
     * @return the player head value
     */
    private static String getHeadValue(String name) {
        try {
            String result = getURLContent("https://api.mojang.com/users/profiles/minecraft/" + name);

            Gson g = new Gson();
            JsonObject obj = g.fromJson(result, JsonObject.class);

            String uid = obj.get("id").toString().replace("\"", "");
            String signature = getURLContent("https://sessionserver.mojang.com/session/minecraft/profile/" + uid);

            obj = g.fromJson(signature, JsonObject.class);

            String value = obj.getAsJsonArray("properties").get(0).getAsJsonObject().get("value").getAsString();
            String decoded = new String(Base64.getDecoder().decode(value));

            obj = g.fromJson(decoded, JsonObject.class);

            String skinURL = obj.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();

            byte[] skinByte = ("{\"textures\":{\"SKIN\":{\"url\":\"" + skinURL + "\"}}}").getBytes();

            return new String(Base64.getEncoder().encode(skinByte));
        } catch (Exception ignored) {
        }

        return null;
    }

    /**
     * Get the specified url contents
     *
     * @param urlStr the url
     * @return the url contents
     */
    private static String getURLContent(String urlStr) {
        URL url;
        BufferedReader in = null;
        StringBuilder sb = new StringBuilder();
        try {
            url = new URL(urlStr);
            in = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8));
            String str;
            while ((str = in.readLine()) != null) {
                sb.append(str);
            }
        } catch (Throwable ignored) {
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Throwable ignored) {
            }
        }

        return sb.toString();
    }

    /**
     * Open the player a member list in that
     * page
     *
     * @param page the page
     */
    public final void openPage(int page) {
        player.openInventory(pages.get(page));
        playerPage.put(player.getUniqueId(), page);
    }

    /**
     * Get the player inventory page
     *
     * @return a integer
     */
    public final int getPlayerPage() {
        if (player != null) {
            if (playerPage.get(player.getUniqueId()) != null) {
                return playerPage.get(player.getUniqueId());
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    /**
     * Get all the inventory pages
     *
     * @return an integer
     */

    public final int getPages() {
        return pages.size();
    }

    /**
     * Creates a new inventory page
     *
     * @return an Inventory
     */
    private Inventory getBlankPage() {
        PluginMessages messages = CurrentPlatform.getMessages();

        String title = StringUtils.toColor(StringUtils.toColor(messages.infoTitle()));
        if (title.length() > 32)
            title = title.substring(0, 32);

        Inventory inv = plugin.getServer().createInventory(this, 54, title);

        inv.setItem(45, Button.back());
        inv.setItem(53, Button.next());
        return inv;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return pages.get(getPlayerPage());
    }

    /**
     * Parse the player info message read
     * from plugin_messages.properties
     *
     * @param condition if the player account is locked
     * @return the parsed player info message
     */
    private List<String> parseMessages(final boolean condition) {
        String[] propData = properties.getProperty("player_information_message", "&7Locked: &d{0},condition=&7Locked by: &d{0}%&7Locked since: &d{0};,&7Name: &d{0},&7Account ID: &d{0},&7Registered: &d{0},&7Logged: &d{0},&7Temp logged: &d{0},&72FA: &d{0},&7Pin: &d{0},&7Created on: &d{0} ( {1} ago )").split(",");
        List<String> cmdMessages = new ArrayList<>();

        for (String propMSG : propData) {
            if (propMSG.startsWith("condition=")) {
                String[] conditionData = propMSG.split(";");

                String condition_message;
                if (condition) {
                    condition_message = conditionData[0].replaceFirst("condition=", "");
                } else {
                    try {
                        condition_message = conditionData[1];
                    } catch (Throwable ex) {
                        condition_message = "";
                    }
                }

                if (!condition_message.isEmpty()) {
                    if (condition_message.contains("%")) {
                        String[] conditionMsgData = condition_message.split("%");

                        cmdMessages.addAll(Arrays.asList(conditionMsgData));
                    } else {
                        cmdMessages.add(condition_message);
                    }
                }
            } else {
                cmdMessages.add(propMSG);
            }
        }

        return cmdMessages;
    }

    /**
     * Alts account inventory manager
     * utilities
     */
    public interface manager {

        /**
         * Get the player alts account
         * inventory
         *
         * @param player the player to get from
         * @return the player alts account inventory
         */
        @Nullable
        static PlayersInfoInventory getInventory(final Player player) {
            return inventories.getOrDefault(player.getUniqueId(), null);
        }
    }
}
