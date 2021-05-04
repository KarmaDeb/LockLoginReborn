package ml.karmaconfigs.locklogin.plugin.bukkit.plugin.bungee.data;

import ml.karmaconfigs.locklogin.api.account.AccountID;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

public class AccountParser {

    private final String parsed;

    /**
     * Initialize the account parser
     *
     * @param data the data to parse
     */
    public AccountParser(final String data) {
        parsed = data;
    }

    /**
     * Parse the data
     *
     * @return the parsed data
     */
    @Nullable
    public final BungeeAccount parse() {
        try {
            String[] data = parsed.split(" \\$ ");

            String name = data[0];
            AccountID id = AccountID.fromTrimmed(data[1]);
            String password = data[2];
            String pin = data[3];
            String token = data[4];
            boolean gAuth = Boolean.getBoolean(data[5]);
            Instant created = Instant.parse(data[6].replace(":", ";"));

            return new BungeeAccount(name, id, password, pin, token, gAuth, created);
        } catch (Throwable ignored) {
        }

        return null;
    }
}
