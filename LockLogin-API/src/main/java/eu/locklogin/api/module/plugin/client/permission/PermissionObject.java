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

/**
 * LockLogin permission object
 */
public abstract class PermissionObject {

    /**
     * Get the permission
     *
     * @return the permission node
     */
    public abstract String getPermission();

    /**
     * Get the permission criteria
     *
     * @return the permission default
     */
    public abstract PermissionDefault getCriteria();
}
