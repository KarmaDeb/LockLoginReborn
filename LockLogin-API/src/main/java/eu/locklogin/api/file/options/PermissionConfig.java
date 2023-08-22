package eu.locklogin.api.file.options;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Permissions configuration
 */
@Getter
@AllArgsConstructor
public class PermissionConfig {

    @Accessors(fluent = true)
    private final boolean block_operator;

    @Accessors(fluent = true)
    private final boolean remove_permissions;
    
    @Accessors(fluent = true)
    private final boolean allow_wildcard;
}
