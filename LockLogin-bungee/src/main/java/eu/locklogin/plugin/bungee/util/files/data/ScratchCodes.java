package eu.locklogin.plugin.bungee.util.files.data;

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

import eu.locklogin.api.encryption.CryptoFactory;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.karma.file.element.KarmaArray;
import ml.karmaconfigs.api.common.karma.file.element.KarmaElement;
import ml.karmaconfigs.api.common.karma.file.element.KarmaObject;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static eu.locklogin.plugin.bungee.LockLogin.plugin;

public class ScratchCodes {

    private final KarmaMain codesFile;

    /**
     * Initialize the scratch code utility
     *
     * @param _player the player to manage scratch codes who
     */
    public ScratchCodes(final ProxiedPlayer _player) {
        codesFile = new KarmaMain(plugin, _player.getUniqueId().toString().replace("-", "") + ".kf", "data", ".codes");
    }

    /**
     * Store the user scratch codes
     *
     * @param scratch_codes the codes to store
     */
    public final void store(final List<Integer> scratch_codes) {
        List<KarmaElement> codes = new ArrayList<>();
        PluginConfiguration config = CurrentPlatform.getConfiguration();
        for (int code : scratch_codes) {
            CryptoFactory util = CryptoFactory.getBuilder().withPassword(code).build();
            codes.add(new KarmaObject(util.hash(config.pinEncryption(), false)));
        }

        codesFile.set("codes", new KarmaArray(codes.toArray(new KarmaElement[0])));
    }

    /**
     * Validate the scratch code
     *
     * @return if the scratch code is valid
     */
    public final boolean validate(final int code) {
        boolean status = false;

        if (codesFile.isSet("codes")) {
            KarmaElement element = codesFile.get("codes");
            if (element.isArray()) {
                KarmaElement remove = null;
                List<KarmaElement> codes = new ArrayList<>();
                element.getArray().iterator().forEachRemaining(codes::add);

                for (KarmaElement token : codes) {
                    if (token.isString()) {
                        CryptoFactory util = CryptoFactory.getBuilder().withPassword(code).withToken(token.getObjet().toString()).build();
                        if (util.validate()) {
                            remove = token;
                            status = true;

                            break;
                        }
                    }
                }

                if (remove != null) {
                    element.getArray().remove(remove);
                    codesFile.set("codes", element);

                    codesFile.save();
                }
            }
        }

        return status;
    }

    /**
     * Check if the client needs new scratch codes
     *
     * @return if the client needs new scratch codes
     */
    public final boolean needsNew() {
        KarmaElement codes = codesFile.get("codes");
        if (codes != null && codes.isArray()) {
            AtomicInteger size = new AtomicInteger();
            codes.getArray().iterator().forEachRemaining((element) -> size.incrementAndGet());

            return size.get() > 0;
        }

        return true;
    }
}
