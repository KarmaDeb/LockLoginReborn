package eu.locklogin.api.account;

import eu.locklogin.api.encryption.CryptoFactory;
import eu.locklogin.api.encryption.HashType;
import eu.locklogin.api.encryption.Validation;
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.karma.file.element.KarmaPrimitive;
import ml.karmaconfigs.api.common.karma.file.element.types.Element;
import ml.karmaconfigs.api.common.karma.file.element.types.ElementPrimitive;
import ml.karmaconfigs.api.common.karma.source.APISource;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.string.random.RandomString;

import java.net.InetAddress;
import java.time.Instant;
import java.time.ZonedDateTime;

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

    private final KarmaMain data;

    /**
     * Initialize the brute force provider
     *
     * @param player the player that is trying to be accessed
     * @param ip     the IP that is trying to access the player
     */
    public BruteForce(ModulePlayer player, InetAddress ip) {
        super(player, ip);

        data = new KarmaMain(APISource.loadProvider("LockLogin"), player.getUUID().toString() + ".kf", "data", "locked");
    }

    /**
     * Login failed
     */
    @Override
    public void fail() {
        ZonedDateTime dateTime = ZonedDateTime.now();

        data.setRaw(getAddress().getHostAddress(), dateTime.plusDays(1).toInstant());
    }

    /**
     * Set the account panic status
     *
     * @param status the account panic status
     */
    @Override
    public void setPanicStatus(final boolean status) {
        data.setRaw("panic", status);

        for (String key : data.getKeys()) {
            if (!key.equalsIgnoreCase("panic")) {
                data.set(key, null);
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
        Element<?> element = data.get("panic");
        if (element.isPrimitive()) {
            ElementPrimitive primitive = element.getAsPrimitive();
            if (primitive.isBoolean()) return primitive.asBoolean();
        }

        return false;
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

            data.setRaw("token", factory.hash(HashType.pickRandom(), true));

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
        data.set("token", null);

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
        CryptoFactory factory = CryptoFactory.getBuilder()
                .withPassword(token)
                .withToken(data.get(token, new KarmaPrimitive(new RandomString().create())))
                .build();

        return factory.validate(Validation.MODERN);
    }

    /**
     * Get if the IP can access that account
     *
     * @return if the IP can access that account
     */
    @Override
    public boolean canJoin() {
        Element<?> ip_data = data.get(getAddress().getHostAddress(), new KarmaPrimitive(""));
        if (ip_data.isPrimitive()) {
            ElementPrimitive primitive = ip_data.getAsPrimitive();
            if (primitive.isString()) {
                String data = ip_data.getAsString();
                if (!StringUtils.isNullOrEmpty(data)) {
                    Instant now = Instant.now();
                    Instant time = Instant.parse(data);

                    return now.isAfter(time);
                }
            }
        }

        return true;
    }
}
