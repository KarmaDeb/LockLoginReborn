package eu.locklogin.api.common.utils;

/*
 * Private GSA code
 *
 * The use of this code
 * without GSA team authorization
 * will be a violation of
 * terms of use determined
 * in <a href="http://karmaconfigs.cf/license/"> here </a>
 * or (fallback domain) <a href="https://karmaconfigs.github.io/page/license"> here </a>
 */

import eu.locklogin.api.common.utils.plugin.Messages;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * LockLogin instant parser
 */
public final class InstantParser {

    private final Instant instant;

    /**
     * Initialize the instant parser
     *
     * @param date the instant date
     * @throws IllegalStateException if the instant date is null
     */
    public InstantParser(final Instant date) throws IllegalStateException {
        if (date == null)
            throw new IllegalStateException("Couldn't initialize instant parser from null instant");

        instant = date;
    }

    /**
     * Get the instant year
     *
     * @return the instant year
     */
    public final int getYear() {
        ZonedDateTime instantZone = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        Calendar calendar = GregorianCalendar.from(instantZone);

        return calendar.get(Calendar.YEAR);
    }

    /**
     * Get the instant month
     *
     * @return the instant month
     */
    public final String getMonth() {
        ZonedDateTime instantZone = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        Calendar calendar = GregorianCalendar.from(instantZone);

        return calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
    }

    /**
     * Get the instant day
     *
     * @return the instant day
     */
    public final int getDay() {
        ZonedDateTime instantZone = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        Calendar calendar = GregorianCalendar.from(instantZone);

        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * Get the instant hour
     *
     * @return the instant hour
     */
    public final int getHour() {
        ZonedDateTime instantZone = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        Calendar calendar = GregorianCalendar.from(instantZone);

        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    /**
     * Get the instant minute
     *
     * @return the instant minute
     */
    public final int getMinute() {
        ZonedDateTime instantZone = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        Calendar calendar = GregorianCalendar.from(instantZone);

        return calendar.get(Calendar.MINUTE);
    }

    /**
     * Get the instant second
     *
     * @return the instant second
     */
    public final int getSecond() {
        ZonedDateTime instantZone = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        Calendar calendar = GregorianCalendar.from(instantZone);

        return calendar.get(Calendar.SECOND);
    }

    /**
     * Parse the instant to display all the util date
     * info
     *
     * @return the parsed instant to string
     */
    public final String parse() {
        return getYear() + " / " + getMonth() + " / " + getDay() + " " + getHour() + ":" + getMinute() + ":" + getSecond();
    }

    /**
     * Get the time ago from the specified instant
     *
     * @param instance the instant
     * @return the time ago
     */
    public final String getDifference(final Instant instance) {
        Messages properties = new Messages();

        ZonedDateTime instantZone = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        Calendar calendar = GregorianCalendar.from(instantZone);

        ZonedDateTime instanceZone = ZonedDateTime.ofInstant(instance, ZoneId.systemDefault());
        Calendar instanceCalendar = GregorianCalendar.from(instanceZone);

        int creation_year = calendar.get(Calendar.YEAR);
        int creation_month = calendar.get(Calendar.MONTH);
        int creation_day = calendar.get(Calendar.DAY_OF_MONTH);
        int creation_hour = calendar.get(Calendar.HOUR_OF_DAY);
        int creation_minute = calendar.get(Calendar.MINUTE);
        int creation_second = calendar.get(Calendar.SECOND);

        int today_year = instanceCalendar.get(Calendar.YEAR);
        int today_month = instanceCalendar.get(Calendar.MONTH);
        int today_day = instanceCalendar.get(Calendar.DAY_OF_MONTH);
        int today_hour = instanceCalendar.get(Calendar.HOUR_OF_DAY);
        int today_minute = instanceCalendar.get(Calendar.MINUTE);
        int today_second = instanceCalendar.get(Calendar.SECOND);

        return Math.abs(today_year - creation_year) + " " + properties.getProperty("year", "year(s)") + ", " +
                Math.abs(today_month - creation_month) + " " + properties.getProperty("month", "month(s)") + ", " +
                Math.abs(today_day - creation_day) + " " + properties.getProperty("day", "day(s)") + " " +
        properties.getProperty("time_at", "at") + " " + Math.abs(today_hour - creation_hour) + ":" + Math.abs(today_minute - creation_minute) +
                " " + Math.abs(today_second - creation_second);
    }
}
