package eu.locklogin.api.module.plugin.client.permission.plugin;
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
import eu.locklogin.api.module.plugin.client.permission.PermissionDefault;
import eu.locklogin.api.module.plugin.client.permission.PermissionObject;

public final class PluginPermissions {
    public static PermissionObject reload() {
        PermissionObject permission = new AdvancedPermission("locklogin.reload");
        permission.addChildren(new AdvancedPermission("locklogin.reload.config", PermissionDefault.FALSE, true));
        permission.addChildren(new AdvancedPermission("locklogin.reload.messages", PermissionDefault.FALSE, true));
        return permission;
    }
    public static PermissionObject reload_config() {
        PermissionObject permission = new AdvancedPermission("locklogin.reload.config", PermissionDefault.FALSE, true);
        permission.addParent(new AdvancedPermission("locklogin.reload"));
        return permission;
    }
    public static PermissionObject reload_messages() {
        PermissionObject permission = new AdvancedPermission("locklogin.reload.messages", PermissionDefault.FALSE, true);
        permission.addParent(new AdvancedPermission("locklogin.reload"));
        return permission;
    }
    public static PermissionObject location() {
        PermissionObject permission = new AdvancedPermission("locklogin.location");
        permission.addChildren(new AdvancedPermission("locklogin.location.spawn", PermissionDefault.FALSE, true));
        permission.addChildren(new AdvancedPermission("locklogin.location.client", PermissionDefault.FALSE, true));
        return permission;
    }
    public static PermissionObject location_spawn() {
        PermissionObject permission = new AdvancedPermission("locklogin.location.spawn", PermissionDefault.FALSE, true);
        permission.addParent(new AdvancedPermission("locklogin.location"));
        return permission;
    }
    public static PermissionObject location_client() {
        PermissionObject permission = new AdvancedPermission("locklogin.location.client", PermissionDefault.FALSE, true);
        permission.addParent(new AdvancedPermission("locklogin.location"));
        return permission;
    }
    public static PermissionObject info() {
        PermissionObject permission = new AdvancedPermission("locklogin.info");
        permission.addChildren(new AdvancedPermission("locklogin.info.request", PermissionDefault.FALSE, true));
        permission.addChildren(new AdvancedPermission("locklogin.info.alt", PermissionDefault.FALSE, true));
        permission.addChildren(new AdvancedPermission("locklogin.info.alt.alert", PermissionDefault.FALSE, false));
        return permission;
    }
    public static PermissionObject info_request() {
        PermissionObject permission = new AdvancedPermission("locklogin.info.request", PermissionDefault.OP, true);
        permission.addParent(new AdvancedPermission("locklogin.info"));
        return permission;
    }
    public static PermissionObject info_alt() {
        PermissionObject permission = new AdvancedPermission("locklogin.info.alt", PermissionDefault.OP, true);
        permission.addParent(new AdvancedPermission("locklogin.info"));
        permission.addChildren(new AdvancedPermission("locklogin.info.alt.alert", PermissionDefault.FALSE, true));
        return permission;
    }
    public static PermissionObject info_alt_alert() {
        PermissionObject permission = new AdvancedPermission("locklogin.info.alt.alert", PermissionDefault.FALSE, true);
        permission.addParent(new AdvancedPermission("locklogin.info.alt", PermissionDefault.FALSE, true));
        return permission;
    }
    public static PermissionObject account() {
        PermissionObject permission = new AdvancedPermission("locklogin.account");
        permission.addChildren(new AdvancedPermission("locklogin.account.close", PermissionDefault.FALSE, true));
        permission.addChildren(new AdvancedPermission("locklogin.account.remove", PermissionDefault.FALSE, true));
        permission.addChildren(new AdvancedPermission("locklogin.account.unlock", PermissionDefault.FALSE, true));
        permission.addParent(new AdvancedPermission("locklogin.account.switch", PermissionDefault.OP, false));

        return permission;
    }

    
        
        
    
    @@ -121,6 +122,13 @@ public static PermissionObject account_unlock() {
  
    public static PermissionObject account_close() {
        PermissionObject permission = new AdvancedPermission("locklogin.account.close", PermissionDefault.FALSE, true);
        permission.addParent(new AdvancedPermission("locklogin.account"));
        return permission;
    }
    public static PermissionObject account_remove() {
        PermissionObject permission = new AdvancedPermission("locklogin.account.remove", PermissionDefault.FALSE, true);
        permission.addParent(new AdvancedPermission("locklogin.account"));
        return permission;
    }
    public static PermissionObject account_unlock() {
        PermissionObject permission = new AdvancedPermission("locklogin.account.unlock", PermissionDefault.FALSE, true);
        permission.addParent(new AdvancedPermission("locklogin.account.unlock"));
        return permission;
    }

    public static PermissionObject session_switch() {
        PermissionObject permission = new AdvancedPermission("locklogin.account.switch", PermissionDefault.OP, false);
        permission.addParent(new AdvancedPermission("locklogin.account"));

        return permission;
    }

    public static PermissionObject force_2fa() {
        return new AdvancedPermission("locklogin.forcefa", PermissionDefault.OP);
    }

    
          
    
  
    public static PermissionObject alias() {
        return new AdvancedPermission("locklogin.alias", PermissionDefault.FALSE);
    }
    public static PermissionObject modules() {
        PermissionObject permission = new AdvancedPermission("locklogin.module");
        permission.addChildren(new AdvancedPermission("locklogin.module.reload", PermissionDefault.FALSE, false));
        permission.addChildren(new AdvancedPermission("locklogin.module.load", PermissionDefault.FALSE, true));
        permission.addChildren(new AdvancedPermission("locklogin.module.unload", PermissionDefault.FALSE, true));
        permission.addChildren(new AdvancedPermission("locklogin.module.list", PermissionDefault.FALSE, true));
        return permission;
    }
    public static PermissionObject module_reload() {
        PermissionObject permission = new AdvancedPermission("locklogin.module.reload", PermissionDefault.FALSE, false);
        permission.addParent(new AdvancedPermission("locklogin.module"));
        permission.addChildren(new AdvancedPermission("locklogin.module.load", PermissionDefault.FALSE, true));
        permission.addChildren(new AdvancedPermission("locklogin.module.unload", PermissionDefault.FALSE, true));
        return permission;
    }
    public static PermissionObject module_load() {
        PermissionObject permission = new AdvancedPermission("locklogin.module.load", PermissionDefault.FALSE, true);
        permission.addParent(new AdvancedPermission("locklogin.module"));
        permission.addParent(new AdvancedPermission("locklogin.module.reload"));
        return permission;
    }
    public static PermissionObject module_unload() {
        PermissionObject permission = new AdvancedPermission("locklogin.module.unload", PermissionDefault.FALSE, true);
        permission.addParent(new AdvancedPermission("locklogin.module"));
        permission.addParent(new AdvancedPermission("locklogin.module.reload"));
        return permission;
    }
    public static PermissionObject module_list() {
        PermissionObject permission = new AdvancedPermission("locklogin.module.list", PermissionDefault.FALSE, true);
        permission.addParent(new AdvancedPermission("locklogin.module"));
        return permission;
    }
    public static PermissionObject updater() {
        PermissionObject permission = new AdvancedPermission("locklogin.updater");
        permission.addChildren(new AdvancedPermission("locklogin.updater.apply", PermissionDefault.FALSE, true));
        permission.addChildren(new AdvancedPermission("locklogin.updater.version", PermissionDefault.OP, true));
        permission.addChildren(new AdvancedPermission("locklogin.updater.changelog", PermissionDefault.OP, true));
        permission.addChildren(new AdvancedPermission("locklogin.updater.check", PermissionDefault.FALSE, true));
        return permission;
    }
    public static PermissionObject updater_apply() {
        PermissionObject permission = new AdvancedPermission("locklogin.updater.apply", PermissionDefault.FALSE, true);
        permission.addParent(new AdvancedPermission("locklogin.updater"));
        return permission;
    }
    public static PermissionObject updater_version() {
        PermissionObject permission = new AdvancedPermission("locklogin.updater.version", PermissionDefault.OP, true);
        permission.addParent(new AdvancedPermission("locklogin.updater"));
        return permission;
    }
    public static PermissionObject updater_changelog() {
        PermissionObject permission = new AdvancedPermission("locklogin.updater.changelog", PermissionDefault.OP, true);
        permission.addParent(new AdvancedPermission("locklogin.updater"));
        return permission;
    }
    public static PermissionObject updater_check() {
        PermissionObject permission = new AdvancedPermission("locklogin.updater.check", PermissionDefault.FALSE, true);
        permission.addParent(new AdvancedPermission("locklogin.updater"));
        return permission;
    }
    public static PermissionObject join_limbo() {
        return new AdvancedPermission("locklogin.join.limbo");
    }
    public static PermissionObject web() {
        PermissionObject permission = new AdvancedPermission("locklogin.web");
        permission.addChildren(new AdvancedPermission("locklogin.web.log", PermissionDefault.FALSE, true));
        permission.addChildren(new AdvancedPermission("locklogin.web.sync", PermissionDefault.FALSE, true));
        permission.addChildren(new AdvancedPermission("locklogin.web.remove", PermissionDefault.FALSE, true));
        permission.addChildren(new AdvancedPermission("locklogin.web.execute", PermissionDefault.FALSE, true));
        return permission;
    }
    public static PermissionObject web_log() {
        PermissionObject permission = new AdvancedPermission("locklogin.web.log", PermissionDefault.FALSE, true);
        permission.addParent(new AdvancedPermission("locklogin.web"));
        return permission;
    }
    public static PermissionObject web_sync() {
        PermissionObject permission = new AdvancedPermission("locklogin.web.sync", PermissionDefault.FALSE, true);
        permission.addParent(new AdvancedPermission("locklogin.web"));
        return permission;
    }
    public static PermissionObject web_remove() {
        PermissionObject permission = new AdvancedPermission("locklogin.web.remove", PermissionDefault.FALSE, true);
        permission.addParent(new AdvancedPermission("locklogin.web"));
        return permission;
    }
    public static PermissionObject web_execute() {
        PermissionObject permission = new AdvancedPermission("locklogin.web.execute", PermissionDefault.FALSE, true);
        permission.addParent(new AdvancedPermission("locklogin.web"));
        return permission;
    }
}