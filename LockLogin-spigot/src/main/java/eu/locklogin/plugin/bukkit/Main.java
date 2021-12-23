package eu.locklogin.plugin.bukkit;

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

import eu.locklogin.api.common.web.ChecksumTables;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.api.util.platform.Platform;
import ml.karmaconfigs.api.bukkit.KarmaPlugin;
import ml.karmaconfigs.api.common.utils.URLUtils;

import java.net.HttpURLConnection;
import java.net.URL;

public final class Main extends KarmaPlugin {

    private final MainBootstrap plugin;

    public Main() throws Throwable {
        super(false);

        CurrentPlatform.setMain(Main.class);
        CurrentPlatform.setPlatform(Platform.BUKKIT);
        CurrentPlatform.setOnline(getServer().getOnlineMode());

        ChecksumTables tables = new ChecksumTables();
        tables.checkTables();

        plugin = new MainBootstrap(this);
    }

    @Override
    public void enable() {
        plugin.enable();
    }

    @Override
    public void onDisable() {
        plugin.disable();
        stopTasks();
    }

    @Override
    public String name() {
        return getDescription().getName();
    }

    @Override
    public String version() {
        return getDescription().getVersion();
    }

    @Override
    public String description() {
        return getDescription().getDescription();
    }

    @Override
    public String[] authors() {
        return getDescription().getAuthors().toArray(new String[0]);
    }

    @Override
    public String updateURL() {
        String[] hosts = new String[]{
                "https://karmadev.es/locklogin/version/",
                "https://karmarepo.000webhostapp.com/locklogin/version/",
                "https://karmaconfigs.github.io/updates/LockLogin/version/"
        };

        URL host = null;
        for (String url : hosts) {
            String check;
            if (url.startsWith("https://karmadev.es")) {
                check = "https://karmadev.es/";
            } else {
                if (url.startsWith("https://karmarepo.000webhostapp.com")) {
                    check = "https://karmarepo.000webhostapp.com/";
                } else {
                    check = "https://karmaconfigs.github.io";
                }
            }

            int response = URLUtils.getResponseCode(check);
            if (response == HttpURLConnection.HTTP_OK) {
                host = URLUtils.getOrNull(url);
                if (host != null)
                    break;
            }
        }

        if (host != null) {
            PluginConfiguration config = CurrentPlatform.getConfiguration();
            if (config != null) {
                switch (config.getUpdaterOptions().getChannel()) {
                    case SNAPSHOT:
                        return host + "snapshot.kupdter";
                    case RC:
                        return host + "candidate.kupdter";
                    case RELEASE:
                    default:
                        return host + "release.kupdter";
                }
            }

            return host + "release.kupdter";
        }

        return null;
    }
}
