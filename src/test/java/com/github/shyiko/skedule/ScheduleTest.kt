package com.github.shyiko.skedule

import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class ScheduleTest {

    private fun iterateOverSchedule(schedule: String, timestamp: ZonedDateTime, limit: Int = 3) =
        Schedule.parse(schedule).iterate(timestamp).asSequence().take(limit)
            .map { it.format(DateTimeFormatter.ISO_ZONED_DATE_TIME) }.toList()

    @Test
    fun testEvery12Hours() {
        assertThat(
            iterateOverSchedule(
                "every 12 hours",
                ZonedDateTime.parse("2007-12-03T10:15:30+02:00[Europe/Kiev]")
            )
        ).isEqualTo(listOf(
            "2007-12-03T22:15:30+02:00[Europe/Kiev]",
            "2007-12-04T10:15:30+02:00[Europe/Kiev]",
            "2007-12-04T22:15:30+02:00[Europe/Kiev]"
        ))
    }

    @Test
    fun testEvery5MinutesFrom1000to1400() {
        assertThat(
            iterateOverSchedule(
                "every 5 minutes from 10:00 to 14:00",
                ZonedDateTime.parse("2007-12-03T10:15:30+02:00[Europe/Kiev]")
            )
        ).isEqualTo(listOf(
            "2007-12-03T10:20:00+02:00[Europe/Kiev]",
            "2007-12-03T10:25:00+02:00[Europe/Kiev]",
            "2007-12-03T10:30:00+02:00[Europe/Kiev]"
        ))
    }

    @Test
    fun testEvery5MinutesSynchronized() {
        assertThat(
            iterateOverSchedule(
                "every 5 minutes synchronized",
                ZonedDateTime.parse("2007-12-03T10:15:30+02:00[Europe/Kiev]")
            )
        ).isEqualTo(listOf(
            "2007-12-03T10:20:00+02:00[Europe/Kiev]",
            "2007-12-03T10:25:00+02:00[Europe/Kiev]",
            "2007-12-03T10:30:00+02:00[Europe/Kiev]"
        ))
    }

    @Test
    fun testEveryDay0000() {
        assertThat(
            iterateOverSchedule(
                "every day 00:00",
                ZonedDateTime.parse("2007-12-03T10:15:30+02:00[Europe/Kiev]")
            )
        ).isEqualTo(listOf(
            "2007-12-04T00:00:00+02:00[Europe/Kiev]",
            "2007-12-05T00:00:00+02:00[Europe/Kiev]",
            "2007-12-06T00:00:00+02:00[Europe/Kiev]"
        ))
    }

    @Test
    fun testEveryMonday0900() {
        assertThat(
            iterateOverSchedule(
                "every monday 09:00",
                ZonedDateTime.parse("2007-12-03T10:15:30+02:00[Europe/Kiev]")
            )
        ).isEqualTo(listOf(
            "2007-12-10T09:00:00+02:00[Europe/Kiev]",
            "2007-12-17T09:00:00+02:00[Europe/Kiev]",
            "2007-12-24T09:00:00+02:00[Europe/Kiev]"
        ))
    }

    @Test
    fun test2ndThirdMonWedThuOfMarch1700() {
        assertThat(
            iterateOverSchedule(
                "2nd,third mon,wed,thu of march 17:00",
                ZonedDateTime.parse("2007-12-03T10:15:30+02:00[Europe/Kiev]"),
                7
            )
        ).isEqualTo(listOf(
            "2008-03-10T17:00:00+02:00[Europe/Kiev]",
            "2008-03-12T17:00:00+02:00[Europe/Kiev]",
            "2008-03-13T17:00:00+02:00[Europe/Kiev]",
            "2008-03-17T17:00:00+02:00[Europe/Kiev]",
            "2008-03-19T17:00:00+02:00[Europe/Kiev]",
            "2008-03-20T17:00:00+02:00[Europe/Kiev]",
            "2009-03-09T17:00:00+02:00[Europe/Kiev]"
        ))
    }

    @Test
    fun test1stMondayOfSepOctNov1700() {
        assertThat(
            iterateOverSchedule(
                "1st monday of sep,oct,nov 17:00",
                ZonedDateTime.parse("2007-12-03T10:15:30+02:00[Europe/Kiev]"),
                4
            )
        ).isEqualTo(listOf(
            "2008-09-01T17:00:00+03:00[Europe/Kiev]",
            "2008-10-06T17:00:00+03:00[Europe/Kiev]",
            "2008-11-03T17:00:00+02:00[Europe/Kiev]",
            "2009-09-07T17:00:00+03:00[Europe/Kiev]"
        ))
    }

    @Test
    fun test1OfJanAprilJulyOct0000() {
        assertThat(
            iterateOverSchedule(
                "1 of jan,april,july,oct 00:00",
                ZonedDateTime.parse("2007-12-03T10:15:30+02:00[Europe/Kiev]"),
                5
            )
        ).isEqualTo(listOf(
            "2008-01-01T00:00:00+02:00[Europe/Kiev]",
            "2008-04-01T00:00:00+03:00[Europe/Kiev]",
            "2008-07-01T00:00:00+03:00[Europe/Kiev]",
            "2008-10-01T00:00:00+03:00[Europe/Kiev]",
            "2009-01-01T00:00:00+02:00[Europe/Kiev]"
        ))
    }

}
