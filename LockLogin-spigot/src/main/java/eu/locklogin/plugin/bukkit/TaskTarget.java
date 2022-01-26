package eu.locklogin.plugin.bukkit;

import java.util.UUID;

public enum TaskTarget {
    COMMAND_EXECUTE("useCommand"),
    COMMAND_FORCE("forceCommand"),
    EVENT("eventAction"),
    TELEPORT("clientTeleport"),
    DATA_SAVE("dataStore"),
    DATA_LOAD("dataLoad"),
    CACHE("dataPreCache"),
    VISION_CHECK("visibilityCheck"),
    VISION_TOGGLE("visibilityToggleOnOff"),
    PLUGIN_HOOK("pluginAPIHook"),
    INVENTORY("inventoryAction"),
    METADATA("objectMetadata"),
    MODE_SWITCH("gamemodeSwitch"),
    POTION_EFFECT("entityEffect"),
    KICK("playerDisconnectForce");

    private final String action;

    TaskTarget(final String name) {
        action = name;
    }

    public final String getTaskName() {
        return action;
    }

    public final UUID getTaskId() {
        return UUID.nameUUIDFromBytes(("Task:" + action).getBytes());
    }

    public final String errorCode(final Throwable reason) {
        StringBuilder codeBuilder = new StringBuilder("EC-");
        String reasonStr = reason.toString();
        for (int i = 0; i < reasonStr.length(); i++) {
            char character = reasonStr.charAt(i);
            codeBuilder.append(Integer.toString(character, 32));
        }

        return codeBuilder.toString();
    }

    public static TaskTarget forName(final String name) {
        switch (name.toLowerCase()) {
            case "usecommand":
                return COMMAND_EXECUTE;
            case "forcecommand":
                return COMMAND_FORCE;
            case "eventaction":
                return EVENT;
            case "clientteleport":
                return TELEPORT;
            case "datastore":
                return DATA_SAVE;
            case "dataload":
                return DATA_LOAD;
            case "dataprecache":
                return CACHE;
            case "visibilitycheck":
                return VISION_CHECK;
            case "visibilitytoggleonoff":
                return VISION_TOGGLE;
            case "pluginapihook":
                return PLUGIN_HOOK;
            case "inventoryaction":
                return INVENTORY;
            case "objectmetadata":
                return METADATA;
            case "gamemodeSwitch":
                return MODE_SWITCH;
            case "entityeffect":
                return POTION_EFFECT;
            case "playerdisconnectforce":
                return KICK;
        }

        return null;
    }
}
