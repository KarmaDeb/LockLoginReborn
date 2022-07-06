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
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class AdvancedPermission extends PermissionObject {

    private final Set<PermissionObject> nodes_child = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<PermissionObject> nodes_parent = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final PermissionDefault def;
    private final boolean inherits;

    /**
     * Initialize the plugin permission
     *
     * @param name the permission name
     */
    public AdvancedPermission(final String name) {
        super(name);

        def = PermissionDefault.FALSE;
        inherits = false;
    }

    /**
     * Initialize the plugin permission
     *
     * @param name the permission name
     * @param criteria the permission criteria
     */
    public AdvancedPermission(final String name, final PermissionDefault criteria) {
        super(name);

        def = criteria;
        inherits = false;
    }

    /**
     * Initialize the plugin permission
     *
     * @param name the permission name
     * @param criteria the permission criteria
     * @param inheritance if the user has the parent permission apply also this one
     */
    public AdvancedPermission(final String name, final PermissionDefault criteria, final boolean inheritance) {
        super(name);

        def = criteria;
        inherits = inheritance;
    }

    /**
     * Add a child to the permission
     *
     * @param permission the permission to add as children
     */
    @Override
    public void addChildren(final @NotNull PermissionObject permission) {
        permission.addParent(this);
        nodes_child.add(permission);
    }

    /**
     * Add a parent to the permission
     *
     * @param permission the permission to add as parent
     */
    @Override
    public void addParent(final @NotNull PermissionObject permission) {
        nodes_parent.add(permission);
    }

    /**
     * Get the children permissions
     *
     * @return the children permissions
     */
    @Override
    public Set<PermissionObject> getChildren() {
        return Collections.unmodifiableSet(nodes_child);
    }

    /**
     * Get the parent permissions
     *
     * @return the parent permissions
     */
    @Override
    public Set<PermissionObject> getParent() {
        return Collections.unmodifiableSet(nodes_parent);
    }

    /**
     * Get the permission criteria
     *
     * @return the permission default
     */
    @Override
    public PermissionDefault getCriteria() {
        return def;
    }

    /**
     * Get if the permission inherits from its parent
     *
     * @return if the permission inherits from its parent
     */
    @Override
    public boolean inheritsParent() {
        return inherits;
    }

    /**
     * Get if the permission is a children of the parent
     * permission
     *
     * @param permission the parent permission
     * @return if the parent permission is children of the permission
     */
    @Override
    public boolean isChildOf(final PermissionObject permission) {
        for (PermissionObject p : nodes_parent)
            if (p.getPermission().equals(permission.getPermission()))
                return true;

        return false;
    }
}
