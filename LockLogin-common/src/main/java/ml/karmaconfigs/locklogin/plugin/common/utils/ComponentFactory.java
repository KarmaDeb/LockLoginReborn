package ml.karmaconfigs.locklogin.plugin.common.utils;

import ml.karmaconfigs.api.common.utils.StringUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

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
    public final ComponentFactory hover(final String hoverText) {
        text.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(StringUtils.toColor(hoverText))));
        return this;
    }

    /**
     * Set the click action of the component
     *
     * @param action the click action
     * @param string the action parameter
     * @return this instance
     */
    public final ComponentFactory click(final ClickEvent.Action action, final String string) {
        text.setClickEvent(new ClickEvent(action, string));
        return this;
    }

    /**
     * Add extra component factories to this factory
     *
     * @param factories the factories to add
     * @return this instance
     */
    public final ComponentFactory addExtra(final ComponentFactory... factories) {
        for (ComponentFactory factory : factories)
            text.addExtra(factory.get());

        return this;
    }

    /**
     * Get the text component
     *
     * @return the component
     */
    public final TextComponent get() {
        return text;
    }
}
