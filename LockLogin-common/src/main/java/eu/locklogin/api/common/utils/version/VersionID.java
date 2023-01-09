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

import ml.karmaconfigs.api.common.version.updater.VersionCheckType;
import ml.karmaconfigs.api.common.version.updater.VersionResolver;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LockLogin version id
 */
public final class VersionID extends VersionResolver {

    private final static Pattern old_id_pattern = Pattern.compile("[aA-zZ]+:[0-9]+[0-9]+[0-9]+");
    private final static Pattern less_new_id_pattern = Pattern.compile("[aA-zZ]+-[0-9]+/[0-9]+/[0-9]+");
    private final static Pattern new_id_pattern = Pattern.compile("[aA-zZ]+-[0-9]+/[0-9]+/[0-9]+.[0-9]+");

    private final static Map<String, String> versionMap = new HashMap<>();

    private final String rootVersion;
    private final String keyName;

    /**
     * Initialize the version id
     *
     * @param root   the root version id
     * @param update the update name
     */
    public VersionID(final String root, final String update) {
        rootVersion = root;
        keyName = update;
    }

    /**
     * Transform the version into a version ID
     *
     * @return a new version id object which contains
     * the current version id string
     */
    public VersionID generate() {
        switch (getVersionType(rootVersion)) {
            case NUMBER:
                if (!versionMap.containsKey(keyName)) {
                    String[] version_data = rootVersion.split("\\.");
                    String build = version_data[0];
                    String build_number = version_data[1];
                    String release_number = version_data[2];

                    int sum = Integer.parseInt(build) + Integer.parseInt(build_number) + Integer.parseInt(release_number);

                    build = String.valueOf(Math.abs(Integer.parseInt(build) + sum));
                    build_number = String.valueOf(Math.abs(Integer.parseInt(build_number) + sum));
                    release_number = String.valueOf(Math.abs(Integer.parseInt(release_number) + sum));

                    String name_string = build + "/" + build_number + "/" + release_number;

                    versionMap.put(keyName, getOnlyUpper() + "-" + name_string + "." + sum);
                }
                break;
            case ID:
            case RESOLVABLE_ID:
            default:
                versionMap.put(keyName, rootVersion);
                break;
        }

        return this;
    }

    /**
     * Get the generated version ID
     *
     * @return the version iD
     */
    public String getVersionID() {
        if (versionMap.getOrDefault(keyName, null) == null || versionMap.getOrDefault(keyName, null).isEmpty())
            return generate().getVersionID();
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

    /**
     * Resolve the version id
     *
     * @param id the version
     * @return the resolved version id
     */
    @Override
    public String resolve(String id) {
        switch (getVersionType(id)) {
            case NUMBER:
                return id;
            case ID:
            case RESOLVABLE_ID:
            default:
                if (id.contains("-")) {
                    String versionID = id.split("-")[1];
                    if (versionID.contains(".")) {
                        String[] data = versionID.split("\\.");
                        versionID = data[0];
                        int rest = Integer.parseInt(data[1]);

                        StringBuilder builder = new StringBuilder();

                        for (String str : versionID.split("/")) {
                            int number = Integer.parseInt(str);
                            builder.append(Math.abs(number - rest)).append(".");
                        }

                        return builder.substring(0, builder.toString().length() - 1);
                    } else {
                        return versionID;
                    }
                } else {
                    return id.split(":")[1];
                }
        }
    }

    /**
     * Get the version type
     *
     * @param id the version id
     * @return the version type
     */
    private VersionCheckType getVersionType(final String id) {
        Matcher old_id_matcher = old_id_pattern.matcher(id);
        Matcher new_id_matcher = new_id_pattern.matcher(id);
        Matcher new_less_id_matcher = less_new_id_pattern.matcher(id);

        if (old_id_matcher.matches() || new_id_matcher.matches() || new_less_id_matcher.matches()) {
            return VersionCheckType.RESOLVABLE_ID;
        }

        return VersionCheckType.NUMBER;
    }
}
