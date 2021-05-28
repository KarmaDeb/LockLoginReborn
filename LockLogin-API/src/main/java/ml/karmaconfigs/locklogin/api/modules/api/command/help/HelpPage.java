package ml.karmaconfigs.locklogin.api.modules.api.command.help;

import ml.karmaconfigs.locklogin.api.modules.util.javamodule.JavaModuleManager;
import ml.karmaconfigs.locklogin.api.modules.api.command.CommandData;
import ml.karmaconfigs.locklogin.api.utils.platform.CurrentPlatform;

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
     * Scan for command data and fill pages
     */
    public final void scan() {
        Set<CommandData> tmpData = JavaModuleManager.getCommandsData();

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
    public final Set<String> getHelp() {
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
