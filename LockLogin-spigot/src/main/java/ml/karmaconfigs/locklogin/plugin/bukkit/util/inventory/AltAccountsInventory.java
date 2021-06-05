package ml.karmaconfigs.locklogin.plugin.bukkit.util.inventory;

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
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.account.AccountID;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.Message;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.client.OfflineClient;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.inventory.object.Button;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.inventory.object.SkullCache;
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
import java.util.*;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.plugin;

public final class AltAccountsInventory implements InventoryHolder {

    private static final HashMap<UUID, Integer> playerPage = new HashMap<>();
    private static final HashMap<UUID, AltAccountsInventory> inventories = new HashMap<>();

    private final ArrayList<Inventory> pages = new ArrayList<>();
    private final Player player;

    /**
     * Initialize the infinite inventory page
     *
     * @param owner the inventory owner
     * @param uuids the id of players to show in the GUI
     */
    public AltAccountsInventory(final Player owner, final Set<AccountID> uuids) {
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
                        if (player.getUniqueId().toString().equals(manager.getUUID().getId())) {
                            meta.setLore(Arrays.asList("\n", StringUtils.toColor("&7UUID: &e" + manager.getUUID().getId()), StringUtils.toColor("&8&l&o( &cYOU &8&l&o)")));
                        } else {
                            meta.setLore(Arrays.asList("\n", StringUtils.toColor("&7UUID: &e" + manager.getUUID().getId())));
                        }

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
     * @param managers the account manager instances
     */
    public AltAccountsInventory(final Set<AccountManager> managers, final Player owner) {
        player = owner;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            if (player != null && player.isOnline()) {
                Inventory page = getBlankPage();

                for (AccountManager manager : managers) {
                    ItemStack item = getPlayerHead(manager.getName());
                    ItemMeta meta = item.getItemMeta();
                    assert meta != null;

                    meta.setDisplayName(StringUtils.toColor("&f" + manager.getName()));
                    if (player.getUniqueId().toString().equals(manager.getUUID().getId())) {
                        meta.setLore(Arrays.asList("\n", StringUtils.toColor("&7UUID: &e" + manager.getUUID().getId()), StringUtils.toColor("&8&l&o( &cYOU &8&l&o)")));
                    } else {
                        meta.setLore(Arrays.asList("\n", StringUtils.toColor("&7UUID: &e" + manager.getUUID().getId())));
                    }

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
        Message messages = new Message();

        String title = StringUtils.toColor(messages.altTitle());
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
        static AltAccountsInventory getInventory(final Player player) {
            return inventories.getOrDefault(player.getUniqueId(), null);
        }
    }
}
