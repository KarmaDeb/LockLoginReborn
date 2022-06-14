package eu.locklogin.api.common.web.editor.task;

/**
 * Known editor task operations
 */
public enum EditorTask {
    /**
     * No task
     */
    NONE,
    /**
     * Force new user register
     */
    ADD_USER,
    /**
     * Remove a user
     */
    DEL_USER,
    /**
     * Modify a user
     */
    MOD_USER,
    /**
     * Modify the plugin configuration
     */
    MOD_CONF,
    /**
     * Modify the plugin messages
     */
    MOD_LANG,
    /**
     * Execute a locklogin command
     */
    EXEC_CMD,
}
