package ml.karmaconfigs.locklogin.plugin.common.utils.enums;

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
        return countryLocalizer(communityLang).split("_")[0].toLowerCase();
    }
}
