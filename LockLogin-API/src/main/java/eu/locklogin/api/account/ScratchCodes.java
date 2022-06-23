package eu.locklogin.api.account;

import eu.locklogin.api.encryption.CryptoFactory;
import eu.locklogin.api.encryption.Validation;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.karma.file.element.KarmaArray;
import ml.karmaconfigs.api.common.karma.file.element.KarmaElement;
import ml.karmaconfigs.api.common.karma.file.element.KarmaObject;

import java.util.List;

/**
 * LockLogin scratch codes storage
 */
public final class ScratchCodes {

    private final KarmaMain codesFile;

    /**
     * Initialize the scratch code utility
     *
     * @param client the player to manage scratch codes who
     */
    public ScratchCodes(final AccountID client) {
        codesFile = new KarmaMain(APISource.loadProvider("LockLogin"), client.getId().replace("-", "") + ".lldb", "data", ".codes");
    }

    /**
     * Store the user scratch codes
     *
     * @param scratch_codes the codes to store
     */
    public void store(final List<Integer> scratch_codes) {
        KarmaArray codes = new KarmaArray();
        PluginConfiguration config = CurrentPlatform.getConfiguration();
        for (int code : scratch_codes) {
            CryptoFactory util = CryptoFactory.getBuilder().withPassword(code).build();
            codes.add(new KarmaObject(util.hash(config.pinEncryption(), false)));
        }

        codesFile.set("codes", codes);
        codesFile.save();
    }

    /**
     * Validate the scratch code
     *
     * @return if the scratch code is valid
     */
    public boolean validate(final int code) {
        boolean status = false;

        KarmaElement codes = codesFile.get("codes");
        KarmaElement remove = null;
        KarmaArray stored = new KarmaArray();
        if (codes != null && codes.isArray()) {
            stored = codes.getArray();

            for (KarmaElement token : stored) {
                CryptoFactory util = CryptoFactory.getBuilder().withPassword(code).withToken(token).build();
                if (util.validate(Validation.ALL)) {
                    remove = token;
                    status = true;

                    break;
                }
            }
        }

        if (remove != null) {
            stored.remove(remove);
            codesFile.set("codes", stored);
        }

        return status;
    }

    /**
     * Check if the client needs new scratch codes
     *
     * @return if the client needs new scratch codes
     */
    public boolean needsNew() {
        KarmaElement codes = codesFile.get("codes");
        if (codes.isArray()) {
            return codes.getArray().size() <= 0;
        }

        return true;
    }
}
