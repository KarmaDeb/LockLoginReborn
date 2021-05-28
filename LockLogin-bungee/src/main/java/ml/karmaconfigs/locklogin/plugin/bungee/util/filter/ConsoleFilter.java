package ml.karmaconfigs.locklogin.plugin.bungee.util.filter;

import ml.karmaconfigs.locklogin.api.modules.api.command.CommandData;
import ml.karmaconfigs.locklogin.api.modules.util.javamodule.JavaModuleManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.message.Message;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
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
@PluginFilter
public class ConsoleFilter implements Filter {

    private static final Set<String> SENSITIVE_COMMANDS = new HashSet<>();

    static {
        for (CommandData command : JavaModuleManager.getCommandsData()) {
            if (command.getOwner().isSensitive()) {
                SENSITIVE_COMMANDS.addAll(Arrays.asList(command.getArguments()));
            }
        }
    }

    public ConsoleFilter(final Set<String> commands) {
        SENSITIVE_COMMANDS.addAll(commands);
    }

    /**
     * Check if the console message is sensitive
     * and should be blocked
     *
     * @param msg the console message
     * @return if the console message should be blocked
     */
    private boolean isSensitiveMessage(String msg) {
        if (msg == null) return false;

        msg = msg.toLowerCase();

        return SENSITIVE_COMMANDS.stream().anyMatch(msg::contains);
    }

    public final Result filter(LogEvent record) {
        if (record != null) {
            Message message = record.getMessage();
            if (message != null && isSensitiveMessage(message.getFormattedMessage())) {
                return Result.DENY;
            }
        }

        return Result.NEUTRAL;
    }

    public final Result filter(Logger arg0, Level arg1, Marker arg2, String message, Object... arg4) {
        return isSensitiveMessage(message) ? Result.DENY : Result.NEUTRAL;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0) {
        if (isSensitiveMessage(message)) {
            return Result.DENY;
        }
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1) {
        if (isSensitiveMessage(message)) {
            return Result.DENY;
        }
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2) {
        if (isSensitiveMessage(message)) {
            return Result.DENY;
        }
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3) {
        if (isSensitiveMessage(message)) {
            return Result.DENY;
        }
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
        if (isSensitiveMessage(message)) {
            return Result.DENY;
        }
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
        if (isSensitiveMessage(message)) {
            return Result.DENY;
        }
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
        if (isSensitiveMessage(message)) {
            return Result.DENY;
        }
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {
        if (isSensitiveMessage(message)) {
            return Result.DENY;
        }
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {
        if (isSensitiveMessage(message)) {
            return Result.DENY;
        }
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {
        if (isSensitiveMessage(message)) {
            return Result.DENY;
        }
        return Result.NEUTRAL;
    }

    public final Result filter(Logger arg0, Level arg1, Marker arg2, Object message, Throwable arg4) {
        return isSensitiveMessage(message.toString()) ? Result.DENY : Result.NEUTRAL;
    }

    public final Result filter(Logger arg0, Level arg1, Marker arg2, Message message, Throwable arg4) {
        return message != null && isSensitiveMessage(message.getFormattedMessage()) ? Result.DENY : Result.NEUTRAL;
    }

    public final Result getOnMatch() {
        return Result.NEUTRAL;
    }

    public final Result getOnMismatch() {
        return Result.NEUTRAL;
    }

    @Override
    public State getState() {
        return null;
    }

    @Override
    public void initialize() {

    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public boolean isStarted() {
        return false;
    }

    @Override
    public boolean isStopped() {
        return false;
    }
}
