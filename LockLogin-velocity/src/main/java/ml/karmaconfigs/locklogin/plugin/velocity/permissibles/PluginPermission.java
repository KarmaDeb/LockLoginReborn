package ml.karmaconfigs.locklogin.plugin.velocity.permissibles;

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

public final class PluginPermission {

    public static Permission reload() {
        Permission permission = new Permission("locklogin.reload");
        permission.addNode("locklogin.reload.config", true, true);
        permission.addNode("locklogin.reload.messages", true, true);

        return permission;
    }

    public static Permission reload_config() {
        Permission permission = new Permission("locklogin.reload.config");
        permission.addNode("locklogin.reload", true, false);

        return permission;
    }

    public static Permission reload_messages() {
        Permission permission = new Permission("locklogin.reload.messages");
        permission.addNode("locklogin.reload", true, false);

        return permission;
    }

    public static Permission applyUpdates() {
        Permission permission = new Permission("locklogin.applyupdates");
        permission.addNode("locklogin.applyupdates.unsafe", false, true);

        return permission;
    }

    public static Permission applyUnsafeUpdates() {
        Permission permission = new Permission("locklogin.applyupdates.unsafe");
        permission.addNode("locklogin.applyupdates", true, false);

        return permission;
    }

    public static Permission infoRequest() {
        Permission permission = new Permission("locklogin.info");
        permission.addNode(playerInfo().getName(), false, true);
        permission.addNode(altInfo().getName(), false, true);

        return permission;
    }

    public static Permission playerInfo() {
        Permission permission = new Permission("locklogin.info.request");
        permission.addNode("locklogin.info", true, false);

        return permission;
    }

    public static Permission altInfo() {
        Permission permission = new Permission("locklogin.info.alt");
        permission.addNode("locklogin.info", false, true);
        permission.addNode("locklogin.info.alt.alert", true, false);

        return permission;
    }

    public static Permission altAlert() {
        Permission permission = new Permission("locklogin.info.alt.alert");
        permission.addNode("locklogin.info.alt", true, false);

        return permission;
    }

    public static Permission account() {
        Permission permission = new Permission("locklogin.account");
        permission.addNode("locklogin.account.close", true, true);
        permission.addNode("locklogin.account.remove", true, true);
        permission.addNode("locklogin.account.unlock", true, true);

        return permission;
    }

    public static Permission closeAccount() {
        Permission permission = new Permission("locklogin.account.close");
        permission.addNode("locklogin.account", true, false);

        return permission;
    }

    public static Permission delAccount() {
        Permission permission = new Permission("locklogin.account.remove");
        permission.addNode("locklogin.account", true, false);

        return permission;
    }

    public static Permission unlockAccount() {
        Permission permission = new Permission("locklogin.account.unlock");
        permission.addNode("locklogin.account", true, false);

        return permission;
    }

    public static Permission forceFA() {
        return new Permission("locklogin.forcefa");
    }

    public static Permission alias() {
        return new Permission("locklogin.alias");
    }

    public static Permission modules() {
        Permission permission = new Permission("locklogin.module");
        permission.addNode("locklogin.module.load", true, true);
        permission.addNode("locklogin.module.unload", true, true);
        permission.addNode("locklogin.module.list", true, true);

        return permission;
    }

    public static Permission loadModules() {
        Permission permission = new Permission("locklogin.module.load");
        permission.addNode("locklogin.updater", true, false);

        return permission;
    }

    public static Permission unloadModules() {
        Permission permission = new Permission("locklogin.module.unload");
        permission.addNode("locklogin.updater", true, false);

        return permission;
    }

    public static Permission listModules() {
        Permission permission = new Permission("locklogin.module.list");
        permission.addNode("locklogin.updater", true, false);

        return permission;
    }

    public static Permission updater() {
        Permission permission = new Permission("locklogin.updater");
        permission.addNode("locklogin.updater.version", true, true);
        permission.addNode("locklogin.updater.changelog", true, true);
        permission.addNode("locklogin.updater.check", true, true);

        return permission;
    }

    public static Permission version() {
        Permission permission = new Permission("locklogin.updater.version");
        permission.addNode("locklogin.updater", true, false);

        return permission;
    }

    public static Permission changelog() {
        Permission permission = new Permission("locklogin.updater.changelog");
        permission.addNode("locklogin.updater", true, false);

        return permission;
    }

    public static Permission check() {
        Permission permission = new Permission("locklogin.updater.check");
        permission.addNode("locklogin.updater", true, false);

        return permission;
    }
}
