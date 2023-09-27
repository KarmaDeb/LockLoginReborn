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

import eu.locklogin.api.common.session.online.SessionDataContainer;
import eu.locklogin.api.encryption.CryptoFactory;
import eu.locklogin.api.encryption.HashType;
import eu.locklogin.api.encryption.Validation;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.string.random.RandomString;
import ml.karmaconfigs.api.common.utils.enums.Level;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static eu.locklogin.plugin.bukkit.LockLogin.logger;

public final class BungeeDataStorager {

    //This could be modified by the cache loader or when a bungeecord message has been received for the first time, so it can't be final
    private static String proxyKey = "";
    private static String serverName = "";
    @SuppressWarnings("FieldMayBeFinal")
    private static Set<UUID> pin_confirmation = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Get if the specified key is the proxy
     * owner key
     *
     * @param key the provided key
     * @return if the key is proxy owner key
     */
    public boolean isProxyKey(final String key) {
        try {
            if (proxyKey.replaceAll("\\s", "").isEmpty()) {
                return true;
            } else {
                return CryptoFactory.getBuilder().withPassword(key).withToken(proxyKey).build().validate(Validation.ALL);
            }
        } catch (Throwable ex) {
            return false;
        }
    }

    /**
     * Get if the player needs a pin confirmation
     *
     * @param player the player
     * @return if the player needs pin confirmation
     */
    public boolean needsPinConfirmation(final Player player) {
        return pin_confirmation.contains(player.getUniqueId());
    }

    /**
     * Get the server name
     *
     * @return the server name
     */
    public String getServerName() {
        if (!StringUtils.isNullOrEmpty(serverName)) {
            return serverName;
        }/* else {
            return TokenGen.requestNode();
        }*/

        return new RandomString().create();
    }

    /**
     * Set the server name
     *
     * @param name the name
     */
    public void setServerName(final String name) {
        serverName = name;
    }

    /**
     * Set the player pin status
     *
     * @param player the player
     * @param status the pin status
     */
    public void setPinConfirmation(final Player player, final boolean status) {
        if (status) {
            pin_confirmation.add(player.getUniqueId());
        } else {
            pin_confirmation.remove(player.getUniqueId());
        }
    }

    /**
     * Set the proxy owner id
     *
     * @param key the proxy owner id
     */
    public void setProxyKey(final String key) {
        if (StringUtils.isNullOrEmpty(proxyKey)) {
            proxyKey = CryptoFactory.getBuilder().withPassword(key).unsafe().hash(HashType.SHA256, true);
            logger.scheduleLog(Level.INFO, "Registered proxy access key to register new proxy IDs");
        }
    }

    /**
     * Set the registered accounts amount
     *
     * @param amount the registered accounts amount
     */
    public void setRegisteredAccounts(final int amount) {
        SessionDataContainer.setRegistered(amount);
    }

    /**
     * Set the logged accounts amount
     *
     * @param amount the logged accounts amount
     */
    public void setLoggedAccounts(final int amount) {
        SessionDataContainer.setLogged(amount);
    }

    /**
     * Get if the server has proxy
     *
     * @return if the server has a proxy
     */
    @SuppressWarnings("unused")
    public boolean needsProxy() {
        return StringUtils.isNullOrEmpty(proxyKey);
    }
}
