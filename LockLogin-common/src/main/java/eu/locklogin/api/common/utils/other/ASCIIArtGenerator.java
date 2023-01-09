package eu.locklogin.api.common.utils.other;

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

import ml.karmaconfigs.api.common.karma.source.APISource;
import ml.karmaconfigs.api.common.karma.source.KarmaSource;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Ascii art generator
 */
@SuppressWarnings("unused")
public class ASCIIArtGenerator {

    public static final int ART_SIZE_SMALL = 12;
    public static final int ART_SIZE_MEDIUM = 18;
    public static final int ART_SIZE_LARGE = 24;
    public static final int ART_SIZE_HUGE = 32;
    private final static KarmaSource lockLogin = APISource.loadProvider("LockLogin");
    private static final String DEFAULT_ART_SYMBOL = "*";

    /**
     * Prints ASCII art for the specified text. For size, you can use predefined sizes or a custom size.
     * Usage - printTextArt("Hi",30,ASCIIArtFont.ART_FONT_SERIF,"@");
     *
     * @param color      the text color
     * @param artText    the text to generate
     * @param textHeight - Use a predefined size or a custom type
     * @param fontType   - Use one of the available fonts
     * @param artSymbol  - Specify the character for printing the ascii art
     */
    public final void print(String color, String artText, int textHeight, ASCIIArtFont fontType, String artSymbol) {
        try {
            String fontName = fontType.getValue();
            int imageWidth = findImageWidth(textHeight, artText, fontName);

            BufferedImage image = new BufferedImage(imageWidth, textHeight, BufferedImage.TYPE_INT_RGB);
            Graphics g = image.getGraphics();
            Font font = new Font(fontName, Font.BOLD, textHeight);
            g.setFont(font);

            Graphics2D graphics = (Graphics2D) g;
            graphics.drawString(artText, 0, getBaselinePosition(g, font));

            for (int y = 0; y < textHeight; y++) {
                StringBuilder sb = new StringBuilder();
                for (int x = 0; x < imageWidth; x++)
                    sb.append(image.getRGB(x, y) == Color.WHITE.getRGB() ? artSymbol : " ");
                if (sb.toString().trim().isEmpty())
                    continue;

                lockLogin.console().send(color + sb);
            }
        } catch (Throwable ex) {
            lockLogin.console().send(color + artText);
        }
    }

    /**
     * Convenience method for printing ascii text art.
     * Font default - Dialog,  Art symbol default - *
     *
     * @param artText    the text to generate
     * @param textHeight the text height
     */
    public final void print(String artText, int textHeight) {
        print("\u001B[0m", artText, textHeight, ASCIIArtFont.ART_FONT_DIALOG, DEFAULT_ART_SYMBOL);
    }

    /**
     * Using the Current font and current art text find the width of the full image
     *
     * @param textHeight the text height
     * @param artText    the text to generate
     * @param fontName   the text font name
     * @return the text width
     */
    private int findImageWidth(int textHeight, String artText, String fontName) {
        BufferedImage im = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics g = im.getGraphics();
        g.setFont(new Font(fontName, Font.BOLD, textHeight));
        return g.getFontMetrics().stringWidth(artText);
    }

    /**
     * Find where the text baseline should be drawn so that the characters are within image
     *
     * @param g    the graphics instance
     * @param font the text font
     * @return the baseline expected location
     */
    private int getBaselinePosition(Graphics g, Font font) {
        FontMetrics metrics = g.getFontMetrics(font);
        return metrics.getAscent() - metrics.getDescent();
    }

    public enum ASCIIArtFont {
        ART_FONT_DIALOG("Dialog"), ART_FONT_DIALOG_INPUT("DialogInput"),
        ART_FONT_MONO("Monospaced"), ART_FONT_SERIF("Serif"), ART_FONT_SANS_SERIF("SansSerif");

        private final String value;

        ASCIIArtFont(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
