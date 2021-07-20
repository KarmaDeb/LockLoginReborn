package eu.locklogin.api.module.plugin.client;

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
 * ModulePlayer title sender, this
 * contains the module player who should see
 * the message, and the title
 */
public class TitleSender {

    private final ModulePlayer target;
    private final String title;
    private final String subtitle;
    private final int fadeOut;
    private final int keepIn;
    private final int hideIn;

    /**
     * Initialize the title sender
     *
     * @param tar the target
     * @param t the title
     * @param s the subtitle
     * @param fo the time before showing the title
     * @param ki the time to show the title
     * @param hi the time that will take to hide the title
     */
    public TitleSender(final ModulePlayer tar, final String t, final String s, final int fo, final int ki, final int hi) {
        target = tar;
        title = t;
        subtitle = s;
        fadeOut = fo;
        keepIn = ki;
        hideIn = hi;
    }

    /**
     * Get the message target
     *
     * @return the message player
     */
    public final ModulePlayer getPlayer() {
        return target;
    }

    /**
     * Get the title
     *
     * @return the title
     */
    public final String getTitle() {
        return title;
    }

    /**
     * Get the subtitle
     *
     * @return the subtitle
     */
    public final String getSubtitle() {
        return subtitle;
    }

    /**
     * Get the time before showing the title
     *
     * @return the time before showing the title
     */
    public final int getFadeOut() {
        return fadeOut;
    }

    /**
     * Get the time to show the title
     *
     * @return the time to show the title
     */
    public final int getKeepIn() {
        return keepIn;
    }

    /**
     * Get the time that will take to hide the title
     *
     * @return the time that will take to hide the title
     */
    public final int getHideIn() {
        return hideIn;
    }
}
