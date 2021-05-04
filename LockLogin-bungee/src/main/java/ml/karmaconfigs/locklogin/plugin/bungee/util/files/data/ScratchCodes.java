package ml.karmaconfigs.locklogin.plugin.bungee.util.files.data;

import ml.karmaconfigs.api.bungee.KarmaFile;
import ml.karmaconfigs.locklogin.api.files.PluginConfiguration;
import ml.karmaconfigs.locklogin.api.encryption.CryptoUtil;
import ml.karmaconfigs.locklogin.api.utils.platform.CurrentPlatform;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.List;

import static ml.karmaconfigs.locklogin.plugin.bungee.LockLogin.plugin;

public class ScratchCodes {

    private final KarmaFile codesFile;

    /**
     * Initialize the scratch code utility
     *
     * @param _player the player to manage scratch codes who
     */
    public ScratchCodes(final ProxiedPlayer _player) {
        codesFile = new KarmaFile(plugin, _player.getUniqueId().toString().replace("-", "") + ".lldb", "data", ".codes");

        if (!codesFile.exists())
            codesFile.create();
    }

    /**
     * Store the user scratch codes
     *
     * @param scratch_codes the codes to store
     */
    public final void store(final List<Integer> scratch_codes) {
        List<String> codes = new ArrayList<>();
        PluginConfiguration config = CurrentPlatform.getConfiguration();
        for (int code : scratch_codes) {
            CryptoUtil util = CryptoUtil.getBuilder().withPassword(code).build();
            codes.add(util.hash(config.pinEncryption(), false));
        }

        codesFile.set("CODES", codes);
    }

    /**
     * Validate the scratch code
     *
     * @return if the scratch code is valid
     */
    public final boolean validate(final int code) {
        boolean status = false;

        List<String> codes = codesFile.getStringList("CODES");
        String remove = "";
        if (!codes.isEmpty()) {
            for (String token : codes) {
                CryptoUtil util = CryptoUtil.getBuilder().withPassword(code).withToken(token).build();
                if (util.validate()) {
                    remove = token;
                    status = true;

                    break;
                }
            }
        }

        if (!remove.replaceAll("\\s", "").isEmpty()) {
            codes.remove(remove);
            codesFile.set("CODES", codes);
        }

        return status;
    }

    /**
     * Check if the client needs new scratch codes
     *
     * @return if the client needs new scratch codes
     */
    public final boolean needsNew() {
        List<String> codes = codesFile.getStringList("CODES");
        return codes.isEmpty();
    }

    /**
     * Convert the numeric code into a string code
     *
     * @param code the number code
     * @return the string code
     * @deprecated no longer used, but may be util
     * in some future
     */
    @Deprecated
    private String codeToString(final int code) {
        String codeString = String.valueOf(code);
        StringBuilder codeBuilder = new StringBuilder();
        for (int i = 0; i < codeString.length(); i++) {
            String number = String.valueOf(codeString.charAt(i));

            switch (Integer.parseInt(number)) {
                case 0:
                    codeBuilder.append("A");
                    break;
                case 1:
                    codeBuilder.append("B");
                    break;
                case 2:
                    codeBuilder.append("C");
                    break;
                case 3:
                    codeBuilder.append("D");
                    break;
                case 4:
                    codeBuilder.append("E");
                    break;
                case 5:
                    codeBuilder.append("F");
                    break;
                case 6:
                    codeBuilder.append("G");
                    break;
                case 7:
                    codeBuilder.append("H");
                    break;
                case 8:
                    codeBuilder.append("I");
                    break;
                case 9:
                    codeBuilder.append("J");
                    break;
            }
        }

        return codeBuilder.toString();
    }
}
