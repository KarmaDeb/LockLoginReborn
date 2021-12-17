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

import eu.locklogin.api.file.plugin.PluginProperties;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
import ml.karmaconfigs.api.common.utils.string.util.KarmaUnit;
import ml.karmaconfigs.api.common.utils.string.util.time.TimeName;

import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
    public int getYear() {
        ZonedDateTime instantZone = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        Calendar calendar = GregorianCalendar.from(instantZone);

        return calendar.get(Calendar.YEAR);
    }

    /**
     * Get the instant month
     *
     * @return the instant month
     */
    public String getMonth() {
        ZonedDateTime instantZone = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        Calendar calendar = GregorianCalendar.from(instantZone);

        return calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
    }

    /**
     * Get the instant day
     *
     * @return the instant day
     */
    public int getDay() {
        ZonedDateTime instantZone = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        Calendar calendar = GregorianCalendar.from(instantZone);

        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * Get the instant hour
     *
     * @return the instant hour
     */
    public int getHour() {
        ZonedDateTime instantZone = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        Calendar calendar = GregorianCalendar.from(instantZone);

        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    /**
     * Get the instant minute
     *
     * @return the instant minute
     */
    public int getMinute() {
        ZonedDateTime instantZone = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        Calendar calendar = GregorianCalendar.from(instantZone);

        return calendar.get(Calendar.MINUTE);
    }

    /**
     * Get the instant second
     *
     * @return the instant second
     */
    public int getSecond() {
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
    public String parse() {
        return getYear() + " / " + getMonth() + " / " + getDay() + " " + getHour() + ":" + getMinute() + ":" + getSecond();
    }

    /**
     * Get the time ago from today
     *
     * @return the time ago
     */
    public String getDifference() {
        PluginProperties properties = new PluginProperties();
        Instant today = Instant.now();

        long diff = (today.toEpochMilli() - instant.toEpochMilli());

        String year = properties.getProperty("diff_year", "year");
        String years = properties.getProperty("diff_years", "years");

        String month = properties.getProperty("diff_month", "month");
        String months = properties.getProperty("diff_months", "months");

        String week = properties.getProperty("diff_week", "week");
        String weeks = properties.getProperty("diff_weeks", "weeks");

        String day = properties.getProperty("diff_day", "day");
        String days = properties.getProperty("diff_days", "days");

        String hour = properties.getProperty("diff_hour", "hour");
        String hours = properties.getProperty("diff_hours", "hours");

        String minute = properties.getProperty("diff_minute", "minute");
        String minutes = properties.getProperty("diff_minutes", "minutes");

        String second = properties.getProperty("diff_second", "second");
        String seconds = properties.getProperty("diff_seconds", "seconds");

        String milli = properties.getProperty("diff_milli", "ms");
        String millis = properties.getProperty("diff_millis", "ms");

        return StringUtils.timeToString(diff, TimeName.create()
                .add(KarmaUnit.YEAR, year)
                .add(KarmaUnit.YEARS, years)

                .add(KarmaUnit.MONTH, month)
                .add(KarmaUnit.MONTHS, months)

                .add(KarmaUnit.WEEK, week)
                .add(KarmaUnit.WEEKS, weeks)

                .add(KarmaUnit.DAY, day)
                .add(KarmaUnit.DAYS, days)

                .add(KarmaUnit.HOUR, hour)
                .add(KarmaUnit.HOURS, hours)

                .add(KarmaUnit.MINUTE, minute)
                .add(KarmaUnit.MINUTES, minutes)

                .add(KarmaUnit.SECOND, second)
                .add(KarmaUnit.SECONDS, seconds)

                .add(KarmaUnit.MILLISECOND, milli)
                .add(KarmaUnit.MILLISECONDS, millis))
                .replace(", ", properties.getProperty("diff_spacer", ", ").replace("\"", ""))
                .replace(" and ", properties.getProperty("diff_final", " and "));
    }

    /**
     * Get the time ago from the specified instant
     *
     * @param difference the time to use as check
     * @return the time ago
     * @deprecated As argument was never being used, use {@link InstantParser#getDifference()} instead
     */
    @Deprecated
    public String getDifference(final Instant difference) {
        return getDifference();
    }
}
