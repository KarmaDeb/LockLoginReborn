package eu.locklogin.plugin.bukkit.plugin.bungee.data;

/*
 * Private GSA code
 *
 * The use of this code
 * without GSA team authorization
 * will be a violation of
 * terms of use determined
 * in <a href="http://karmaconfigs.cf/license/"> here </a>
 * or (fallback domain) <a href="https://karmaconfigs.github.io/page/license"> here </a>
 */

import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.StringUtils;
import eu.locklogin.api.encryption.CryptoUtil;
import eu.locklogin.api.encryption.HashType;
import eu.locklogin.api.common.session.SessionDataContainer;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static eu.locklogin.plugin.bukkit.LockLogin.logger;

public final class BungeeDataStorager {

    @SuppressWarnings("FieldMayBeFinal")
    //This could be modified by the cache loader or when a bungeecord message has been received for the first time, so it can't be final
    private static String proxyKey = "";
    private static boolean multiBungee = false;
    @SuppressWarnings("FieldMayBeFinal")
    private static Map<UUID, String> proxies = new ConcurrentHashMap<>();
    @SuppressWarnings("FieldMayBeFinal")
    private static Set<UUID> pin_confirmation = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final String provided;

    /**
     * Initialize the bungee data storager
     *
     * @param providedKey the bungeecord key
     */
    public BungeeDataStorager(final String providedKey) {
        if (providedKey != null) {
            provided = providedKey;
        } else {
            provided = "";
        }
    }

    /**
     * Validate the specified bungee storager key
     *
     * @return if the specified key is valid
     */
    public final boolean validate(final UUID id) {
        String key = proxies.getOrDefault(id, "");
        if (!key.replaceAll("\\s", "").isEmpty()) {
            try {
                return CryptoUtil.getBuilder().withPassword(provided).withToken(proxies.get(id)).build().validate();
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }

        return false;
    }

    /**
     * Get if the specified key is the proxy
     * owner key
     *
     * @param key the provided key
     * @return if the key is proxy owner key
     */
    public final boolean isProxyKey(final String key) {
        try {
            if (proxyKey.replaceAll("\\s", "").isEmpty()) {
                return true;
            } else {
                return CryptoUtil.getBuilder().withPassword(key).withToken(proxyKey).build().validate();
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Get if a new proxy can be registered
     *
     * @return if a new proxy can be registered
     */
    public final boolean canRegister() {
        if (multiBungee) {
            return proxies.size() <= 0;
        } else {
            return true;
        }
    }

    /**
     * Get if the player needs a pin confirmation
     *
     * @param player the player
     * @return if the player needs pin confirmation
     */
    public final boolean needsPinConfirmation(final Player player) {
        return pin_confirmation.contains(player.getUniqueId());
    }

    /**
     * Set the player pin status
     *
     * @param player the player
     * @param status the pin status
     */
    public final void setPinConfirmation(final Player player, final boolean status) {
        if (status) {
            pin_confirmation.add(player.getUniqueId());
        } else {
            pin_confirmation.remove(player.getUniqueId());
        }
    }

    /**
     * Set the multi bungee status
     *
     * @param status the multi bungee status
     */
    public final void setMultiBungee(final boolean status) {
        multiBungee = status;
    }

    /**
     * Set the proxy owner id
     *
     * @param key the proxy owner id
     */
    public final void setProxyKey(final String key) {
        if (StringUtils.isNullOrEmpty(proxyKey)) {
            proxyKey = CryptoUtil.getBuilder().withPassword(key).build().hash(HashType.SHA256, true);
            logger.scheduleLog(Level.INFO, "Registered proxy access key to register new proxy IDs");
        }
    }

    /**
     * Set the proxy id
     *
     * @param id the proxy id
     */
    public final void addProxy(final UUID id) {
        String hashed = CryptoUtil.getBuilder().withPassword(provided).build().hash(HashType.SHA256, true);
        String old = proxies.put(id, hashed);

        if (old != null) {
            if (!old.equalsIgnoreCase(hashed)) {
                logger.scheduleLog(Level.INFO, "Updated proxy with id {0}", id);
            }
        } else {
            logger.scheduleLog(Level.INFO, "Registered proxy with id {0}", id);
        }
    }

    /**
     * Remove a proxy
     *
     * @param id the proxy id
     */
    public final void delProxy(final UUID id) {
        if (validate(id))
            proxies.remove(id);
    }

    /**
     * Set the registered accounts amount
     *
     * @param amount the registered accounts amount
     */
    public final void setRegisteredAccounts(final int amount) {
        SessionDataContainer.setRegistered(amount);
    }

    /**
     * Set the logged accounts amount
     *
     * @param amount the logged accounts amount
     */
    public final void setLoggedAccounts(final int amount) {
        SessionDataContainer.setLogged(amount);
    }
}
