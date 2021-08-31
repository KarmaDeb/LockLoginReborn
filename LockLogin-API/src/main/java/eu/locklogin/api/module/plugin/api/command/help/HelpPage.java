package eu.locklogin.api.module.plugin.api.command.help;

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

import eu.locklogin.api.module.plugin.api.command.CommandData;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.util.platform.CurrentPlatform;

import java.util.*;

/**
 * LockLogin help page
 */
public final class HelpPage {

    private final static Map<Integer, Set<String>> pages = new LinkedHashMap<>();

    private final int current_page;

    /**
     * Initialize the help page
     *
     * @param page the page
     */
    public HelpPage(final int page) {
        current_page = page;
    }

    /**
     * Get all the available pages
     *
     * @return the help pages amount
     */
    public static int getPages() {
        return Math.min(pages.size() - 1, 0);
    }

    /**
     * Remove commands from unloaded
     * modules
     */
    private void removeUnloaded() {
        for (int page : pages.keySet()) {
            Set<String> lines = pages.getOrDefault(page, new LinkedHashSet<>());
            Set<String> remove = new LinkedHashSet<>();
            for (String line : lines) {
                String command = line.split(" ")[0];
                if (!ModulePlugin.parseCommand(command)) {
                    remove.add(line);
                }
            }

            lines.removeAll(remove);
            pages.put(page, lines);
        }
    }

    /**
     * Scan for command data and fill pages
     */
    public void scan() {
        removeUnloaded();
        Set<CommandData> tmpData = ModulePlugin.getCommandsData();

        List<CommandData> data = new ArrayList<>(tmpData);
        data.sort(Comparator.comparing(o -> o.getArguments()[0]));

        StringBuilder builder = new StringBuilder();
        for (CommandData cmd_data : data) {
            int lower_page = getLowerPage();
            Set<String> info = pages.getOrDefault(lower_page, new LinkedHashSet<>());

            builder.append(CurrentPlatform.getPrefix());
            for (String arg : cmd_data.getArguments())
                builder.append(arg).append(" | ");

            builder.append(cmd_data.getDescription());

            info.add(builder.toString());
            pages.put(lower_page, info);
            builder = new StringBuilder();
        }
    }

    /**
     * Get the help pages
     *
     * @return the help pages
     */
    public Set<String> getHelp() {
        return new LinkedHashSet<>(pages.getOrDefault(current_page, new LinkedHashSet<>()));
    }

    /**
     * Get the page with less info
     *
     * @return the lowest help page
     */
    private int getLowerPage() {
        int last_page = 0;
        for (int page : pages.keySet()) {
            Set<String> info = pages.getOrDefault(page, new LinkedHashSet<>());

            if (info.size() < 7) {
                last_page = page;
                break;
            }
        }

        return last_page;
    }

    /**
     * Get and update all the pages command
     * prefix in case the module command prefix has changed
     */
    public static void updatePagesPrefix() {
        for (int page : pages.keySet()) {
            List<String> info = new ArrayList<>(new LinkedHashSet<>(pages.getOrDefault(page, new LinkedHashSet<>())));
            for (int i = 0; i < info.size(); i++) {
                info.set(i, CurrentPlatform.getPrefix() + info.get(i).substring(1));
            }
            pages.put(page, new LinkedHashSet<>(info));
        }
    }
}
