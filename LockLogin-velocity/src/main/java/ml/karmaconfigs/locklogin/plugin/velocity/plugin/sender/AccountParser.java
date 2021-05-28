package ml.karmaconfigs.locklogin.plugin.velocity.plugin.sender;

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

import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.account.AccountManager;

import java.util.Set;

public final class AccountParser {

    private final Set<AccountManager> managers;

    public AccountParser(final Set<AccountManager> accounts) {
        managers = accounts;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AccountParser(");
        for (AccountManager account : managers) {
            builder
                    .append(account.getName()).append(" $ ")
                    .append(account.getUUID().getId()).append(" $ ")
                    .append(account.getPassword()).append(" $ ")
                    .append(account.getPin()).append(" $ ")
                    .append(account.getGAuth()).append(" $ ")
                    .append(account.has2FA()).append(" $ ")
                    .append(account.getCreationTime().toString().replace(";", ":")).append(";");
        }
        builder.append(")");

        return StringUtils.replaceLast(builder.toString(), ";", "");
    }
}