package eu.locklogin.api.account;

import eu.locklogin.api.encryption.CryptoFactory;
import eu.locklogin.api.encryption.HashType;
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.karmafile.KarmaFile;
import ml.karmaconfigs.api.common.karmafile.Key;
import ml.karmaconfigs.api.common.utils.string.StringUtils;

import java.net.InetAddress;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Random;

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

/**
 * Initialize the brute force class
 */
public final class BruteForce extends BruteForceProvider {

    private final KarmaFile data;

    /**
     * Initialize the brute force provider
     *
     * @param player the player that is trying to be accessed
     * @param ip     the IP that is trying to access the player
     */
    public BruteForce(ModulePlayer player, InetAddress ip) {
        super(player, ip);

        data = new KarmaFile(APISource.loadProvider("LockLogin"), player.getUUID().toString(), "data", "locked");
    }

    /**
     * Login failed
     */
    @Override
    public void fail() {
        ZonedDateTime dateTime = ZonedDateTime.now();

        data.set(getAddress().getHostAddress(), dateTime.plusDays(1).toInstant().toString());
    }

    /**
     * Set the account panic status
     *
     * @param status the account panic status
     */
    @Override
    public void setPanicStatus(final boolean status) {
        data.set("PANIC", status);

        for (Key key : data.getKeys(false)) {
            if (!key.getPath().equalsIgnoreCase("panic")) {
                if (data.isList(key.getPath())) {
                    data.unsetList(key.getPath());
                } else {
                    data.unset(key.getPath());
                }
            }
        }
    }

    /**
     * Get if the account is in panic mode
     *
     * @return if the account is in panic mode
     */
    @Override
    public boolean isInPanic() {
        return data.getBoolean("PANIC", false);
    }

    /**
     * Create a new token
     *
     * @return a new generated token
     */
    public String createToken() {
        if (!data.isSet("TOKEN")) {
            String token = generate();

            CryptoFactory factory = CryptoFactory
                    .getBuilder()
                    .withPassword(token)
                    .build();

            HashType[] types = HashType.values();
            int random = new Random().nextInt(types.length);
            if (random == types.length) {
                random = random - 1;
            }

            data.set("TOKEN", factory.hash(types[random], true));

            return token;
        }

        return "ALREADY_HAS_TOKEN";
    }

    /**
     * Login success
     *
     * @return the new login token
     */
    @Override
    public String success() {
        setPanicStatus(false);
        data.unset("TOKEN");

        return createToken();
    }

    /**
     * Validate a login token
     *
     * @param token the login token
     * @return if the token is valid
     */
    @Override
    public boolean validate(final String token) {
        CryptoFactory factory = CryptoFactory.getBuilder().
                withPassword(token).
                withToken(data.getString("TOKEN", StringUtils.generateString().create())).
                build();

        return factory.validate();
    }

    /**
     * Get if the IP can access that account
     *
     * @return if the IP can access that account
     */
    @Override
    public boolean canJoin() {
        String ipData = data.getString(getAddress().getHostAddress(), "");
        if (!StringUtils.isNullOrEmpty(ipData)) {
            Instant now = Instant.now();
            Instant time = Instant.parse(data.getString(getAddress().getHostAddress(), now.toString()));

            return now.isAfter(time);
        }

        return true;
    }
}
