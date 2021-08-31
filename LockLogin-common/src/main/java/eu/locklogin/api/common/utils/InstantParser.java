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
     * Get the time ago from the specified instant
     *
     * @param difference the instant
     * @return the time ago
     */
    public String getDifference(final Instant difference) {
        PluginProperties properties = new PluginProperties();

        ZonedDateTime instZT = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        ZonedDateTime diffZT = ZonedDateTime.ofInstant(difference, ZoneId.systemDefault());

        Period period = Period.between(instZT.toLocalDate(), diffZT.toLocalDate());

        long diffInMillis = GregorianCalendar.from(diffZT).getTime().getTime() - GregorianCalendar.from(instZT).getTime().getTime();

        List<TimeUnit> units = new ArrayList<>(EnumSet.allOf(TimeUnit.class));
        Collections.reverse(units);

        Map<TimeUnit, Long> result = new LinkedHashMap<>();
        long millisRest = diffInMillis;
        for ( TimeUnit unit : units ) {

            long diff = unit.convert(millisRest,TimeUnit.MILLISECONDS);
            long diffInMillisForUnit = unit.toMillis(diff);
            millisRest = millisRest - diffInMillisForUnit;

            result.put(unit,diff);
        }

        return Math.abs(period.getYears()) + " " + properties.getProperty("year", "year(s)") + ", " +
                Math.abs(period.getMonths()) + " " + properties.getProperty("month", "month(s)") + ", " +
                Math.abs(result.get(TimeUnit.DAYS).intValue()) + " " + properties.getProperty("day", "day(s)") + ", " +
                Math.abs(result.get(TimeUnit.HOURS).intValue()) + ":" + Math.abs(result.get(TimeUnit.MINUTES).intValue()) +
                " " + Math.abs(result.get(TimeUnit.SECONDS).intValue());
    }
}
