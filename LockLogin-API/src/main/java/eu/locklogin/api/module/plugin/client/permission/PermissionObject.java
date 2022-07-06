package eu.locklogin.api.module.plugin.client.permission;

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

import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LockLogin permission object
 */
public abstract class PermissionObject {

    private final static Map<String, PermissionObject> registered_permissions = new ConcurrentHashMap<>();

    private final String permission;

    /**
     * Initialize the permission object
     *
     * @param node the permission node
     */
    public PermissionObject(final String node) {
        permission = node;

        PermissionObject registered = registered_permissions.getOrDefault(permission.toUpperCase().replace(".", "_"), null);
        if (registered == null) {
            registered_permissions.put(permission, this);
        } else {
            registered.getChildren().forEach(this::addChildren);
            registered.getParent().forEach(this::addParent);
        }
    }

    /**
     * Get the permission
     *
     * @return the permission node
     */
    public String getPermission() {
        return permission;
    }

    /**
     * Add a child to the permission
     *
     * @param permission the permission to add as children
     */
    public abstract void addChildren(final PermissionObject permission);

    /**
     * Add a parent to the permission
     *
     * @param permission the permission to add as parent
     */
    public abstract void addParent(final PermissionObject permission);

    /**
     * Get the children permissions
     *
     * @return the children permissions
     */
    public abstract Set<PermissionObject> getChildren();

    /**
     * Get the parent permissions
     *
     * @return the parent permissions
     */
    public abstract Set<PermissionObject> getParent();

    /**
     * Get the permission criteria
     *
     * @return the permission default
     */
    public abstract PermissionDefault getCriteria();

    /**
     * Get if the permission inherits from its parent
     *
     * @return if the permission inherits from its parent
     */
    public abstract boolean inheritsParent();

    /**
     * Get if the permission is a children of the parent
     * permission
     *
     * @param permission the parent permission
     * @return if the parent permission is children of the permission
     */
    public abstract boolean isChildOf(final PermissionObject permission);

    /**
     * Get if the specified permission is permissible
     *
     * @param player the player to check permission with
     * @return if the specified permission applies to this
     * one or vice-verse
     */
    public boolean isPermissible(final ModulePlayer player) {
        if (player.hasPermission(this))
            return true;

        if (inheritsParent()) {
            for (PermissionObject permission : getParent()) {
                if (player.hasPermission(permission))
                    return true;
            }
        }

        return false;
    }

    /**
     * Get all the registered permissions
     *
     * @return all the registered permissions
     */
    public static Set<String> getRegisteredPermissions() {
        return Collections.unmodifiableSet(registered_permissions.keySet());
    }

    /**
     * Get the registered permission
     *
     * @param node the registered permission node
     * @return the registered permission if exists
     */
    public static PermissionObject getRegisteredPermission(final String node) {
        return registered_permissions.getOrDefault(node, null);
    }
}
