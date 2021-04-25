package ml.karmaconfigs.locklogin.plugin.common.utils.plugin;

import ml.karmaconfigs.api.common.utils.FileUtilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public final class Messages {

    public final String getProperty(final String name, final String def) {
        File propFile = new File(FileUtilities.getPluginsFolder() + File.separator + "LockLogin", "lang/plugin_messages.properties");

        try {
            if (propFile.exists()) {
                FileInputStream in = new FileInputStream(propFile);

                Properties properties = new Properties();
                properties.load(in);

                return properties.getProperty(name, def);
            } else {
                InputStream in = getClass().getResourceAsStream("/lang/plugin_messages.properties");
                if (in != null) {
                    Files.copy(in, propFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    Properties properties = new Properties();
                    properties.load(in);

                    return properties.getProperty(name, def);
                }
            }
        } catch (Throwable ignored) {}

        return "";
    }
}
