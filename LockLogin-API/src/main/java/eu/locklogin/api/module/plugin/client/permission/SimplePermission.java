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

import java.util.HashSet;
import java.util.Set;

/**
 * LockLogin permission object
 */
public class SimplePermission extends PermissionObject {

    private final PermissionDefault criteria;

    /**
     * Initialize the permission
     *
     * @param perm the permission
     * @param cr the permission criteria
     */
    public SimplePermission(final String perm, final PermissionDefault cr) {
        super(perm);

        criteria = cr;
    }

    /**
     * Add a child to the permission
     *
     * @param permission the permission to add as children
     */
    @Override
    public void addChildren(PermissionObject permission) {}

    /**
     * Add a parent to the permission
     *
     * @param permission the permission to add as parent
     */
    @Override
    public void addParent(PermissionObject permission) {}

    /**
     * Get the children permissions
     *
     * @return the children permissions
     */
    @Override
    public Set<PermissionObject> getChildren() {
        return new HashSet<>();
    }

    /**
     * Get the parent permissions
     *
     * @return the parent permissions
     */
    @Override
    public Set<PermissionObject> getParent() {
        return new HashSet<>();
    }

    /**
     * Get the permission criteria
     *
     * @return the permission default
     */
    @Override
    public PermissionDefault getCriteria() {
        return criteria;
    }

    /**
     * Get if the permission inherits from its parent
     *
     * @return if the permission inherits from its parent
     */
    @Override
    public boolean inheritsParent() {
        return false;
    }

    /**
     * Get if the permission is a children of the parent
     * permission
     *
     * @param permission the parent permission
     * @return if the parent permission is children of the permission
     */
    @Override
    public boolean isChildOf(PermissionObject permission) {
        return false;
    }
}
