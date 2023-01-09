package eu.locklogin.plugin.bukkit.util.files;

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

import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.karma.file.yaml.KarmaYamlManager;
import ml.karmaconfigs.api.common.minecraft.rgb.RGBTextComponent;

import static eu.locklogin.plugin.bukkit.LockLogin.plugin;

public final class Message extends PluginMessages {

    /**
     * Initialize messages file
     */
    public Message() {
        super(plugin);
    }

    /**
     * Parse the message
     *
     * @param original the original string
     * @return the parsed message
     */
    @Override
    protected String parse(String original) {
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        RGBTextComponent component = new RGBTextComponent(true, true);
        if ((original.contains("<captcha>") || original.contains("{captcha}")) && !config.captchaOptions().isEnabled())
            original = original.replace("<captcha>", "").replace("{captcha}", "");

        return component.parse(original.replace("{ServerName}", config.serverName()))
                .replace("{newline}", "\n");
    }

    /**
     * Load the messages from the specified yaml
     * text
     *
     * @param yaml the yaml to load
     */
    @Override
    public void loadString(String yaml) {
        PluginConfiguration config = CurrentPlatform.getConfiguration();
        if (config.isBungeeCord()) {
            KarmaYamlManager bungee = new KarmaYamlManager(yaml, false);
            getManager().update(bungee, true);
        }
    }
}
