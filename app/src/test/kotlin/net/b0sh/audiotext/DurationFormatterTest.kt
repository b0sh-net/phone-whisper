package net.b0sh.audiotext

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * JVM tests for duration formatting in minutes and seconds (FR-011, FR-020).
 */
class DurationFormatterTest {

    @Test
    fun zero() {
        assertEquals("0:00", formatDuration(0))
    }

    @Test
    fun secondsArePadded() {
        assertEquals("0:07", formatDuration(7_000))
    }

    @Test
    fun oneMinuteTwoSeconds() {
        assertEquals("1:02", formatDuration(62_000))
    }

    @Test
    fun overOneHourShowsTotalMinutes() {
        assertEquals("62:05", formatDuration(3_725_000))
    }

    @Test
    fun roundsToNearestSecond() {
        assertEquals("1:01", formatDuration(61_400))
        assertEquals("1:02", formatDuration(61_500))
    }

    @Test
    fun negativeClampsToZero() {
        assertEquals("0:00", formatDuration(-5_000))
    }
}
