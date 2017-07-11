package com.github.shyiko.skedule.internal

import com.github.shyiko.skedule.InvalidScheduleException
import com.github.shyiko.skedule.Schedule
import java.lang.AssertionError
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalTime
import java.time.Month
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.EnumSet
import java.util.concurrent.TimeUnit

internal object ScheduleImpl {

    private val TIME_FORMAT = DateTimeFormatterBuilder()
        .appendValue(ChronoField.HOUR_OF_DAY, 2)
        .appendLiteral(':')
        .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
        .toFormatter()

    private val ORDINAL_SUFFIX = Regex("st|nd|rd|th")

    private val MONTH_ALL = EnumSet.allOf(Month::class.java)
    private val DAY_OF_WEEK_ALL = EnumSet.allOf(DayOfWeek::class.java)
    private val WEEK_RANGE = 1..5
    private val WEEK_ALL = WEEK_RANGE.toSet()
    private val DAY_RANGE = 1..31
    private val DAY_ALL = DAY_RANGE.toSet()

    // "every" N ("hours"|"mins"|"minutes") ["from" (time) "to" (time)]
    private val TIME = "\\d\\d:\\d\\d"
    private val EVERY_PREFIX = "every "
    private val EVERY = Regex("$EVERY_PREFIX(\\d+) (\\w+)( from ($TIME) to ($TIME)| synchronized)?")

    // ("every"|ordinal) (days) ["of" (monthspec)] (time)
    private val ORDINAL0 = "\\d\\d?(?:st|nd|rd|th)?|[a-z]+"
    private val ORDINAL = "(?:$ORDINAL0)(?:,(?:$ORDINAL0))*"
    private val DAY0 = "[a-z]+"
    private val DAY = "(?:$DAY0)(?:,(?:$DAY0))*"
    private val MONTH0 = "[a-z]+"
    private val MONTH = "(?:$MONTH0)(?:,(?:$MONTH0))*"
    private val SPECIFIC = Regex("(every|$ORDINAL)( $DAY)?(?: of ($MONTH))? ($TIME)")

    // alias for "every mon,tue,wed,thu,fri,sat,sun (time)"
    private val EVERY_DAY = Regex("every day ($TIME)")

    private val ORDINAL_MAP = listOf(
        "first", "second", "third", "fourth", "fifth", "sixth", "seventh", "eighth", "ninth",
        // [10..19]
        "tenth", "eleventh", "twelfth", "thirteenth", "fourteenth", "fifteenth", "sixteenth", "seventeenth",
        "eighteenth", "nineteenth",
        // [20..29]
        "twentieth", "twenty-first", "twenty-second", "twenty-third", "twenty-fourth", "twenty-fifth",
        "twenty-sixth", "twenty-seventh", "twenty-eighth", "twenty-ninth",
        // [30..31]
        "thirtieth", "thirty-first"
    ).mapIndexed { index, ordinal -> ordinal to index + 1 }.toMap()
    private val DAY_OF_WEEK_MAP = (
        // mon, ...
        DayOfWeek.values().map { it.name.toLowerCase().substring(0, 3) to it } +
            // monday, ...
            DayOfWeek.values().map { it.name.toLowerCase() to it }
        ).toMap()
    private val MONTH_MAP = (
        // mar, ...
        Month.values().map { it.name.toLowerCase().substring(0, 3) to it } +
            // march, ...
            Month.values().map { it.name.toLowerCase() to it }
        ).toMap()
    private val UNIT_MAP = mapOf("mins" to ChronoUnit.MINUTES, "minutes" to ChronoUnit.MINUTES,
        "hours" to ChronoUnit.HOURS)

    private val H24 = IntervalSchedule.TimeInterval(LocalTime.MIN, LocalTime.MAX)

    private fun parseNumber(number: String) =
        number.toLongOrNull() ?: throw InvalidScheduleException("\"$number\" is not a number")

    private fun parseUnit(unit: String) =
        UNIT_MAP[unit] ?:
            throw InvalidScheduleException("\"$unit\" isn't a valid unit (it must be either minutes(mins) or hours)")

    private fun parseTime(time: String) =
        try { LocalTime.parse(time, TIME_FORMAT) } catch (e: Exception) {
            throw InvalidScheduleException("\"$time\" isn't a valid time expression")
        }

    private fun parseOrdinal(ordinal: String) =
        ORDINAL_MAP[ordinal] ?: ordinal.replace(ORDINAL_SUFFIX, "").toIntOrNull()
            ?: throw InvalidScheduleException("\"$ordinal\" isn't ordinal")

    private fun parseDayOfWeek(weekDays: String) =
        DAY_OF_WEEK_MAP[weekDays] ?: throw InvalidScheduleException("\"$weekDays\" isn't a valid day-of-week")

    private fun parseMonth(month: String) =
        MONTH_MAP[month] ?: throw InvalidScheduleException("\"$month\" isn't a valid month")

    @JvmStatic
    fun parse(schedule: String): Schedule {
        val exp = schedule.replaceFirst("every hour", "every 1 hours")
            .replaceFirst("every minute", "every 1 minutes")
        return when {
            exp.startsWith("every day ") -> {
                val match = EVERY_DAY.matchEntire(exp) ?:
                    throw InvalidScheduleException("\"$exp\" isn't valid \"every day...\" expression")
                val (rawTime) = match.groupValues.tail()
                TimeSchedule(time = parseTime(rawTime))
            }
            exp.startsWith(EVERY_PREFIX) && exp.length > EVERY_PREFIX.length &&
                exp[EVERY_PREFIX.length].isDigit() -> {
                val match = EVERY.matchEntire(exp) ?:
                    throw InvalidScheduleException("\"$exp\" isn't valid \"every ...\" expression")
                val (rawLength, rawUnit, interval, start, end) = match.groupValues.tail()
                val length = parseNumber(rawLength)
                val unit = parseUnit(rawUnit)
                val spreadEvenlyOver24hPeriod = interval.trim() == "synchronized"
                if (spreadEvenlyOver24hPeriod &&
                    TimeUnit.DAYS.toMillis(1) % Duration.of(length, unit).toMillis() != 0L) {
                    throw InvalidScheduleException("interval($length/$unit) doesn't divide 24h (evenly)")
                }
                IntervalSchedule(interval = IntervalSchedule.Interval(length, unit,
                    when {
                        spreadEvenlyOver24hPeriod -> H24
                        start != "" && end != "" -> IntervalSchedule.TimeInterval(parseTime(start), parseTime(end))
                        else -> null
                    }
                ))
            }
            else -> {
                val match = SPECIFIC.matchEntire(exp) ?:
                    throw InvalidScheduleException("\"$exp\" isn't valid \"(every|ordinal) ...\" expression")
                val (rawOrdinal, day, month, rawTime) = match.groupValues.tail()
                TimeSchedule(
                    weekDayIndexesInAMonth = if (day == "" || rawOrdinal == "every") WEEK_ALL else
                        rawOrdinal.split(',').map { parseOrdinal(it) }.toSet(),
                    weekDays = if (day == "") DAY_OF_WEEK_ALL else
                        EnumSet.copyOf(day.trim().split(',').map { parseDayOfWeek(it) }),
                    days = if (day != "") DAY_ALL else
                        rawOrdinal.split(',').map { parseOrdinal(it) }.toSet(),
                    months = if (month == "") MONTH_ALL else
                        EnumSet.copyOf(month.split(',').map { parseMonth(it) }),
                    time = parseTime(rawTime)
                )
            }
        }
    }

    private fun <T>List<T>.tail() = this.subList(1, this.size)

    @JvmStatic
    fun from(start: LocalTime, end: LocalTime): Schedule.FromScheduleBuilder = IntervalScheduleBuilder(start, end)
    @JvmOverloads
    @JvmStatic
    fun every(length: Long, unit: ChronoUnit, sync: Boolean = false): Schedule =
        IntervalSchedule(interval = IntervalSchedule.Interval(length, unit, if (sync) H24 else null))

    private class IntervalScheduleBuilder internal constructor(
        private val start: LocalTime,
        private val end: LocalTime
    ) : Schedule.FromScheduleBuilder {
        override fun every(length: Long, unit: ChronoUnit): Schedule =
            IntervalSchedule(interval = IntervalSchedule.Interval(length, unit, IntervalSchedule.TimeInterval(
                start.truncatedTo(ChronoUnit.SECONDS), end.truncatedTo(ChronoUnit.SECONDS)
            )))
    }

    @JvmStatic
    fun at(time: LocalTime): Schedule.AtScheduleBuilder = TimeScheduleBuilder(time)

    private class TimeScheduleBuilder internal constructor(private val time: LocalTime) : Schedule.AtScheduleBuilder {
        override fun everyDay(): Schedule = TimeSchedule(time = time)
        override fun everyDay(months: Set<Month>): Schedule = TimeSchedule(time = time, months = months)
        override fun every(weekDays: Set<DayOfWeek>): Schedule = TimeSchedule(time = time, weekDays = weekDays)
        override fun every(weekDays: Set<DayOfWeek>, months: Set<Month>): Schedule =
            TimeSchedule(time = time, weekDays = weekDays, months = months)
        override fun nth(weeks: Set<Int>, weekDays: Set<DayOfWeek>): Schedule.NthScheduleBuilder =
            NthDayOfWeekScheduleBuilder(time, weeks, weekDays)
        override fun nth(days: Set<Int>): Schedule.NthScheduleBuilder = NthDayOfMonthScheduleBuilder(time, days)
    }
    private class NthDayOfWeekScheduleBuilder internal constructor(
        private val time: LocalTime,
        private val weeks: Set<Int>,
        private val weekDays: Set<DayOfWeek>
    ) : Schedule.NthScheduleBuilder {
        override fun of(months: Set<Month>): Schedule =
            TimeSchedule(time = time, weekDayIndexesInAMonth = weeks, weekDays = weekDays, months = months)
    }
    private class NthDayOfMonthScheduleBuilder internal constructor(
        private val time: LocalTime,
        private val days: Set<Int>
    ) : Schedule.NthScheduleBuilder {
        override fun of(months: Set<Month>): Schedule = TimeSchedule(time = time, days = days, months = months)
    }

    private abstract class AbstractSchedule : Schedule {

        abstract val lazyString: Lazy<String>

        override fun iterate(timestamp: ZonedDateTime): Schedule.ScheduleIterator<ZonedDateTime> {
            var cursor = timestamp
            return object : Schedule.ScheduleIterator<ZonedDateTime> {
                override fun hasNext(): Boolean = true
                override fun nextOrSame(): ZonedDateTime { cursor = nextOrSame(cursor); return cursor }
                override fun next(): ZonedDateTime { cursor = next(cursor); return cursor }
                override fun remove() = throw UnsupportedOperationException()
            }
        }

        override fun equals(other: Any?): Boolean = this === other ||
            (other?.javaClass == javaClass && lazyString.value == (other as AbstractSchedule).lazyString.value)
        override fun hashCode(): Int = lazyString.value.hashCode()

        override fun toString(): String = lazyString.value

    }

    // "every" N ...
    private class IntervalSchedule constructor(
        private val interval: Interval
    ) : AbstractSchedule() {

        data class TimeInterval(val start: LocalTime, val end: LocalTime) {
            init {
                validate(!start.isAfter(end), { "start($start) must be <= end($end)" })
            }
        }
        data class Interval(val length: Long, val unit: ChronoUnit, val time: TimeInterval? = null) {
            init {
                validate(length > 0, { "length($length) must be > 0" })
                validate(unit == ChronoUnit.MINUTES || unit == ChronoUnit.HOURS, {
                    "unit($unit) must be either in minutes or hours" })
            }
        }

        override val lazyString = lazy(LazyThreadSafetyMode.PUBLICATION, {
            val unit = when (interval.unit) {
                ChronoUnit.MINUTES -> "minutes"
                ChronoUnit.HOURS -> "hours"
                else -> throw AssertionError()
            }
            val intervalTime = interval.time
            "every ${interval.length} $unit" + if (intervalTime == null) "" else
                (if (intervalTime.start == LocalTime.MIN && intervalTime.end == LocalTime.MAX) " synchronized"
                else " from ${intervalTime.start.format(TIME_FORMAT)} to ${intervalTime.end.format(TIME_FORMAT)}")
        })

        override fun nextOrSame(timestamp: ZonedDateTime): ZonedDateTime = roll(timestamp, false)
        override fun next(timestamp: ZonedDateTime): ZonedDateTime = roll(timestamp)

        fun roll(timestamp: ZonedDateTime, force: Boolean = true): ZonedDateTime {
            val stepInSeconds = Duration.of(interval.length, interval.unit).toMillis() / 1000
            var cursor = timestamp
            val intervalTime = interval.time
            if (intervalTime != null) {
                val cursorTime = cursor.toLocalTime()
                val overflow = (cursorTime.toSecondOfDay() - intervalTime.start.toSecondOfDay()) % stepInSeconds
                if (cursorTime < intervalTime.start) {
                    cursor = cursor.withTime(intervalTime.start)
                } else
                if (cursorTime.plusSeconds(stepInSeconds - overflow) > intervalTime.end) {
                    cursor = cursor.withTime(intervalTime.start).plusDays(1)
                } else {
                    if (overflow != 0L) {
                        val time = cursor.minusSeconds(overflow).toLocalTime()
                        cursor = cursor.withTime(if (time.isAfter(intervalTime.start)) time else intervalTime.start)
                    }
                    if (overflow != 0L || force) {
                        cursor = cursor.plusSeconds(stepInSeconds)
                    }
                }
                cursor = cursor.truncatedTo(ChronoUnit.MINUTES)
            } else {
                if (force) {
                    cursor = cursor.plusSeconds(stepInSeconds)
                }
            }
            return cursor
        }

    }

    inline fun validate(valid: Boolean, msg: () -> String) {
        if (!valid) throw InvalidScheduleException(msg())
    }
    fun ZonedDateTime.withTime(value: LocalTime) = this.withHour(value.hour).withMinute(value.minute)

    // ("every"|ordinal) ...
    private class TimeSchedule constructor(
        private val weekDayIndexesInAMonth: Set<Int> = WEEK_ALL,
        private val weekDays: Set<DayOfWeek> = DAY_OF_WEEK_ALL,
        private val days: Set<Int> = DAY_ALL,
        private val months: Set<Month> = MONTH_ALL,
        private val time: LocalTime
    ) : AbstractSchedule() {

        init {
            validate(!weekDayIndexesInAMonth.isEmpty() && weekDayIndexesInAMonth.all { it in WEEK_RANGE },
                { "Invalid week(s) (${weekDayIndexesInAMonth.filter { it !in WEEK_RANGE }})" })
            validate(!weekDays.isEmpty(),
                { "At least one day-of-week is required" })
            validate(!days.isEmpty() && days.all { it in DAY_RANGE },
                { "Invalid day(s) (${days.filter { it !in DAY_RANGE }})" })
            validate(!months.isEmpty(),
                { "At least one month is required" })
        }

        override val lazyString = lazy(LazyThreadSafetyMode.PUBLICATION, {
            val time = time.format(TIME_FORMAT)
            when {
                days != DAY_ALL ->
                    "${days.sorted().joinToString(",")} of ${months.joinSorted()} $time"
                weekDayIndexesInAMonth == WEEK_ALL && weekDays == DAY_OF_WEEK_ALL ->
                    if (months != MONTH_ALL) "every day of ${months.joinSorted()} $time" else "every day $time"
                weekDayIndexesInAMonth == WEEK_ALL && months == MONTH_ALL ->
                    "every ${weekDays.joinSorted()} $time"
                else -> "${if (weekDayIndexesInAMonth == WEEK_ALL) "every" else weekDayIndexesInAMonth.joinToString(",")
                } ${weekDays.joinSorted()} of ${months.joinSorted()} $time"
            }
        })

        private fun <T: Enum<T>> Set<T>.joinSorted() =
            this.sorted().map { it.toString().toLowerCase() }.joinToString(",")

        private val ZonedDateTime.week: Int
            get() = (this.dayOfMonth + 6) / 7

        override fun nextOrSame(timestamp: ZonedDateTime): ZonedDateTime = roll(timestamp, false)
        override fun next(timestamp: ZonedDateTime): ZonedDateTime = roll(timestamp)

        fun roll(timestamp: ZonedDateTime, force: Boolean = true): ZonedDateTime {
            var cursor = timestamp.truncatedTo(ChronoUnit.MINUTES)
            if (cursor.toLocalTime() != time) {
                if (time < cursor.toLocalTime()) {
                    cursor = cursor.plusDays(1)
                }
                cursor = cursor.withTime(time)
            } else {
                if (force) {
                    cursor = cursor.plusDays(1)
                }
            }
            var i = 0
            do {
                val prev = cursor
                if (months != MONTH_ALL) {
                    val nextOfSameMonth = months.filter { it >= cursor.month }.min()
                        ?: months.min()!!
                    if (nextOfSameMonth < cursor.month) {
                        cursor = cursor.plusYears(1).withDayOfMonth(1)
                    }
                    cursor = cursor.withMonth(nextOfSameMonth.value)
                }
                if (days != DAY_ALL) {
                    val nextOrSameDay = days.filter { it >= cursor.dayOfMonth }.min()
                        ?: days.min()!!
                    if (nextOrSameDay < cursor.dayOfMonth) {
                        cursor = cursor.plusMonths(1)
                    }
                    cursor = cursor.withDayOfMonth(nextOrSameDay)
                }
                if (weekDays != DAY_OF_WEEK_ALL) {
                    val nextOrSameDayOfWeek = weekDays.filter { it >= cursor.dayOfWeek }.min()
                        ?: weekDays.min()!!
                    cursor = cursor.with(TemporalAdjusters.nextOrSame(nextOrSameDayOfWeek))
                }
                if (weekDayIndexesInAMonth != WEEK_ALL) {
                    val nextOrSameWeek = weekDayIndexesInAMonth.filter { it >= cursor.week }.min()
                        ?: weekDayIndexesInAMonth.min()!!
                    if (nextOrSameWeek < cursor.week) {
                        cursor = cursor.with(TemporalAdjusters.firstDayOfNextMonth())
                    } else {
                        cursor = cursor.plusWeeks((nextOrSameWeek - cursor.week).toLong())
                    }
                }
                if (i++ > 10) {
                    throw AssertionError("Timestamp should have converged by now. " +
                        "Please create a ticket at https://github.com/shyiko/skedule/issue " +
                        "(schedule expression: \"$this\")")
                }
            } while (prev != cursor)
            return cursor
        }

    }

}
