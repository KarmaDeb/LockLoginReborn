package eu.locklogin.api.common.utils.version;

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

import java.util.HashMap;
import java.util.Map;

/**
 * LockLogin version id
 */
public class NonTested {

    private final static Map<String, String> versionMap = new HashMap<>();
    private final String rootVersion;
    private final String keyName;

    /**
     * Initialize the version id
     *
     * @param root   the root version id
     * @param update the update name
     */
    public NonTested(final String root, final String update) {
        rootVersion = root;
        keyName = update;
    }

    /**
     * Transform the version into a version ID
     *
     * @return a new version id object which contains
     * the current version id string
     */
    public final NonTested generate() {
        if (!versionMap.containsKey(keyName)) {
            String[] version_data = rootVersion.split("\\.");
            String build = version_data[0];
            String build_number = version_data[1];
            String release_number = version_data[2];

            int sum = Integer.parseInt(build) + Integer.parseInt(build_number) + Integer.parseInt(release_number);

            build = String.valueOf(Math.abs(Integer.parseInt(build) + sum));
            build_number = String.valueOf(Math.abs(Integer.parseInt(build_number) + sum));
            release_number = String.valueOf(Math.abs(Integer.parseInt(release_number) + sum));

            String name_string = build + "\u200b" + build_number + "\u200b" + release_number;

            versionMap.put(keyName, getOnlyUpper() + "-" + name_string + ":" + sum);
        }

        return this;
    }

    /**
     * Get the version from the version ID 
     */
    public final String getVersion() {
        String versionID = rootVersion.split("-")[1].split(":")[0];
        String[] data = versionID.split("\u200b");
        int rest = Integer.parseInt(rootVersion.split("-")[1].split(":")[1]);
        
        StringBuilder builder = new StringBuilder();
        
        for (String str : data) {
            int number = Integer.parseInt(str);
            builder.append(Math.abs(number - rest)).append(".");
        }
        
        return builder.toString().substring(0, builder.toString().length() - 1);
    }

    /**
     * Get the generated version ID
     *
     * @return the version iD
     */
    public final String get() {
        if (versionMap.getOrDefault(keyName, null) == null || versionMap.getOrDefault(keyName, null).isEmpty())
            return generate().get();
        else
            return versionMap.get(keyName);
    }

    /**
     * Get only the upper letters of
     * the update name
     */
    private String getOnlyUpper() {
        StringBuilder upperBuilder = new StringBuilder();
        for (int i = 0; i < keyName.length(); i++) {
            char character = keyName.charAt(i);
            if (Character.isUpperCase(character)) {
                upperBuilder.append(character);
            }
        }
        
        String result = upperBuilder.toString();
        if (result.isEmpty()) {
            return keyName.substring(0, 2).toUpperCase();
        } else {
            return result;
        }
    }
    
    public static void main(String[] args) {
        VersionID id = new VersionID("1.12.13", "RemasteringUpdate").generate();
        String idVersion = id.get();
        
        System.out.println(idVersion);
        
        VersionID resolved = new VersionID(idVersion, "RemateringUpdate");
        System.out.println(resolved.getVersion());
    }
}
