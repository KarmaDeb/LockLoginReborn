package eu.locklogin.api.common.web;

import ml.karmaconfigs.api.common.karma.KarmaAPI;
import ml.karmaconfigs.api.common.karma.source.APISource;
import ml.karmaconfigs.api.common.karma.source.KarmaSource;
import ml.karmaconfigs.api.common.logger.KarmaLogger;
import ml.karmaconfigs.api.common.logger.Logger;
import ml.karmaconfigs.api.common.string.ListTransformation;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.utils.JavaVM;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.enums.LogCalendar;
import ml.karmaconfigs.api.common.utils.enums.LogExtension;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PluginLogger extends KarmaLogger {

    /**
     * A map that contains source => calendar type
     */
    private static final Map<KarmaSource, LogCalendar> calendar_type = new ConcurrentHashMap<>();
    /**
     * A map that contains source => log file extension type
     */
    private static final Map<KarmaSource, LogExtension> ext_type = new ConcurrentHashMap<>();
    /**
     * A map that contains source => log header
     */
    private static final Map<KarmaSource, List<String>> header = new ConcurrentHashMap<>();

    private static final Map<KarmaSource, Integer> header_size = new ConcurrentHashMap<>();
    /**
     * A map that contains source => log scheduled for cleanup
     */
    private static final Map<KarmaSource, Boolean> locked = new ConcurrentHashMap<>();

    /**
     * The logger source
     */
    private final static KarmaSource source = APISource.loadProvider("LockLogin");

    /**
     * Initialize the logger
     */
    public PluginLogger() {
        super(source);

        List<String> stored = header.getOrDefault(source, new ArrayList<>());

        if (stored.isEmpty()) {
            List<String> header_text = new ArrayList<>();
            header_text.add("# System information<br>\n<br>\n");
            header_text.add(StringUtils.formatString("Os name: {0}<br>\n", JavaVM.osName()));
            header_text.add(StringUtils.formatString("Os version: {0}<br>\n", JavaVM.osVersion()));
            header_text.add(StringUtils.formatString("Os model: {0}<br>\n", JavaVM.osModel()));
            header_text.add(StringUtils.formatString("Os arch: {0}<br>\n", JavaVM.osArchitecture()));
            header_text.add(StringUtils.formatString("Os max memory: {0}<br>\n", JavaVM.osMaxMemory()));
            header_text.add(StringUtils.formatString("Os free memory: {0}<br>\n", JavaVM.osFreeMemory()));
            header_text.add("\n# VM information<br>\n<br>\n");
            header_text.add(StringUtils.formatString("Architecture: {0}<br>\n", JavaVM.jvmArchitecture()));
            header_text.add(StringUtils.formatString("Max memory: {0}<br>\n", JavaVM.jvmMax()));
            header_text.add(StringUtils.formatString("Free memory: {0}<br>\n", JavaVM.jvmAvailable()));
            header_text.add(StringUtils.formatString("Processors: {0}<br>\n", JavaVM.jvmProcessors()));
            header_text.add(StringUtils.formatString("Version: {0}<br>\n", JavaVM.javaVersion()));
            header_text.add("\n# API Information<br>\n");
            header_text.add(StringUtils.formatString("API Version: {0}<br>\n", KarmaAPI.getVersion()));
            header_text.add(StringUtils.formatString("API Compiler: {0}<br>\n", KarmaAPI.getCompilerVersion()));
            header_text.add(StringUtils.formatString("API Date: {0}<br>\n", KarmaAPI.getBuildDate()));
            header_text.add("\n# Source information<br>\n");
            header_text.add(StringUtils.formatString("Name: {0}<br>\n", source.name()));
            header_text.add(StringUtils.formatString("Version: {0}<br>\n", source.version()));
            header_text.add(StringUtils.formatString("Description: {0}<br>\n", source.description().replace("\n", "<br>")));
            header_text.add(StringUtils.formatString("Author(s): {0}<br>\n", source.authors(true, "<br>- ")));
            header_text.add(StringUtils.formatString("Update URL: {0}<br>\n", source.updateURL()));
            header_text.add("\n# Beginning of log<br><br>\n\n");

            header.put(source, header_text);
            header_size.put(source, header_text.size());
        } else {
            int index = 0;
            for (String line : stored) {
                boolean modified = false;

                switch (line) {
                    case "Os free memory":
                        line = "Os free memory: " + JavaVM.osFreeMemory() + "<br>\n";
                        modified = true;
                        break;
                    case "Free memory":
                        line = "Free memory: " + JavaVM.jvmAvailable() + "<br>\n";
                        modified = true;
                        break;
                    case "Version":
                        line = "Version: " + source.version() + "<br>\n";
                        modified = true;
                        break;
                    case "Update URL":
                        line = "Update URL: " + source.updateURL() + "<br>\n";
                        modified = true;
                        break;
                }
                if (modified) {
                    stored.set(index, line);
                }

                index++;
            }

            header.put(source, stored);
        }
    }

    /**
     * Append a header line to the header
     *
     * @param headerLine the header line
     * @return this instance
     */
    @Override
    public KarmaLogger appendHeader(final String headerLine) {
        List<String> lines = header.get(source);
        lines.add(lines.size() - 1, headerLine);
        header.put(source, lines);

        return this;
    }

    /**
     * Remove a header line from the header
     *
     * @param headerLine the header line index
     * @return this instance
     */
    @Override
    public KarmaLogger removeHeader(final int headerLine) {
        List<String> lines = header.get(source);

        int real_line = Math.min(Math.max(header_size.get(source) + 1, headerLine + header_size.get(source)), lines.size() - 1);
        lines.remove(real_line);

        header.put(source, lines);
        return this;
    }

    /**
     * Get the log header
     *
     * @return the log header
     */
    @Override
    public String getHeader() {
        return StringUtils.listToString(header.get(source), ListTransformation.NONE);
    }

    /**
     * Set the logger calendar type
     *
     * @param calendar the logger calendar
     * @return this instance
     */
    @SuppressWarnings("unused")
    public KarmaLogger calendar(LogCalendar calendar) {
        calendar_type.put(source, calendar);
        return this;
    }

    /**
     * Set the logger extension type
     *
     * @param extension the logger extension
     * @return this instance
     */
    @SuppressWarnings("unused")
    public KarmaLogger extension(LogExtension extension) {
        ext_type.put(source, extension);
        return this;
    }

    /**
     * Run the log function on a new
     * thread
     *
     * @param level    the log level
     * @param info     the info to log
     * @param replaces the info replaces
     */
    @Override
    public void scheduleLog(final @NotNull Level level, final @NotNull CharSequence info, final @NotNull Object... replaces) {
        source.async().queue("asynchronous_log", () -> logInfo(level, printInfo(), info, replaces));
    }

    /**
     * Run the log function on a new
     * thread
     *
     * @param level the log level
     * @param error the error to log
     */
    @Override
    public void scheduleLog(final @NotNull Level level, final @NotNull Throwable error) {
        source.async().queue("asynchronous_log", () -> logError(level, printError(), error));
    }

    /**
     * Run the log function on a new
     * thread
     *
     * @param level    the log level
     * @param print    print info to console
     * @param info     the info to log
     * @param replaces the info replaces
     */
    @Override
    public void scheduleLogOption(final Level level, final boolean print, final CharSequence info, final Object... replaces) {
        source.async().queue("asynchronous_log", () -> logInfo(level, print, info, replaces));
    }

    /**
     * Run the log function on a new
     * thread
     *
     * @param level the log level
     * @param print print info to console
     * @param error the error to log
     */
    @Override
    public void scheduleLogOption(final Level level, final boolean print, final Throwable error) {
        source.async().queue("asynchronous_log", () -> logError(level, print, error));
    }

    /**
     * Run the log function on the main
     * known thread
     *
     * @param level    the log level
     * @param info     the info to log
     * @param replaces the info replaces
     */
    @Override
    public void syncedLog(final Level level, final CharSequence info, final Object... replaces) {
        source.sync().queue("synchronous_log", () -> logInfo(level, printInfo(), info, replaces));
    }

    /**
     * Run the log function on the main
     * known thread
     *
     * @param level the log level
     * @param error the error to log
     */
    @Override
    public void syncedLog(final Level level, final Throwable error) {
        source.sync().queue("synchronous_log", () -> logError(level, printError(), error));
    }

    /**
     * Run the log function on the main
     * known thread
     *
     * @param level    the log level
     * @param print    print info to console
     * @param info     the info to log
     * @param replaces the info replaces
     */
    @Override
    public void syncedLogOption(final Level level, final boolean print, final CharSequence info, final Object... replaces) {
        source.sync().queue("synchronous_log", () -> logInfo(level, print, info, replaces));
    }

    /**
     * Run the log function on the main
     * known thread
     *
     * @param level the log level
     * @param print print info to console
     * @param error the error to log
     */
    @Override
    public void syncedLogOption(final Level level, final boolean print, final Throwable error) {
        source.sync().queue("synchronous_log", () -> logError(level, print, error));
    }

    /**
     * Log info
     *
     * @param level    the info level
     * @param print    print info to console
     * @param info     the info
     * @param replaces the info replaces
     */
    private void logInfo(final Level level, final boolean print, final CharSequence info, final Object... replaces) {
        if (!locked.getOrDefault(source, false)) {
            LogExtension extension = ext_type.getOrDefault(source, LogExtension.MARKDOWN);

            Path log = getLoggerFile(extension);
            String time = fetchTime(calendar_type.getOrDefault(source, LogCalendar.GREGORIAN));

            if (extension.equals(LogExtension.MARKDOWN)) {
                String[] timeData = time.split(":");

                StringBuilder formattedTime = new StringBuilder();
                for (int i = 0; i < timeData.length; i++) {
                    String unit = timeData[i];
                    formattedTime.append(unit).append((i != timeData.length - 1 ? ":" : ""));
                }

                time = formattedTime.toString();
            }

            try {
                List<String> lines = removeHeader(Files.readAllLines(log));
                BufferedWriter writer = Files.newBufferedWriter(log, StandardCharsets.UTF_8);
                writer.write(StringUtils.listToString(header.get(source), ListTransformation.NONE));
                for (String line : lines)
                    writer.write(line + "\n");

                writer.write(StringUtils.formatString("[ {0} - {1} ] {2}<br>", (extension.equals(LogExtension.MARKDOWN) ? level.getMarkdown() : level.name()), time, StringUtils.formatString(info, replaces)));
                writer.flush();
                writer.close();
            } catch (Throwable ex) {
                ex.printStackTrace();
            } finally {
                if (print) {
                    source.console().send(info, level);
                }
            }
        }
    }

    /**
     * Log error info
     *
     * @param level the error level
     * @param print print error info to console
     * @param error the error
     */
    private void logError(final Level level, final boolean print, final Throwable error) {
        if (!locked.getOrDefault(source, false)) {
            LogExtension extension = ext_type.getOrDefault(source, LogExtension.MARKDOWN);

            Path log = getLoggerFile(extension);
            String time = fetchTime(calendar_type.getOrDefault(source, LogCalendar.GREGORIAN));

            if (extension.equals(LogExtension.MARKDOWN)) {
                String[] timeData = time.split(":");

                StringBuilder formattedTime = new StringBuilder();
                for (int i = 0; i < timeData.length; i++) {
                    String unit = timeData[i];
                    formattedTime.append(unit).append((i != timeData.length - 1 ? ":" : ""));
                }

                time = formattedTime.toString();
            }

            try {
                List<String> lines = removeHeader(Files.readAllLines(log));
                BufferedWriter writer = Files.newBufferedWriter(log, StandardCharsets.UTF_8);
                writer.write(StringUtils.listToString(header.get(source), ListTransformation.NONE));
                for (String line : lines)
                    writer.write(line + "\n");

                Throwable prefix = new Throwable(error);
                writer.write(StringUtils.formatString("[ {0} - {1} ] {2}\n", (extension.equals(LogExtension.MARKDOWN) ? level.getMarkdown() : level.name()), time, prefix.fillInStackTrace()));
                writer.write("```java\n");
                for (StackTraceElement element : error.getStackTrace())
                    writer.write(element + "\n");
                writer.write("```");
                writer.flush();
                writer.close();
            } catch (Throwable ex) {
                ex.printStackTrace();
            } finally {
                Throwable prefix = new Throwable(error);

                if (print) {
                    source.console().send("An internal error occurred ( {0} )", level, prefix.fillInStackTrace());
                    for (StackTraceElement element : error.getStackTrace())
                        source.console().send(element.toString(), Level.INFO);
                }
            }
        }
    }

    /**
     * Clear the log file
     *
     * @throws IllegalStateException if the log file could not be
     *                               cleared
     */
    @Override
    public synchronized void clearLog() throws IllegalStateException {
        locked.put(source, true);
        source.async().queue("clear_log", () -> {
            Path logFile = getLoggerFile(ext_type.getOrDefault(source, LogExtension.MARKDOWN));

            try {
                BufferedWriter writer = Files.newBufferedWriter(logFile, StandardCharsets.UTF_8);
                writer.write("");

                writer.flush();
                writer.close();
            } catch (Throwable ex) {
                ex.printStackTrace();
                throw new IllegalStateException("Failed to clear log file ");
            } finally {
                locked.remove(source);
            }
        });
    }

    /**
     * Flush the log data if the
     * log auto flush is turned off
     * <p>
     * WARNING: This will replace all the log file
     * content, this should be used only for applications
     * that runs once -> generate a log file and then
     * switch log file. You can change the log file
     * by overriding {@link KarmaLogger#getLoggerFile(LogExtension)}
     * <p>
     * DOES NOTHING ON {@link Logger}
     *
     * @return if the log could be flushed
     */
    @Override
    public boolean flush() {
        return true;
    }

    /**
     * Remove the log file header
     *
     * @param lines the file lines
     * @return the log file lines without header
     */
    private List<String> removeHeader(final List<String> lines) {
        List<String> copy = new ArrayList<>();
        boolean begone = false;
        for (String line : lines) {
            if (begone) {
                copy.add(line);
                continue;
            }
            if (line.startsWith("# Beginning of log<br><br>"))
                begone = true;
        }
        if (copy.size() > 1 &&
                StringUtils.isNullOrEmpty(copy.get(0)) && StringUtils.isNullOrEmpty(copy.get(1)))
            copy.remove(0);

        return copy;
    }
}
