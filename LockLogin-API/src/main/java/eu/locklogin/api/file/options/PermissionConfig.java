package eu.locklogin.api.file.options;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Permissions configuration
 */
@AllArgsConstructor
public class PermissionConfig {

    @Getter
    @Accessors(fluent = true)
    private final boolean block_operator;

    @Getter
    @Accessors(fluent = true)
    private final boolean remove_permissions;

    @Getter
    @Accessors(fluent = true)
    private final boolean allow_wildcard;
}
