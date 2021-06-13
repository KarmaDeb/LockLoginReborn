package eu.locklogin.plugin.bukkit.util;

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

import eu.locklogin.plugin.bukkit.util.player.User;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.plugin.bukkit.util.files.client.OfflineClient;
import eu.locklogin.api.common.session.SessionDataContainer;
import eu.locklogin.api.common.utils.InstantParser;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

public final class LockLoginPlaceholder extends PlaceholderExpansion {

    /**
     * This method should always return true unless we
     * have a dependency we need to make sure is on the server
     * for our placeholders to work!
     *
     * @return always true since we do not have any dependencies.
     */
    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public boolean persist() {
        return true;
    }

    /**
     * The placeholder identifier should go here.
     * <br>This is what tells PlaceholderAPI to call our onRequest
     * method to obtain a value if a placeholder starts with our
     * identifier.
     * <br>The identifier has to be lowercase and can't contain _ or %
     *
     * @return The identifier in {@code %<identifier>_<value>%} as String.
     */
    @Override
    public @NotNull String getIdentifier() {
        return "locklogin";
    }

    /**
     * The name of the person who created this expansion should go here.
     *
     * @return The name of the author as a String.
     */
    @Override
    public @NotNull String getAuthor() {
        return "KarmaDev";
    }

    /**
     * This is the version of this expansion.
     * <br>You don't have to use numbers, since it is set as a String.
     *
     * @return The version as a String.
     */
    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    /**
     * This is the method called when a placeholder with our identifier
     * is found and needs a value.
     * <br>We specify the value identifier in this method.
     * <br>Since version 2.9.1 can you use OfflinePlayers in your requests.
     *
     * @param player     A {@link org.bukkit.OfflinePlayer OfflinePlayer}.
     * @param identifier A String containing the identifier/value.
     * @return Possibly-null String of the requested identifier.
     */
    @Override
    public String onRequest(final OfflinePlayer player, final String identifier) {
        OfflineClient client = new OfflineClient(player.getName());
        AccountManager manager = client.getAccount();

        InstantParser parser = new InstantParser(Instant.now());
        switch (identifier.toLowerCase()) {
            case "logged":
                return String.valueOf(SessionDataContainer.getLogged());
            case "registered":
                return String.valueOf(SessionDataContainer.getRegistered());
            case "islogged":
                if (player.getPlayer() != null && player.getPlayer().isOnline()) {
                    User user = new User(player.getPlayer());
                    ClientSession session = user.getSession();

                    return String.valueOf((session.isValid() && session.isValid() && session.isTempLogged()))
                            .replace("true", "&ayes").replace("false", "&cno");
                } else {
                    return "&cno";
                }
            case "isregistered":
                return String.valueOf((manager != null && manager.exists() && !manager.getPassword().replaceAll("\\s", "").isEmpty()))
                        .replace("true", "&ayes").replace("false", "&cno");
            case "creation":
                if (manager != null && manager.exists())
                    parser = new InstantParser(manager.getCreationTime());

                return parser.getDay() + "/" + parser.getMonth() + "/" + parser.getYear();
            case "age":
                if (manager != null && manager.exists())
                    parser = new InstantParser(manager.getCreationTime());

                return parser.getDifference(Instant.now());
            default:
                return "404 ( " + identifier + " )";
        }
    }
}
