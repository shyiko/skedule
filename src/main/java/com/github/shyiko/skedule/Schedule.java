package com.github.shyiko.skedule;

import com.github.shyiko.skedule.internal.ScheduleImpl;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A human-friendly alternative to cron.
 * Designed after GAE's <a href="https://cloud.google.com/appengine/docs/standard/java/config/cronref#schedule_format">schedule</a>.
 */
// (kotlin 1.1 does NOT support static methods on interfaces - hence a separate .java interface definition)
public interface Schedule extends Serializable {

    // method below is always overridden to avoid needless iterator construction,
    // the only reason it has default implementation is to make stubbing easier (for testing)
    default ZonedDateTime next(ZonedDateTime timestamp) { return iterate(timestamp).next(); }

    Iterator<ZonedDateTime> iterate(ZonedDateTime timestamp);

    /**
     * @param schedule schedule-as-a-string (e.g. "every monday 12:00")
     * @return schedule instance
     * @throws InvalidScheduleException in case of invalid schedule
     */
    static Schedule parse(String schedule) { return ScheduleImpl.parse(schedule); }

    /**
     * @param start start time (inclusive)
     * @param end end time (inclusive)
     * @return schedule builder
     * @see #every(long, ChronoUnit)
     */
    static FromScheduleBuilder from(LocalTime start, LocalTime end) { return ScheduleImpl.from(start, end); }
    /**
     * Alias for {@link #every(long, ChronoUnit, boolean)} with sync=false.
     * @param length interval duration
     * @param unit either {@link ChronoUnit#MINUTES} or {@link ChronoUnit#HOURS}
     * @return schedule instance
     * @throws InvalidScheduleException in case of invalid schedule
     */
    static Schedule every(long length, ChronoUnit unit) { return ScheduleImpl.every(length, unit); }
    /**
     * @param length interval duration
     * @param unit either {@link ChronoUnit#MINUTES} or {@link ChronoUnit#HOURS}
     * @param sync indicates whether schedule should be "synchronized" (spread evenly over 24h)
     * @return schedule instance
     * @throws InvalidScheduleException in case of invalid schedule
     */
    static Schedule every(long length, ChronoUnit unit, boolean sync) { return ScheduleImpl.every(length, unit, sync); }

    static AtScheduleBuilder at(LocalTime time) { return ScheduleImpl.at(time); }

    interface FromScheduleBuilder {
        /**
         * @param length interval duration
         * @param unit either {@link ChronoUnit#MINUTES} or {@link ChronoUnit#HOURS}
         * @return schedule instance
         * @throws InvalidScheduleException in case of invalid schedule
         */
        Schedule every(long length, ChronoUnit unit);
    }

    interface AtScheduleBuilder {
        /**
         * @return schedule instance
         * @throws InvalidScheduleException in case of invalid schedule
         */
        Schedule everyDay();

        /**
         * @param months e.g. EnumSet.of(Month.JANUARY)
         * @return schedule instance
         * @throws InvalidScheduleException in case of invalid schedule
         */
        Schedule everyDay(Set<Month> months);
        default Schedule everyDay(Month month, Month... months) { return everyDay(EnumSet.of(month, months)); }

        /**
         * @param weekDays e.g. EnumSet.of(DayOfWeek.MONDAY)
         * @return schedule instance
         * @throws InvalidScheduleException in case of invalid schedule
         */
        Schedule every(Set<DayOfWeek> weekDays);
        default Schedule every(DayOfWeek weekDay, DayOfWeek... weekDays) {
            return every(EnumSet.of(weekDay, weekDays));
        }

        /**
         * @param weekDays e.g. EnumSet.of(DayOfWeek.MONDAY)
         * @param months e.g. EnumSet.of(Month.JANUARY)
         * @return schedule instance
         * @throws InvalidScheduleException in case of invalid schedule
         */
        Schedule every(Set<DayOfWeek> weekDays, Set<Month> months);
        // thanks glob there are only 7 days in a week
        default Schedule every(DayOfWeek day1, Month month, Month... months) {
            return every(EnumSet.of(day1), EnumSet.of(month, months));
        }
        default Schedule every(DayOfWeek day1, DayOfWeek day2, Month month, Month... months) {
            return every(EnumSet.of(day1, day2), EnumSet.of(month, months));
        }
        default Schedule every(DayOfWeek day1, DayOfWeek day2, DayOfWeek day3, Month month, Month... months) {
            return every(EnumSet.of(day1, day2, day3), EnumSet.of(month, months));
        }
        default Schedule every(DayOfWeek day1, DayOfWeek day2, DayOfWeek day3, DayOfWeek day4,
            Month month, Month... months) {
            return every(EnumSet.of(day1, day2, day3, day4), EnumSet.of(month, months));
        }
        default Schedule every(DayOfWeek day1, DayOfWeek day2, DayOfWeek day3, DayOfWeek day4, DayOfWeek day5,
            Month month, Month... months) {
            return every(EnumSet.of(day1, day2, day3, day4, day5), EnumSet.of(month, months));
        }
        default Schedule every(DayOfWeek day1, DayOfWeek day2, DayOfWeek day3, DayOfWeek day4, DayOfWeek day5,
            DayOfWeek day6, Month month, Month... months) {
            return every(EnumSet.of(day1, day2, day3, day4, day5, day6), EnumSet.of(month, months));
        }
        default Schedule every(DayOfWeek day1, DayOfWeek day2, DayOfWeek day3, DayOfWeek day4, DayOfWeek day5,
            DayOfWeek day6, DayOfWeek day7, Month month, Month... months) {
            return every(EnumSet.of(day1, day2, day3, day4, day5, day6, day7), EnumSet.of(month, months));
        }

        /**
         * @param weekDayIndexesInAMonth 1..5
         * @param weekDays e.g. EnumSet.of(DayOfWeek.MONDAY)
         * @return schedule builder
         */
        NthScheduleBuilder nth(Set<Integer> weekDayIndexesInAMonth, Set<DayOfWeek> weekDays);
        // groovy experience is about to pay off
        default NthScheduleBuilder nth(int weekDayIndexInAMonth1, DayOfWeek weekDay, DayOfWeek... weekDays) {
            return nth($Set.of(weekDayIndexInAMonth1), EnumSet.of(weekDay, weekDays));
        }
        default NthScheduleBuilder nth(int weekDayIndexInAMonth1, int weekDayIndexInAMonth2,
            DayOfWeek weekDay, DayOfWeek... weekDays) {
            return nth($Set.of(weekDayIndexInAMonth1, weekDayIndexInAMonth2), EnumSet.of(weekDay, weekDays));
        }
        default NthScheduleBuilder nth(int weekDayIndexInAMonth1, int weekDayIndexInAMonth2, int weekDayIndexInAMonth3,
            DayOfWeek weekDay, DayOfWeek... weekDays) {
            return nth($Set.of(weekDayIndexInAMonth1, weekDayIndexInAMonth2, weekDayIndexInAMonth3),
                EnumSet.of(weekDay, weekDays));
        }
        default NthScheduleBuilder nth(int weekDayIndexInAMonth1, int weekDayIndexInAMonth2, int weekDayIndexInAMonth3,
            int weekDayIndexInAMonth4, DayOfWeek weekDay, DayOfWeek... weekDays) {
            return nth($Set.of(weekDayIndexInAMonth1, weekDayIndexInAMonth2, weekDayIndexInAMonth3,
                weekDayIndexInAMonth4), EnumSet.of(weekDay, weekDays));
        }
        default NthScheduleBuilder nth(int weekDayIndexInAMonth1, int weekDayIndexInAMonth2, int weekDayIndexInAMonth3,
            int weekDayIndexInAMonth4, int weekDayIndexInAMonth5, DayOfWeek weekDay, DayOfWeek... weekDays) {
            return nth($Set.of(weekDayIndexInAMonth1, weekDayIndexInAMonth2, weekDayIndexInAMonth3,
                weekDayIndexInAMonth4, weekDayIndexInAMonth5), EnumSet.of(weekDay, weekDays));
        }

        /**
         * @param days 1..31
         * @return schedule builder
         */
        NthScheduleBuilder nth(Set<Integer> days);
        default NthScheduleBuilder nth(int day, int... days) {
            return nth(IntStream.concat(IntStream.of(day), IntStream.of(days)).boxed()
                .collect(Collectors.toCollection(HashSet::new)));
        }
    }

    interface NthScheduleBuilder {
        /**
         * @param months e.g. EnumSet.of(Month.JANUARY)
         * @return schedule instance
         * @throws InvalidScheduleException in case of invalid schedule
         */
        Schedule of(Set<Month> months);
        default Schedule of(Month month, Month... months) { return of(EnumSet.of(month, months)); }
    }

}

// java 9 Set.of(...) backport
class $Set { static Set<Integer> of(Integer... values) { return new HashSet<>(Arrays.asList(values)); } }
