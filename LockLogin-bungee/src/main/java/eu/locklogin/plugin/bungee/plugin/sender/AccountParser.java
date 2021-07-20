package eu.locklogin.plugin.bungee.plugin.sender;

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

import eu.locklogin.api.account.AccountManager;
import ml.karmaconfigs.api.common.utils.StringUtils;

import java.util.Set;

public final class AccountParser {

    private final Set<AccountManager> managers;

    public AccountParser(final Set<AccountManager> accounts) {
        managers = accounts;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (AccountManager account : managers)
            builder.append(account.getUUID().getId()).append(";");

        return StringUtils.replaceLast(builder.toString(), ";", "");
    }
}
