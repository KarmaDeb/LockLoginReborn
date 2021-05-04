package ml.karmaconfigs.locklogin.plugin.common.utils;

import ml.karmaconfigs.locklogin.plugin.common.utils.plugin.Messages;

import java.time.Instant;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

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
        String year = instant.toString().split("-")[0];

        return Integer.parseInt(year);
    }

    /**
     * Get the instant month
     *
     * @return the instant month
     */
    public final String getMonth() {
        String month_id = instant.toString().split("-")[1];
        int month_num = Integer.parseInt(month_id);

        switch (month_num) {
            case 0:
                return Month.JANUARY.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            case 1:
                return Month.FEBRUARY.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            case 2:
                return Month.MARCH.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            case 3:
                return Month.APRIL.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            case 4:
                return Month.MAY.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            case 5:
                return Month.JUNE.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            case 6:
                return Month.JULY.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            case 7:
                return Month.OCTOBER.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            case 8:
                return Month.AUGUST.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            case 9:
                return Month.SEPTEMBER.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            case 10:
                return Month.NOVEMBER.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            case 11:
            default:
                return Month.DECEMBER.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        }
    }

    /**
     * Get the instant day
     *
     * @return the instant day
     */
    public final int getDay() {
        String day = instant.toString().split("-")[2].split("T")[0];
        return Integer.parseInt(day);
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

        int today_year = instanceCalendar.get(Calendar.YEAR);
        int today_month = instanceCalendar.get(Calendar.MONTH);
        int today_day = instanceCalendar.get(Calendar.DAY_OF_MONTH);

        return Math.abs(today_year - creation_year) + " " + properties.getProperty("year", "year(s)") + ", " +
                Math.abs(today_month - creation_month) + " " + properties.getProperty("month", "month(s)") + ", " +
                Math.abs(today_day - creation_day) + " " + properties.getProperty("day", "day(s)");
    }
}
