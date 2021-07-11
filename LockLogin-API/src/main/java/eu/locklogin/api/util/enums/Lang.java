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
 * Valid LockLogin langs
 */
public enum Lang {
    /**
     * LockLogin english language
     */
    ENGLISH,
    /**
     * LockLogin spanish language
     */
    SPANISH,
    /**
     * LockLogin french language
     */
    FRENCH,
    /**
     * LockLogin german language
     */
    GERMAN,
    /**
     * LockLogin community language
     */
    COMMUNITY;

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
            case GERMAN:
                return "German";
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
            case GERMAN:
                return "de_DE";
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
