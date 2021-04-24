package ml.karmaconfigs.locklogin.plugin.common.utils.version;

import java.util.*;

public final class VersionID {

    private final String rootVersion;
    private final String keyName;

    private final static Map<String, String> versionMap = new HashMap<>();

    /**
     * Initialize the version id
     *
     * @param root the root version id
     * @param update the update name
     */
    public VersionID(final String root, final String update) {
        rootVersion = root;
        keyName = update;
    }

    /**
     * Transform the version into a version ID
     */
    public final VersionID generate() {
        if (!versionMap.containsKey(keyName)) {
            String[] version_data = rootVersion.split("\\.");
            String version_build = version_data[0] + version_data[1];
            Set<Integer> rest = new HashSet<>();
            for (int i = 2; i < version_data.length; i++) {
                int id = Integer.parseInt(version_data[i]);
                rest.add(id);
            }

            String version_id = "";
            for (int id : rest) {
                if (!version_id.isEmpty()) {
                    int actual = Integer.parseInt(version_id);
                    version_id = String.valueOf(Math.abs(id + actual));
                    continue;
                }
                version_id = String.valueOf(id);
            }

            if (version_id.charAt(0) == '0')
                version_id = "Z" + version_id.substring(1);

            String name_string = version_build + version_id.replace("Z", "0");

            versionMap.put(keyName, keyName.substring(0, 2).toUpperCase() + ":" + name_string);
        }

        return this;
    }

    /**
     * Get the generated version ID
     *
     * @return the version iD
     */
    public final String get() {
        return versionMap.get(keyName);
    }
}
