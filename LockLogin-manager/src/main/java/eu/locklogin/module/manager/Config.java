package eu.locklogin.module.manager;

import eu.locklogin.api.module.plugin.api.command.help.HelpPage;
import ml.karmaconfigs.api.common.karmafile.karmayaml.KarmaYamlManager;
import ml.karmaconfigs.api.common.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class Config {

    private final KarmaYamlManager manager = LockLoginManager.module.loadYaml("config.yml");

    public String getHeader(final int num) {
        List<String> messages = manager.getStringList("HelpHeader", "&5&m--------------&r&e LockLogin module commands &5&m--------------");
        List<String> parsed = new ArrayList<>();
        for (String str : messages)
            parsed.add(str
                    .replace("{page}", String.valueOf(num))
                    .replace("{pages}", String.valueOf(HelpPage.getPages())) + "&r");

        return StringUtils.listToString(parsed, false);
    }

    public String getFooter(final int num) {
        List<String> messages = manager.getStringList("HelpFooter", "&5&m--------------&r&e {page}&5/&e{pages} &5&m--------------");
        List<String> parsed = new ArrayList<>();
        for (String str : messages)
            parsed.add(str
                    .replace("{page}", String.valueOf(num))
                    .replace("{pages}", String.valueOf(HelpPage.getPages())) + "&r");

        return StringUtils.listToString(parsed, false);
    }

    public List<String> formatHelp(final HelpPage page) {
        List<String> format = manager.getStringList("HelpFormat", "&7{command} &d&m-&r &7{description}");

        List<String> formatted = new ArrayList<>();
        for (String help : page.getHelp()) {
            String[] data = help.split(StringUtils.escapeString(" | "));
            String cmd = data[0];
            String description = help.replaceFirst(StringUtils.escapeString(cmd + " | "), "");

            for (String f : format) {
                formatted.add(f
                        .replace("{command}", StringUtils.stripColor(cmd))
                        .replace("{description}", StringUtils.stripColor(description)) + "&r");
            }
        }

        return formatted;
    }
}
