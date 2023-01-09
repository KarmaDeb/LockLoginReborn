package eu.locklogin.api.common.utils.plugin;

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

import ml.karmaconfigs.api.common.string.StringUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

/**
 * BungeeCord component factory
 */
@SuppressWarnings("unused")
public final class ComponentFactory {

    private final TextComponent text;

    /**
     * Initialize the component factory
     *
     * @param message the component message
     */
    public ComponentFactory(final String message) {
        text = new TextComponent(StringUtils.toColor(message));
    }

    /**
     * Set the over text of the component
     *
     * @param hoverText the hover text
     * @return this instance
     */
    @SuppressWarnings("deprecation")
    public ComponentFactory hover(final String hoverText) {
        try {
            text.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(StringUtils.toColor(hoverText))));
        } catch (Throwable ex) {
            try {
                text.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder().append(StringUtils.toColor(hoverText)).create()));
            } catch (Throwable e) {
                text.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(StringUtils.toColor(hoverText))));
            }
        }
        return this;
    }

    /**
     * Set the click action of the component
     *
     * @param action the click action
     * @param string the action parameter
     * @return this instance
     */
    public ComponentFactory click(final ClickEvent.Action action, final String string) {
        text.setClickEvent(new ClickEvent(action, string));
        return this;
    }

    /**
     * Add extra component factories to this factory
     *
     * @param factories the factories to add
     */
    public void addExtra(final ComponentFactory... factories) {
        for (ComponentFactory factory : factories)
            text.addExtra(factory.get());
    }

    /**
     * Get the text component
     *
     * @return the component
     */
    public TextComponent get() {
        return text;
    }
}
