package eu.locklogin.api.util.enums;

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
 * Valid LockLogin languages
 */
public enum Lang {
    /**
     * LockLogin English language
     */
    ENGLISH(true, "plugin_messages"),
    /**
     * LockLogin Spanish language
     */
    SPANISH(),
    /**
     * LockLogin French language
     */
    FRENCH(),
    /**
     * LockLogin Chinese simplified language
     */
    CHINESE_SIMPLIFIED(),
    /**
     * LockLogin Hebrew language
     */
    HEBREW(),
    /**
     * LockLogin Polish language
     */
    POLISH(true, "plugin_messages_polish"),
    /**
     * LockLogin Turkish language
     */
    TURKISH(true, "plugin_messages_turkish"),
    /**
     * LockLogin community language
     */
    COMMUNITY();

    private boolean include_properties = false;
    private String properties_name = "";

    /**
     * Initialize the language
     */
    Lang() {
        this(false, "");
    }

    /**
     * Initialize the language
     *
     * @param props if the language include properties
     */
    Lang(final boolean props, final String props_name) {
        include_properties = props;
        properties_name = props_name;
    }

    /**
     * Get if the language file includes properties
     *
     * @return if the language file includes properties
     */
    public boolean includeProperties() {
        return include_properties;
    }

    /**
     * Get the internal properties file name
     *
     * @return the internal properties file name
     */
    public String propertiesName() {
        return properties_name;
    }

    /**
     * Get the language friendly name
     *
     * @param communityLang the community language name
     * @return the language friendly name
     */
    public String friendlyName(final String communityLang) {
        switch (this) {
            case ENGLISH:
                return "English";
            case SPANISH:
                return "Spanish";
            case FRENCH:
                return "French";
            case CHINESE_SIMPLIFIED:
                return "Chinese ( Simplified )";
            case HEBREW:
                return "Hebrew";
            case POLISH:
                return "Polish";
            case COMMUNITY:
            default:
                return "Community ( " + communityLang + " )";
        }
    }

    /**
     * Get the country localizer
     *
     * @param communityLang the community language name
     * @return the country localizer
     */
    public String countryLocalizer(final String communityLang) {
        switch (this) {
            case ENGLISH:
                return "en_EN";
            case SPANISH:
                return "es_ES";
            case FRENCH:
                return "fr_FR";
            case CHINESE_SIMPLIFIED:
                return "zh_CN";
            case HEBREW:
                return "he_IL";
            case COMMUNITY:
            default:
                return "community_" + communityLang;
        }
    }

    /**
     * Get the country of the country localizer
     *
     * @param communityLang the community language name
     * @return the country
     */
    public String country(final String communityLang) {
        if (this.equals(COMMUNITY)) {
            return countryLocalizer(communityLang).replaceFirst("community_", "");
        } else {
            return countryLocalizer(communityLang).split("_")[0].toLowerCase();
        }
    }
}
